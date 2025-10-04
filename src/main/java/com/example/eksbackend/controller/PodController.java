package com.example.eksbackend.controller;


import com.example.eksbackend.model.ClusterInfo;
import com.example.eksbackend.model.LogFalco;
import com.example.eksbackend.dto.LogWithAnalysis;
import com.example.eksbackend.service.PodService;
import com.fasterxml.jackson.databind.JsonNode;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@RestController
@RequestMapping("api")
@CrossOrigin(origins = "http://localhost:3001", allowCredentials = "true")
public class PodController {

    @Autowired
    PodService podService;

    @CrossOrigin(origins = "http://localhost:3001")
    @GetMapping("get-pods")
    public List<Pod> getPods() {
        try (DefaultKubernetesClient client = new DefaultKubernetesClient()) {
            List<Pod> pods = client.pods().inAnyNamespace().list().getItems();
            return pods;
        }
    }


    @CrossOrigin(origins = "http://localhost:3001")
    @GetMapping("/image-scan/{image_name}")
    public ResponseEntity<JsonNode> getTrivyResultByImageName(@PathVariable("image_name") String imageName) throws IOException, InterruptedException {

        return ResponseEntity.ok(podService.getTrivyResultByImageName(imageName));
    }

    private final List<SseEmitter> emitters = new CopyOnWriteArrayList<>();

    @CrossOrigin(origins = "http://localhost:3001")
    @GetMapping("k8s-images")
    public ResponseEntity<ClusterInfo> getK8sImages() throws IOException, InterruptedException {

        return ResponseEntity.ok(podService.getK8sImagesDetailed());
    }


    @CrossOrigin(origins = "http://localhost:3001")
    @GetMapping("/alert-filter")
    public ResponseEntity<Page<LogWithAnalysis>> getAlertPage(
            Pageable pageable,
            @RequestParam(required = false) String priority,
            @RequestParam(required = false) String rule,
            @RequestParam(required = false) String pod,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Date start,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Date end
    ) {
        Page<LogWithAnalysis> alerts = podService.getAlertByPage(pageable, priority, rule, pod, start, end);
        return ResponseEntity.ok(alerts);
    }

    @CrossOrigin(origins = "http://localhost:3001")
    @GetMapping("/falco-stream")
    public SseEmitter streamFalco() {
        SseEmitter emitter = new SseEmitter(0L); // 0 = infinite
        emitters.add(emitter);

        emitter.onCompletion(() -> emitters.remove(emitter));
        emitter.onTimeout(() -> emitters.remove(emitter));
        emitter.onError((e) -> emitters.remove(emitter));

        try {
            emitter.send(SseEmitter.event()
                    .name("connection")
                    .data("{\"message\":\"Connected to Falco stream\"}"));
        } catch (IOException e) {
            emitters.remove(emitter);
        }

        return emitter;
    }

    @CrossOrigin(origins = "http://localhost:3001")
    @PostMapping("/falco-alert")
    public ResponseEntity<LogFalco> receiveAlert(@RequestBody String payload) throws IOException {
        System.out.println("Falco Event: " + payload);

        String jsonPayload = payload;
        if (!payload.trim().startsWith("{")) {
            jsonPayload = "{\"alert\":\"" + payload.replace("\"", "\\\"") + "\"}";
        }

        LogFalco logFalco = podService.receiveAlert(jsonPayload);

        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event().name("falco-alert").data(logFalco));
            } catch (Exception e) {
                emitters.remove(emitter);
            }
        }

        return ResponseEntity.ok(logFalco);
    }


}

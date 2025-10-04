package com.example.eksbackend.service;


import com.example.eksbackend.config.OllamaClient;
import com.example.eksbackend.dto.LogWithAnalysis;
import com.example.eksbackend.model.*;
import com.example.eksbackend.repository.AiAnalyzeRepositroy;
import com.example.eksbackend.repository.LogsFalcoRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

@Service

public class PodService {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private LogsFalcoRepository falcoRepository;
    @Autowired
    OllamaClient ollamaClient;
    @Autowired
    private LogsFalcoRepository logsFalcoRepository;

    @Autowired
    private AiAnalyzeRepositroy aiAnalyzeRepositroy;

    public List<Pod> getPodList() {
        try (DefaultKubernetesClient client = new DefaultKubernetesClient()) {
            List<Pod> pods = client.pods().inAnyNamespace().list().getItems();
            return pods;
        }
    }

    public void aiLogAnalyze(LogFalco logFalco )
    {

        Map<String, Object> aiResponse = ollamaClient.analyze("mistral:latest", logFalco.getOutput());
        AiAnalyze aiAnalyze=new AiAnalyze();

        aiAnalyze.setAnalyze(aiResponse);
        aiAnalyze.setLogFalco(logFalco.getId());
        aiAnalyzeRepositroy.save(aiAnalyze);

        System.out.println(aiResponse);

    }

    @Cacheable(value = "image_scans", key = "#imageName")
    public JsonNode getTrivyResultByImageName(String imageName) throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder(
                "trivy",
                "image",
                "--quiet",
                "--skip-update",
                "-f", "json",
                imageName
        );
        pb.redirectErrorStream(true);
        Process process = pb.start();

        InputStream is = process.getInputStream();
        String jsonOutput = new String(is.readAllBytes(), StandardCharsets.UTF_8);


        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(jsonOutput);

        ArrayNode filteredResults = mapper.createArrayNode();

        for (JsonNode result : root.get("Results")) {
            ObjectNode filteredResult = mapper.createObjectNode();
            filteredResult.put("Target", result.get("Target").asText());

            ArrayNode filteredVulns = mapper.createArrayNode();
            if (result.has("Vulnerabilities")) {
                for (JsonNode vuln : result.get("Vulnerabilities")) {
                    ObjectNode v = mapper.createObjectNode();
                    v.put("VulnerabilityID", vuln.get("VulnerabilityID").asText());
                    v.put("PkgName", vuln.get("PkgName").asText());
                    v.put("InstalledVersion", vuln.get("InstalledVersion").asText());
                    v.put("FixedVersion", vuln.has("FixedVersion") ? vuln.get("FixedVersion").asText() : null);
                    v.put("Title", vuln.has("Title") ? vuln.get("Title").asText() : null);

                    v.put("Severity", vuln.has("Severity") ? vuln.get("Severity").asText() : null);
                    filteredVulns.add(v);
                }
            }

            filteredResult.set("Vulnerabilities", filteredVulns);
            filteredResults.add(filteredResult);
        }

        ObjectNode finalResult = mapper.createObjectNode();

        finalResult.set("Results", filteredResults);

        return finalResult;
    }

    public Page<LogWithAnalysis> getAlertByPage(
            Pageable pageable,
            String priority,
            String rule,
            String pod,
            Date start,
            Date end
    ) {
        // Logları sayfalı getir
        Page<LogFalco> logs = falcoRepository.findAlertsWithFilters(priority, rule, pod, start, end, pageable);

        // Her log için AI analizini al ve LogWithAnalysis oluştur
        List<LogWithAnalysis> content = logs.stream().map(log -> {
            AiAnalyze analysis = aiAnalyzeRepositroy.findByLogFalcoId(log.getId()); // null dönebilir
            return new LogWithAnalysis(log, analysis); // analysis null olabilir
        }).toList();

        // Yeni Page<LogWithAnalysis> oluştur
        return new PageImpl<>(content, pageable, logs.getTotalElements());
    }




    public LogFalco receiveAlert(String payload) throws IOException {
        JsonNode root = objectMapper.readTree(payload);

        LogFalco event = new LogFalco();
        event.setHostname(root.path("hostname").asText());
        event.setOutput(root.path("output").asText());
        event.setPriority(root.path("priority").asText());
        event.setRule(root.path("rule").asText());
        event.setSource(root.path("source").asText());

        // Time parsing
        String timeStr = root.path("time").asText();
        Date parsedDate = null;
        try {
            // ISO 8601 format
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSX");
            sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
            parsedDate = sdf.parse(timeStr);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        event.setTime(parsedDate);

        // Tags
        List<String> tags = new ArrayList<>();
        if (root.has("tags") && root.get("tags").isArray()) {
            for (JsonNode tag : root.get("tags")) {
                tags.add(tag.asText());
            }
        }
        event.setTags(tags);

        // Output Fields: normalize dots
        Map<String, String> outputFields = new HashMap<>();
        if (root.has("output_fields")) {
            JsonNode fields = root.get("output_fields");
            fields.fieldNames().forEachRemaining(key -> {
                String normalizedKey = key.replace(".", "_");
                outputFields.put(normalizedKey, fields.get(key).asText(""));
            });
        }
        event.setOutputFields(outputFields);

        // Save to Mongo
        falcoRepository.save(event);
       aiLogAnalyze(event);


        return event;
    }



    public ClusterInfo getK8sImagesDetailed() throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder("kubectl", "get", "pods", "-A", "-o", "json");
        pb.redirectErrorStream(true);
        Process process = pb.start();

        InputStream is = process.getInputStream();
        String jsonOutput = new String(is.readAllBytes(), StandardCharsets.UTF_8);

        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(jsonOutput);

        ClusterInfo cluster = new ClusterInfo("my-cluster"); // opsiyonel cluster adı

        if (root.has("items")) {
            Map<String, NameSpaceInfo> namespaceMap = new HashMap<>();

            for (JsonNode pod : root.get("items")) {
                String namespaceName = pod.path("metadata").path("namespace").asText();
                String podName = pod.path("metadata").path("name").asText();
                String podStatus = pod.path("status").path("phase").asText();
                String nodeName = pod.path("spec").path("nodeName").asText();
                String creationTimestamp = pod.path("metadata").path("creationTimestamp").asText();
                JsonNode labelsNode = pod.path("metadata").path("labels");
                Map<String, String> labels = mapper.convertValue(labelsNode, Map.class);
                JsonNode annotationsNode = pod.path("metadata").path("annotations");
                Map<String, String> annotations = mapper.convertValue(annotationsNode, Map.class);

                int restarts = 0;
                JsonNode containerStatuses = pod.path("status").path("containerStatuses");
                if (containerStatuses.isArray()) {
                    for (JsonNode status : containerStatuses) {
                        restarts += status.path("restartCount").asInt();
                    }
                }

                NameSpaceInfo nsInfo = namespaceMap.getOrDefault(namespaceName, new NameSpaceInfo(namespaceName));
                JsonNode containers = pod.path("spec").path("containers");
                List<ContainerInfo> containerInfos = new ArrayList<>();

                if (containers.isArray()) {
                    for (JsonNode container : containers) {
                        String containerName = container.path("name").asText();
                        String image = container.path("image").asText();

                        // Ports
                        List<Integer> ports = new ArrayList<>();
                        JsonNode portsNode = container.path("ports");
                        if (portsNode.isArray()) {
                            for (JsonNode port : portsNode) {
                                ports.add(port.path("containerPort").asInt());
                            }
                        }

                        // Env variables
                        Map<String, String> env = new HashMap<>();
                        JsonNode envNode = container.path("env");
                        if (envNode.isArray()) {
                            for (JsonNode e : envNode) {
                                env.put(e.path("name").asText(), e.path("value").asText(""));
                            }
                        }

                        // Resources
                        Map<String, String> resources = new HashMap<>();
                        JsonNode resNode = container.path("resources").path("limits");
                        if (resNode.isObject()) {
                            resNode.fields().forEachRemaining(entry -> resources.put(entry.getKey(), entry.getValue().asText()));
                        }

                        boolean readiness = false;
                        if (containerStatuses.isArray()) {
                            for (JsonNode status : containerStatuses) {
                                if (status.path("name").asText().equals(containerName)) {
                                    readiness = status.path("ready").asBoolean(false);
                                }
                            }
                        }

                        containerInfos.add(new ContainerInfo(containerName, image, ports, env, resources, readiness, "pending"));
                    }
                }

                nsInfo.addPod(new PodInfo(podName, podStatus, nodeName, creationTimestamp, labels, annotations, restarts, containerInfos));
                namespaceMap.put(namespaceName, nsInfo);
            }

            cluster.setNamespaces(new ArrayList<>(namespaceMap.values()));
        }

        return cluster;
    }

}

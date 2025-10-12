package com.example.eksbackend.service;


import com.example.eksbackend.config.OllamaClient;
import com.example.eksbackend.model.LogFalco;
import com.example.eksbackend.repository.LogsFalcoRepository;
import com.example.eksbackend.utilities.ConvertLog;
import lombok.RequiredArgsConstructor;
import lombok.extern.java.Log;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Log4j2
public class KafkaProducer {

    private final LogsFalcoRepository falcoRepository;

    private final PodService podService;

    private final SimpMessagingTemplate messagingTemplate;


    @KafkaListener(topics = "falco-events", groupId = "falco-group")
    public void listen(String eventJson) {


        try {
            LogFalco log = ConvertLog.mapTopEntityLog(eventJson);

            falcoRepository.save(log);
            podService.aiLogAnalyze(log);

            messagingTemplate.convertAndSend("/topic/falco", log);

        } catch (Exception e) {

            e.printStackTrace();
        }

    }

}

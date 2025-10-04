package com.example.eksbackend.config;


import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Component
public class OllamaClient {


    private final String ollamaUrl = "http://localhost:11434/api/generate"; // Ollama API endpoint
    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * Tekli prompt gönderir ve Ollama'dan JSON cevabı alır
     */
    public Map<String, Object> analyze(String model, String logMessage) {
        try {
            // Prompt şablonu
            String promptTemplate = "Bir güvenlik asistanısın. Aşağıdaki Falco uyarısını analiz et ve SADECE JSON olarak cevap ver. Format şu şekilde olmalı:\n" +
                    "{\n" +
                    "  \"severity\": \"CRITICAL|HIGH|LOW\",\n" +
                    "  \"cause\": \"15 kelimeyi geçmeyecek kısa açıklama\",\n" +
                    "  \"recommended_action\": \"15 kelimeyi geçmeyecek öneri\",\n" +
                    "  \"confidence\": 0.0\n" +
                    "}\n" +
                    "Kurallar:\n" +
                    "1) Yanıt sadece JSON olmalı, ekstra metin yok.\n" +
                    "2) severity CRITICAL, HIGH veya LOW olmalı.\n" +
                    "3) Emin değilsen severity LOW, confidence 0.2 olmalı.\n\n" +
                    "Örnek:\n" +
                    "Input: \"User root executed /bin/sh in pod/nginx-abc (container nginx) from 10.0.0.5\"\n" +
                    "Output: {\"severity\":\"CRITICAL\",\"cause\":\"Yetkisiz container shell çalıştırması\",\"recommended_action\":\"Pod'u inceleyin ve root erişimini kaldırın\",\"confidence\":0.95}\n\n" +
                    "Şimdi bu uyarıyı analiz et ve sadece JSON olarak yanıt ver:\n\"" + logMessage + "\"";

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", model);               // örn. "mistral:latest"
            requestBody.put("prompt", promptTemplate);
            requestBody.put("format", "json");
            requestBody.put("stream", false);
            requestBody.put("temperature", 0.0);
            requestBody.put("top_p", 0.3);
            requestBody.put("max_tokens", 128);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

            ResponseEntity<Map> response = restTemplate.postForEntity(ollamaUrl, request, Map.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Object rawResponse = response.getBody().get("response"); // JSON string
                if (rawResponse instanceof String) {
                    // JSON string'i Map olarak parse et
                    return new ObjectMapper().readValue((String) rawResponse, Map.class);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return Map.of("error", "AI analysis failed");
    }


}

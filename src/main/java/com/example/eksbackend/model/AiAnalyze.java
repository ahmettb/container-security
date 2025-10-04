package com.example.eksbackend.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Map;


@Document
public class AiAnalyze {

    @Id
    private String id;

    public String getId() {
        return id;
    }

    public String getLogFalco() {
        return logFalcoId;
    }

    public void setLogFalco(String logFalcoId) {
        this.logFalcoId = logFalcoId;
    }

    public Map<String, Object> getAnalyze() {
        return analyze;
    }

    public void setAnalyze(Map<String, Object> analyze) {
        this.analyze = analyze;
    }

    public void setId(String id) {
        this.id = id;
    }

    private String logFalcoId;

    private Map<String, Object> analyze;
}

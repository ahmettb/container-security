package com.example.eksbackend.dto;

import com.example.eksbackend.model.AiAnalyze;
import com.example.eksbackend.model.LogFalco;

public class LogWithAnalysis {


    private LogFalco log;

    public LogFalco getLog() {
        return log;
    }

    public LogWithAnalysis(LogFalco log, AiAnalyze aiAnalyze) {
        this.log = log;
        this.aiAnalyze = aiAnalyze;
    }

    public void setLog(LogFalco log) {
        this.log = log;
    }

    public AiAnalyze getAiAnalyze() {
        return aiAnalyze;
    }

    public void setAiAnalyze(AiAnalyze aiAnalyze) {
        this.aiAnalyze = aiAnalyze;
    }

    private AiAnalyze aiAnalyze;
}

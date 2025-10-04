package com.example.eksbackend.repository;

import com.example.eksbackend.model.AiAnalyze;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface AiAnalyzeRepositroy extends MongoRepository <AiAnalyze,String >{

    AiAnalyze findByLogFalcoId(String logFalcoId);
}

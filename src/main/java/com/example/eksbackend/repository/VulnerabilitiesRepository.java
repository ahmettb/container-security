package com.example.eksbackend.repository;

import com.example.eksbackend.model.LogFalco;
import com.example.eksbackend.model.Vulnerabilities;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface VulnerabilitiesRepository extends MongoRepository<Vulnerabilities, String> {
}

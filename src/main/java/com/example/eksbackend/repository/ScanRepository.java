package com.example.eksbackend.repository;

import com.example.eksbackend.model.Scan;
import org.apache.kafka.common.protocol.types.Field;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface ScanRepository extends MongoRepository<Scan, String> {

    Optional<Scan> findByImageDigest(String digest);


}

package com.example.eksbackend.repository;

import com.example.eksbackend.model.LogFalco;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Date;

public interface LogsFalcoRepository extends MongoRepository<LogFalco, String> , LogFalcoRepositoryCustom {

    Page<LogFalco> findAllByTimeBetween(Pageable pageable, Date start, Date end);


    Page<LogFalco> findAllByPriorityAndRule(String priority, Pageable pageable);

}

package com.example.eksbackend.repository;

import com.example.eksbackend.model.LogFalco;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Date;

public interface LogFalcoRepositoryCustom {
    Page<LogFalco> findAlertsWithFilters(String priority, String rule, String pod, Date start, Date end, Pageable pageable);

}


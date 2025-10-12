package com.example.eksbackend.repository;

import com.example.eksbackend.model.LogFalco;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;


import java.util.Date;

@Repository
public class LogFalcoRepositoryCustomImpl implements LogFalcoRepositoryCustom {


    private final MongoTemplate mongoTemplate;

    public LogFalcoRepositoryCustomImpl(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;


    }


    @Override
    public Page<LogFalco> findAlertsWithFilters(String priority, String rule, String pod, Date start, Date end, Pageable pageable) {
        Query query = new Query();

        if (priority != null && !priority.isEmpty()) {
            query.addCriteria(Criteria.where("priority").is(priority));
        }

        if (rule != null && !rule.isEmpty()) {
            query.addCriteria(Criteria.where("rule").is(rule));
        }

        if (start != null && end != null) {
            query.addCriteria(Criteria.where("time").gte(start).lte(end));
        } else if (start != null) {
            query.addCriteria(Criteria.where("time").gte(start));
        } else if (end != null) {
            query.addCriteria(Criteria.where("time").lte(end));
        }

        long count = mongoTemplate.count(query, LogFalco.class);

        // Pageable
        if (pageable != null) {
            query.with(pageable);
        }

        var results = mongoTemplate.find(query, LogFalco.class);

        return new PageImpl<>(results, pageable, count);
    }
}

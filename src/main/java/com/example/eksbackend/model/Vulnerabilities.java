package com.example.eksbackend.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Document
public class Vulnerabilities {
    @Id
    private String id;

    private Scan scan;

    private String cveId;

    private String severity;

    private  String title;

    private String packageName;

    private String installedV;

    private String fixedV;

}

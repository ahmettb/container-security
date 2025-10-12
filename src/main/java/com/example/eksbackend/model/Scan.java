package com.example.eksbackend.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;
import java.util.List;

@Data
@Document
public class Scan {


    @Id
    private String id;

    private String imageDigest;

    private List<String> imageTag;

    private Date scanDate;

    private List<Vulnerabilities> vulnerabilitiesList;


}

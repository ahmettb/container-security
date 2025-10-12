package com.example.eksbackend.dto;

import lombok.Data;
import org.springframework.data.annotation.Id;

import java.util.Date;
import java.util.List;


@Data
public class ScanDto {

    private String imageDigest;

    private List<String> imageTag;

    private Date scanDate;
}

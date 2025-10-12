package com.example.eksbackend.dto;

import com.example.eksbackend.model.Scan;
import lombok.Data;
import org.springframework.data.annotation.Id;

@Data
public class VulnerabilitiesDto {



    private String cveId;

    private String severity;

    private  String title;

    private String packageName;

    private String installedV;

    private String fixedV;

}

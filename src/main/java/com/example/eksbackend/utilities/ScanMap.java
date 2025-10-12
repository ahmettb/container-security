package com.example.eksbackend.utilities;

import com.example.eksbackend.dto.ScanDto;
import com.example.eksbackend.dto.VulnerabilitiesDto;
import com.example.eksbackend.model.Scan;
import com.example.eksbackend.model.Vulnerabilities;

import java.util.ArrayList;
import java.util.List;

public class ScanMap {
    public static ScanDto toDto(Scan scan) {

        ScanDto scanDto = new ScanDto();
        scanDto.setScanDate(scan.getScanDate());
        scanDto.setImageDigest(scan.getImageDigest());
        scanDto.setImageTag(scan.getImageTag());

        return scanDto;

    }

    public static List<VulnerabilitiesDto> toVulDtoList(List<Vulnerabilities> vulnerabilities) {

        List<VulnerabilitiesDto> vulnerabilitiesDtoList = new ArrayList<>();

        for (Vulnerabilities vulnerability : vulnerabilities) {
            VulnerabilitiesDto vulnerabilitiesDto = toVulDto(vulnerability);
            vulnerabilitiesDtoList.add(vulnerabilitiesDto);
        }

        return vulnerabilitiesDtoList;

    }

    public static VulnerabilitiesDto toVulDto(Vulnerabilities vulnerabilities) {

        VulnerabilitiesDto vulnerabilitiesDto = new VulnerabilitiesDto();

        vulnerabilitiesDto.setTitle(vulnerabilities.getTitle());
        vulnerabilitiesDto.setSeverity(vulnerabilities.getSeverity());
        vulnerabilitiesDto.setFixedV(String.valueOf(vulnerabilities.getFixedV()));
        vulnerabilitiesDto.setInstalledV(vulnerabilities.getInstalledV());
        vulnerabilitiesDto.setCveId(vulnerabilities.getCveId());


        return vulnerabilitiesDto;

    }
}

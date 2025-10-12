package com.example.eksbackend.service;


import com.example.eksbackend.dto.VulnerabilitiesDto;
import com.example.eksbackend.model.ActiveImage;
import com.example.eksbackend.model.Scan;
import com.example.eksbackend.model.Vulnerabilities;
import com.example.eksbackend.repository.ScanRepository;
import com.example.eksbackend.repository.VulnerabilitiesRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Log4j2
public class ImageScanWorker {

    private final ScanRepository scanRepository;

    private final PodService podService;
    private final VulnerabilitiesRepository vulnerabilitiesRepository;

    @KafkaListener(
            topics = "image-scan-jobs",
            groupId = "trivy-scanner-group",
            containerFactory = "imageScanListenerContainerFactory"
    )
    public void consumeScanJob(ActiveImage activeImage) {
        log.info("Received scan job for image digest: {}", activeImage.getDigest());

        Optional<Scan> existingScan = scanRepository.findByImageDigest(activeImage.getDigest());

        if (existingScan.isPresent()) {
            log.info("Image digest {} has already been scanned. Skipping job.", activeImage.getDigest());
            return;
        }

        try {
            log.info("New image digest {}. Proceeding with scan and save process.", activeImage.getDigest());
            scanAndSaveNewImage(activeImage);
        } catch (IOException | InterruptedException e) {
            log.error("Failed to process scan job for image digest: {}", activeImage.getDigest(), e);
        }
    }


    @Transactional
    public void scanAndSaveNewImage(ActiveImage activeImage) throws IOException, InterruptedException {
        log.info("Starting Trivy scan for image digest: {}", activeImage.getDigest());

        List<VulnerabilitiesDto> vulnerabilitiesDtoList = podService.getTrivyResultByImageName(activeImage);

        log.info("Trivy scan completed for digest {}. Found {} vulnerabilities.", activeImage.getDigest(), vulnerabilitiesDtoList.size());

        Scan scan = new Scan();
        scan.setImageDigest(activeImage.getDigest());
        scan.setImageTag(activeImage.getTags());
        scan.setScanDate(new Date());

        List<Vulnerabilities> vulnerabilitiesList = new ArrayList<>();
        for (VulnerabilitiesDto dto : vulnerabilitiesDtoList) {
            Vulnerabilities vulnerability = new Vulnerabilities();
            vulnerability.setScan(scan);
            vulnerability.setTitle(dto.getTitle());
            vulnerability.setPackageName(dto.getPackageName());
            vulnerability.setInstalledV(dto.getInstalledV());
            vulnerability.setFixedV(dto.getFixedV());
            vulnerabilitiesList.add(vulnerability);
        }

        vulnerabilitiesRepository.saveAll(vulnerabilitiesList);

        scan.setVulnerabilitiesList(vulnerabilitiesList);

        log.info("Saving scan results to the database for digest: {}", activeImage.getDigest());

        scanRepository.save(scan);

        log.info("Successfully saved scan results for digest: {}", activeImage.getDigest());
    }

}

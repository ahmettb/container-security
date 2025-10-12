package com.example.eksbackend.service;


import com.example.eksbackend.config.KubernetesCommandConfig;
import com.example.eksbackend.config.OllamaClient;
import com.example.eksbackend.dto.ImageScanResponseDto;
import com.example.eksbackend.dto.LogWithAnalysis;
import com.example.eksbackend.dto.VulnerabilitiesDto;
import com.example.eksbackend.model.*;
import com.example.eksbackend.repository.AiAnalyzeRepositroy;
import com.example.eksbackend.repository.LogsFalcoRepository;
import com.example.eksbackend.repository.ScanRepository;
import com.example.eksbackend.repository.VulnerabilitiesRepository;
import com.example.eksbackend.utilities.ScanMap;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.Configuration;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.V1ContainerStatus;
import io.kubernetes.client.openapi.models.V1Pod;
import io.kubernetes.client.openapi.models.V1PodList;
import io.kubernetes.client.util.ClientBuilder;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;


@Service
@Log4j2
public class PodService {

    private final ObjectMapper objectMapper = new ObjectMapper();

    private final KafkaTemplate<String, ActiveImage> imageScanKafkaTemplate;

    private static final String KAFKA_TOPIC = "image-scan-jobs";

    public PodService(KubernetesCommandConfig kubernetesCommandConfig, AiAnalyzeRepositroy aiAnalyzeRepository, ScanRepository scanRepository, KafkaService kafkaService, LogsFalcoRepository logsFalcoRepository, OllamaClient ollamaClient, LogsFalcoRepository falcoRepository, @Qualifier("imageScanKafkaTemplate") KafkaTemplate<String, ActiveImage> imageScanKafkaTemplate) {
        this.scanRepository = scanRepository;
        this.aiAnalyzeRepository = aiAnalyzeRepository;
        this.kafkaService = kafkaService;
        this.logsFalcoRepository = logsFalcoRepository;
        this.ollamaClient = ollamaClient;
        this.falcoRepository = falcoRepository;
        this.imageScanKafkaTemplate = imageScanKafkaTemplate;
        this.kubernetesCommandConfig = kubernetesCommandConfig;
    }

    private final LogsFalcoRepository falcoRepository;

    private final OllamaClient ollamaClient;
    private final LogsFalcoRepository logsFalcoRepository;

    private final KafkaService kafkaService;

    private final ScanRepository scanRepository;

    private final AiAnalyzeRepositroy aiAnalyzeRepository;

    private final KubernetesCommandConfig kubernetesCommandConfig;

    public List<Pod> getPodList() {
        List<Pod> pods = kubernetesCommandConfig.defaultKubernetesClient().pods().inAnyNamespace().list().getItems();
        return pods;

    }

    @Async
    public void aiLogAnalyze(LogFalco logFalco) {

        try {
            Map<String, Object> aiResponse = ollamaClient.analyze(logFalco.getOutput());
            AiAnalyze aiAnalyze = new AiAnalyze();
            aiAnalyze.setAnalyze(aiResponse);
            aiAnalyze.setLogFalco(logFalco.getId());
            aiAnalyzeRepository.save(aiAnalyze);

            log.info("AI analyze result: {}", aiResponse);
        } catch (Exception e) {
            log.error("Error in AI analysis", e);
        }

    }


    @Transactional
    public List<ImageScanResponseDto> scanImageBatch() throws IOException, ApiException, InterruptedException {


        List<ImageScanResponseDto> imageScanResponseDtoList = new ArrayList<>();
        ApiClient client = ClientBuilder.defaultClient();
        Configuration.setDefaultApiClient(client);

        CoreV1Api api = new CoreV1Api();
        Map<String, Set<String>> imageMap = new HashMap<>();

        V1PodList podList = api.listPodForAllNamespaces(null, null, "status.phase=Running", null, null, null, null, null, null, null, null);

        for (V1Pod pod : podList.getItems()) {
            if (pod.getStatus() == null || pod.getStatus().getContainerStatuses() == null) {
                continue;
            }
            for (V1ContainerStatus status : pod.getStatus().getContainerStatuses()) {
                String imageTag = status.getImage();
                String imageDigestWithPrefix = status.getImageID();

                if (imageDigestWithPrefix != null && imageDigestWithPrefix.contains("sha256:")) {
                    String digest = "sha256:" + imageDigestWithPrefix.split("@sha256:")[1];

                    imageMap.putIfAbsent(digest, new HashSet<>());
                    imageMap.get(digest).add(imageTag);
                }
            }
        }

        List<ActiveImage> result = new ArrayList<>();
        for (Map.Entry<String, Set<String>> entry : imageMap.entrySet()) {
            ActiveImage activeImage = new ActiveImage();
            activeImage.digest = entry.getKey();
            activeImage.tags = new ArrayList<>(entry.getValue());
            result.add(activeImage);
        }


        List<Vulnerabilities> vulnerabilitiesEntityList = new ArrayList<>();
        for (ActiveImage activeImage : result) {

            ImageScanResponseDto imageScanResponseDto = new ImageScanResponseDto();


            if (scanRepository.findByImageDigest(activeImage.getDigest()).isPresent()) {


                Scan scan = scanRepository.findByImageDigest(activeImage.getDigest()).get();
                imageScanResponseDto.setScanDto(ScanMap.toDto(scan));
                imageScanResponseDto.setVulnerabilitiesDtoList(ScanMap.toVulDtoList(scan.getVulnerabilitiesList()));
            } else {

                String fullTag = activeImage.getTags().getFirst();
                String imageName;

                int colonIndex = fullTag.indexOf(':');

                if (colonIndex != -1) {
                    imageName = fullTag.substring(0, colonIndex);
                } else {
                    imageName = fullTag;
                }

                List<VulnerabilitiesDto> vulnerabilitiesList = getTrivyResultByImageName(null);

                Scan scan = new Scan();


                scan.setImageDigest(activeImage.getDigest());
                scan.setImageTag(activeImage.getTags());
                scan.setScanDate(new Date());

                for (VulnerabilitiesDto vulnerabilities : vulnerabilitiesList) {

                    Vulnerabilities vulnerabilities1 = new Vulnerabilities();
                    vulnerabilities1.setScan(scan);
                    vulnerabilities1.setTitle(vulnerabilities.getTitle());
                    vulnerabilities1.setPackageName(vulnerabilities.getPackageName());
                    vulnerabilities1.setInstalledV(vulnerabilities.getInstalledV());
                    vulnerabilities1.setFixedV(vulnerabilities.getFixedV());

                    vulnerabilitiesEntityList.add(vulnerabilities1);

                    scan.getVulnerabilitiesList().add(vulnerabilities1);

                }
                scanRepository.save(scan);

                imageScanResponseDto.setScanDto(ScanMap.toDto(scan));
                imageScanResponseDto.setVulnerabilitiesDtoList(ScanMap.toVulDtoList(scan.getVulnerabilitiesList() == null ? null : (scan.getVulnerabilitiesList())));
            }
            imageScanResponseDtoList.add(imageScanResponseDto);

        }

        return imageScanResponseDtoList;
    }


    public void discoverAndQueueImagesForScanning() {
        try {
            ApiClient client = ClientBuilder.defaultClient();
            Configuration.setDefaultApiClient(client);
            CoreV1Api api = new CoreV1Api();
            Map<String, Set<String>> imageMap = new HashMap<>();

            V1PodList podList = api.listPodForAllNamespaces(null, null, "status.phase=Running", null, null, null, null, null, null, null, null);

            for (V1Pod pod : podList.getItems()) {
                if (pod.getStatus() == null || pod.getStatus().getContainerStatuses() == null) continue;

                for (V1ContainerStatus status : pod.getStatus().getContainerStatuses()) {
                    if (status.getImageID() != null && status.getImageID().contains("sha256:")) {

                        String digest = "sha256:" + status.getImageID().split("@sha256:")[1];
                        imageMap.putIfAbsent(digest, new HashSet<>());
                        imageMap.get(digest).add(status.getImage());
                    }
                }
            }

            for (Map.Entry<String, Set<String>> entry : imageMap.entrySet()) {
                ActiveImage activeImage = new ActiveImage();
                activeImage.setDigest(entry.getKey());
                activeImage.setTags(new ArrayList<>(entry.getValue()));
                imageScanKafkaTemplate.send(KAFKA_TOPIC, activeImage.getDigest(), activeImage);
            }
        } catch (IOException | ApiException e) {
            e.printStackTrace();
        }
    }


    @Cacheable(value = "image_scans", key = "#image.digest")
    public List<VulnerabilitiesDto> getTrivyResultByImageName(ActiveImage image)
            throws IOException, InterruptedException {

        String tag = image.getTags().getFirst();
        if (tag == null || !tag.contains(":")) {
            throw new IllegalArgumentException("Invalid image tag format: " + tag);
        }

        String beforeColon = tag.substring(0, tag.indexOf(":"));
        String digest = image.getDigest();

        if (!digest.matches("^sha256:[a-fA-F0-9]{64}$")) {
            throw new IllegalArgumentException("Invalid digest format: " + digest);
        }
        if (!beforeColon.matches("^[a-z0-9]+([._-][a-z0-9]+)*(?:/[a-z0-9]+([._-][a-z0-9]+)*)*$")) {
            throw new IllegalArgumentException("Invalid image name: " + beforeColon);
        }

        ProcessBuilder pb = new ProcessBuilder(
                "trivy",
                "image",
                "--quiet",
                "--no-progress",
                "--format", "json",
                beforeColon + "@" + digest
        );
        pb.redirectErrorStream(false);

        Process process = pb.start();

        boolean finished = process.waitFor(90, TimeUnit.SECONDS);
        if (!finished) {
            process.destroyForcibly();
            throw new IOException("Trivy scan timed out for image: " + beforeColon);
        }

        try (InputStream inputStream = process.getInputStream();
             InputStream errorStream = process.getErrorStream()) {

            String jsonOutput = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
            String errorOutput = new String(errorStream.readAllBytes(), StandardCharsets.UTF_8);

            int exitCode = process.exitValue();
            if (exitCode != 0) {
                System.err.println("[Trivy] Non-zero exit code (" + exitCode + ")");
                if (!errorOutput.isBlank()) {
                    System.err.println("[Trivy Error] " + errorOutput);
                }
                return Collections.emptyList();
            }

            if (jsonOutput.isBlank()) {
                System.err.println("[Trivy] Empty JSON output for image: " + beforeColon);
                return Collections.emptyList();
            }

            ObjectMapper mapper = new ObjectMapper()
                    .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

            List<VulnerabilitiesDto> vulnerabilitiesList = new ArrayList<>();
            try {
                JsonNode root = mapper.readTree(jsonOutput);

                JsonNode results = root.get("Results");
                if (results != null && results.isArray()) {
                    for (JsonNode result : results) {
                        JsonNode vulns = result.get("Vulnerabilities");
                        if (vulns != null && vulns.isArray()) {
                            for (JsonNode vuln : vulns) {
                                VulnerabilitiesDto dto = new VulnerabilitiesDto();
                                dto.setPackageName(vuln.path("PkgName").asText("N/A"));
                                dto.setInstalledV(vuln.path("InstalledVersion").asText("N/A"));
                                dto.setFixedV(vuln.path("FixedVersion").asText("N/A"));
                                dto.setSeverity(vuln.path("Severity").asText("UNKNOWN"));
                                dto.setTitle(vuln.path("Title").asText("No title available"));
                                vulnerabilitiesList.add(dto);
                            }
                        }
                    }
                }
            } catch (JsonProcessingException e) {
                System.err.println("[Trivy] Failed to parse JSON: " + e.getMessage());
                return Collections.emptyList();
            }

            return vulnerabilitiesList;
        }
    }


    public Page<LogWithAnalysis> getAlertByPage(
            Pageable pageable,
            String priority,
            String rule,
            String pod,
            Date start,
            Date end
    ) {
        Page<LogFalco> logs = falcoRepository.findAlertsWithFilters(priority, rule, pod, start, end, pageable);

        List<LogWithAnalysis> content = logs.stream().map(log -> {
            AiAnalyze analysis = aiAnalyzeRepository.findByLogFalcoId(log.getId());
            return new LogWithAnalysis(log, analysis);
        }).toList();

        return new PageImpl<>(content, pageable, logs.getTotalElements());
    }


    public LogFalco receiveAlert(String payload) throws IOException, InterruptedException, ApiException {


        kafkaService.sendMessage(payload);
        return null;

    }


    private static final int MAX_OUTPUT_BYTES = 5 * 1024 * 1024;

    public ClusterInfo getK8sImagesDetailed() throws IOException, InterruptedException, ApiException {
        discoverAndQueueImagesForScanning();

        ProcessBuilder pb = new ProcessBuilder("kubectl", "get", "pods", "-A", "-o", "json");

        pb.redirectErrorStream(false);
        Process process = pb.start();

        var stdoutFuture = Executors.newSingleThreadExecutor().submit(() -> readStreamWithLimit(process.getInputStream(), MAX_OUTPUT_BYTES));
        var stderrFuture = Executors.newSingleThreadExecutor().submit(() -> readStreamWithLimit(process.getErrorStream(), MAX_OUTPUT_BYTES));

        boolean finished = process.waitFor(30, TimeUnit.SECONDS);
        if (!finished) {
            process.destroyForcibly();
            log.error("kubectl command timed out ({}). Process destroyed.", "kubectl get pods -A -o json");
            throw new IOException("Timed out while running kubectl");
        }

        int exitCode = process.exitValue();
        String stdout = "";
        String stderr = "";
        try {
            stdout = stdoutFuture.get(5, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.warn("Failed to read kubectl stdout: {}", e.getMessage());
        }
        try {
            stderr = stderrFuture.get(5, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.warn("Failed to read kubectl stderr: {}", e.getMessage());
        }

        if (exitCode != 0) {
            log.error("kubectl exited with code {}. Stderr length: {} bytes", exitCode, stderr.length());
            if (!stderr.isBlank()) {
                log.debug("kubectl stderr (truncated): {}", truncate(stderr, 2000));
            }
            throw new IOException("kubectl returned non-zero exit code: " + exitCode);
        }

        if (stdout.isBlank()) {
            log.warn("kubectl returned empty stdout.");
            return new ClusterInfo("my-cluster");
        }

        ObjectMapper mapper = new ObjectMapper();
        JsonNode root;
        try {
            root = mapper.readTree(stdout);
        } catch (JsonProcessingException e) {
            log.error("Failed to parse kubectl JSON output: {}", e.getMessage());
            log.debug("kubectl stdout (truncated): {}", truncate(stdout, 2000));
            throw new IOException("Invalid JSON from kubectl", e);
        }

        ClusterInfo cluster = new ClusterInfo("my-cluster");

        if (root.has("items") && root.get("items").isArray()) {
            Map<String, NameSpaceInfo> namespaceMap = new HashMap<>();

            for (JsonNode pod : root.get("items")) {
                String namespaceName = pod.path("metadata").path("namespace").asText("default");
                String podName = pod.path("metadata").path("name").asText("unknown");
                String podStatus = pod.path("status").path("phase").asText("Unknown");
                String nodeName = pod.path("spec").path("nodeName").asText("");
                String creationTimestamp = pod.path("metadata").path("creationTimestamp").asText("");
                JsonNode labelsNode = pod.path("metadata").path("labels");
                Map<String, String> labels = mapper.convertValue(labelsNode, new TypeReference<Map<String, String>>() {
                });
                JsonNode annotationsNode = pod.path("metadata").path("annotations");
                Map<String, String> annotations = mapper.convertValue(annotationsNode, new TypeReference<Map<String, String>>() {
                });

                int restarts = 0;
                JsonNode containerStatuses = pod.path("status").path("containerStatuses");
                if (containerStatuses.isArray()) {
                    for (JsonNode status : containerStatuses) {
                        restarts += status.path("restartCount").asInt(0);
                    }
                }

                NameSpaceInfo nsInfo = namespaceMap.getOrDefault(namespaceName, new NameSpaceInfo(namespaceName));
                JsonNode containers = pod.path("spec").path("containers");
                List<ContainerInfo> containerInfos = new ArrayList<>();

                if (containers.isArray()) {
                    for (JsonNode container : containers) {
                        String containerName = container.path("name").asText("unknown");
                        String image = container.path("image").asText("");

                        if (image.length() > 2000) {
                            log.warn("Container image name unusually long for pod {}/{}: length={}", namespaceName, podName, image.length());
                            image = image.substring(0, 2000);
                        }

                        List<Integer> ports = new ArrayList<>();
                        JsonNode portsNode = container.path("ports");
                        if (portsNode.isArray()) {
                            for (JsonNode port : portsNode) {
                                ports.add(port.path("containerPort").asInt());
                            }
                        }

                        Map<String, String> env = new HashMap<>();
                        JsonNode envNode = container.path("env");
                        if (envNode.isArray()) {
                            for (JsonNode e : envNode) {
                                env.put(e.path("name").asText(""), e.path("value").asText(""));
                            }
                        }

                        Map<String, String> resources = new HashMap<>();
                        JsonNode resNode = container.path("resources").path("limits");
                        if (resNode.isObject()) {
                            resNode.fields().forEachRemaining(entry -> resources.put(entry.getKey(), entry.getValue().asText()));
                        }

                        boolean readiness = false;
                        if (containerStatuses.isArray()) {
                            for (JsonNode status : containerStatuses) {
                                if (status.path("name").asText("").equals(containerName)) {
                                    readiness = status.path("ready").asBoolean(false);
                                }
                            }
                        }

                        containerInfos.add(new ContainerInfo(containerName, image, ports, env, resources, readiness, "pending"));
                    }
                }

                nsInfo.addPod(new PodInfo(podName, podStatus, nodeName, creationTimestamp, labels, annotations, restarts, containerInfos));
                namespaceMap.put(namespaceName, nsInfo);
            }

            cluster.setNamespaces(new ArrayList<>(namespaceMap.values()));
        }

        return cluster;
    }

    private String readStreamWithLimit(InputStream in, int maxBytes) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int total = 0;
        try {
            int r;
            while ((r = in.read(buffer)) != -1) {
                if (total + r > maxBytes) {
                    baos.write(buffer, 0, Math.max(0, maxBytes - total));
                    log.warn("Stream truncated after {} bytes", maxBytes);
                    break;
                } else {
                    baos.write(buffer, 0, r);
                    total += r;
                }
            }
        } catch (IOException e) {
            log.warn("Error reading stream: {}", e.getMessage());
        } finally {
            try {
                in.close();
            } catch (IOException ignored) {
            }
        }
        return baos.toString(StandardCharsets.UTF_8);
    }

    private String truncate(String s, int max) {
        if (s == null) return "";
        return s.length() <= max ? s : s.substring(0, max) + "...(truncated)";
    }

}

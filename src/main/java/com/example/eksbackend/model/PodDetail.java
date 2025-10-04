package com.example.eksbackend.model;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;

public class PodDetail {
    public String podName;
    public String status;
    public String node;
    public ZonedDateTime creationTimestamp;
    public Map<String, String> labels;
    public Map<String, String> annotations;
    public int restarts;
    public List<ContainerDetail> containers;

    public PodDetail(String podName, String status, String node, ZonedDateTime creationTimestamp, Map<String, String> labels, Map<String, String> annotations, int restarts, List<ContainerDetail> containers) {
        this.podName = podName;
        this.status = status;
        this.node = node;
        this.creationTimestamp = creationTimestamp;
        this.labels = labels;
        this.annotations = annotations;
        this.restarts = restarts;
        this.containers = containers;
    }
}

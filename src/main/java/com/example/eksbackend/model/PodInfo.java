package com.example.eksbackend.model;

import java.util.List;
import java.util.Map;

public  class PodInfo {
    public String podName;
    public String status;
    public String node;
    public String creationTimestamp;
    public Map<String, String> labels;
    public Map<String, String> annotations;
    public int restarts;
    public List<ContainerInfo> containers;

    public PodInfo(String podName, String status, String node, String creationTimestamp,
                   Map<String, String> labels, Map<String, String> annotations,
                   int restarts, List<ContainerInfo> containers) {
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
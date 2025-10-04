package com.example.eksbackend.model;

import java.util.List;
import java.util.Map;

public  class ContainerInfo {
    public String containerName;
    public String image;
    public List<Integer> ports;
    public Map<String, String> env;
    public Map<String, String> resources;
    public boolean readiness;
    public String scanStatus; // pending / completed / failed

    public ContainerInfo(String containerName, String image,
                         List<Integer> ports, Map<String, String> env,
                         Map<String, String> resources, boolean readiness,
                         String scanStatus) {
        this.containerName = containerName;
        this.image = image;
        this.ports = ports;
        this.env = env;
        this.resources = resources;
        this.readiness = readiness;
        this.scanStatus = scanStatus;
    }
}
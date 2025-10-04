package com.example.eksbackend.model;

import java.util.ArrayList;
import java.util.List;

public  class NameSpaceInfo {
    public String namespace;
    public List<PodInfo> pods;

    public NameSpaceInfo(String namespace) {
        this.namespace = namespace;
        this.pods = new ArrayList<>();
    }

    public void addPod(PodInfo pod) {
        this.pods.add(pod);
    }
}

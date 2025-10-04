package com.example.eksbackend.model;

import java.util.ArrayList;
import java.util.List;

public  class ClusterInfo {
    public String clusterName;
    public List<NameSpaceInfo> namespaces;

    public ClusterInfo(String clusterName) {
        this.clusterName = clusterName;
        this.namespaces = new ArrayList<>();
    }

    public void setNamespaces(List<NameSpaceInfo> namespaces) {
        this.namespaces = namespaces;
    }
}
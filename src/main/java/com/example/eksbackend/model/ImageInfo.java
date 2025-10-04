package com.example.eksbackend.model;

public  class ImageInfo {
    public String image;
    public String namespace;
    public String pod;
    public String container;

    public ImageInfo(String image, String namespace, String pod, String container) {
        this.image = image;
        this.namespace = namespace;
        this.pod = pod;
        this.container = container;
    }
}
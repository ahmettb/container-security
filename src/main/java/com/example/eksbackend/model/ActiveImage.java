package com.example.eksbackend.model;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
public  class ActiveImage {
    private  String imageName;
    public String digest;
    public List<String> tags;

    @Override
    public String toString() {
        return "ActiveImage{" + "digest='" + digest + '\'' + ", tags=" + tags + '}';
    }
}
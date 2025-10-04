package com.example.eksbackend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class EksBackendApplication {

    public static void main(String[] args) {


        SpringApplication.run(EksBackendApplication.class, args);
    }

}

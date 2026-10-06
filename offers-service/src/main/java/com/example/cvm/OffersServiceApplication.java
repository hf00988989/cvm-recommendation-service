package com.example.cvm;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class OffersServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(OffersServiceApplication.class, args);
    }
}

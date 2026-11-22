package com.triviaquick;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class TriviaQuickApplication {

    public static void main(String[] args) {
        SpringApplication.run(TriviaQuickApplication.class, args);
    }
}

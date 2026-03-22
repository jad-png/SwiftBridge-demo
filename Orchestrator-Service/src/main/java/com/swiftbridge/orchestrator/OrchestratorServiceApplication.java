package com.swiftbridge.orchestrator;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = {"com.swiftbridge.orchestrator"})
public class OrchestratorServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(OrchestratorServiceApplication.class, args);
    }

}

package com.swiftbridge.orchestrator;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

/**
 * Orchestrator Service - Main Application Entry Point
 * 
 * This is the REST API Gateway for the SwiftBridge platform. It:
 * - Exposes the public REST API for SWIFT conversion requests
 * - Handles JWT security (stubbed for PoC)
 * - Persists transaction metadata to PostgreSQL
 * - Orchestrates calls to the Core-Converter-Service
 * 
 * Port: 8080
 * Database: PostgreSQL (5432) - Shared via Docker network
 * 
 * @author SwiftBridge Team
 * @version 1.0.0
 */
@SpringBootApplication
@ComponentScan(basePackages = {"com.swiftbridge.orchestrator"})
public class OrchestratorServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(OrchestratorServiceApplication.class, args);
    }

}

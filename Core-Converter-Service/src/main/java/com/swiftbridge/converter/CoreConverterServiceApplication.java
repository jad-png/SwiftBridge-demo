package com.swiftbridge.converter;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

/**
 * Core Converter Service - Main Application Entry Point
 * 
 * This is the lightweight conversion engine for the SwiftBridge platform. It:
 * - Processes ISO 20022 (pacs.008 XML) to Legacy MT103 (TXT) conversion
 * - Operates as a stateless microservice (no database)
 * - Communicates ONLY with Orchestrator-Service (internal)
 * - Handles high-volume conversion requests with optimal performance
 * 
 * Port: 8081
 * Database: None (stateless)
 * 
 * @author SwiftBridge Team
 * @version 1.0.0
 */
@SpringBootApplication
@ComponentScan(basePackages = {"com.swiftbridge.converter"})
public class CoreConverterServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(CoreConverterServiceApplication.class, args);
    }

}

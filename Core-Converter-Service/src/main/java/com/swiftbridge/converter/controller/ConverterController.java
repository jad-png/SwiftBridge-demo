package com.swiftbridge.converter.controller;

import com.swiftbridge.converter.service.XmlToMtMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * ConverterController
 * 
 * REST Controller for the Core Converter Service.
 * Exposes the internal API endpoint for XML to MT103 conversion.
 * Endpoint: POST /api/v1/internal/convert
 * 
 * This is an internal-only endpoint, called by the Orchestrator Service.
 * It should not be exposed to external clients.
 * 
 * @author SwiftBridge Team
 * @version 1.0.0
 */
@RestController
@RequestMapping("/internal/convert")
@Slf4j
public class ConverterController {

    @Autowired
    private XmlToMtMapper xmlToMtMapper;

    /**
     * Converts pacs.008 XML to MT103 string format
     * 
     * Accepts raw XML content in the request body and returns the converted MT103 string.
     * 
     * @param xmlContent the pacs.008 XML content as a plain string in request body
     * @return ResponseEntity containing the MT103 string or error message
     */
    @PostMapping(
        consumes = MediaType.TEXT_PLAIN_VALUE,
        produces = MediaType.TEXT_PLAIN_VALUE
    )
    public ResponseEntity<?> convertPacs008ToMt103(@RequestBody String xmlContent) {
        try {
            // Validate input
            if (xmlContent == null || xmlContent.trim().isEmpty()) {
                log.warn("Empty XML content received in conversion request");
                return ResponseEntity.badRequest().body("XML content cannot be empty");
            }

            if (xmlContent.length() > 10_000_000) { // 10MB limit
                log.warn("XML content size exceeds 10MB: {} bytes", xmlContent.length());
                return ResponseEntity.badRequest().body("XML content size exceeds 10MB limit");
            }

            // Validate XML format (basic check)
            if (!xmlContent.trim().startsWith("<")) {
                log.warn("Invalid XML format: does not start with '<'");
                return ResponseEntity.badRequest().body("Invalid XML format");
            }

            log.info("Processing conversion request for XML of size: {} bytes", xmlContent.length());

            // Convert XML to MT103
            String mt103Result = xmlToMtMapper.convertPacs008ToMt103(xmlContent);

            log.info("Conversion successful. MT103 length: {}", mt103Result.length());

            // Return MT103 as plain text
            return ResponseEntity.ok()
                .contentType(MediaType.TEXT_PLAIN)
                .body(mt103Result);

        } catch (IllegalArgumentException e) {
            log.error("Invalid input provided", e);
            return ResponseEntity.badRequest()
                .body("Invalid input: " + e.getMessage());

        } catch (RuntimeException e) {
            log.error("Conversion failed due to conversion error", e);
            return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body("Conversion failed: " + e.getMessage());

        } catch (Exception e) {
            log.error("Unexpected error during XML to MT103 conversion", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("An unexpected error occurred: " + e.getMessage());
        }
    }

    /**
     * Health check endpoint
     * 
     * @return status message
     */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Converter Service is running");
    }

    /**
     * Info endpoint showing supported formats
     * 
     * @return information message
     */
    @GetMapping("/info")
    public ResponseEntity<String> info() {
        return ResponseEntity.ok("Core Converter Service - Converts pacs.008 XML to MT103 format");
    }
}

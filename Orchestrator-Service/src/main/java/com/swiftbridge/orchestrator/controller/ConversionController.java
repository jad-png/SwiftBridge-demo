package com.swiftbridge.orchestrator.controller;

import com.swiftbridge.orchestrator.service.ConversionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * ConversionController
 * 
 * REST Controller for the Orchestrator Service.
 * Exposes the public API endpoint for XML to MT103 conversion.
 * Endpoint: POST /api/convert
 * 
 * @author SwiftBridge Team
 * @version 1.0.0
 */
@RestController
@RequestMapping("/convert")
@Slf4j
public class ConversionController {

    @Autowired
    private ConversionService conversionService;

    /**
     * Converts an uploaded XML file to MT103 format
     * 
     * @param file the multipart XML file to convert
     * @return ResponseEntity containing the MT103 string or error message
     */
    @PostMapping
    public ResponseEntity<?> convertXmlToMt103(@RequestParam("file") MultipartFile file) {
        try {
            // Validate file
            if (file.isEmpty()) {
                log.warn("Empty file received");
                return ResponseEntity.badRequest().body("File is empty");
            }

            if (file.getSize() > 10_000_000) { // 10MB limit
                log.warn("File size exceeds 10MB: {} bytes", file.getSize());
                return ResponseEntity.badRequest().body("File size exceeds 10MB limit");
            }

            // Validate file extension (optional)
            String filename = file.getOriginalFilename();
            if (filename != null && !filename.toLowerCase().endsWith(".xml")) {
                log.warn("Non-XML file uploaded: {}", filename);
                return ResponseEntity.badRequest().body("Only XML files are accepted");
            }

            log.info("Processing conversion request for file: {}", filename);

            // Convert file content to string
            String xmlContent = new String(file.getBytes());

            // Call conversion service
            String mt103Result = conversionService.convertXmlToMt103(xmlContent, filename);

            log.info("Conversion completed successfully for file: {}", filename);

            // Return MT103 result
            return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=\"" + filename.replace(".xml", ".txt") + "\"")
                .body(mt103Result);

        } catch (IOException e) {
            log.error("IO error reading file", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Failed to read file: " + e.getMessage());

        } catch (RuntimeException e) {
            log.error("Conversion failed", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Conversion failed: " + e.getMessage());

        } catch (Exception e) {
            log.error("Unexpected error during conversion", e);
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
        return ResponseEntity.ok("Orchestrator Service is running");
    }
}

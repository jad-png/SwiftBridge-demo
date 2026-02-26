package com.swiftbridge.orchestrator.service;

import com.swiftbridge.orchestrator.entity.TransactionHistory;
import com.swiftbridge.orchestrator.repository.TransactionHistoryRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;

/**
 * ConversionService
 * 
 * Handles the orchestration of XML to MT103 conversion.
 * Calls the Core Converter Service via HTTP and persists results to PostgreSQL.
 * 
 * @author SwiftBridge Team
 * @version 1.0.0
 */
@Service
@Slf4j
public class ConversionService {

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private TransactionHistoryRepository transactionHistoryRepository;

    @Value("${converter.service.url:http://localhost:8081}")
    private String converterServiceUrl;

    @Value("${converter.service.endpoint:/api/v1/internal/convert}")
    private String converterEndpoint;

    /**
     * Converts XML to MT103 format by calling the Core Converter Service
     * 
     * @param xmlContent the pacs.008 XML content as a string
     * @param filename the original filename for audit tracking
     * @return the converted MT103 string
     * @throws Exception if conversion fails
     */
    public String convertXmlToMt103(String xmlContent, String filename) throws Exception {
        try {
            log.info("Starting XML to MT103 conversion for file: {}", filename);

            // Call the Core Converter Service
            String mt103Result = callConverterService(xmlContent);

            // Save SUCCESS record to PostgreSQL
            TransactionHistory transaction = TransactionHistory.builder()
                .filename(filename)
                .status("SUCCESS")
                .timestamp(LocalDateTime.now())
                .mt103Output(mt103Result)
                .errorMessage(null)
                .build();

            transactionHistoryRepository.save(transaction);
            log.info("Conversion successful for file: {}. MT103 length: {}", filename, mt103Result.length());

            return mt103Result;

        } catch (RestClientException e) {
            log.error("Failed to call converter service for file: {}", filename, e);
            saveFailedTransaction(filename, "Converter service unavailable: " + e.getMessage());
            throw new RuntimeException("Failed to convert XML: Converter service error", e);

        } catch (Exception e) {
            log.error("Unexpected error during conversion for file: {}", filename, e);
            saveFailedTransaction(filename, "Conversion error: " + e.getMessage());
            throw new RuntimeException("Failed to convert XML: " + e.getMessage(), e);
        }
    }

    /**
     * Makes an HTTP POST request to the Core Converter Service
     * 
     * @param xmlContent the XML content to convert
     * @return the converted MT103 string
     */
    private String callConverterService(String xmlContent) {
        String url = converterServiceUrl + converterEndpoint;
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        // Send XML as a plain string in the request body
        HttpEntity<String> request = new HttpEntity<>(xmlContent, headers);

        log.debug("Calling converter service at: {}", url);
        ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);

        if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
            log.debug("Converter service returned MT103 successfully");
            return response.getBody();
        } else {
            throw new RuntimeException("Converter service returned non-success status: " + response.getStatusCode());
        }
    }

    /**
     * Saves a failed conversion record to the database
     * 
     * @param filename the filename of the failed conversion
     * @param errorMessage the error message
     */
    private void saveFailedTransaction(String filename, String errorMessage) {
        try {
            TransactionHistory transaction = TransactionHistory.builder()
                .filename(filename)
                .status("FAILED")
                .timestamp(LocalDateTime.now())
                .mt103Output(null)
                .errorMessage(errorMessage)
                .build();

            transactionHistoryRepository.save(transaction);
            log.info("Saved FAILED transaction record for file: {}", filename);
        } catch (Exception e) {
            log.error("Failed to save transaction history for failed conversion: {}", filename, e);
        }
    }
}

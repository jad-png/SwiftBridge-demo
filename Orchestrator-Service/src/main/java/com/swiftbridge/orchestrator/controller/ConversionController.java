package com.swiftbridge.orchestrator.controller;

import com.swiftbridge.orchestrator.dto.conversion.ConversionResponse;
import com.swiftbridge.orchestrator.exception.ValidationFailedException;
import com.swiftbridge.orchestrator.service.ConversionService;
import com.swiftbridge.orchestrator.validation.ConversionFileValidator;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping({"/convert", "/api/convert"})
@Slf4j
@RequiredArgsConstructor
public class ConversionController {

    private final ConversionService conversionService;
    private final ConversionFileValidator conversionFileValidator;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> convertXmlToMt103(@RequestParam("file") MultipartFile file) throws IOException {
        
        conversionFileValidator.validate(file);
        
        String filename = file.getOriginalFilename();
        
        log.info("Processing conversion request for file: {}", filename);
        
        String xmlContent = new String(file.getBytes());
        
        ConversionResponse conversionResponse = conversionService.convertXmlToMt103(xmlContent, filename);
        
        log.info("Conversion completed successfully for file: {}", filename);
        
        return ResponseEntity.ok(conversionResponse);
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Orchestrator Service is running");
    }
}

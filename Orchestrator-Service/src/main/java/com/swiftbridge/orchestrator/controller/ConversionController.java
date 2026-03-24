package com.swiftbridge.orchestrator.controller;

import com.swiftbridge.orchestrator.dto.conversion.ConversionResponse;
import com.swiftbridge.orchestrator.exception.ValidationFailedException;
import com.swiftbridge.orchestrator.service.ConversionService;
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

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> convertXmlToMt103(@RequestParam("file") MultipartFile file) throws IOException {
        if (file.isEmpty()) {
            throw new ValidationFailedException("ERR_EMPTY_FILE", "File is empty");
        }

        if (file.getSize() > 10_000_000) {
            throw new ValidationFailedException("ERR_FILE_TOO_LARGE", "File size exceeds 10MB limit");
        }

        String filename = file.getOriginalFilename();
        if (filename == null || !filename.toLowerCase().endsWith(".xml")) {
            throw new ValidationFailedException("ERR_FILE_TYPE", "Only XML files are accepted");
        }

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

package com.swiftbridge.converter.controller;

import com.swiftbridge.converter.aspect.annotation.ValidXmlFile;
import com.swiftbridge.converter.dto.ConversionResponse;
import com.swiftbridge.converter.exception.FileValidationException;
import com.swiftbridge.converter.mapping.model.ConversionResult;
import com.swiftbridge.converter.service.XmlToMtMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/internal/convert")
@Slf4j
@RequiredArgsConstructor
public class ConverterController {

    private final XmlToMtMapper xmlToMtMapper;

    @PostMapping(
        consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    @ValidXmlFile
    public ResponseEntity<?> convertPacs008ToMt103(@RequestParam("file") MultipartFile file)
        throws IOException, FileValidationException {
        log.info("Request received for file: {}", file.getOriginalFilename());
        long startNanos = System.nanoTime();

        String xmlContent = readXmlContent(file);

        ConversionResult conversionResult = xmlToMtMapper.convertPacs008ToMt103(xmlContent);
        long processingTimeMs = Math.max(0L, (System.nanoTime() - startNanos) / 1_000_000L);

        log.info("Successfully generated MT103 for {} in {}ms", file.getOriginalFilename(), processingTimeMs);

        return ResponseEntity.ok(ConversionResponse.builder()
            .mt103(conversionResult.mt103())
            .warnings(conversionResult.warnings())
            .processingTimeMs(processingTimeMs)
            .build());
    }

    private String readXmlContent(MultipartFile file) throws IOException {
        return new String(file.getBytes(), StandardCharsets.UTF_8);
    }
}

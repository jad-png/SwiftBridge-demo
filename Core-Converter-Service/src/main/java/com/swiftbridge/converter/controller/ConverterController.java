package com.swiftbridge.converter.controller;

import com.swiftbridge.converter.aspect.annotation.ValidXmlFile;
import com.swiftbridge.converter.service.XmlToMtMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.convert.ConversionService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/internal/convert")
@Slf4j
public class ConverterController {

    @Autowired
    private XmlToMtMapper xmlToMtMapper;
    @Autowired
    private ConversionService conversionService;

    @PostMapping(
        consumes = MediaType.TEXT_PLAIN_VALUE,
        produces = MediaType.TEXT_PLAIN_VALUE
    )
    @ValidXmlFile
    public ResponseEntity<?> convertPacs008ToMt103(@RequestParam("file") MultipartFile file) throws IOException {
        log.info("Request received for file: {}", file.getOriginalFilename());

        String xmlContent = new String (file.getBytes(), StandardCharsets.UTF_8);

//        String mt03Result = conversionService.convert();
        return ResponseEntity.ok();
    }
}

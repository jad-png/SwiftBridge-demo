package com.swiftbridge.converter.utils;

import com.swiftbridge.converter.mapping.model.ConversionResult;
import com.swiftbridge.converter.mapping.pacs008.mapper.Pacs008ToMt103Mapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class SwiftCbprMapper {

    private final Pacs008ToMt103Mapper pacs008ToMt103Mapper;

    public ConversionResult mapPacs008ToMt103(String xmlContent) {
        log.info("SwiftCbprMapper: Starting CBPR+ XML to MT103 conversion");

        try {

            ConversionResult conversionResult = pacs008ToMt103Mapper.mapPacs008ToMt103(xmlContent);

            log.info("SwiftCbprMapper: CBPR+ to MT103 conversion completed successfully. " +
                     "Generated MT103 message length: {} characters", conversionResult.mt103().length());

            return conversionResult;

        } catch (Exception e) {
            log.error("SwiftCbprMapper: CBPR+ to MT103 conversion failed", e);
            throw new RuntimeException("Failed to map CBPR+ XML to MT103: " + e.getMessage(), e);
        }
    }
}

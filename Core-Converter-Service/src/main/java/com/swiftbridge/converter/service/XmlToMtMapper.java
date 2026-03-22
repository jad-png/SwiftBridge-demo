package com.swiftbridge.converter.service;

import com.swiftbridge.converter.exception.ConversionFailedException;
import com.swiftbridge.converter.mapping.model.ConversionResult;
import com.swiftbridge.converter.utils.SwiftCbprMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class XmlToMtMapper {

    private final SwiftCbprMapper swiftCbprMapper;

    public ConversionResult convertPacs008ToMt103(String xmlContent) {
        log.info("XmlToMtMapper: Starting pacs.008 to MT103 conversion");

        try {
            ConversionResult mt103 = mapViaCbprMapper(xmlContent);
            log.info("XmlToMtMapper: MT103 conversion completed successfully");
            return mt103;

        } catch (Exception e) {
            log.error("XmlToMtMapper: Error converting XML to MT103", e);
            throw new ConversionFailedException(
                "ERR_MT103_CONVERSION",
                "Failed to convert pacs.008 to MT103: " + e.getMessage(),
                e
            );
        }
    }

    private ConversionResult mapViaCbprMapper(String xmlContent) {
        return swiftCbprMapper.mapPacs008ToMt103(xmlContent);
    }
}

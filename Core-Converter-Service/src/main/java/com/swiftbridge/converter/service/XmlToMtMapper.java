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

        } catch (com.swiftbridge.converter.exception.SwiftMappingException e) {
            throw e;
        } catch (Exception e) {
            log.error("XmlToMtMapper: Error converting XML to MT103", e);
            throw wrapConversionFailure(e);
        }
    }

    private ConversionResult mapViaCbprMapper(String xmlContent) {
        return swiftCbprMapper.mapPacs008ToMt103(xmlContent);
    }

    private ConversionFailedException wrapConversionFailure(Exception exception) {
        return new ConversionFailedException(
                com.swiftbridge.converter.exception.SwiftErrorCode.ERR_MT103_CONVERSION_FAILED.getCode(),
                "Failed to convert pacs.008 to MT103: " + exception.getMessage(),
                exception);
    }
}

package com.swiftbridge.orchestrator.service;

import com.swiftbridge.orchestrator.dto.ConversionResponse;

public interface ConversionService {

    ConversionResponse convertXmlToMt103(String xmlContent, String filename);
}

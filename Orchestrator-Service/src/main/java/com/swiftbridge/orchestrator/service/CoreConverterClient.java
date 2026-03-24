package com.swiftbridge.orchestrator.service;

import com.swiftbridge.orchestrator.dto.conversion.ConversionResponse;

public interface CoreConverterClient {

    ConversionResponse convert(String xmlContent, String filename);
}

package com.swiftbridge.orchestrator.service;

import com.swiftbridge.orchestrator.dto.ConversionResponse;

public interface CoreConverterClient {

    ConversionResponse convert(String xmlContent, String filename);
}

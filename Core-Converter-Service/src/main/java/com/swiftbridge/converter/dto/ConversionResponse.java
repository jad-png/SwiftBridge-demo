package com.swiftbridge.converter.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class ConversionResponse {

    private String mt103;
    private List<String> warnings;
    private long processingTimeMs;
}

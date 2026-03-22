package com.swiftbridge.converter.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Singular;

import java.util.List;
import java.util.Objects;

@Getter
@Builder(toBuilder = true)
public class ConversionResponse {

    private String mt103;
    @Singular("warning")
    private List<String> warnings;
    private long processingTimeMs;

    public void validateResponse() {
        Objects.requireNonNull(mt103, "mt103 is required");
        if (processingTimeMs < 0) {
            throw new IllegalArgumentException("processingTimeMs cannot be negative");
        }
    }
}

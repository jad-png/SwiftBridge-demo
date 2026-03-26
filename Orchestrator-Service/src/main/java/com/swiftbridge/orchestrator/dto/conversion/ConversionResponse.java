package com.swiftbridge.orchestrator.dto.conversion;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;


@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConversionResponse {

    private String mt103;
    private List<String> warnings;
    private long processingTimeMs;
    private String messageReference;
}

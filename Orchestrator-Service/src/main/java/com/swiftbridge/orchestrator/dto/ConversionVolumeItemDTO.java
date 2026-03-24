package com.swiftbridge.orchestrator.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;

@Getter
@Builder
public class ConversionVolumeItemDTO {

    private LocalDate day;
    private long total;
    private long success;
    private double successRate;
}

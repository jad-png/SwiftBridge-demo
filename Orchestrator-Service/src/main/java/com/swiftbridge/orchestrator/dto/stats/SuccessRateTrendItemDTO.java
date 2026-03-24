package com.swiftbridge.orchestrator.dto.stats;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;

@Getter
@Builder
public class SuccessRateTrendItemDTO {

    private LocalDate day;
    private double successRate;
    private long total;
}

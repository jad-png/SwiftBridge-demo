package com.swiftbridge.orchestrator.dto.stats;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SuccessRateTrendItemDTO {

    private LocalDate day;
    private double successRate;
    private long total;
}

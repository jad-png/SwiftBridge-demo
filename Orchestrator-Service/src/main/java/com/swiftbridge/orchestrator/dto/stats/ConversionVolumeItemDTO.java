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
public class ConversionVolumeItemDTO {

    private LocalDate day;
    private long total;
    private long success;
    private double successRate;
}

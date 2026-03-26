package com.swiftbridge.orchestrator.dto.stats;

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
public class AdminStatsResponse {

    private AdminMetricsDTO metrics;
    private List<ConversionVolumeItemDTO> conversionVolume;
    private List<SuccessRateTrendItemDTO> successRateTrend;
}

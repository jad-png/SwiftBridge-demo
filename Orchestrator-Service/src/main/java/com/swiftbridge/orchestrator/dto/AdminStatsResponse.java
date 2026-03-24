package com.swiftbridge.orchestrator.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class AdminStatsResponse {

    private AdminMetricsDTO metrics;
    private List<ConversionVolumeItemDTO> conversionVolume;
    private List<SuccessRateTrendItemDTO> successRateTrend;
}

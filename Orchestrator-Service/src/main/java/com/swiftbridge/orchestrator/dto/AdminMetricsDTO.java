package com.swiftbridge.orchestrator.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AdminMetricsDTO {

    private long totalUsers;
    private long totalConversions;
    private long totalSuccessfulConversions;
    private double conversionSuccessRate;
    private long totalGuests;
}

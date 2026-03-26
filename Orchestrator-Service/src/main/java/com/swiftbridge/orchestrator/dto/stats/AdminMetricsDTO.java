package com.swiftbridge.orchestrator.dto.stats;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminMetricsDTO {

    private long totalUsers;
    private long totalConversions;
    private long totalSuccessfulConversions;
    private double conversionSuccessRate;
    private long totalGuests;
}


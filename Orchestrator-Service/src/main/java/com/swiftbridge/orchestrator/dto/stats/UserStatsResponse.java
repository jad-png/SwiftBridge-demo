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
public class UserStatsResponse {

    private long totalConversions;
    private long successfulConversions;
    private long failedConversions;
    private double successRate;
    private List<RecentActivityItem> recentActivity;
    private List<ActivityTrendItem> activityTrend;
}

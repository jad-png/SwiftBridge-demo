package com.swiftbridge.orchestrator.dto.stats;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class UserStatsResponse {

    private long totalConversions;
    private long successfulConversions;
    private long failedConversions;
    private double successRate;
    private List<RecentActivityItem> recentActivity;
    private List<ActivityTrendItem> activityTrend;
}

package com.swiftbridge.orchestrator.service;

import com.swiftbridge.orchestrator.dto.ActivityTrendItem;
import com.swiftbridge.orchestrator.dto.RecentActivityItem;
import com.swiftbridge.orchestrator.dto.UserStatsResponse;
import com.swiftbridge.orchestrator.entity.ConversionStatus;
import com.swiftbridge.orchestrator.repository.TransactionHistoryRepository;
import com.swiftbridge.orchestrator.repository.projection.DailyConversionStatsProjection;
import com.swiftbridge.orchestrator.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UserStatsService {

    private static final int RECENT_ACTIVITY_LIMIT = 5;
    private static final int ACTIVITY_TREND_DAYS = 7;

    private final TransactionHistoryRepository transactionHistoryRepository;
    private final SecurityUtils securityUtils;

    @Transactional(readOnly = true)
    public UserStatsResponse getCurrentUserStats() {
        Long currentUserId = securityUtils.getCurrentUser().getId();

        long totalConversions = transactionHistoryRepository.countByUser_Id(currentUserId);
        long successfulConversions = transactionHistoryRepository
            .countByUser_IdAndConversionStatus(currentUserId, ConversionStatus.SUCCESS);
        long failedConversions = Math.max(0, totalConversions - successfulConversions);

        List<RecentActivityItem> recentActivity = transactionHistoryRepository
            .findTop5ByUser_IdOrderByRequestTimestampDesc(currentUserId)
            .stream()
            .limit(RECENT_ACTIVITY_LIMIT)
            .map(item -> RecentActivityItem.builder()
                .id(item.getTransactionId())
                .status(item.getConversionStatus().name())
                .createdAt(item.getRequestTimestamp())
                .build())
            .toList();

        List<ActivityTrendItem> activityTrend = transactionHistoryRepository
            .findDailyStatsByUserId(currentUserId)
            .stream()
            .limit(ACTIVITY_TREND_DAYS)
            .sorted(Comparator.comparing(DailyConversionStatsProjection::getDay))
            .map(this::toActivityTrend)
            .toList();

        return UserStatsResponse.builder()
            .totalConversions(totalConversions)
            .successfulConversions(successfulConversions)
            .failedConversions(failedConversions)
            .successRate(calculateSuccessRate(successfulConversions, totalConversions))
            .recentActivity(recentActivity)
            .activityTrend(activityTrend)
            .build();
    }

    private ActivityTrendItem toActivityTrend(DailyConversionStatsProjection projection) {
        long total = safeLong(projection.getTotal());
        long success = safeLong(projection.getSuccess());

        return ActivityTrendItem.builder()
            .day(projection.getDay())
            .total(total)
            .success(success)
            .successRate(calculateSuccessRate(success, total))
            .build();
    }

    private double calculateSuccessRate(long success, long total) {
        if (total <= 0) {
            return 0.0;
        }
        return (success * 100.0) / total;
    }

    private long safeLong(Long value) {
        return value == null ? 0L : value;
    }
}

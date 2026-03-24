package com.swiftbridge.orchestrator.service;

import com.swiftbridge.orchestrator.dto.AdminMetricsDTO;
import com.swiftbridge.orchestrator.dto.AdminStatsResponse;
import com.swiftbridge.orchestrator.dto.ConversionVolumeItemDTO;
import com.swiftbridge.orchestrator.dto.SuccessRateTrendItemDTO;
import com.swiftbridge.orchestrator.entity.ConversionStatus;
import com.swiftbridge.orchestrator.repository.AppUserRepository;
import com.swiftbridge.orchestrator.repository.TransactionHistoryRepository;
import com.swiftbridge.orchestrator.repository.projection.DailyConversionStatsProjection;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminStatsService {

    private static final int TREND_DAYS = 14;

    private final AppUserRepository appUserRepository;
    private final TransactionHistoryRepository transactionHistoryRepository;

    @Transactional(readOnly = true)
    public AdminStatsResponse getAdminStats() {
        long totalUsers = appUserRepository.count();
        long totalConversions = transactionHistoryRepository.count();
        long totalSuccessfulConversions = transactionHistoryRepository.countByConversionStatus(ConversionStatus.SUCCESS);

        AdminMetricsDTO metrics = AdminMetricsDTO.builder()
            .totalUsers(totalUsers)
            .totalConversions(totalConversions)
            .totalSuccessfulConversions(totalSuccessfulConversions)
            .conversionSuccessRate(calculateSuccessRate(totalSuccessfulConversions, totalConversions))
            .totalGuests(0L)
            .build();

        List<DailyConversionStatsProjection> dailyStats = transactionHistoryRepository.findDailyStatsGlobal()
            .stream()
            .limit(TREND_DAYS)
            .sorted(Comparator.comparing(DailyConversionStatsProjection::getDay))
            .toList();

        List<ConversionVolumeItemDTO> conversionVolume = dailyStats.stream()
            .map(this::toConversionVolume)
            .toList();

        List<SuccessRateTrendItemDTO> successRateTrend = dailyStats.stream()
            .map(this::toSuccessRateTrend)
            .toList();

        return AdminStatsResponse.builder()
            .metrics(metrics)
            .conversionVolume(conversionVolume)
            .successRateTrend(successRateTrend)
            .build();
    }

    private ConversionVolumeItemDTO toConversionVolume(DailyConversionStatsProjection projection) {
        long total = safeLong(projection.getTotal());
        long success = safeLong(projection.getSuccess());

        return ConversionVolumeItemDTO.builder()
            .day(projection.getDay())
            .total(total)
            .success(success)
            .successRate(calculateSuccessRate(success, total))
            .build();
    }

    private SuccessRateTrendItemDTO toSuccessRateTrend(DailyConversionStatsProjection projection) {
        long total = safeLong(projection.getTotal());
        long success = safeLong(projection.getSuccess());

        return SuccessRateTrendItemDTO.builder()
            .day(projection.getDay())
            .successRate(calculateSuccessRate(success, total))
            .total(total)
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

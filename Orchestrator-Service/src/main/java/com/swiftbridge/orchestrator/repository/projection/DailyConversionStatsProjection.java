package com.swiftbridge.orchestrator.repository.projection;

import java.time.LocalDate;

public interface DailyConversionStatsProjection {

    LocalDate getDay();

    Long getTotal();

    Long getSuccess();
}

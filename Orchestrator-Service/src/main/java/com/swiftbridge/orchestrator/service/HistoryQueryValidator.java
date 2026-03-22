package com.swiftbridge.orchestrator.service;

import java.time.LocalDate;

public interface HistoryQueryValidator {

    void validateFilters(LocalDate date, int page, int size);

    void validateHistoryId(Long id);
}

package com.swiftbridge.orchestrator.service.Impl;

import com.swiftbridge.orchestrator.service.HistoryQueryValidator;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
public class HistoryQueryValidatorImpl implements HistoryQueryValidator {

    private static final int MAX_PAGE_SIZE = 200;

    @Override
    public void validateFilters(LocalDate date, int page, int size) {
        if (page < 0) {
            throw new IllegalArgumentException("Parameter 'page' must be greater than or equal to 0");
        }

        if (size < 1 || size > MAX_PAGE_SIZE) {
            throw new IllegalArgumentException("Parameter 'size' must be between 1 and " + MAX_PAGE_SIZE);
        }

        if (date != null && date.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("Parameter 'date' cannot be in the future");
        }
    }

    @Override
    public void validateHistoryId(Long id) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("Parameter 'id' must be a positive number");
        }
    }

    @Override
    public void validateTransactionId(String transactionId) {
        if (transactionId == null || transactionId.isBlank()) {
            throw new IllegalArgumentException("Parameter 'txnId' is required");
        }
    }
}

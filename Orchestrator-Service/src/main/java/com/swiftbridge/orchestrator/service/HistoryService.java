package com.swiftbridge.orchestrator.service;

import com.swiftbridge.orchestrator.entity.ConversionStatus;
import com.swiftbridge.orchestrator.entity.TransactionHistory;
import org.springframework.data.domain.Page;

import java.time.LocalDate;
import java.util.Optional;

public interface HistoryService {

    Page<TransactionHistory> findFilteredHistory(LocalDate date,
                                                 ConversionStatus status,
                                                 int page,
                                                 int size);

    Optional<TransactionHistory> getHistoryById(Long id);

    void saveSuccess(String messageReference,
                     long processingDurationMs);

    void saveFailureInNewTransaction(String messageReference,
                                     long processingDurationMs);
}

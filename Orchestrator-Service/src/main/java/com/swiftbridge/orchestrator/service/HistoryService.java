package com.swiftbridge.orchestrator.service;

import com.swiftbridge.orchestrator.dto.history.ConversionAuditDTO;
import com.swiftbridge.orchestrator.dto.history.HistoryItemDTO;
import com.swiftbridge.orchestrator.dto.history.HistoryListResponse;
import com.swiftbridge.orchestrator.entity.ConversionStatus;

import java.time.LocalDate;
import java.util.Optional;

public interface HistoryService {

    Optional<ConversionAuditDTO> getAuditByTransactionId(String transactionId);

    HistoryListResponse findGlobalHistory(LocalDate date,
            ConversionStatus status,
            int page,
            int size);

    HistoryListResponse findFilteredHistory(LocalDate date,
            ConversionStatus status,
            int page,
            int size);

    Optional<HistoryItemDTO> getHistoryByTransactionId(String transactionId);

    Optional<HistoryItemDTO> getHistoryById(Long id);

    void saveSuccess(String messageReference,
            long processingDurationMs);

    void saveFailureInNewTransaction(String messageReference,
            long processingDurationMs);
}

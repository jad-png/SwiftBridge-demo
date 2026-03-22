package com.swiftbridge.orchestrator.service.Impl;

import com.swiftbridge.orchestrator.entity.ConversionStatus;
import com.swiftbridge.orchestrator.entity.TransactionHistory;
import com.swiftbridge.orchestrator.repository.TransactionHistoryRepository;
import com.swiftbridge.orchestrator.service.HistoryService;
import com.swiftbridge.orchestrator.service.HistoryQueryValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class HistoryServiceImpl implements HistoryService {

    private final TransactionHistoryRepository transactionHistoryRepository;
    private final HistoryQueryValidator historyQueryValidator;

    @Override
    @Transactional(readOnly = true)
    public Page<TransactionHistory> findFilteredHistory(LocalDate date,
                                                        ConversionStatus status,
                                                        int page,
                                                        int size) {
        historyQueryValidator.validateFilters(date, page, size);

        LocalDateTime startTime = date == null ? null : date.atStartOfDay();
        LocalDateTime endTime = date == null ? null : date.plusDays(1).atStartOfDay().minusNanos(1);
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "requestTimestamp"));
        return transactionHistoryRepository.findByFilters(startTime, endTime, status, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<TransactionHistory> getHistoryById(Long id) {
        historyQueryValidator.validateHistoryId(id);
        return transactionHistoryRepository.findById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveSuccess(String messageReference,
                            long processingDurationMs) {
        TransactionHistory transaction = TransactionHistory.builder()
            .transactionId(UUID.randomUUID().toString())
            .conversionStatus(ConversionStatus.SUCCESS)
            .requestTimestamp(LocalDateTime.now())
            .messageReference(messageReference)
            .messageType("MT103")
            .processingDurationMs(processingDurationMs)
            .build();

        transactionHistoryRepository.save(transaction);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void saveFailureInNewTransaction(String messageReference,
                                            long processingDurationMs) {
        TransactionHistory transaction = TransactionHistory.builder()
            .transactionId(UUID.randomUUID().toString())
            .conversionStatus(ConversionStatus.FAILED)
            .requestTimestamp(LocalDateTime.now())
            .messageReference(messageReference)
            .messageType("MT103")
            .processingDurationMs(processingDurationMs)
            .build();

        transactionHistoryRepository.save(transaction);
        log.info("Saved FAILED transaction history record with reference: {}", messageReference);
    }
}

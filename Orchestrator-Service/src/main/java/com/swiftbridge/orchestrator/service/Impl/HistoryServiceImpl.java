package com.swiftbridge.orchestrator.service.Impl;

import com.swiftbridge.orchestrator.dto.history.ConversionAuditDTO;
import com.swiftbridge.orchestrator.dto.history.HistoryItemDTO;
import com.swiftbridge.orchestrator.dto.history.HistoryListResponse;
import com.swiftbridge.orchestrator.entity.ConversionStatus;
import com.swiftbridge.orchestrator.entity.TransactionHistory;
import com.swiftbridge.orchestrator.entity.User;
import com.swiftbridge.orchestrator.repository.TransactionHistoryRepository;
import com.swiftbridge.orchestrator.repository.TransactionHistorySpecifications;
import com.swiftbridge.orchestrator.security.SecurityUtils;
import com.swiftbridge.orchestrator.service.AuditService;
import com.swiftbridge.orchestrator.service.HistoryQueryValidator;
import com.swiftbridge.orchestrator.service.HistoryService;
import com.swiftbridge.orchestrator.mapper.TransactionHistoryMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class HistoryServiceImpl implements HistoryService {

    private final TransactionHistoryRepository transactionHistoryRepository;
    private final HistoryQueryValidator historyQueryValidator;
    private final SecurityUtils securityUtils;
    private final AuditService auditService;
    private final TransactionHistoryMapper historyMapper;

    @Override
    @Transactional(readOnly = true)
    public Optional<ConversionAuditDTO> getAuditByTransactionId(String transactionId) {
        historyQueryValidator.validateTransactionId(transactionId);

        Long currentUserId = securityUtils.getCurrentUser().getId();

        return transactionHistoryRepository.findByTransactionId(transactionId)
                .filter(history -> isOwnedByCurrentUserOrAdmin(history, currentUserId))
                .map(historyMapper::toAuditDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public HistoryListResponse findGlobalHistory(LocalDate date, ConversionStatus status, int page, int size) {
        return fetchHistory(null, date, status, page, size);
    }

    @Override
    @Transactional(readOnly = true)
    public HistoryListResponse findFilteredHistory(LocalDate date, ConversionStatus status, int page, int size) {
        historyQueryValidator.validateFilters(date, page, size);

        Long currentUserId = securityUtils.getCurrentUser().getId();

        return fetchHistory(currentUserId, date, status, page, size);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<HistoryItemDTO> getHistoryByTransactionId(String transactionId) {
        historyQueryValidator.validateTransactionId(transactionId);

        Long currentUserId = securityUtils.getCurrentUser().getId();

        return transactionHistoryRepository.findByTransactionId(transactionId)
                .filter(history -> isOwnedByCurrentUserOrAdmin(history, currentUserId))
                .map(historyMapper::toHistoryItemDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<HistoryItemDTO> getHistoryById(Long id) {
        historyQueryValidator.validateHistoryId(id);

        Long currentUserId = securityUtils.getCurrentUser().getId();

        return transactionHistoryRepository.findById(id)
                .filter(history -> isOwnedByCurrentUserOrAdmin(history, currentUserId))
                .map(historyMapper::toHistoryItemDTO);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveSuccess(String messageReference, long processingDurationMs) {
        TransactionHistory transaction = buildTransaction(
                messageReference,
                processingDurationMs,
                ConversionStatus.SUCCESS
        );

        auditService.saveAudit(transaction);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void saveFailureInNewTransaction(String messageReference, long processingDurationMs) {
        TransactionHistory transaction = buildTransaction(
                messageReference,
                processingDurationMs,
                ConversionStatus.FAILED
        );

        auditService.saveAudit(transaction);

        log.info("Saved FAILED transaction history record with reference: {}", messageReference);
    }

    private TransactionHistory buildTransaction(String messageReference,
                                                long duration,
                                                ConversionStatus status) {
        return TransactionHistory.builder()
                .transactionId(UUID.randomUUID().toString())
                .conversionStatus(status)
                .requestTimestamp(LocalDateTime.now())
                .messageReference(messageReference)
                .messageType("MT103")
                .processingDurationMs(duration)
                .user(resolveCurrentUser())
                .build();
    }

    private User resolveCurrentUser() {
        try {
            return securityUtils.getCurrentUser().getUser();
        } catch (Exception ex) {
            log.debug("No authenticated user resolved for history persistence");
            return null;
        }
    }

    private boolean isOwnedByCurrentUserOrAdmin(TransactionHistory history, Long currentUserId) {
        if (securityUtils.isAdmin()) return true;

        return history.getUser() != null &&
                history.getUser().getId().equals(currentUserId);
    }

    private HistoryListResponse fetchHistory(Long userId,
                                             LocalDate date,
                                             ConversionStatus status,
                                             int page,
                                             int size) {

        LocalDateTime startTime = date == null ? null : date.atStartOfDay();
        LocalDateTime endTime = date == null ? null : date.plusDays(1).atStartOfDay().minusNanos(1);

        Pageable pageable = PageRequest.of(
                page,
                size,
                Sort.by(Sort.Direction.DESC, "requestTimestamp")
        );

        Page<TransactionHistory> result = transactionHistoryRepository.findAll(
                TransactionHistorySpecifications.userFilters(userId, startTime, endTime, status),
                pageable
        );

        List<HistoryItemDTO> items = historyMapper.toHistoryItemDTOList(result.getContent());

        return HistoryListResponse.builder()
                .items(items)
                .pagination(HistoryListResponse.PaginationDTO.builder()
                        .total(result.getTotalElements())
                        .build())
                .build();
    }
}
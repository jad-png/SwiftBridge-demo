
package com.swiftbridge.orchestrator.service.Impl;

import com.swiftbridge.orchestrator.dto.history.ConversionAuditDTO;
import com.swiftbridge.orchestrator.dto.history.HistoryItemDTO;
import com.swiftbridge.orchestrator.dto.history.HistoryListResponse;
import com.swiftbridge.orchestrator.entity.AppUser;
import com.swiftbridge.orchestrator.entity.ConversionStatus;
import com.swiftbridge.orchestrator.entity.TransactionHistory;
import com.swiftbridge.orchestrator.repository.AppUserRepository;
import com.swiftbridge.orchestrator.repository.TransactionHistoryRepository;
import com.swiftbridge.orchestrator.repository.TransactionHistorySpecifications;
import com.swiftbridge.orchestrator.security.SecurityUtils;
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
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class HistoryServiceImpl implements HistoryService {

    private final TransactionHistoryRepository transactionHistoryRepository;
    private final HistoryQueryValidator historyQueryValidator;
    private final SecurityUtils securityUtils;
    private final AppUserRepository appUserRepository;

            @Override
        @Transactional(readOnly = true)
        public Optional<ConversionAuditDTO> getAuditByTransactionId(String transactionId) {
        historyQueryValidator.validateTransactionId(transactionId);
        Long currentUserId = securityUtils.getCurrentUser().getId();
        return transactionHistoryRepository.findByTransactionId(transactionId)
            .filter(history -> isOwnedByCurrentUserOrAdmin(history, currentUserId))
            .map(history -> {
                List<ConversionAuditDTO.ValidationError> validationErrors = null;
                if (history.getValidationErrors() != null && !history.getValidationErrors().isBlank()) {
                    try {
                        com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                        validationErrors = java.util.Arrays.asList(
                            mapper.readValue(history.getValidationErrors(), ConversionAuditDTO.ValidationError[].class)
                        );
                    } catch (Exception e) {
                        log.warn("Failed to parse validationErrors JSON for transaction {}: {}", history.getTransactionId(), e.getMessage());
                    }
                }
                return ConversionAuditDTO.builder()
                    .transactionId(history.getTransactionId())
                    .username(history.getUser() != null ? history.getUser().getUsername() : null)
                    .conversionStatus(history.getConversionStatus().name())
                    .messageReference(history.getMessageReference())
                    .processingDurationMs(history.getProcessingDurationMs())
                    .requestTimestamp(history.getRequestTimestamp() != null ? history.getRequestTimestamp().toString() : null)
                    .inputData(history.getInputData())
                    .outputContent(history.getOutputContent())
                    .validationErrors(validationErrors)
                    .errorMessage(history.getErrorMessage())
                    .build();
            });
        }
        
    @Override
    @Transactional(readOnly = true)
    public HistoryListResponse findGlobalHistory(LocalDate date, ConversionStatus status, int page, int size) {
        LocalDateTime startTime = date == null ? null : date.atStartOfDay();
        LocalDateTime endTime = date == null ? null : date.plusDays(1).atStartOfDay().minusNanos(1);
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "requestTimestamp"));
        Page<TransactionHistory> result = transactionHistoryRepository.findAll(
            TransactionHistorySpecifications.userFilters(null, startTime, endTime, status),
            pageable
        );
        List<HistoryItemDTO> items = result.getContent().stream()
            .map(this::toHistoryItem)
            .toList();
        return HistoryListResponse.builder()
            .items(items)
            .pagination(HistoryListResponse.PaginationDTO.builder()
                .total(result.getTotalElements())
                .build())
            .build();
    }
    
    @Override
    @Transactional(readOnly = true)
    public HistoryListResponse findFilteredHistory(LocalDate date,
                                                   ConversionStatus status,
                                                   int page,
                                                   int size) {
        historyQueryValidator.validateFilters(date, page, size);

        Long currentUserId = securityUtils.getCurrentUser().getId();
        LocalDateTime startTime = date == null ? null : date.atStartOfDay();
        LocalDateTime endTime = date == null ? null : date.plusDays(1).atStartOfDay().minusNanos(1);
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "requestTimestamp"));

        Page<TransactionHistory> result = transactionHistoryRepository.findAll(
            TransactionHistorySpecifications.userFilters(currentUserId, startTime, endTime, status),
            pageable
        );

        List<HistoryItemDTO> items = result.getContent().stream()
            .map(this::toHistoryItem)
            .toList();

        return HistoryListResponse.builder()
            .items(items)
            .pagination(HistoryListResponse.PaginationDTO.builder()
                .total(result.getTotalElements())
                .build())
            .build();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<HistoryItemDTO> getHistoryByTransactionId(String transactionId) {
        historyQueryValidator.validateTransactionId(transactionId);
        Long currentUserId = securityUtils.getCurrentUser().getId();

        return transactionHistoryRepository.findByTransactionId(transactionId)
            .filter(history -> isOwnedByCurrentUserOrAdmin(history, currentUserId))
            .map(this::toHistoryItem);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<HistoryItemDTO> getHistoryById(Long id) {
        historyQueryValidator.validateHistoryId(id);

        Long currentUserId = securityUtils.getCurrentUser().getId();
        return transactionHistoryRepository.findById(id)
            .filter(history -> isOwnedByCurrentUserOrAdmin(history, currentUserId))
            .map(this::toHistoryItem);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveSuccess(String messageReference,
                            long processingDurationMs) {
        AppUser currentUser = resolveCurrentUser();

        TransactionHistory transaction = TransactionHistory.builder()
            .transactionId(UUID.randomUUID().toString())
            .conversionStatus(ConversionStatus.SUCCESS)
            .requestTimestamp(LocalDateTime.now())
            .messageReference(messageReference)
            .messageType("MT103")
            .processingDurationMs(processingDurationMs)
            .user(currentUser)
            .build();

        transactionHistoryRepository.save(transaction);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void saveFailureInNewTransaction(String messageReference,
                                            long processingDurationMs) {
        AppUser currentUser = resolveCurrentUser();

        TransactionHistory transaction = TransactionHistory.builder()
            .transactionId(UUID.randomUUID().toString())
            .conversionStatus(ConversionStatus.FAILED)
            .requestTimestamp(LocalDateTime.now())
            .messageReference(messageReference)
            .messageType("MT103")
            .processingDurationMs(processingDurationMs)
            .user(currentUser)
            .build();

        transactionHistoryRepository.save(transaction);
        log.info("Saved FAILED transaction history record with reference: {}", messageReference);
    }

    private AppUser resolveCurrentUser() {
        try {
            Long userId = securityUtils.getCurrentUser().getId();
            return appUserRepository.findById(userId).orElse(null);
        } catch (Exception ex) {
            log.debug("No authenticated user resolved for history persistence");
            return null;
        }
    }

    private boolean isOwnedByCurrentUserOrAdmin(TransactionHistory history, Long currentUserId) {
        if (securityUtils.isAdmin()) {
            return true;
        }

        if (history.getUser() == null) {
            return false;
        }

        return history.getUser().getId().equals(currentUserId);
    }

    private HistoryItemDTO toHistoryItem(TransactionHistory transactionHistory) {
        return HistoryItemDTO.builder()
            .transactionId(transactionHistory.getTransactionId())
            .username(transactionHistory.getUser() != null ? transactionHistory.getUser().getUsername() : null)
            .conversionStatus(transactionHistory.getConversionStatus().name())
            .requestTimestamp(transactionHistory.getRequestTimestamp())
            .processingDurationMs(transactionHistory.getProcessingDurationMs())
            .messageType(transactionHistory.getMessageType())
            .messageReference(transactionHistory.getMessageReference())
            .build();
    }
}

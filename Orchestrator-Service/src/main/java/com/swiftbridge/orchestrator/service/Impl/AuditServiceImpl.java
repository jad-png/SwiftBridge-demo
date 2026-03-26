package com.swiftbridge.orchestrator.service.Impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.swiftbridge.orchestrator.entity.TransactionHistory;
import com.swiftbridge.orchestrator.repository.TransactionHistoryRepository;
import com.swiftbridge.orchestrator.service.AuditService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuditServiceImpl implements AuditService {

    private final TransactionHistoryRepository historyRepository;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void saveAudit(TransactionHistory transactionHistory) {
        try {
            historyRepository.save(transactionHistory);
        } catch (Exception e) {
            log.error("Failed to persist audit record", e);
        }
    }

    @Override
    public String serializeValidationErrors(Object obj) {
        try {
            if (obj == null) return null;
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            log.warn("Failed to serialize validation errors", e);
            return obj.toString();
        }
    }
}

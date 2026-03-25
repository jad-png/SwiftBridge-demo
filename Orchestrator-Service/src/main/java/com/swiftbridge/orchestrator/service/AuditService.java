package com.swiftbridge.orchestrator.service;

import com.swiftbridge.orchestrator.entity.TransactionHistory;

public interface AuditService {
    void saveAudit(TransactionHistory transactionHistory);
}

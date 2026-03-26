package com.swiftbridge.orchestrator.service.impl;

import com.swiftbridge.orchestrator.dto.history.ConversionAuditDTO;
import com.swiftbridge.orchestrator.dto.history.HistoryItemDTO;
import com.swiftbridge.orchestrator.dto.history.HistoryListResponse;
import com.swiftbridge.orchestrator.entity.ConversionStatus;
import com.swiftbridge.orchestrator.security.SecurityUtils;
import com.swiftbridge.orchestrator.service.HistoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;

@Service
public class HistoryServiceFacade {
    private final HistoryService historyService;
    private final SecurityUtils securityUtils;

    @Autowired
    public HistoryServiceFacade(HistoryService historyService, SecurityUtils securityUtils) {
        this.historyService = historyService;
        this.securityUtils = securityUtils;
    }

    public HistoryListResponse getConversions(LocalDate date, ConversionStatus status, int page, int size, boolean allUsers) {
        if (allUsers) {
            if (!securityUtils.isAdmin()) {
                throw new org.springframework.security.access.AccessDeniedException("Access denied");
            }
            return historyService.findGlobalHistory(date, status, page, size);
        } else {
            return historyService.findFilteredHistory(date, status, page, size);
        }
    }

    public ConversionAuditDTO getConversionByTransactionId(String txnId) {
        return historyService.getAuditByTransactionId(txnId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Conversion not found"));
    }

    public HistoryItemDTO getConversionById(Long id) {
        return historyService.getHistoryById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "History item not found"));
    }
}

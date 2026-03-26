package com.swiftbridge.orchestrator.controller;

import com.swiftbridge.orchestrator.dto.history.HistoryItemDTO;
import com.swiftbridge.orchestrator.dto.history.HistoryListResponse;
import com.swiftbridge.orchestrator.dto.history.ConversionAuditDTO;
import com.swiftbridge.orchestrator.entity.ConversionStatus;
import com.swiftbridge.orchestrator.service.impl.HistoryServiceFacade;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping({"/api/conversions", "/api/history"})

@RequiredArgsConstructor
public class ConversionHistoryController {

    private final HistoryServiceFacade historyServiceFacade;

    @GetMapping
    public ResponseEntity<HistoryListResponse> getConversions(
        @RequestParam(name = "date", required = false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
        @RequestParam(name = "status", required = false) ConversionStatus status,
        @RequestParam(name = "page", defaultValue = "0") int page,
        @RequestParam(name = "size", defaultValue = "20") int size,
        @RequestParam(name = "allUsers", required = false, defaultValue = "false") boolean allUsers
    ) {
        return ResponseEntity.ok(historyServiceFacade.getConversions(date, status, page, size, allUsers));
    }

    @GetMapping("/{txnId}")
    public ResponseEntity<ConversionAuditDTO> getConversionByTransactionId(@PathVariable("txnId") String txnId) {
        return ResponseEntity.ok(historyServiceFacade.getConversionByTransactionId(txnId));
    }

    @GetMapping("/id/{id}")
    public ResponseEntity<HistoryItemDTO> getConversionById(@PathVariable("id") Long id) {
        return ResponseEntity.ok(historyServiceFacade.getConversionById(id));
    }
}

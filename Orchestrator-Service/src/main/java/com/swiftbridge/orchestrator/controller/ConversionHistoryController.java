package com.swiftbridge.orchestrator.controller;

import com.swiftbridge.orchestrator.dto.HistoryItemDTO;
import com.swiftbridge.orchestrator.dto.HistoryListResponse;
import com.swiftbridge.orchestrator.entity.ConversionStatus;
import com.swiftbridge.orchestrator.service.HistoryService;
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

    private final HistoryService historyService;

    @GetMapping
    public ResponseEntity<HistoryListResponse> getConversions(
        @RequestParam(name = "date", required = false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
        @RequestParam(name = "status", required = false) ConversionStatus status,
        @RequestParam(name = "page", defaultValue = "0") int page,
        @RequestParam(name = "size", defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(historyService.findFilteredHistory(date, status, page, size));
    }

    @GetMapping("/{txnId}")
    public ResponseEntity<HistoryItemDTO> getConversionByTransactionId(@PathVariable("txnId") String txnId) {
        return historyService.getHistoryByTransactionId(txnId)
            .map(ResponseEntity::ok)
            .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/id/{id}")
    public ResponseEntity<HistoryItemDTO> getConversionById(@PathVariable("id") Long id) {
        return historyService.getHistoryById(id)
            .map(ResponseEntity::ok)
            .orElseGet(() -> ResponseEntity.notFound().build());
    }
}

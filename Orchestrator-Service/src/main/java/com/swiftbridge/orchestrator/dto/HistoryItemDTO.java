package com.swiftbridge.orchestrator.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class HistoryItemDTO {

    private String transactionId;
    private String conversionStatus;
    private LocalDateTime requestTimestamp;
    private Long processingDurationMs;
    private String messageType;
    private String messageReference;
}

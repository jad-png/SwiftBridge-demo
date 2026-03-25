package com.swiftbridge.orchestrator.dto.history;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class HistoryItemDTO {

    private String transactionId;
    private String username;
    private String conversionStatus;
    private LocalDateTime requestTimestamp;
    private Long processingDurationMs;
    private String messageType;
    private String messageReference;
}

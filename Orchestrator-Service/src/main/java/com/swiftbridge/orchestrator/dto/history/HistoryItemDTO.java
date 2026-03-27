package com.swiftbridge.orchestrator.dto.history;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
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

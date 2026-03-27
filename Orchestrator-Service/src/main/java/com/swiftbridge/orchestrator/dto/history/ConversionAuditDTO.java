package com.swiftbridge.orchestrator.dto.history;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConversionAuditDTO {
    private String transactionId;
    private String username;
    private String inputData;
    private String outputContent;
    private List<ValidationError> validationErrors;
    private String errorMessage;
    private String conversionStatus;
    private String messageReference;
    private Long processingDurationMs;
    private String requestTimestamp;

    @Getter
    @Builder
    public static class ValidationError {
        private String message;
        private String path;
    }
}

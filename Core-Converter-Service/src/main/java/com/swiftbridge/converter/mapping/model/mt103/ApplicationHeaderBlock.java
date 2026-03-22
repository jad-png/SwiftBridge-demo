package com.swiftbridge.converter.mapping.model.mt103;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Objects;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApplicationHeaderBlock {

    private String inputOutputId;
    private String messageType;
    private String receiverLogicalTerminal;
    private String priority;

    public void validateFormat() {
        Objects.requireNonNull(inputOutputId, "inputOutputId is required");
        if (messageType == null || messageType.length() != 3) {
            throw new IllegalArgumentException("messageType must be 3 characters");
        }
        if (receiverLogicalTerminal == null || receiverLogicalTerminal.length() != 12) {
            throw new IllegalArgumentException("receiverLogicalTerminal must be 12 characters");
        }
        if (priority == null || priority.isBlank()) {
            throw new IllegalArgumentException("priority is required");
        }
    }
}

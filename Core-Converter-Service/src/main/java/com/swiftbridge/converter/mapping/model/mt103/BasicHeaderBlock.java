package com.swiftbridge.converter.mapping.model.mt103;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Objects;

@Getter
@Builder(toBuilder = true)
@NoArgsConstructor(force = true)
@AllArgsConstructor
public class BasicHeaderBlock {

    private static final int LOGICAL_TERMINAL_LENGTH = 12;
    private static final int SESSION_NUMBER_LENGTH = 4;
    private static final int SEQUENCE_NUMBER_LENGTH = 6;

    private String applicationId;
    private String serviceId;
    private String logicalTerminal;
    private String sessionNumber;
    private String sequenceNumber;

    public void validateFormat() {
        Objects.requireNonNull(applicationId, "applicationId is required");
        Objects.requireNonNull(serviceId, "serviceId is required");
        if (logicalTerminal == null || logicalTerminal.length() != LOGICAL_TERMINAL_LENGTH) {
            throw new IllegalArgumentException("logicalTerminal must be 12 characters");
        }
        if (sessionNumber == null || sessionNumber.length() != SESSION_NUMBER_LENGTH) {
            throw new IllegalArgumentException("sessionNumber must be 4 characters");
        }
        if (sequenceNumber == null || sequenceNumber.length() != SEQUENCE_NUMBER_LENGTH) {
            throw new IllegalArgumentException("sequenceNumber must be 6 characters");
        }
    }
}

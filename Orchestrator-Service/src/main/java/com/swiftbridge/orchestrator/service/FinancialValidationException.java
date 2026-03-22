package com.swiftbridge.orchestrator.service;

import com.swiftbridge.orchestrator.exception.ValidationFailedException;
import lombok.Getter;

@Getter
public class FinancialValidationException extends ValidationFailedException {

    private final String instructionId;
    private final String uetr;

    public FinancialValidationException(String errorCode, String message, String instructionId, String uetr) {
        super(errorCode, message);
        this.instructionId = instructionId;
        this.uetr = uetr;
    }
}

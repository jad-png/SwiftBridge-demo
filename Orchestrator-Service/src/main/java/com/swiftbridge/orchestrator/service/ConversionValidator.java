package com.swiftbridge.orchestrator.service;

public interface ConversionValidator {

    ValidationResult validate(String mt103Message);

    record ValidationResult(String instructionId, String uetr) {
    }
}

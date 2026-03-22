package com.swiftbridge.orchestrator.exception;

import lombok.Getter;

@Getter
public class ValidationFailedException extends RuntimeException {

    private final String errorCode;

    public ValidationFailedException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public ValidationFailedException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }
}

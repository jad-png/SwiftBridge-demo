package com.swiftbridge.converter.exception;

import lombok.Getter;

@Getter
public class ConversionFailedException extends RuntimeException {

    private final String errorCode;

    public ConversionFailedException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public ConversionFailedException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }
}

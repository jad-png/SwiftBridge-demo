package com.swiftbridge.converter.exception;

import lombok.Getter;

@Getter
public class SwiftMappingException extends RuntimeException {

    private final SwiftErrorCode errorCode;

    public SwiftMappingException(SwiftErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    public SwiftMappingException(SwiftErrorCode errorCode, String additionalContext) {
        super(formatMessage(errorCode.getMessage(), additionalContext));
        this.errorCode = errorCode;
    }

    public SwiftMappingException(SwiftErrorCode errorCode, Throwable cause) {
        super(errorCode.getMessage(), cause);
        this.errorCode = errorCode;
    }

    public SwiftMappingException(SwiftErrorCode errorCode, String additionalContext, Throwable cause) {
        super(formatMessage(errorCode.getMessage(), additionalContext), cause);
        this.errorCode = errorCode;
    }

    private static String formatMessage(String baseMessage, String context) {
        if (context == null || context.isEmpty()) {
            return baseMessage;
        }
        return baseMessage + " (" + context + ")";
    }

    public String getErrorCodeId() {
        return this.errorCode.getCode();
    }

    public SwiftMappingException withContext(String additionalContext) {
        return new SwiftMappingException(this.errorCode, additionalContext, this);
    }
}

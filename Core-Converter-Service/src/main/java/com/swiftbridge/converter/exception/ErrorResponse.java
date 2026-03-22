package com.swiftbridge.converter.exception;

import com.swiftbridge.converter.dto.ErrorResponseDTO;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ErrorResponse {

    private String timestamp;
    private int status;
    private String error;
    private String errorCode;
    private String message;
    private String path;

    public static ErrorResponse of(String timestamp, int status, String error, String errorCode, String message, String path) {
        return ErrorResponse.builder()
            .timestamp(timestamp)
            .status(status)
            .error(error)
            .errorCode(errorCode)
            .message(message)
            .path(path)
            .build();
    }

    public ErrorResponseDTO toDto() {
        return ErrorResponseDTO.builder()
            .timestamp(timestamp)
            .errorCode(errorCode)
            .message(message)
            .status(status)
            .path(path)
            .build();
    }
}

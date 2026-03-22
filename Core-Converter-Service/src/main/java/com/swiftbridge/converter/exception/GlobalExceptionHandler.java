package com.swiftbridge.converter.exception;

import com.swiftbridge.converter.dto.ErrorResponseDTO;
import com.swiftbridge.converter.utils.CorrelationIdUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

        private static final String CLIENT_ERROR_CODE = "SB-4000";
        private static final String SERVER_ERROR_CODE = "SB-5000";
        private static final String CLIENT_ERROR_MESSAGE = "Invalid request. Please check your input and try again.";
        private static final String SERVER_ERROR_MESSAGE = "An internal error occurred. Please try again later or contact support.";

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponseDTO> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
            HttpServletRequest request) {
                String correlationId = extractCorrelationId(request);
        String message = "Validation failed";
        FieldError firstError = ex.getBindingResult().getFieldError();
        if (firstError != null && firstError.getDefaultMessage() != null) {
            message = firstError.getDefaultMessage();
        }

        log.error("[Correlation-ID: {}] Request validation failed at {} {} | Details: {} | Stack:",
                correlationId,
                requestSignature(request),
                message,
                ex);

        return buildSecureResponse(HttpStatus.BAD_REQUEST, CLIENT_ERROR_CODE, message, request);
    }

    @ExceptionHandler(FileValidationException.class)
    public ResponseEntity<ErrorResponseDTO> handleFileValidation(FileValidationException ex,
            HttpServletRequest request) {
                String correlationId = extractCorrelationId(request);

        log.error("[Correlation-ID: {}] File validation failed at {} {} | Reason: {} | Stack:",
                correlationId,
                requestSignature(request),
                ex.getMessage(),
                ex);

        return buildSecureResponse(HttpStatus.BAD_REQUEST, CLIENT_ERROR_CODE,
                "File validation failed. Please ensure the file is a valid XML document.", request);
    }

    @ExceptionHandler(SwiftMappingException.class)
    public ResponseEntity<ErrorResponseDTO> handleSwiftMapping(SwiftMappingException ex,
            HttpServletRequest request) {
                String correlationId = extractCorrelationId(request);
        SwiftErrorCode errorCode = ex.getErrorCode();

        log.error("[Correlation-ID: {}] Swift mapping failed at {} {} | Error Code: {} | Error Message: {} | Stack:",
                correlationId,
                requestSignature(request),
                errorCode.getCode(),
                errorCode.getMessage(),
                ex);

        return buildSecureResponse(
                HttpStatus.BAD_REQUEST,
                errorCode.getCode(),
                errorCode.getMessage(),
                request);
    }

    @ExceptionHandler(ConversionFailedException.class)
    public ResponseEntity<ErrorResponseDTO> handleConversionFailed(ConversionFailedException ex,
            HttpServletRequest request) {
                String correlationId = extractCorrelationId(request);
        boolean isClientError = isClientErrorCode(ex.getErrorCode());
        HttpStatus status = isClientError ? HttpStatus.BAD_REQUEST : HttpStatus.INTERNAL_SERVER_ERROR;

        String clientMessage = isClientError
                ? "Conversion failed. Please verify your input data."
                : "An internal error occurred. Please try again later or contact support.";

        log.error(
                "[Correlation-ID: {}] Conversion failed at {} {} | Error Code: {} | Status: {} | Message: {} | Stack:",
                correlationId,
                requestSignature(request),
                ex.getErrorCode(),
                status.value(),
                ex.getMessage(),
                ex);

        return buildSecureResponse(status, ex.getErrorCode(), clientMessage, request);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponseDTO> handleIllegalArgument(IllegalArgumentException ex,
            HttpServletRequest request) {
                String correlationId = extractCorrelationId(request);

        log.error("[Correlation-ID: {}] Illegal argument at {} {} | Message: {} | Stack:",
                correlationId,
                requestSignature(request),
                ex.getMessage(),
                ex);

        return buildSecureResponse(HttpStatus.BAD_REQUEST, CLIENT_ERROR_CODE,
                CLIENT_ERROR_MESSAGE, request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponseDTO> handleGenericException(Exception ex, HttpServletRequest request) {
                String correlationId = extractCorrelationId(request);

        log.error("[Correlation-ID: {}] UNHANDLED EXCEPTION at {} {} | Exception Type: {} | Message: {} | Stack:",
                correlationId,
                requestSignature(request),
                ex.getClass().getName(),
                ex.getMessage(),
                ex);

        return buildSecureResponse(HttpStatus.INTERNAL_SERVER_ERROR, SERVER_ERROR_CODE,
                SERVER_ERROR_MESSAGE, request);
    }

    private ResponseEntity<ErrorResponseDTO> buildSecureResponse(HttpStatus status,
            String errorCode,
            String message,
            HttpServletRequest request) {
        String correlationId = extractCorrelationId(request);
        ErrorResponse payload = ErrorResponse.of(
                Instant.now().toString(),
                status.value(),
                status.getReasonPhrase(),
                errorCode,
                message,
                request.getRequestURI());

        ErrorResponseDTO dto = payload.toDto().toBuilder()
                .correlationId(correlationId)
                .build();
        dto.validate();

        return ResponseEntity.status(status).body(dto);
    }

    private boolean isClientErrorCode(String errorCode) {
        return errorCode != null && errorCode.startsWith("SB-4");
    }

        private String extractCorrelationId(HttpServletRequest request) {
                return CorrelationIdUtil.extractOrGenerateCorrelationId(request);
        }

        private String requestSignature(HttpServletRequest request) {
                return request.getMethod() + " " + request.getRequestURI();
        }
}

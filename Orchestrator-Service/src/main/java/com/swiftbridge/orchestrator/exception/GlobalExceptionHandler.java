package com.swiftbridge.orchestrator.exception;

import com.swiftbridge.orchestrator.dto.ErrorResponseDTO;
import com.swiftbridge.orchestrator.utils.CorrelationIdUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponseDTO> handleAuthenticationException(AuthenticationException ex,
                                                                          HttpServletRequest request) {
        String correlationId = CorrelationIdUtil.extractOrGenerateCorrelationId(request);

        log.error("[Correlation-ID: {}] Authentication failed at {} {} | Details: {} | Stack:",
            correlationId,
            request.getMethod(),
            request.getRequestURI(),
            ex.getMessage(),
            ex);

        return buildSecureResponse(HttpStatus.UNAUTHORIZED, "SB-6001",
            "Unauthorized. Please provide valid credentials.");
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponseDTO> handleAccessDeniedException(AccessDeniedException ex,
                                                                        HttpServletRequest request) {
        String correlationId = CorrelationIdUtil.extractOrGenerateCorrelationId(request);

        log.error("[Correlation-ID: {}] Access denied at {} {} | Message: {} | Stack:",
            correlationId,
            request.getMethod(),
            request.getRequestURI(),
            ex.getMessage(),
            ex);

        return buildSecureResponse(HttpStatus.FORBIDDEN, "SB-6003",
            "Access denied. You do not have permission to access this resource.");
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponseDTO> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
                                                                         HttpServletRequest request) {
        String correlationId = CorrelationIdUtil.extractOrGenerateCorrelationId(request);
        String message = "Validation failed";
        FieldError firstError = ex.getBindingResult().getFieldError();
        if (firstError != null && firstError.getDefaultMessage() != null) {
            message = firstError.getDefaultMessage();
        }

        log.error("[Correlation-ID: {}] Request validation failed at {} {} | Details: {} | Stack:",
            correlationId,
            request.getMethod(),
            request.getRequestURI(),
            message,
            ex);

        return buildSecureResponse(HttpStatus.BAD_REQUEST, "SB-4000", message);
    }

    @ExceptionHandler(ConversionFailedException.class)
    public ResponseEntity<ErrorResponseDTO> handleConversionFailed(ConversionFailedException ex,
                                                                   HttpServletRequest request) {
        String correlationId = CorrelationIdUtil.extractOrGenerateCorrelationId(request);
        boolean isClientError = isClientErrorCode(ex.getErrorCode());
        HttpStatus status = isClientError ? HttpStatus.BAD_REQUEST : HttpStatus.INTERNAL_SERVER_ERROR;

        String clientMessage = isClientError
            ? "Conversion failed. Please verify your XML document."
            : "An internal error occurred. Please try again later or contact support.";

        log.error("[Correlation-ID: {}] Conversion failed at {} {} | Error Code: {} | Status: {} | Message: {} | Stack:",
            correlationId,
            request.getMethod(),
            request.getRequestURI(),
            ex.getErrorCode(),
            status.value(),
            ex.getMessage(),
            ex);

        return buildSecureResponse(status, ex.getErrorCode(), clientMessage);
    }

    @ExceptionHandler(ValidationFailedException.class)
    public ResponseEntity<ErrorResponseDTO> handleValidationFailed(ValidationFailedException ex,
                                                                   HttpServletRequest request) {
        String correlationId = CorrelationIdUtil.extractOrGenerateCorrelationId(request);

        log.error("[Correlation-ID: {}] Domain validation failed at {} {} | Error Code: {} | Message: {} | Stack:",
            correlationId,
            request.getMethod(),
            request.getRequestURI(),
            ex.getErrorCode(),
            ex.getMessage(),
            ex);

        return buildSecureResponse(HttpStatus.BAD_REQUEST, ex.getErrorCode(), ex.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponseDTO> handleIllegalArgument(IllegalArgumentException ex,
                                                                  HttpServletRequest request) {
        String correlationId = CorrelationIdUtil.extractOrGenerateCorrelationId(request);

        log.error("[Correlation-ID: {}] Illegal argument at {} {} | Message: {} | Stack:",
            correlationId,
            request.getMethod(),
            request.getRequestURI(),
            ex.getMessage(),
            ex);

        return buildSecureResponse(HttpStatus.BAD_REQUEST, "SB-4000",
            "Invalid request. Please check your input and try again.");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponseDTO> handleGenericException(Exception ex, HttpServletRequest request) {
        String correlationId = CorrelationIdUtil.extractOrGenerateCorrelationId(request);

        log.error("[Correlation-ID: {}] UNHANDLED EXCEPTION at {} {} | Exception Type: {} | Message: {} | Stack:",
            correlationId,
            request.getMethod(),
            request.getRequestURI(),
            ex.getClass().getName(),
            ex.getMessage(),
            ex);

        return buildSecureResponse(HttpStatus.INTERNAL_SERVER_ERROR, "SB-5000",
            "An internal error occurred. Please try again later or contact support.");
    }

    private ResponseEntity<ErrorResponseDTO> buildSecureResponse(HttpStatus status,
                                                                 String errorCode,
                                                                 String message) {
        ErrorResponseDTO payload = ErrorResponseDTO.builder()
            .timestamp(Instant.now().toString())
            .errorCode(errorCode)
            .message(message)

            .build();

        return ResponseEntity.status(status).body(payload);
    }

    private boolean isClientErrorCode(String errorCode) {
        return errorCode != null && errorCode.startsWith("SB-4");
    }
}

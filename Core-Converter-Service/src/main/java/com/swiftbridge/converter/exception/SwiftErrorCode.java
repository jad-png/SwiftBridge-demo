package com.swiftbridge.converter.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum SwiftErrorCode {

    ERR_MAPPING_AMOUNT_MISSING(
            "SB-4001",
            "Amount field is missing from the pacs.008 message",
            HttpStatus.BAD_REQUEST),

    ERR_INVALID_CURRENCY(
            "SB-4002",
            "Currency code is invalid or missing from the amount field",
            HttpStatus.BAD_REQUEST),

    ERR_INVALID_XML_STRUCTURE(
            "SB-4003",
            "XML structure is invalid or does not conform to pacs.008 specification",
            HttpStatus.BAD_REQUEST),

    ERR_DEBTOR_NAME_MISSING(
            "SB-4004",
            "Debtor name is missing from the pacs.008 message",
            HttpStatus.BAD_REQUEST),

    ERR_CREDITOR_NAME_MISSING(
            "SB-4005",
            "Creditor name is missing from the pacs.008 message",
            HttpStatus.BAD_REQUEST),

    ERR_INVALID_SETTLEMENT_DATE(
            "SB-4006",
            "Settlement date is missing or invalid (expected YYYY-MM-DD format)",
            HttpStatus.BAD_REQUEST),

    ERR_FILE_VALIDATION_FAILED(
            "SB-4007",
            "File validation failed: invalid format, size, or content",
            HttpStatus.BAD_REQUEST),

    ERR_REQUEST_VALIDATION_FAILED(
            "SB-4008",
            "Request validation failed: invalid or missing required fields",
            HttpStatus.BAD_REQUEST),

    ERR_CORE_SERVICE_UNAVAILABLE(
            "SB-5001",
            "Core Converter service is unavailable or not responding",
            HttpStatus.SERVICE_UNAVAILABLE),

    ERR_XML_PARSING_FAILED(
            "SB-5002",
            "Failed to parse XML document: internal parsing error",
            HttpStatus.INTERNAL_SERVER_ERROR),

    ERR_MT103_CONVERSION_FAILED(
            "SB-5003",
            "Failed to convert pacs.008 to MT103 format",
            HttpStatus.INTERNAL_SERVER_ERROR),

    ERR_DATABASE_FAILURE(
            "SB-5004",
            "Database operation failed: unable to process request",
            HttpStatus.INTERNAL_SERVER_ERROR),

    ERR_INTERNAL_SERVER_ERROR(
            "SB-5000",
            "An unexpected internal error occurred. Please contact support.",
            HttpStatus.INTERNAL_SERVER_ERROR);

    private final String code;
    private final String message;
    private final HttpStatus httpStatus;

    SwiftErrorCode(String code, String message, HttpStatus httpStatus) {
        this.code = code;
        this.message = message;
        this.httpStatus = httpStatus;
    }

    public int getHttpStatusCode() {
        return this.httpStatus.value();
    }

        public boolean isClientError() {
                return this.httpStatus.is4xxClientError();
        }

        public boolean isServerError() {
                return this.httpStatus.is5xxServerError();
        }
}

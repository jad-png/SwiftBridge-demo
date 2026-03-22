package com.swiftbridge.converter.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Error Code Mapping Tests")
class ErrorCodeMappingTest {

    @Test
    @DisplayName("SB-4001 maps to 400 Bad Request for missing amount")
    void testAmountMissingErrorCode() {

        SwiftErrorCode errorCode = SwiftErrorCode.ERR_MAPPING_AMOUNT_MISSING;

        assertCodeAndStatus(errorCode, "SB-4001", HttpStatus.BAD_REQUEST);
        assertTrue(errorCode.getMessage().contains("Amount"));
    }

    @Test
    @DisplayName("SB-4002 maps to 400 Bad Request for invalid currency")
    void testCurrencyErrorCode() {

        SwiftErrorCode errorCode = SwiftErrorCode.ERR_INVALID_CURRENCY;

        assertCodeAndStatus(errorCode, "SB-4002", HttpStatus.BAD_REQUEST);
        assertTrue(errorCode.getMessage().contains("Currency"));
    }

    @Test
    @DisplayName("SB-4003 maps to 400 Bad Request for invalid XML")
    void testInvalidXmlErrorCode() {

        SwiftErrorCode errorCode = SwiftErrorCode.ERR_INVALID_XML_STRUCTURE;

        assertCodeAndStatus(errorCode, "SB-4003", HttpStatus.BAD_REQUEST);
        assertTrue(errorCode.getMessage().contains("XML"));
    }

    @Test
    @DisplayName("SB-5000 maps to 500 Internal Server Error")
    void testInternalServerErrorCode() {

        SwiftErrorCode errorCode = SwiftErrorCode.ERR_INTERNAL_SERVER_ERROR;

        assertCodeAndStatus(errorCode, "SB-5000", HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Test
    @DisplayName("SB-5001 maps to 503 Service Unavailable for core service")
    void testCoreServiceUnavailableErrorCode() {

        SwiftErrorCode errorCode = SwiftErrorCode.ERR_CORE_SERVICE_UNAVAILABLE;

        assertCodeAndStatus(errorCode, "SB-5001", HttpStatus.SERVICE_UNAVAILABLE);
        assertTrue(errorCode.getMessage().contains("Core"));
    }

    @Test
    @DisplayName("All error codes have valid code format (SB-XXXX)")
    void testAllErrorCodesHaveValidFormat() {

        for (SwiftErrorCode code : SwiftErrorCode.values()) {
            assertTrue(code.getCode().startsWith("SB-"),
                "Error code " + code.name() + " should start with SB-");
            assertTrue(code.getCode().matches("SB-\\d{4}"),
                "Error code " + code.getCode() + " should match SB-XXXX format");
            assertNotNull(code.getMessage(),
                "Error code " + code.getCode() + " should have a message");
            assertNotNull(code.getHttpStatus(),
                "Error code " + code.getCode() + " should have HTTP status");
        }
    }

    @Test
    @DisplayName("Client error codes (4XXX) map to 4XX HTTP status")
    void testClientErrorCodesMapTo4XX() {

        assertEquals(HttpStatus.BAD_REQUEST, SwiftErrorCode.ERR_MAPPING_AMOUNT_MISSING.getHttpStatus());
        assertEquals(HttpStatus.BAD_REQUEST, SwiftErrorCode.ERR_INVALID_CURRENCY.getHttpStatus());
        assertEquals(HttpStatus.BAD_REQUEST, SwiftErrorCode.ERR_INVALID_XML_STRUCTURE.getHttpStatus());
        assertEquals(HttpStatus.BAD_REQUEST, SwiftErrorCode.ERR_DEBTOR_NAME_MISSING.getHttpStatus());
    }

    @Test
    @DisplayName("Server error codes (5XXX) map to 5XX HTTP status")
    void testServerErrorCodesMapTo5XX() {

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, SwiftErrorCode.ERR_INTERNAL_SERVER_ERROR.getHttpStatus());
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, SwiftErrorCode.ERR_CORE_SERVICE_UNAVAILABLE.getHttpStatus());
    }

    @Test
    @DisplayName("SwiftMappingException wraps SwiftErrorCode correctly")
    void testSwiftMappingExceptionWrapsErrorCode() {

        SwiftErrorCode errorCode = SwiftErrorCode.ERR_INVALID_CURRENCY;
        String context = "Amount element is malformed";

        SwiftMappingException exception = new SwiftMappingException(errorCode, context);

        assertEquals(errorCode, exception.getErrorCode());
        assertTrue(exception.getMessage().contains(context));
    }

    private void assertCodeAndStatus(SwiftErrorCode errorCode, String expectedCode, HttpStatus expectedStatus) {
        assertEquals(expectedCode, errorCode.getCode());
        assertEquals(expectedStatus, errorCode.getHttpStatus());
    }
}

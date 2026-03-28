package com.swiftbridge.orchestrator.service.Impl;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.swiftbridge.orchestrator.exception.ConversionFailedException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("CoreConversionClientImpl Unit Tests")
class CoreConversionClientTest {

    @Mock
    private RestTemplate restTemplate;

    private ObjectMapper objectMapper;

    private CoreConversionClientImpl client;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        client = new CoreConversionClientImpl(restTemplate, objectMapper);
    }

    @Test
    @DisplayName("Should parse and propagate specific error message from Core Converter")
    void testConvertPropagatesError() throws Exception {
        // Read valid XML from test resources
        String xmlContent;
        try (java.io.InputStream is = getClass().getClassLoader().getResourceAsStream("valid-pacs008.xml")) {
            if (is == null) throw new RuntimeException("Test XML file not found");
            xmlContent = new String(is.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
        }


        String coreErrorJson = "{\"errorCode\": \"SB-4001\", \"message\": \"Amount field is missing\"}";
        org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
        headers.set(org.springframework.http.HttpHeaders.CONTENT_TYPE, "application/json");
        HttpClientErrorException ex = HttpClientErrorException.create(
            HttpStatus.BAD_REQUEST,
            "Bad Request",
            headers,
            coreErrorJson.getBytes(StandardCharsets.UTF_8),
            StandardCharsets.UTF_8);

        when(restTemplate.postForEntity(any(String.class), any(),
                eq(com.swiftbridge.orchestrator.dto.conversion.ConversionResponse.class)))
                .thenThrow(ex);

        ConversionFailedException thrown = assertThrows(ConversionFailedException.class, () -> {
            client.convert(xmlContent, "valid-pacs008.xml");
        });

        assertEquals("SB-4001", thrown.getErrorCode());
        assertEquals("Amount field is missing", thrown.getMessage());
    }
}

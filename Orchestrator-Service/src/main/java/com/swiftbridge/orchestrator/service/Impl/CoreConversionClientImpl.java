package com.swiftbridge.orchestrator.service.Impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.swiftbridge.orchestrator.dto.conversion.ConversionResponse;
import com.swiftbridge.orchestrator.dto.error.ErrorResponseDTO;
import com.swiftbridge.orchestrator.exception.ConversionFailedException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import com.swiftbridge.orchestrator.service.CoreConverterClient;

@Service
@RequiredArgsConstructor
@Slf4j
public class CoreConversionClientImpl implements CoreConverterClient {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${converter.service.url:http://localhost:8081}")
    private String converterServiceUrl;

    @Value("${converter.service.endpoint:/api/v1/internal/convert}")
    private String converterEndpoint;

    @Override
    public ConversionResponse convert(String xmlContent, String filename) {
        String url = converterServiceUrl + converterEndpoint;

        // Ensure ObjectMapper ignores unknown properties (for error parsing)
        objectMapper.configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);

            LinkedMultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            ByteArrayResource contentsAsResource = new ByteArrayResource(xmlContent.getBytes()) {
                @Override
                public String getFilename() {
                    return filename;
                }
            };
            body.add("file", contentsAsResource);

            HttpEntity<LinkedMultiValueMap<String, Object>> request = new HttpEntity<>(body, headers);
            log.debug("Calling converter service at: {}", url);

            ResponseEntity<ConversionResponse> response = restTemplate.postForEntity(url, request,
                    ConversionResponse.class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return response.getBody();
            }

            throw new ConversionFailedException(
                    "ERR_CORE_NON_SUCCESS",
                    "Converter service returned non-success status: " + response.getStatusCode());
        } catch (HttpStatusCodeException ex) {
            String errorBody = ex.getResponseBodyAsString();
            log.warn("Converter service returned error: {} | Body: {}", ex.getStatusCode(), errorBody);

            if (errorBody != null && errorBody.trim().startsWith("{")) {
                try {
                    ErrorResponseDTO errorResponse = objectMapper.readValue(errorBody, ErrorResponseDTO.class);
                    System.out.println("DEBUG: Parsed ErrorResponseDTO: errorCode=" + errorResponse.getErrorCode() + ", message=" + errorResponse.getMessage());
                    if (errorResponse != null && errorResponse.getMessage() != null) {
                        throw new ConversionFailedException(
                                errorResponse.getErrorCode() != null ? errorResponse.getErrorCode() : "ERR_CORE_CONVERSION",
                                errorResponse.getMessage());
                    }
                } catch (com.fasterxml.jackson.core.JsonProcessingException parseEx) {
                    log.warn("Failed to parse error response body from CORE: {} | Original Body: {}", parseEx.getMessage(),
                            errorBody, parseEx);
                    parseEx.printStackTrace();
                }
            }

            throw new ConversionFailedException(
                    "ERR_CORE_UNAVAILABLE",
                    "Converter service error: " + ex.getMessage(),
                    ex);
        } catch (RestClientException ex) {
            throw new ConversionFailedException(
                    "ERR_CORE_UNAVAILABLE",
                    "Converter service unavailable: " + ex.getMessage(),
                    ex);
        }
    }
}

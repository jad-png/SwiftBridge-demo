package com.swiftbridge.orchestrator.service.Impl;

import com.swiftbridge.orchestrator.dto.ConversionResponse;
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
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import com.swiftbridge.orchestrator.service.CoreConverterClient;

@Service
@RequiredArgsConstructor
@Slf4j
public class CoreConversionClientImpl implements CoreConverterClient {

    private final RestTemplate restTemplate;

    @Value("${converter.service.url:http://localhost:8081}")
    private String converterServiceUrl;

    @Value("${converter.service.endpoint:/api/v1/internal/convert}")
    private String converterEndpoint;

    @Override
    public ConversionResponse convert(String xmlContent, String filename) {
        String url = converterServiceUrl + converterEndpoint;

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

            ResponseEntity<ConversionResponse> response = restTemplate.postForEntity(url, request, ConversionResponse.class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return response.getBody();
            }

            throw new ConversionFailedException(
                "ERR_CORE_NON_SUCCESS",
                "Converter service returned non-success status: " + response.getStatusCode()
            );
        } catch (RestClientException ex) {
            throw new ConversionFailedException(
                "ERR_CORE_UNAVAILABLE",
                "Converter service unavailable: " + ex.getMessage(),
                ex
            );
        }
    }
}

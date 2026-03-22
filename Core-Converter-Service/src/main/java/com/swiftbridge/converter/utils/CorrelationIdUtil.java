package com.swiftbridge.converter.utils;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.MDC;

import java.util.UUID;

public class CorrelationIdUtil {

    public static final String CORRELATION_ID_HEADER = "X-Correlation-ID";
    public static final String MDC_CORRELATION_ID = "correlationId";

    private CorrelationIdUtil() {
        throw new IllegalStateException("Utility class");
    }

    public static String extractOrGenerateCorrelationId(HttpServletRequest request) {
        String correlationId = normalizeCorrelationId(request.getHeader(CORRELATION_ID_HEADER));

        if (correlationId.isBlank()) {
            correlationId = UUID.randomUUID().toString();
        }

        MDC.put(MDC_CORRELATION_ID, correlationId);

        return correlationId;
    }

    public static void setCorrelationId(String correlationId) {
        String normalizedCorrelationId = normalizeCorrelationId(correlationId);
        if (!normalizedCorrelationId.isBlank()) {
            MDC.put(MDC_CORRELATION_ID, normalizedCorrelationId);
        }
    }

    public static String getCorrelationId() {
        return MDC.get(MDC_CORRELATION_ID);
    }

    public static void clearCorrelationId() {
        MDC.remove(MDC_CORRELATION_ID);
    }

    private static String normalizeCorrelationId(String correlationId) {
        return correlationId == null ? "" : correlationId.trim();
    }
}

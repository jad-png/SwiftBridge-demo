package com.swiftbridge.orchestrator.service.Impl;

import com.swiftbridge.orchestrator.dto.conversion.ConversionResponse;
import com.swiftbridge.orchestrator.exception.ConversionFailedException;
import com.swiftbridge.orchestrator.service.ConversionService;
import com.swiftbridge.orchestrator.service.ConversionValidator;
import com.swiftbridge.orchestrator.service.CoreConverterClient;
import com.swiftbridge.orchestrator.service.FinancialValidationException;
import com.swiftbridge.orchestrator.service.HistoryService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ConversionServiceImpl implements ConversionService {

    private static final java.util.regex.Pattern TAG_32A_PATTERN =
        java.util.regex.Pattern.compile("(?m)^:32A:(\\d{6})([A-Z]{3})([0-9,]+(?:\\.[0-9]+)?)$");

    private final CoreConverterClient coreConverterClient;
    private final ConversionValidator conversionValidator;
    private final HistoryService historyService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ConversionResponse convertXmlToMt103(String xmlContent, String filename) {
        long startNanos = System.nanoTime();
        try {
            log.info("Starting XML to MT103 conversion for file: {}", filename);

            ConversionResponse coreResponse = coreConverterClient.convert(xmlContent, filename);
            String mt103Result = coreResponse.getMt103();
            log.info("Pre-validation trace - extracted :32A: amount value: {}", extractAmountFromTag32A(mt103Result));
            ConversionValidator.ValidationResult validationResult = conversionValidator.validate(mt103Result);
            String messageReference = resolveMessageReference(validationResult);

            historyService.saveSuccess(
                messageReference,
                calculateProcessingDurationMs(startNanos)
            );
            long processingDurationMs = calculateProcessingDurationMs(startNanos);
            log.info("Conversion successful for file: {}. MT103 length: {}", filename, mt103Result.length());
            return ConversionResponse.builder()
                .mt103(mt103Result)
                .warnings(coreResponse.getWarnings())
                .processingTimeMs(processingDurationMs)
                .messageReference(messageReference)
                .build();

        } catch (FinancialValidationException ex) {
            log.error("Financial validation failed for file: {} with code {}", filename, ex.getErrorCode(), ex);
            try {
                historyService.saveFailureInNewTransaction(ex.getInstructionId(), calculateProcessingDurationMs(startNanos));
            } catch (Exception logEx) {
                log.error("Failed to persist failure log in REQUIRES_NEW transaction for file: {}", filename, logEx);
            }
            throw ex;

        } catch (ConversionFailedException ex) {
            log.error("Conversion failed for file: {}", filename, ex);
            try {
                historyService.saveFailureInNewTransaction(null, calculateProcessingDurationMs(startNanos));
            } catch (Exception logEx) {
                log.error("Failed to persist failure log in REQUIRES_NEW transaction for file: {}", filename, logEx);
            }
            throw ex;

        } catch (Exception ex) {
            log.error("Unexpected error during conversion for file: {}", filename, ex);
            try {
                historyService.saveFailureInNewTransaction(null, calculateProcessingDurationMs(startNanos));
            } catch (Exception logEx) {
                log.error("Failed to persist failure log in REQUIRES_NEW transaction for file: {}", filename, logEx);
            }
            throw new ConversionFailedException("ERR_CONVERSION_EXECUTION", "Failed to convert XML: " + ex.getMessage(), ex);
        }
    }

    private long calculateProcessingDurationMs(long startNanos) {
        return Math.max(0L, (System.nanoTime() - startNanos) / 1_000_000L);
    }

    private String extractAmountFromTag32A(String mt103) {
        if (mt103 == null || mt103.isBlank()) {
            return "<empty-mt103>";
        }

        java.util.regex.Matcher matcher = TAG_32A_PATTERN.matcher(mt103);
        if (matcher.find()) {
            String date = matcher.group(1);
            String currency = matcher.group(2);
            String amount = matcher.group(3);
            return date + currency + amount;
        }

        return "<tag-32A-not-found>";
    }

    private String resolveMessageReference(ConversionValidator.ValidationResult validationResult) {
        if (validationResult == null) {
            return null;
        }

        String instructionId = validationResult.instructionId();
        if (instructionId == null || instructionId.isBlank()) {
            return null;
        }

        return instructionId;
    }
}

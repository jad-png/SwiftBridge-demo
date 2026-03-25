package com.swiftbridge.orchestrator.service.Impl;

import com.swiftbridge.orchestrator.dto.conversion.ConversionResponse;
import com.swiftbridge.orchestrator.dto.history.ConversionAuditDTO;
import com.swiftbridge.orchestrator.entity.ConversionStatus;
import com.swiftbridge.orchestrator.entity.TransactionHistory;
import com.swiftbridge.orchestrator.entity.User;
import com.swiftbridge.orchestrator.exception.ConversionFailedException;
import com.swiftbridge.orchestrator.repository.UserRepository;
import com.swiftbridge.orchestrator.security.SecurityUtils;
import com.swiftbridge.orchestrator.service.AuditService;
import com.swiftbridge.orchestrator.service.ConversionService;
import com.swiftbridge.orchestrator.service.ConversionValidator;
import com.swiftbridge.orchestrator.service.CoreConverterClient;
import com.swiftbridge.orchestrator.service.FinancialValidationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;

@Service
@RequiredArgsConstructor
@Slf4j
public class ConversionServiceImpl implements ConversionService {

    private static final java.util.regex.Pattern TAG_32A_PATTERN = java.util.regex.Pattern
            .compile("(?m)^:32A:(\\d{6})([A-Z]{3})([0-9,]+(?:\\.[0-9]+)?)$");

    private final CoreConverterClient coreConverterClient;
    private final ConversionValidator conversionValidator;
    private final AuditService auditService;
    private final SecurityUtils securityUtils;
    private final UserRepository userRepository;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ConversionResponse convertXmlToMt103(String xmlContent, String filename) {
        TransactionHistory tx = new TransactionHistory();
        tx.setTransactionId(UUID.randomUUID().toString());
        tx.setRequestTimestamp(LocalDateTime.now());
        tx.setMessageType("MT103");
        tx.setInputData(xmlContent);
        tx.setUser(resolveCurrentUser());

        long startNanos = System.nanoTime();
        try {
            log.info("Starting XML to MT103 conversion for file: {}", filename);

            ConversionResponse coreResponse = coreConverterClient.convert(xmlContent, filename);
            String mt103Result = coreResponse.getMt103();
            tx.setOutputContent(mt103Result);

            log.info("Pre-validation trace - extracted :32A: amount value: {}", extractAmountFromTag32A(mt103Result));

            ConversionValidator.ValidationResult validationResult = conversionValidator.validate(mt103Result);
            String messageReference = resolveMessageReference(validationResult);
            tx.setMessageReference(messageReference);
            tx.setConversionStatus(ConversionStatus.SUCCESS);
            long processingDurationMs = calculateProcessingDurationMs(startNanos);
            tx.setProcessingDurationMs(processingDurationMs);

            log.info("Conversion successful for file: {}. MT103 length: {}", filename, mt103Result.length());

            return ConversionResponse.builder()
                    .mt103(mt103Result)
                    .warnings(coreResponse.getWarnings())
                    .processingTimeMs(processingDurationMs)
                    .messageReference(messageReference)
                    .build();

        } catch (FinancialValidationException ex) {
            log.error("Validation failed for file: {}", filename, ex);
            tx.setConversionStatus(ConversionStatus.FAILED);
            tx.setErrorMessage(ex.getMessage());
            tx.setMessageReference(ex.getInstructionId());
            tx.setValidationErrors(serializeValidationErrors(List.of(
                ConversionAuditDTO.ValidationError.builder()
                    .message(ex.getMessage())
                    .path(ex.getInstructionId())
                    .build()
            )));
            throw ex;
        } catch (ConversionFailedException ex) {
            log.error("Conversion failed for file: {}", filename, ex);
            tx.setConversionStatus(ConversionStatus.FAILED);
            tx.setErrorMessage(ex.getMessage());
        } catch (Exception ex) {
            log.error("Unexpected error during conversion for file: {}", filename, ex);
            tx.setConversionStatus(ConversionStatus.FAILED);
            tx.setErrorMessage(ex.getMessage());
            throw new ConversionFailedException(
                    "ERR_CONVERSION_EXECUTION",
                    "Failed to convert XML: " + ex.getMessage(),
                    ex);
        } finally {
            tx.setProcessingDurationMs(calculateProcessingDurationMs(startNanos));
            auditService.saveAudit(tx);
        }
        return null; // unreachable, but required for compilation
    }

    private User resolveCurrentUser() {
        try {
            Long userId = securityUtils.getCurrentUser().getId();
            return userRepository.findById(userId).orElse(null);
        } catch (Exception e) {
            return null;
        }
    }

    private String serializeValidationErrors(Object obj) {
        try {
            // This should be implemented in AuditServiceImpl for real JSON serialization
            return obj == null ? null : obj.toString();
        } catch (Exception e) {
            log.warn("Failed to serialize validation errors");
            return null;
        }
    }

    private long calculateProcessingDurationMs(long startNanos) {
        return Math.max(0L, (System.nanoTime() - startNanos) / 1_000_000L);
    }

    private String extractAmountFromTag32A(String mt103) {
        if (mt103 == null || mt103.isBlank()) {
            return "<empty-mt103>";
        }
        Matcher matcher = TAG_32A_PATTERN.matcher(mt103);
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
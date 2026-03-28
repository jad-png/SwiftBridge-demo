package com.swiftbridge.converter.mapping.pacs008.mapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;

import com.swiftbridge.converter.exception.ConversionFailedException;
import com.swiftbridge.converter.exception.SwiftMappingException;
import com.swiftbridge.converter.exception.SwiftErrorCode;
import com.swiftbridge.converter.mapping.model.Pacs008Fields;
import com.swiftbridge.converter.mapping.model.ConversionResult;
import com.swiftbridge.converter.mapping.model.mt103.ApplicationHeaderBlock;
import com.swiftbridge.converter.mapping.model.mt103.BasicHeaderBlock;
import com.swiftbridge.converter.mapping.model.mt103.Mt103Message;
import com.swiftbridge.converter.mapping.model.mt103.TextBlock;
import com.swiftbridge.converter.mapping.model.mt103.TrailerBlock;
import com.swiftbridge.converter.mapping.model.mt103.UserHeaderBlock;
import com.swiftbridge.converter.mapping.mt103.formatter.Mt103Formatter;
import com.swiftbridge.converter.mapping.mt103.normalizer.SwiftFieldNormalizer;
import com.swiftbridge.converter.mapping.pacs008.extractor.Pacs008FieldExtractor;
import com.swiftbridge.converter.service.Mt103SerializationService;
import com.swiftbridge.converter.utils.XmlParsingService;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import com.swiftbridge.converter.utils.TruncationResult;

@Component
@RequiredArgsConstructor
@Slf4j
public class Pacs008ToMt103Mapper {

    private static final String MAPPING_FAILURE_PREFIX = "Failed to convert pacs.008 to MT103: ";
    private static final String LOGICAL_TERMINAL_FALLBACK = "BANKBEBBAXXX";
    private static final int LOGICAL_TERMINAL_LENGTH = 12;

    private final XmlParsingService xmlParsingService;
    private final Pacs008FieldExtractor fieldExtractor;
    private final SwiftFieldNormalizer normalizer;
    private final Mt103Formatter formatter;
    private final Mt103SerializationService mt103SerializationService;

    private static final DateTimeFormatter SESSION_FORMATTER = DateTimeFormatter.ofPattern("HHmm");
    private static final DateTimeFormatter SEQUENCE_FORMATTER = DateTimeFormatter.ofPattern("HHmmss");

    public ConversionResult mapPacs008ToMt103(String xmlContent) {
        try {
            log.info("========== Pacs.008 to MT103 Conversion Start ==========");
            log.info("[DEBUG] Full XML Content Received:\n{}", xmlContent);

            Pacs008Fields fields = parseAndExtract(xmlContent);
            log.info("Successfully extracted fields from pacs.008 XML");

            // Print each important extracted field with descriptive label
            log.info("[DEBUG] Extracted Field: InstdAmt: {}", fields.amountValue());
            log.info("[DEBUG] Extracted Field: Settlement Date (IntrBkSttlmDt): {}", fields.settlementDate());
            log.info("[DEBUG] Extracted Field: Amount Currency (@Ccy): {}", fields.amountCurrency());
            log.info("[DEBUG] Extracted Field: Dbtr/Nm (Debtor Name): {}", fields.debtorName());
            log.info("[DEBUG] Extracted Field: InitgPty/Nm (Initiating Party Name): {}", fields.initiatingPartyName());
            log.info("[DEBUG] Extracted Field: Cdtr/Nm (Creditor Name): {}", fields.creditorName());
            log.info("[DEBUG] Extracted Field: Reference (InstrId): {}", fields.reference());
            log.info("[DEBUG] Extracted Field: UETR: {}", fields.uetr());
            log.info("[DEBUG] Extracted Field: Charge Bearer (ChrgBr): {}", fields.chargeBearer());
            log.info("[DEBUG] Extracted Field: Debtor Address: {}", fields.debtorAddress());
            log.info("[DEBUG] Extracted Field: Creditor Address: {}", fields.creditorAddress());

            log.info("Starting mapping process to MT103");

            // Step-by-step mapping with debug output
            String mt20 = mapReference(fields);
            log.info("[DEBUG] Mapped :20: (Reference) from pacs.008 Reference: {} -> {}", fields.reference(), mt20);

            String mt32A = map32A(fields);
            log.info(
                    "[DEBUG] Mapped :32A: (Value Date/Currency/Amount) from SettlementDate: {}, AmountCurrency: {}, AmountValue: {} -> {}",
                    fields.settlementDate(), fields.amountCurrency(), fields.amountValue(), mt32A);

            TruncationResult mt50kResult = mapDebtorPartyLines(fields);
            log.info("[DEBUG] Mapped :50K: (Debtor Party Lines) from Name: {}, Address: {} -> {}", fields.debtorName(),
                    fields.debtorAddress(), mt50kResult.lines());
            if (mt50kResult.truncated()) {
                log.warn("[DEBUG] :50K: field was truncated. Warning: {}", mt50kResult.warning());
            }

            TruncationResult mt59Result = mapCreditorPartyLines(fields);
            log.info("[DEBUG] Mapped :59: (Creditor Party Lines) from Name: {}, Address: {} -> {}",
                    fields.creditorName(), fields.creditorAddress(), mt59Result.lines());
            if (mt59Result.truncated()) {
                log.warn("[DEBUG] :59: field was truncated. Warning: {}", mt59Result.warning());
            }

            List<String> mt50k = mt50kResult.lines();
            List<String> mt59 = mt59Result.lines();

            String mt52A = mapDebtorBic(fields);
            log.info("[DEBUG] Mapped :52A: (Debtor BIC) from {} -> {}", fields.debtorBic(), mt52A);

            String mt57A = mapCreditorBic(fields);
            log.info("[DEBUG] Mapped :57A: (Creditor BIC) from {} -> {}", fields.creditorBic(), mt57A);

            String mt71A = mapChargeBearer(fields);
            log.info("[DEBUG] Mapped :71A: (Charge Bearer) from {} -> {}", fields.chargeBearer(), mt71A);

            BasicHeaderBlock block1 = buildBlock1(mt52A);
            log.info("[DEBUG] Built Block 1 (Basic Header): {}", block1);

            ApplicationHeaderBlock block2 = buildBlock2(mt57A);
            log.info("[DEBUG] Built Block 2 (Application Header): {}", block2);

            UserHeaderBlock block3 = buildBlock3(fields);
            log.info("[DEBUG] Built Block 3 (User Header): {}", block3);

            TextBlock block4 = buildBlock4(mt20, mt32A, mt50k, mt52A, mt57A, mt59, mt71A);
            log.info("[DEBUG] Built Block 4 (Text Block): {}", block4);

            TrailerBlock block5 = buildBlock5();
            log.info("[DEBUG] Built Block 5 (Trailer): {}", block5);

            Mt103Message mt103Message = buildMt103Message(block1, block2, block3, block4, block5);
            log.info("[DEBUG] Built MT103 Message Object: {}", mt103Message);

            String mt103 = serializeMt103(mt103Message);
            log.info("[DEBUG] Serialized MT103 Message: \n{}", mt103);

            List<String> warnings = collectWarnings(mt50kResult, mt59Result);
            if (!warnings.isEmpty()) {
                log.warn("[DEBUG] Warnings collected during mapping: {}", warnings);
            }

            log.info("========== Pacs.008 to MT103 Conversion Completed ==========");
            return new ConversionResult(mt103, warnings);

        } catch (SwiftMappingException | ConversionFailedException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("[DEBUG] Exception during mapping: {}", ex.getMessage(), ex);
            throw handleMappingFailure(ex);
        }
    }

    private String toLogicalTerminal(String bic) {
        String normalizedBic = normalizer.normalizeBic(bic);
        String terminal = normalizedBic.isBlank() ? LOGICAL_TERMINAL_FALLBACK : normalizedBic + "X";
        if (terminal.length() >= LOGICAL_TERMINAL_LENGTH) {
            return terminal.substring(0, LOGICAL_TERMINAL_LENGTH);
        }
        return String.format(Locale.ROOT, "%-12s", terminal).replace(' ', 'X');
    }

    private Pacs008Fields parseAndExtract(String xmlContent) {
        try {
            Document document = xmlParsingService.parseNamespaceAwareXml(xmlContent);
            return fieldExtractor.extract(document);
        } catch (SwiftMappingException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ConversionFailedException(
                    SwiftErrorCode.ERR_XML_PARSING_FAILED.getCode(),
                    "Unable to parse pacs.008 XML content: " + ex.getMessage(),
                    ex);
        }
    }

    private String buildSessionNumber() {
        return currentTimestamp().format(SESSION_FORMATTER);
    }

    private String buildSequenceNumber() {
        return currentTimestamp().format(SEQUENCE_FORMATTER);
    }

    private LocalDateTime currentTimestamp() {
        return LocalDateTime.now();
    }

    private String mapReference(Pacs008Fields fields) {
        return normalizer.normalizeReference(fields.reference());
    }

    private String map32A(Pacs008Fields fields) {
        return normalizer.normalize32A(
                fields.settlementDate(),
                fields.amountCurrency(),
                fields.amountValue());
    }

    private TruncationResult mapDebtorPartyLines(Pacs008Fields fields) {
        return formatter.buildPartyLinesWithWarning(":50K:", fields.debtorName(), fields.debtorAddress());
    }

    private TruncationResult mapCreditorPartyLines(Pacs008Fields fields) {
        return formatter.buildPartyLinesWithWarning(":59:", fields.creditorName(), fields.creditorAddress());
    }

    private String mapDebtorBic(Pacs008Fields fields) {
        return normalizer.normalizeBic(fields.debtorBic());
    }

    private String mapCreditorBic(Pacs008Fields fields) {
        return normalizer.normalizeBic(fields.creditorBic());
    }

    private String mapChargeBearer(Pacs008Fields fields) {
        return normalizer.mapChargeBearer(fields.chargeBearer());
    }

    private BasicHeaderBlock buildBlock1(String senderBic) {
        return BasicHeaderBlock.builder()
                .applicationId("F")
                .serviceId("01")
                .logicalTerminal(toLogicalTerminal(senderBic))
                .sessionNumber(buildSessionNumber())
                .sequenceNumber(buildSequenceNumber())
                .build();
    }

    private ApplicationHeaderBlock buildBlock2(String receiverBic) {
        return ApplicationHeaderBlock.builder()
                .inputOutputId("I")
                .messageType("103")
                .receiverLogicalTerminal(toLogicalTerminal(receiverBic))
                .priority("N")
                .build();
    }

    private UserHeaderBlock buildBlock3(Pacs008Fields fields) {
        return UserHeaderBlock.builder()
                .uetr(resolveUetr(fields.uetr(), fields.reference()))
                .build();
    }

    private TextBlock buildBlock4(String mt20, String mt32A, List<String> mt50k, String mt52A, String mt57A,
            List<String> mt59, String mt71A) {
        return TextBlock.builder()
                .tag20(mt20)
                .tag32A(mt32A)
                .tag50KLines(mt50k)
                .tag52A(mt52A)
                .tag57A(mt57A)
                .tag59Lines(mt59)
                .tag71A(mt71A)
                .build();
    }

    private TrailerBlock buildBlock5() {
        return TrailerBlock.builder().build();
    }

    private Mt103Message buildMt103Message(
            BasicHeaderBlock block1,
            ApplicationHeaderBlock block2,
            UserHeaderBlock block3,
            TextBlock block4,
            TrailerBlock block5) {
        return Mt103Message.builder()
                .block1(block1)
                .block2(block2)
                .block3(block3)
                .block4(block4)
                .block5(block5)
                .build();
    }

    private String serializeMt103(Mt103Message mt103Message) {
        return mt103SerializationService.serialize(mt103Message);
    }

    private List<String> collectWarnings(TruncationResult mt50kResult, TruncationResult mt59Result) {
        List<String> warnings = new ArrayList<>();
        addWarningIfTruncated(warnings, mt50kResult);
        addWarningIfTruncated(warnings, mt59Result);
        return warnings;
    }

    private void addWarningIfTruncated(List<String> warnings, TruncationResult truncationResult) {
        if (truncationResult.truncated() && !truncationResult.warning().isBlank()) {
            warnings.add(truncationResult.warning());
        }
    }

    private RuntimeException handleMappingFailure(Exception ex) {
        log.error("CBPR+ to MT103 mapping failed", ex);
        return new RuntimeException(MAPPING_FAILURE_PREFIX + ex.getMessage(), ex);
    }

    private String resolveUetr(String uetr, String fallbackReference) {
        if (uetr != null
                && uetr.matches("(?i)^[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$")) {
            return uetr.toLowerCase(Locale.ROOT);
        }
        if (fallbackReference != null && fallbackReference
                .matches("(?i)^[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$")) {
            return fallbackReference.toLowerCase(Locale.ROOT);
        }
        return UUID.randomUUID().toString();
    }
}

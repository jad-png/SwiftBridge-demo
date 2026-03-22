package com.swiftbridge.converter.mapping.pacs008.mapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;

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

    private final XmlParsingService xmlParsingService;
    private final Pacs008FieldExtractor fieldExtractor;
    private final SwiftFieldNormalizer normalizer;
    private final Mt103Formatter formatter;
    private final Mt103SerializationService mt103SerializationService;

    private static final DateTimeFormatter SESSION_FORMATTER = DateTimeFormatter.ofPattern("HHmm");
    private static final DateTimeFormatter SEQUENCE_FORMATTER = DateTimeFormatter.ofPattern("HHmmss");

    public ConversionResult mapPacs008ToMt103(String xmlContent) {
        try {
            log.info("Starting pacs.008 to MT103 conversion");

            Pacs008Fields fields = parseAndExtract(xmlContent);

            String mt20 = mapReference(fields);
            String mt32A = map32A(fields);

            TruncationResult mt50kResult = mapDebtorPartyLines(fields);
            TruncationResult mt59Result = mapCreditorPartyLines(fields);
            List<String> mt50k = mt50kResult.lines();
            List<String> mt59 = mt59Result.lines();

            String mt52A = normalizer.normalizeBic(fields.debtorBic());
            String mt57A = normalizer.normalizeBic(fields.creditorBic());
            String mt71A = normalizer.mapChargeBearer(fields.chargeBearer());

            BasicHeaderBlock block1 = BasicHeaderBlock.builder()
                .applicationId("F")
                .serviceId("01")
                .logicalTerminal(toLogicalTerminal(mt52A))
                .sessionNumber(buildSessionNumber())
                .sequenceNumber(buildSequenceNumber())
                .build();

            ApplicationHeaderBlock block2 = ApplicationHeaderBlock.builder()
                .inputOutputId("I")
                .messageType("103")
                .receiverLogicalTerminal(toLogicalTerminal(mt57A))
                .priority("N")
                .build();

            UserHeaderBlock block3 = UserHeaderBlock.builder()
                .uetr(resolveUetr(fields.uetr(), fields.reference()))
                .build();

            TextBlock block4 = TextBlock.builder()
                .tag20(mt20)
                .tag32A(mt32A)
                .tag50KLines(mt50k)
                .tag52A(mt52A)
                .tag57A(mt57A)
                .tag59Lines(mt59)
                .tag71A(mt71A)
                .build();

            TrailerBlock block5 = TrailerBlock.builder().build();

            Mt103Message mt103Message = Mt103Message.builder()
                .block1(block1)
                .block2(block2)
                .block3(block3)
                .block4(block4)
                .block5(block5)
                .build();

            String mt103 = mt103SerializationService.serialize(mt103Message);

            List<String> warnings = new ArrayList<>();
            if (mt50kResult.truncated() && !mt50kResult.warning().isBlank()) {
                warnings.add(mt50kResult.warning());
            }
            if (mt59Result.truncated() && !mt59Result.warning().isBlank()) {
                warnings.add(mt59Result.warning());
            }

            log.info("MT103 conversion completed successfully");
            return new ConversionResult(mt103, warnings);

        } catch (Exception ex) {
            log.error("CBPR+ to MT103 mapping failed", ex);
            throw new RuntimeException("Failed to convert pacs.008 to MT103: " + ex.getMessage(), ex);
        }
    }

    private String toLogicalTerminal(String bic) {
        String normalizedBic = normalizer.normalizeBic(bic);
        String terminal = normalizedBic.isBlank() ? "BANKBEBBAXXX" : normalizedBic + "X";
        if (terminal.length() >= 12) {
            return terminal.substring(0, 12);
        }
        return String.format(Locale.ROOT, "%-12s", terminal).replace(' ', 'X');
    }

    private Pacs008Fields parseAndExtract(String xmlContent) {
        Document document = xmlParsingService.parseNamespaceAwareXml(xmlContent);
        return fieldExtractor.extract(document);
    }

    private String buildSessionNumber() {
        return LocalDateTime.now().format(SESSION_FORMATTER);
    }

    private String buildSequenceNumber() {
        return LocalDateTime.now().format(SEQUENCE_FORMATTER);
    }

    private String mapReference(Pacs008Fields fields) {
        return normalizer.normalizeReference(fields.reference());
    }

    private String map32A(Pacs008Fields fields) {
        return normalizer.normalize32A(
            fields.settlementDate(),
            fields.amountCurrency(),
            fields.amountValue()
        );
    }

    private TruncationResult mapDebtorPartyLines(Pacs008Fields fields) {
        return formatter.buildPartyLinesWithWarning(":50K:", fields.debtorName(), fields.debtorAddress());
    }

    private TruncationResult mapCreditorPartyLines(Pacs008Fields fields) {
        return formatter.buildPartyLinesWithWarning(":59:", fields.creditorName(), fields.creditorAddress());
    }

    private String resolveUetr(String uetr, String fallbackReference) {
        if (uetr != null && uetr.matches("(?i)^[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$")) {
            return uetr.toLowerCase(Locale.ROOT);
        }
        if (fallbackReference != null && fallbackReference.matches("(?i)^[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$")) {
            return fallbackReference.toLowerCase(Locale.ROOT);
        }
        return UUID.randomUUID().toString();
    }
}

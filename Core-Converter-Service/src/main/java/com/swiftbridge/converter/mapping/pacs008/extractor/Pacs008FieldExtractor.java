package com.swiftbridge.converter.mapping.pacs008.extractor;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.swiftbridge.converter.exception.SwiftErrorCode;
import com.swiftbridge.converter.exception.SwiftMappingException;
import com.swiftbridge.converter.mapping.model.Pacs008Fields;
import com.swiftbridge.converter.mapping.mt103.normalizer.SwiftFieldNormalizer;
import com.swiftbridge.converter.utils.Pacs008Xpaths;
import com.swiftbridge.converter.utils.XmlParsingService;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class Pacs008FieldExtractor {

    private final SwiftFieldNormalizer normalizer;
    private final XmlParsingService xmlParsingService;

    public Pacs008Fields extract(Document document) {
        log.info("Starting Pacs.008 field extraction with dynamic namespace awareness");
        XPath xpath = xmlParsingService.createPacs008XPath(document);

        ScalarFields scalarFields = extractScalarFields(xpath, document);

        String reference = scalarFields.reference();
        String uetr = scalarFields.uetr();
        String amountValue = scalarFields.amountValue();
        String amountCurrency = scalarFields.amountCurrency();
        String settlementDate = scalarFields.settlementDate();
        String debtorName = scalarFields.debtorName();
        String creditorName = scalarFields.creditorName();
        String initiatingPartyName = scalarFields.initiatingPartyName();

        validateAmountPresence(amountValue);
        validateAmountCurrency(amountCurrency);

        validateDebtorName(debtorName);
        validateCreditorName(creditorName);
        validateSettlementDate(settlementDate);

        String debtorBic = resolveDebtorBic(xpath, document);
        String creditorBic = resolveCreditorBic(xpath, document);
        String chargeBearer = resolveChargeBearer(xpath, document);

        List<String> debtorAddress = extractStructuredAddress(xpath, document, Pacs008Xpaths.DEBTOR_ADDRESS_ROOT);
        List<String> creditorAddress = extractStructuredAddress(xpath, document, Pacs008Xpaths.CREDITOR_ADDRESS_ROOT);

        log.info("Pacs.008 field extraction completed successfully");
        return buildFields(
                reference,
                uetr,
                amountValue,
                amountCurrency,
                settlementDate,
                debtorName,
                creditorName,
                initiatingPartyName,
                debtorBic,
                creditorBic,
                chargeBearer,
                debtorAddress,
                creditorAddress);
    }

    private ScalarFields extractScalarFields(XPath xpath, Document document) {
        log.debug("Extracting scalar fields using dynamic XPath context");
        String reference = evaluate(xpath, document, Pacs008Xpaths.REFERENCE_INSTR_ID);
        String uetr = "";
        for (String path : Pacs008Xpaths.UETR_PATHS) {
            String value = evaluate(xpath, document, path);
            if (!value.isEmpty()) {
                uetr = value;
                log.debug("Extracted uetr '{}' using path: {}", uetr, path);
                break;
            }
        }

        String amountValue = "";
        for (String path : Pacs008Xpaths.AMOUNT_VALUE_PATHS) {
            String value = evaluate(xpath, document, path);
            if (!value.isEmpty()) {
                amountValue = value;
                log.debug("Extracted amountValue '{}' using path: {}", amountValue, path);
                break;
            }
        }

        String amountCurrency = "";
        for (String path : Pacs008Xpaths.AMOUNT_CURRENCY_PATHS) {
            String currency = evaluate(xpath, document, path);
            if (!currency.isEmpty()) {
                amountCurrency = currency;
                log.debug("Extracted amountCurrency '{}' using path: {}", amountCurrency, path);
                break;
            }
        }

        String settlementDate = evaluate(xpath, document, Pacs008Xpaths.SETTLEMENT_DATE_INTERBANK);
        String debtorName = evaluate(xpath, document, Pacs008Xpaths.DEBTOR_NAME);
        String creditorName = evaluate(xpath, document, Pacs008Xpaths.CREDITOR_NAME);
        String initiatingPartyName = evaluate(xpath, document, Pacs008Xpaths.INITIATING_PARTY_NAME);

        log.info(
                "[DEBUG] Extracted: reference={}, uetr={}, amountValue={}, amountCurrency={}, settlementDate={}, debtorName={}, creditorName={}, initiatingPartyName={}",
                reference, uetr, amountValue, amountCurrency, settlementDate, debtorName, creditorName,
                initiatingPartyName);

        return new ScalarFields(reference, uetr, amountValue, initiatingPartyName, amountCurrency, settlementDate,
                debtorName, creditorName);
    }

    private void validateAmountPresence(String amountValue) {
        if (!amountValue.isEmpty()) {
            return;
        }
        log.error("Amount validation failed: InstdAmt or IntrBkSttlmAmt not found. amountValue is empty.");
        throw new SwiftMappingException(
                SwiftErrorCode.ERR_MAPPING_AMOUNT_MISSING,
                "Amount field is missing from the pacs.008 message (InstdAmt or IntrBkSttlmAmt not found in pacs.008 document. Checked path: "
                        + Pacs008Xpaths.AMOUNT_VALUE_INSTD + ")");
    }

    private void validateAmountCurrency(String amountCurrency) {
        if (!amountCurrency.isEmpty()) {
            return;
        }
        throw new SwiftMappingException(
                SwiftErrorCode.ERR_INVALID_CURRENCY,
                "Currency (@Ccy attribute) not found in amount field");
    }

    private void validateDebtorName(String debtorName) {
        if (!debtorName.isEmpty()) {
            return;
        }
        throw new SwiftMappingException(
                SwiftErrorCode.ERR_DEBTOR_NAME_MISSING,
                "Dbtr/Nm or InitgPty/Nm not found in pacs.008 document");
    }

    private void validateCreditorName(String creditorName) {
        if (!creditorName.isEmpty()) {
            return;
        }
        throw new SwiftMappingException(
                SwiftErrorCode.ERR_CREDITOR_NAME_MISSING,
                "Cdtr/Nm or UltmtDbtr/Nm not found in pacs.008 document");
    }

    private void validateSettlementDate(String settlementDate) {
        if (!settlementDate.isEmpty()) {
            return;
        }
        throw new SwiftMappingException(
                SwiftErrorCode.ERR_INVALID_SETTLEMENT_DATE,
                "IntrBkSttlmDt not found in pacs.008 document");
    }

    private String evaluate(XPath xpath, Document document, String expression) {
        try {
            log.trace("Evaluating XPath: {}", expression);
            String value = xpath.evaluate(expression, document);
            String result = value == null ? "" : value.trim();
            log.trace("XPath result: '{}'", result);
            return result;
        } catch (Exception ex) {
            log.warn("Failed to evaluate XPath: {}. Error: {}", expression, ex.getMessage());
            return "";
        }
    }

    private String resolveDebtorBic(XPath xpath, Document document) {
        return firstNonBlank(
                evaluate(xpath, document, Pacs008Xpaths.DBTR_BICFI),
                evaluate(xpath, document, Pacs008Xpaths.DBTR_BIC));
    }

    private String resolveCreditorBic(XPath xpath, Document document) {
        return firstNonBlank(
                evaluate(xpath, document, Pacs008Xpaths.CDTR_BICFI),
                evaluate(xpath, document, Pacs008Xpaths.CDTR_BIC));
    }

    private String resolveChargeBearer(XPath xpath, Document document) {
        return firstNonBlank(
                evaluate(xpath, document, Pacs008Xpaths.CHARGE_BEARER_PMTINF),
                evaluate(xpath, document, Pacs008Xpaths.CHARGE_BEARER_TX));
    }

    private List<String> extractStructuredAddress(XPath xpath, Document document, String addressRootExpression) {
        List<String> parts = new ArrayList<>();
        try {
            Node addressRoot = (Node) xpath.evaluate(addressRootExpression, document, XPathConstants.NODE);
            if (addressRoot == null) {
                return parts;
            }

            parts.addAll(extractAdrLines(xpath, addressRoot));

            if (!parts.isEmpty()) {
                return deduplicate(parts);
            }

            parts.addAll(extractFallbackAddressParts(xpath, document, addressRootExpression));

            return deduplicate(parts);
        } catch (Exception ex) {
            return parts;
        }
    }

    private List<String> extractFallbackAddressParts(XPath xpath, Document document, String addressRootExpression) {
        List<String> parts = new ArrayList<>();
        String street = evaluate(xpath, document, addressRootExpression + "/doc:StrtNm");
        String building = evaluate(xpath, document, addressRootExpression + "/doc:BldgNb");
        String postal = evaluate(xpath, document, addressRootExpression + "/doc:PstCd");
        String town = evaluate(xpath, document, addressRootExpression + "/doc:TwnNm");
        String country = evaluate(xpath, document, addressRootExpression + "/doc:Ctry");

        String streetLine = normalizer.normalizeText((street + " " + building).trim());
        String townLine = normalizer.normalizeText((postal + " " + town).trim());

        if (!streetLine.isEmpty()) {
            parts.add(streetLine);
        }
        if (!townLine.isEmpty()) {
            parts.add(townLine);
        }
        if (!country.isEmpty()) {
            parts.add(normalizer.normalizeText(country));
        }
        return parts;
    }

    private List<String> extractAdrLines(XPath xpath, Node addressRoot) throws Exception {
        List<String> parts = new ArrayList<>();
        NodeList adrLines = (NodeList) xpath.evaluate("doc:AdrLine", addressRoot, XPathConstants.NODESET);
        for (int index = 0; index < adrLines.getLength(); index++) {
            String line = normalizer.normalizeText(adrLines.item(index).getTextContent());
            if (!line.isEmpty()) {
                parts.add(line);
            }
        }
        return parts;
    }

    private List<String> deduplicate(List<String> values) {
        return new ArrayList<>(new LinkedHashSet<>(values));
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank() && hasNormalizedValue(value)) {
                String normalized = normalizer.normalizeText(value);
                return normalized == null ? "" : normalized;
            }
        }
        return "";
    }

    private boolean hasNormalizedValue(String value) {
        if (value == null || value.isBlank()) {
            return false;
        }
        String normalized = normalizer.normalizeText(value);
        return normalized != null && !normalized.isEmpty();
    }

    private Pacs008Fields buildFields(
            String reference,
            String uetr,
            String amountValue,
            String amountCurrency,
            String settlementDate,
            String debtorName,
            String creditorName,
            String initiatingPartyName,
            String debtorBic,
            String creditorBic,
            String chargeBearer,
            List<String> debtorAddress,
            List<String> creditorAddress) {
        return new Pacs008Fields(
                reference,
                uetr,
                amountValue,
                amountCurrency,
                settlementDate,
                debtorName,
                initiatingPartyName,
                creditorName,
                debtorBic,
                creditorBic,
                chargeBearer,
                debtorAddress,
                creditorAddress);
    }

    private record ScalarFields(
            String reference,
            String uetr,
            String amountValue,
            String initiatingPartyName,
            String amountCurrency,
            String settlementDate,
            String debtorName,
            String creditorName) {
    }
}

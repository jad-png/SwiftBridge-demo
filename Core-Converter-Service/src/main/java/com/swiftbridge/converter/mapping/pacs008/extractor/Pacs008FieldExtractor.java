package com.swiftbridge.converter.mapping.pacs008.extractor;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.swiftbridge.converter.exception.SwiftErrorCode;
import com.swiftbridge.converter.exception.SwiftMappingException;
import com.swiftbridge.converter.mapping.model.Pacs008Fields;
import com.swiftbridge.converter.mapping.mt103.normalizer.SwiftFieldNormalizer;
import com.swiftbridge.converter.utils.Pacs008Xpaths;

import javax.xml.XMLConstants;
import javax.xml.namespace.NamespaceContext;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;

@Component
@RequiredArgsConstructor
public class Pacs008FieldExtractor {

    private static final String PACS_008_NS = "urn:iso:std:iso:20022:tech:xsd:pacs.008.001.02";

    private final SwiftFieldNormalizer normalizer;

    public Pacs008Fields extract(Document document) {
        XPath xpath = buildXPath();

        ScalarFields scalarFields = extractScalarFields(xpath, document);

        String reference = scalarFields.reference();

        String uetr = scalarFields.uetr();

        String amountValue = scalarFields.amountValue();

        String amountCurrency = scalarFields.amountCurrency();

        String settlementDate = scalarFields.settlementDate();

        String debtorName = scalarFields.debtorName();

        String creditorName = scalarFields.creditorName();

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

        return new Pacs008Fields(
            reference,
            uetr,
            amountValue,
            amountCurrency,
            settlementDate,
            debtorName,
            creditorName,
            debtorBic,
            creditorBic,
            chargeBearer,
            debtorAddress,
            creditorAddress
        );
    }

    private ScalarFields extractScalarFields(XPath xpath, Document document) {
        String reference = evaluate(xpath, document, Pacs008Xpaths.REFERENCE_INSTR_ID);
        String uetr = evaluate(xpath, document, Pacs008Xpaths.UETR);
        String amountValue = evaluate(xpath, document, Pacs008Xpaths.AMOUNT_VALUE_INSTD);
        String amountCurrency = evaluate(xpath, document, Pacs008Xpaths.AMOUNT_CCY_INSTD);
        String settlementDate = evaluate(xpath, document, Pacs008Xpaths.SETTLEMENT_DATE_INTERBANK);
        String debtorName = evaluate(xpath, document, Pacs008Xpaths.DEBTOR_NAME);
        String creditorName = evaluate(xpath, document, Pacs008Xpaths.CREDITOR_NAME);
        return new ScalarFields(reference, uetr, amountValue, amountCurrency, settlementDate, debtorName, creditorName);
    }

    private XPath buildXPath() {
        XPath xpath = XPathFactory.newInstance().newXPath();
        xpath.setNamespaceContext(new Pacs008NamespaceContext());
        return xpath;
    }

    private void validateAmountPresence(String amountValue) {
        if (!amountValue.isEmpty()) {
            return;
        }
        throw new SwiftMappingException(
            SwiftErrorCode.ERR_MAPPING_AMOUNT_MISSING,
            "InstdAmt or IntrBkSttlmAmt not found in pacs.008 document"
        );
    }

    private void validateAmountCurrency(String amountCurrency) {
        if (!amountCurrency.isEmpty()) {
            return;
        }
        throw new SwiftMappingException(
            SwiftErrorCode.ERR_INVALID_CURRENCY,
            "Currency (@Ccy attribute) not found in amount field"
        );
    }

    private void validateDebtorName(String debtorName) {
        if (!debtorName.isEmpty()) {
            return;
        }
        throw new SwiftMappingException(
            SwiftErrorCode.ERR_DEBTOR_NAME_MISSING,
            "Dbtr/Nm or InitgPty/Nm not found in pacs.008 document"
        );
    }

    private void validateCreditorName(String creditorName) {
        if (!creditorName.isEmpty()) {
            return;
        }
        throw new SwiftMappingException(
            SwiftErrorCode.ERR_CREDITOR_NAME_MISSING,
            "Cdtr/Nm or UltmtDbtr/Nm not found in pacs.008 document"
        );
    }

    private void validateSettlementDate(String settlementDate) {
        if (!settlementDate.isEmpty()) {
            return;
        }
        throw new SwiftMappingException(
            SwiftErrorCode.ERR_INVALID_SETTLEMENT_DATE,
            "IntrBkSttlmDt not found in pacs.008 document"
        );
    }

    private String evaluate(XPath xpath, Document document, String expression) {
        try {
            String value = xpath.evaluate(expression, document);
            return value == null ? "" : value.trim();
        } catch (Exception ex) {
            return "";
        }
    }

    private String resolveDebtorBic(XPath xpath, Document document) {
        return firstNonBlank(
            evaluate(xpath, document, Pacs008Xpaths.DBTR_BICFI),
            evaluate(xpath, document, Pacs008Xpaths.DBTR_BIC)
        );
    }

    private String resolveCreditorBic(XPath xpath, Document document) {
        return firstNonBlank(
            evaluate(xpath, document, Pacs008Xpaths.CDTR_BICFI),
            evaluate(xpath, document, Pacs008Xpaths.CDTR_BIC)
        );
    }

    private String resolveChargeBearer(XPath xpath, Document document) {
        return firstNonBlank(
            evaluate(xpath, document, Pacs008Xpaths.CHARGE_BEARER_PMTINF),
            evaluate(xpath, document, Pacs008Xpaths.CHARGE_BEARER_TX)
        );
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
            if (value != null && !normalizer.normalizeText(value).isEmpty()) {
                return normalizer.normalizeText(value);
            }
        }
        return "";
    }

    private static final class Pacs008NamespaceContext implements NamespaceContext {
        @Override
        public String getNamespaceURI(String prefix) {
            if (prefix == null) {
                throw new IllegalArgumentException("Prefix cannot be null");
            }
            if ("doc".equals(prefix)) {
                return PACS_008_NS;
            }
            if ("xml".equals(prefix)) {
                return XMLConstants.XML_NS_URI;
            }
            return XMLConstants.NULL_NS_URI;
        }

        @Override
        public String getPrefix(String namespaceURI) {
            if (PACS_008_NS.equals(namespaceURI)) {
                return "doc";
            }
            return null;
        }

        @Override
        public Iterator<String> getPrefixes(String namespaceURI) {
            String prefix = getPrefix(namespaceURI);
            if (prefix == null) {
                return Collections.emptyIterator();
            }
            return Collections.singleton(prefix).iterator();
        }
    }

    private record ScalarFields(
        String reference,
        String uetr,
        String amountValue,
        String amountCurrency,
        String settlementDate,
        String debtorName,
        String creditorName
    ) {
    }
}

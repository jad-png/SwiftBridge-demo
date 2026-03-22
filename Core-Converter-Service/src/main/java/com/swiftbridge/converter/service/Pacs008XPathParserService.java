package com.swiftbridge.converter.service;

import com.swiftbridge.converter.mapping.model.Pacs008XPathExtractionResult;
import com.swiftbridge.converter.utils.Pacs008Xpaths;
import com.swiftbridge.converter.utils.SwiftTruncationUtil;
import com.swiftbridge.converter.utils.TruncationResult;
import com.swiftbridge.converter.utils.XmlParsingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import javax.xml.XMLConstants;
import javax.xml.namespace.NamespaceContext;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class Pacs008XPathParserService {

    private static final String PACS_008_NS = "urn:iso:std:iso:20022:tech:xsd:pacs.008.001.02";

    private final XmlParsingService xmlParsingService;
    private final SwiftTruncationUtil swiftTruncationUtil;

    public Pacs008XPathExtractionResult parse(String xmlContent) {
        try {
            Document document = xmlParsingService.parseNamespaceAwareXml(xmlContent);
            XPath xpath = buildXPath();

            String debtorName = evaluate(xpath, document, Pacs008Xpaths.DEBTOR_NAME);
            String creditorName = evaluate(xpath, document, Pacs008Xpaths.CREDITOR_NAME);
            String interbankSettlementDate = evaluate(xpath, document, Pacs008Xpaths.SETTLEMENT_DATE_INTERBANK);
            String instructionId = evaluate(xpath, document, Pacs008Xpaths.REFERENCE_INSTR_ID);

            TruncationResult debtorNameTruncation = truncatePartyName("Debtor Name", debtorName);
            TruncationResult creditorNameTruncation = truncatePartyName("Creditor Name", creditorName);
            TruncationResult debtorAddressTruncation = truncatePartyAddress(xpath, document, "Debtor Address", Pacs008Xpaths.DEBTOR_ADDRESS_ROOT);
            TruncationResult creditorAddressTruncation = truncatePartyAddress(xpath, document, "Creditor Address", Pacs008Xpaths.CREDITOR_ADDRESS_ROOT);

            List<String> warnings = new ArrayList<>();
            if (debtorNameTruncation.truncated()) {
                warnings.add(debtorNameTruncation.warning());
            }
            if (creditorNameTruncation.truncated()) {
                warnings.add(creditorNameTruncation.warning());
            }
            if (debtorAddressTruncation.truncated()) {
                warnings.add(debtorAddressTruncation.warning());
            }
            if (creditorAddressTruncation.truncated()) {
                warnings.add(creditorAddressTruncation.warning());
            }

            return new Pacs008XPathExtractionResult(
                debtorName,
                creditorName,
                interbankSettlementDate,
                instructionId,
                debtorNameTruncation.lines(),
                creditorNameTruncation.lines(),
                debtorAddressTruncation.lines(),
                creditorAddressTruncation.lines(),
                warnings
            );
        } catch (Exception ex) {
            throw new RuntimeException("Failed to parse pacs.008 fields via XPath", ex);
        }
    }

    private TruncationResult truncatePartyName(String label, String value) {
        return swiftTruncationUtil.truncateNameOrAddress(label, value);
    }

    private TruncationResult truncatePartyAddress(XPath xpath, Document document, String label, String addressRootExpression) {
        return swiftTruncationUtil.truncateNameOrAddress(label, extractAddressLines(xpath, document, addressRootExpression));
    }

    private List<String> extractAddressLines(XPath xpath, Document document, String addressRootExpression) {
        try {
            NodeList lines = (NodeList) xpath.evaluate(addressRootExpression + "/doc:AdrLine", document, XPathConstants.NODESET);
            List<String> result = new ArrayList<>();
            for (int index = 0; index < lines.getLength(); index++) {
                String value = lines.item(index).getTextContent();
                if (value != null && !value.isBlank()) {
                    result.add(value.trim());
                }
            }
            return result;
        } catch (Exception ex) {
            return Collections.emptyList();
        }
    }

    private XPath buildXPath() {
        XPath xpath = XPathFactory.newInstance().newXPath();
        xpath.setNamespaceContext(new Pacs008NamespaceContext());
        return xpath;
    }

    private String evaluate(XPath xpath, Document document, String expression) {
        try {
            String value = xpath.evaluate(expression, document);
            return value == null ? "" : value.trim();
        } catch (Exception ex) {
            return "";
        }
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
}

package com.swiftbridge.converter.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.xml.XMLConstants;
import javax.xml.namespace.NamespaceContext;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathFactory;
import org.w3c.dom.Document;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;

/**
 * XmlToMtMapper Service
 * 
 * Converts pacs.008 ISO 20022 XML to MT103 SWIFT text format.
 * Uses standard javax.xml.xpath for field extraction without heavy JAXB parsing.
 * 
 * MT103 is a SWIFT format for Customer Credit Transfer messages.
 * Key tags used in this implementation:
 *   :20: = Transaction Reference Number
 *   :32A: = Amount and Currency
 *   :50A: = Ordering Customer
 *   :56A: = Intermediary
 *   :57A: = Account with Bank
 *   :59: = Beneficiary Customer
 * 
 * @author SwiftBridge Team
 * @version 1.0.0
 */
@Service
@Slf4j
public class XmlToMtMapper {

    /**
     * Converts pacs.008 XML to MT103 text format
     * 
     * @param xmlContent the pacs.008 XML as a string
     * @return the formatted MT103 string
     * @throws Exception if XML parsing or field extraction fails
     */
    public String convertPacs008ToMt103(String xmlContent) throws Exception {
        log.info("Starting pacs.008 to MT103 conversion");

        try {
            // Parse XML document
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setXIncludeAware(false);
            factory.setExpandEntityReferences(false);

            Document document = factory.newDocumentBuilder()
                .parse(new ByteArrayInputStream(xmlContent.getBytes(StandardCharsets.UTF_8)));

            // Create XPath expression evaluator
            XPath xpath = XPathFactory.newInstance().newXPath();
            xpath.setNamespaceContext(new Pacs008NamespaceContext());

            // Extract required fields from XML using XPath
            String messageId = extractField(xpath, document, "//GrpHdr/MsgId", "UNKNOWN");
            String amount = extractField(xpath, document, "//CdtTrfTxInf/InstdAmt", "0.00");
            String currency = extractField(xpath, document, "//CdtTrfTxInf/InstdAmt/@Ccy", "USD");
            String ordererName = extractField(xpath, document, "//InitgPty/Nm", "ORDERER");
            String beneficiaryName = extractField(xpath, document, "//UltmtDbtr/Nm", "BENEFICIARY");

            log.info("Extracted fields - MsgId: {}, Amount: {} {}", messageId, amount, currency);

            // Generate MT103 string with hardcoded template
            String mt103 = generateMt103(messageId, amount, currency, ordererName, beneficiaryName);

            log.info("MT103 conversion completed successfully");
            return mt103;

        } catch (Exception e) {
            log.error("Error converting XML to MT103", e);
            throw new RuntimeException("Failed to convert pacs.008 to MT103: " + e.getMessage(), e);
        }
    }

    /**
     * Extracts a field value from the XML document using XPath
     * 
     * @param xpath the XPath evaluator
     * @param document the XML document
     * @param expression the XPath expression
     * @param defaultValue the default value if field not found
     * @return the extracted field value or default value
     */
    private String extractField(XPath xpath, Document document, String expression, String defaultValue) {
        try {
            String value = xpath.evaluate(expression, document);
            if (value == null || value.trim().isEmpty()) {
                log.warn("Field not found with expression: {}, using default: {}", expression, defaultValue);
                return defaultValue;
            }
            return value.trim();
        } catch (Exception e) {
            log.warn("Error extracting field with expression: {}, using default: {}", expression, defaultValue);
            return defaultValue;
        }
    }

    /**
     * Generates the MT103 message with a hardcoded template
     * 
     * MT103 is the SWIFT format for Customer Credit Transfer.
     * This is a simplified version for MVP demonstration.
     * 
     * @param messageId the message ID from pacs.008
     * @param amount the transfer amount
     * @param currency the currency code
     * @param ordererName the ordering customer name
     * @param beneficiaryName the beneficiary customer name
     * @return the formatted MT103 string
     */
    private String generateMt103(String messageId, String amount, String currency, 
                                 String ordererName, String beneficiaryName) {
        
        // Parse amount to ensure proper formatting (assuming it's in ISO numeric format)
        String formattedAmount = formatAmount(amount, currency);

        StringBuilder mt103 = new StringBuilder();

        // MT103 Header
        mt103.append(":20:").append(messageId).append("\r\n");

        // Transaction Reference Number (unique identifier for this transfer)
        mt103.append(":13C:/RECI").append(messageId).append("\r\n");

        // Amount and Currency
        mt103.append(":32A:").append(formattedAmount).append("\r\n");

        // Value Date implicit in 32A (YYMMDD format)
        mt103.append(":50A:/").append(messageId).append("\r\n");
        mt103.append(ordererName).append("\r\n");

        // Beneficiary
        mt103.append(":59:/BIC\r\n");
        mt103.append(beneficiaryName).append("\r\n");

        // Details of Charges
        mt103.append(":71A:SHA\r\n");

        // Regulatory Reporting Code (simplified)
        mt103.append(":77B:CONVERSION FROM ISO 20022 PACS.008\r\n");

        // End of message
        mt103.append("-}");

        return mt103.toString();
    }

    /**
     * Formats the amount in MT103 format (YYMMDDCCY with proper padding)
     * 
     * @param amount the amount value
     * @param currency the currency code
     * @return the formatted amount string
     */
    private String formatAmount(String amount, String currency) {
        // For MVP, we'll use a simple date format with current date
        // In production, you'd extract the value date from the XML
        String today = java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("yyMMdd"));
        
        // Ensure amount is padded with leading zeros (e.g., "1000.50" becomes "001000,50")
        String[] parts = amount.split("\\.");
        String integerPart = parts[0];
        String decimalPart = parts.length > 1 ? parts[1] : "00";
        
        // Pad decimal part to 2 digits
        decimalPart = String.format("%-2s", decimalPart).replace(' ', '0');
        
        // MT103 amount format: YYMMDDCCC amount (using comma as decimal separator per SWIFT standard)
        return today + currency + integerPart + "," + decimalPart;
    }

    /**
     * Custom NamespaceContext for pacs.008 namespace handling
     * Handles the default namespace and common prefixes
     */
    private static class Pacs008NamespaceContext implements NamespaceContext {
        @Override
        public String getNamespaceURI(String prefix) {
            if (prefix == null) {
                throw new IllegalArgumentException("Prefix cannot be null");
            }
            if ("xmlns".equals(prefix)) {
                return "http://www.w3.org/2000/xmlns/";
            }
            if ("".equals(prefix) || "doc".equals(prefix)) {
                return "urn:iso:std:iso:20022:tech:xsd:pacs.008.001.02";
            }
            return null;
        }

        @Override
        public String getPrefix(String namespaceURI) {
            if ("urn:iso:std:iso:20022:tech:xsd:pacs.008.001.02".equals(namespaceURI)) {
                return "";
            }
            return null;
        }

        @Override
        public Iterator getPrefixes(String namespaceURI) {
            return java.util.Collections.emptyIterator();
        }
    }
}

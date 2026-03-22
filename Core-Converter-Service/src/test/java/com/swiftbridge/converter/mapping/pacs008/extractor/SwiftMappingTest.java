package com.swiftbridge.converter.mapping.pacs008.extractor;

import com.swiftbridge.converter.exception.SwiftErrorCode;
import com.swiftbridge.converter.exception.SwiftMappingException;
import com.swiftbridge.converter.mapping.model.Pacs008Fields;
import com.swiftbridge.converter.mapping.mt103.normalizer.SwiftFieldNormalizer;
import com.swiftbridge.converter.utils.XmlParsingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.w3c.dom.Document;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Pacs008 Field Extraction and Mapping Tests")
class SwiftMappingTest {

    private static final String TEST_RESOURCE_ROOT = "src/test/resources/";

    @Mock
    private SwiftFieldNormalizer normalizer;

    private Pacs008FieldExtractor fieldExtractor;
    private XmlParsingService xmlParsingService;

    @BeforeEach
    void setUp() {
        xmlParsingService = new XmlParsingService();
        fieldExtractor = new Pacs008FieldExtractor(normalizer);
    }

    @Test
    @DisplayName("Extract valid pacs.008 fields successfully to MT103 format")
    void testExtractValidPacs008Fields() throws Exception {

        Document document = parseFixture("valid-pacs008.xml");

        Pacs008Fields fields = fieldExtractor.extract(document);

        assertNotNull(fields, "Fields object should not be null");
        assertNotNull(fields.amountValue(), "Amount value should be extracted");
        assertNotNull(fields.amountCurrency(), "Currency should be extracted");
        assertNotNull(fields.debtorName(), "Debtor name should be extracted");
        assertNotNull(fields.creditorName(), "Creditor name should be extracted");
        assertNotNull(fields.settlementDate(), "Settlement date should be extracted");

        assertEquals("500000.00", fields.amountValue(), "Amount should be 500000.00");
        assertEquals("USD", fields.amountCurrency(), "Currency should be USD");
        assertTrue(fields.debtorName().contains("JOHN SMITH"),
            "Debtor name should contain JOHN SMITH");
        assertTrue(fields.creditorName().contains("JANE DOE"),
            "Creditor name should contain JANE DOE");
        assertEquals("2026-03-20", fields.settlementDate(), "Settlement date should be 2026-03-20");
    }

    @Test
    @DisplayName("Throw error when amount field is missing from pacs.008")
    void testThrowErrorWhenAmountMissing() throws Exception {

        Document document = parseFixture("missing-amount.xml");

        SwiftMappingException exception = assertThrows(
            SwiftMappingException.class,
            () -> fieldExtractor.extract(document),
            "Should throw SwiftMappingException when amount is missing"
        );

        assertNotNull(exception.getErrorCode(), "Error code should not be null");
        assertEquals("SB-4001", exception.getErrorCode().getCode(),
            "Error code should be SB-4001 for missing amount");
    }

    @Test
    @DisplayName("Extract BIC codes for debtor and creditor")
    void testExtractBicCodes() throws Exception {

        Document document = parseFixture("valid-pacs008.xml");

        Pacs008Fields fields = fieldExtractor.extract(document);

        assertNotNull(fields.debtorBic(), "Debtor BIC should be extracted");
        assertEquals("WESTGB2L", fields.debtorBic(), "Debtor BIC should match pacs.008");
        assertNotNull(fields.creditorBic(), "Creditor BIC should be extracted");
        assertEquals("CHUSUSNYC", fields.creditorBic(), "Creditor BIC should match pacs.008");
    }

    @Test
    @DisplayName("Extract reference and UETR fields (optional but present)")
    void testExtractOptionalReferenceAndUetr() throws Exception {

        Document document = parseFixture("valid-pacs008.xml");

        Pacs008Fields fields = fieldExtractor.extract(document);

        assertNotNull(fields.reference(), "Reference should be extracted");
        assertEquals("12345ABCDE", fields.reference(), "Reference should match pacs.008");
        assertNotNull(fields.uetr(), "UETR should be extracted");
        assertTrue(fields.uetr().contains("UETR"), "UETR should match pacs.008");
    }

    @Test
    @DisplayName("Validate that non-empty fields are correctly populated")
    void testFieldValidationForNonEmptyValues() throws Exception {

        Document document = parseFixture("valid-pacs008.xml");

        Pacs008Fields fields = fieldExtractor.extract(document);

        assertFalse(fields.amountValue().isEmpty(), "Amount value should not be empty");
        assertFalse(fields.amountCurrency().isEmpty(), "Amount currency should not be empty");
        assertFalse(fields.debtorName().isEmpty(), "Debtor name should not be empty");
        assertFalse(fields.creditorName().isEmpty(), "Creditor name should not be empty");
        assertFalse(fields.settlementDate().isEmpty(), "Settlement date should not be empty");
    }

    private String readTestResource(String filename) throws Exception {
        String resourcePath = TEST_RESOURCE_ROOT + filename;
        return new String(Files.readAllBytes(Paths.get(resourcePath)), StandardCharsets.UTF_8);
    }

    private Document parseFixture(String filename) throws Exception {
        return xmlParsingService.parseNamespaceAwareXml(readTestResource(filename));
    }
}

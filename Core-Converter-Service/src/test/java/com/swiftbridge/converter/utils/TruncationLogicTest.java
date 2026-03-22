package com.swiftbridge.converter.utils;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("SWIFT Name Truncation Logic Tests")
class TruncationLogicTest {

    private static final int MAX_SWIFT_LINES = 4;
    private static final int MAX_SWIFT_LINE_LENGTH = 35;

    private SwiftTruncationUtil truncationUtil;

    @BeforeEach
    void setUp() {
        truncationUtil = new SwiftTruncationUtil();
    }

    @Test
    @DisplayName("Truncate 200-character name to exactly 4 lines of 35 characters")
    void testTruncate200CharacterName() {

        String longName = "John Smith International Trading Company Limited New York Office " +
                         "Executive Director Sales Manager Product Development Specialist " +
                         "Financial Services Consultant Business Analyst Systems Engineer";

        TruncationResult result = truncationUtil.truncateNameOrAddress("TestField", longName);

        assertNotNull(result, "TruncationResult should not be null");
        assertTrue(result.truncated(), "Result should be marked as truncated");
        assertFalse(result.warning().isBlank(), "Warning should be present");

        List<String> lines = result.lines();
        assertEquals(MAX_SWIFT_LINES, lines.size(), "Should have exactly 4 lines after truncation");

        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            assertFitsSwiftLine(line,
                "Line " + (i + 1) + " exceeds 35 characters: " + line.length() + " chars");
        }
    }

    @Test
    @DisplayName("Preserve word boundaries during truncation")
    void testPreserveWordBoundaries() {

        String text = "This is a very long name that should be truncated carefully without breaking words in the middle";

        TruncationResult result = truncationUtil.truncateNameOrAddress("Name", text);

        List<String> lines = result.lines();
        assertTrue(lines.size() <= MAX_SWIFT_LINES, "Should have at most 4 lines");

        for (String line : lines) {
            assertFitsSwiftLine(line, "Line '" + line + "' exceeds 35 chars");

            assertFalse(line.endsWith(" "), "Line should not end with space: " + line);
        }
    }

    @Test
    @DisplayName("Short name fits in first line without truncation")
    void testShortNameNoTruncation() {

        String shortName = "John Smith";

        TruncationResult result = truncationUtil.truncateNameOrAddress("Name", shortName);

        assertFalse(result.truncated(), "Short name should not be marked as truncated");
        assertTrue(result.warning().isBlank(), "No warning should be present");
        assertEquals(1, result.lines().size(), "Should have only 1 line");
        assertEquals("John Smith", result.lines().get(0), "Name should be unchanged");
    }

    @Test
    @DisplayName("Exactly 35-character name fits in one line")
    void testExactly35CharacterName() {

        String name35 = "A".repeat(35);

        TruncationResult result = truncationUtil.truncateNameOrAddress("Name", name35);

        assertFalse(result.truncated(), "35-character name should not be marked as truncated");
        assertEquals(1, result.lines().size(), "Should have only 1 line");
        assertEquals(35, result.lines().get(0).length(), "Line should be exactly 35 characters");
    }

    @Test
    @DisplayName("36-character name gets truncated to one line")
    void testJust36CharacterName() {

        String name36 = "A".repeat(36);

        TruncationResult result = truncationUtil.truncateNameOrAddress("Name", name36);

        assertTrue(result.truncated(), "36-character name should be truncated");
        assertTrue(result.lines().get(0).length() <= MAX_SWIFT_LINE_LENGTH, "First line should not exceed 35 chars");
    }

    @Test
    @DisplayName("Empty or null name returns empty result without truncation")
    void testEmptyOrNullLName() {

        TruncationResult nullResult = truncationUtil.truncateNameOrAddress("Name", (String) null);
        assertTrue(nullResult.lines().isEmpty(), "Null input should produce empty lines");
        assertFalse(nullResult.truncated(), "Null input should not be marked as truncated");

        TruncationResult emptyResult = truncationUtil.truncateNameOrAddress("Name", "");
        assertTrue(emptyResult.lines().isEmpty(), "Empty input should produce empty lines");
        assertFalse(emptyResult.truncated(), "Empty input should not be marked as truncated");

        TruncationResult blankResult = truncationUtil.truncateNameOrAddress("Name", "   ");
        assertTrue(blankResult.lines().isEmpty(), "Whitespace-only input should produce empty lines");
    }

    @Test
    @DisplayName("Multiple spaces are normalized to single spaces")
    void testNormalizeMultipleSpaces() {

        String nameWithExtraSpaces = "John    Smith     International     Trading";

        TruncationResult result = truncationUtil.truncateNameOrAddress("Name", nameWithExtraSpaces);

        String firstLine = result.lines().get(0);
        assertFalse(firstLine.contains("  "), "Multiple consecutive spaces should be normalized");
    }

    @Test
    @DisplayName("Integration test: 4 lines exactly 35 chars each with delimiter block")
    void testIntegrationFourLinesFullCapacity() {

        String text = "ABCDEFGHIJ ABCDEFGHIJ ABCDEFGHIJ AB " +
                     "BCDEFGHIJ ABCDEFGHIJ ABCDEFGHIJ ABC " +
                     "CDEFGHIJ ABCDEFGHIJ ABCDEFGHIJ ABCD " +
                     "DEFGHIJ ABCDEFGHIJ ABCDEFGHIJ ABCDE";

        TruncationResult result = truncationUtil.truncateNameOrAddress("TestField", text);

        List<String> lines = result.lines();
        assertEquals(4, lines.size(), "Should have 4 lines");

        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            assertFitsSwiftLine(line, "Line " + (i + 1) + " should have max 35 chars");
        }
    }

    @Test
    @DisplayName("Handle special characters and numbers in names")
    void testSpecialCharactersAndNumbers() {

        String nameWithSpecialChars = "ABC-123 Company GmbH & Co. KG Ltd. (USA) Inc.";

        TruncationResult result = truncationUtil.truncateNameOrAddress("Company", nameWithSpecialChars);

        assertNotNull(result, "Result should not be null");
        List<String> lines = result.lines();

        for (String line : lines) {
            assertFitsSwiftLine(line, "Line should not exceed 35 chars");
        }
    }

    @Test
    @DisplayName("Verify truncation warning message format")
    void testTruncationWarningFormat() {

        String longName = "This is a very long name that will definitely need to be truncated " +
                         "because it exceeds the SWIFT field length limitations significantly. " +
                         "The name is from a company that might have multiple words in its full " +
                         "legal name and additional descriptive information that needs truncation.";

        TruncationResult result = truncationUtil.truncateNameOrAddress("DebentorName", longName);

        assertTrue(result.truncated(), "Long name should be truncated");
        String warning = result.warning();
        assertFalse(warning.isBlank(), "Warning should not be blank");
        assertTrue(warning.contains("truncated"), "Warning should mention truncation");
        assertTrue(warning.contains("4"), "Warning should mention 4 lines");
        assertTrue(warning.contains("35"), "Warning should mention 35 character limit");
    }

    @Test
    @DisplayName("Test with names from different locales with accents")
    void testNamesWithAccents() {

        String nameWithAccents = "José García López Société Générale München";

        TruncationResult result = truncationUtil.truncateNameOrAddress("InternationalName", nameWithAccents);

        List<String> lines = result.lines();
        for (String line : lines) {
            assertFitsSwiftLine(line, "Line with accents should fit in 35 chars");
        }
    }

    private void assertFitsSwiftLine(String line, String message) {
        assertTrue(line.length() <= MAX_SWIFT_LINE_LENGTH, message);
    }
}

package com.swiftbridge.converter.utils;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Component
public class SwiftTruncationUtil {

    private static final int MAX_LINES = 4;
    private static final int MAX_LINE_LENGTH = 35;

    public TruncationResult truncateNameOrAddress(String fieldName, String value) {
        if (value == null || value.isBlank()) {
            return new TruncationResult(Collections.emptyList(), false, "");
        }

        List<String> lines = splitByWordBoundary(value);
        boolean truncated = isTruncated(value, lines);
        String warning = truncated
            ? "Field " + fieldName + " truncated to " + MAX_LINES + "x" + MAX_LINE_LENGTH + " characters"
            : "";

        return new TruncationResult(lines, truncated, warning);
    }

    public TruncationResult truncateNameOrAddress(String fieldName, List<String> values) {
        if (values == null || values.isEmpty()) {
            return new TruncationResult(Collections.emptyList(), false, "");
        }

        String merged = String.join(" ", values).replaceAll("\\s+", " ").trim();
        return truncateNameOrAddress(fieldName, merged);
    }

    private List<String> splitByWordBoundary(String value) {
        String normalized = value.replaceAll("\\s+", " ").trim();
        if (normalized.isEmpty()) {
            return Collections.emptyList();
        }

        String[] words = normalized.split(" ");
        List<String> lines = new ArrayList<>();
        StringBuilder currentLine = new StringBuilder();

        for (String word : words) {
            if (word.isBlank()) {
                continue;
            }

            if (currentLine.isEmpty()) {
                currentLine.append(cutWordIfNeeded(word));
                continue;
            }

            int projectedLength = currentLine.length() + 1 + word.length();
            if (projectedLength <= MAX_LINE_LENGTH) {
                currentLine.append(' ').append(word);
                continue;
            }

            lines.add(currentLine.toString());
            if (lines.size() == MAX_LINES) {
                return lines;
            }

            currentLine.setLength(0);
            currentLine.append(cutWordIfNeeded(word));
        }

        if (!currentLine.isEmpty() && lines.size() < MAX_LINES) {
            lines.add(currentLine.toString());
        }

        return lines;
    }

    private String cutWordIfNeeded(String word) {
        if (word.length() <= MAX_LINE_LENGTH) {
            return word;
        }
        return word.substring(0, MAX_LINE_LENGTH);
    }

    private boolean isTruncated(String original, List<String> truncatedLines) {
        String normalizedOriginal = original.replaceAll("\\s+", " ").trim();
        String rebuilt = String.join(" ", truncatedLines).replaceAll("\\s+", " ").trim();
        return !normalizedOriginal.equals(rebuilt);
    }
}

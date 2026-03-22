package com.swiftbridge.converter.mapping.mt103.formatter;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import com.swiftbridge.converter.mapping.mt103.normalizer.SwiftFieldNormalizer;
import com.swiftbridge.converter.utils.SwiftTruncationUtil;
import com.swiftbridge.converter.utils.TruncationResult;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

@Component
@RequiredArgsConstructor
public class Mt103Formatter {

    private final SwiftFieldNormalizer normalizer;
    private final SwiftTruncationUtil swiftTruncationUtil;

    public String serializeMt103(String tag20,
                                 String tag32A,
                                 List<String> tag50KLines,
                                 String tag52A,
                                 String tag57A,
                                 List<String> tag59Lines,
                                 String tag71A) {

        StringBuilder builder = new StringBuilder();
        builder.append("{4:\r\n");
        builder.append(":20:").append(tag20).append("\r\n");
        builder.append(":32A:").append(tag32A).append("\r\n");

        appendTag50K(builder, tag50KLines);

        appendOptionalTag(builder, "52A", tag52A);
        appendOptionalTag(builder, "57A", tag57A);

        appendTag59(builder, tag59Lines);

        builder.append(":71A:").append(tag71A).append("\r\n");
        builder.append("-}");

        return builder.toString();
    }

    private void appendTag50K(StringBuilder builder, List<String> tag50KLines) {
        builder.append(":50K:").append("\r\n");
        for (String line : tag50KLines) {
            builder.append(trimToLength(line, 35)).append("\r\n");
        }
    }

    private void appendTag59(StringBuilder builder, List<String> tag59Lines) {
        builder.append(":59:").append("\r\n");
        for (String line : tag59Lines) {
            builder.append(trimToLength(line, 35)).append("\r\n");
        }
    }

    private void appendOptionalTag(StringBuilder builder, String tagName, String value) {
        if (!value.isBlank()) {
            builder.append(':').append(tagName).append(':').append(value).append("\r\n");
        }
    }

    public List<String> buildPartyLines(String name, List<String> addressLines) {
        return buildPartyLinesWithWarning("", name, addressLines).lines();
    }

    public TruncationResult buildPartyLinesWithWarning(String fieldTag, String name, List<String> addressLines) {
        List<String> sourceParts = new ArrayList<>();
        if (name != null && !normalizer.normalizeText(name).isEmpty()) {
            sourceParts.add(normalizer.normalizeText(name));
        }
        if (addressLines != null) {
            addressLines.stream()
                .filter(Objects::nonNull)
                .map(normalizer::normalizeText)
                .filter(value -> !value.isEmpty())
                .forEach(sourceParts::add);
        }

        String merged = String.join(" ", sourceParts).trim();
            String normalizedFieldTag = normalizer.normalizeText(fieldTag);
            String warningField = normalizedFieldTag.isEmpty() ? "field" : normalizedFieldTag;
            return swiftTruncationUtil.truncateNameOrAddress(warningField, merged);
    }

    public List<String> smartTruncateToMtLines(String text, int maxLines, int lineLength) {
        if (text == null || normalizer.normalizeText(text).isEmpty()) {
            return Collections.singletonList("NOTPROVIDED");
        }

        String normalized = normalizer.normalizeText(text);
        String[] words = normalized.split(" ");
        List<String> lines = new ArrayList<>();
        StringBuilder current = new StringBuilder();

        for (String word : words) {
            if (word.isBlank()) {
                continue;
            }

            if (current.isEmpty()) {
                current.append(word);
                continue;
            }

            int projectedLength = current.length() + 1 + word.length();
            if (projectedLength <= lineLength) {
                current.append(' ').append(word);
                continue;
            }

            lines.add(trimToLength(current.toString(), lineLength));
            current.setLength(0);
            current.append(word);

            if (lines.size() == maxLines) {
                break;
            }
        }

        if (lines.size() < maxLines && !current.isEmpty()) {
            lines.add(trimToLength(current.toString(), lineLength));
        }

        if (lines.size() > maxLines) {
            return new ArrayList<>(lines.subList(0, maxLines));
        }
        return lines;
    }

    public String trimToLength(String value, int maxLength) {
        if (value.length() <= maxLength) {
            return value;
        }
        int lastSpace = value.lastIndexOf(' ', maxLength);
        if (lastSpace > 0) {
            return value.substring(0, lastSpace).trim();
        }
        return value.substring(0, maxLength).trim();
    }
}

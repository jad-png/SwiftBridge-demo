package com.swiftbridge.converter.mapping.mt103.builder;

import com.swiftbridge.converter.mapping.model.mt103.ApplicationHeaderBlock;
import com.swiftbridge.converter.mapping.model.mt103.BasicHeaderBlock;
import com.swiftbridge.converter.mapping.model.mt103.Mt103Message;
import com.swiftbridge.converter.mapping.model.mt103.TextBlock;
import com.swiftbridge.converter.mapping.model.mt103.TrailerBlock;
import com.swiftbridge.converter.mapping.model.mt103.UserHeaderBlock;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

@Component
public class MT103Builder {

    public String build(Mt103Message message) {
        BasicHeaderBlock block1 = message.getBlock1();
        ApplicationHeaderBlock block2 = message.getBlock2();
        UserHeaderBlock block3 = message.getBlock3();
        TextBlock block4 = message.getBlock4();
        TrailerBlock block5 = message.getBlock5();

        String block1Value = buildBlock1(block1);
        String block2Value = buildBlock2(block2);
        String block3Value = buildBlock3(block3);
        String block4Value = buildBlock4(block4);
        String block5Value = buildBlock5(block5, block1Value, block2Value, block3Value, block4Value);

        return "{1:" + block1Value + "}"
            + "{2:" + block2Value + "}"
            + "{3:" + block3Value + "}"
            + "{4:\r\n" + block4Value + "\r\n-}"
            + "{5:" + block5Value + "}";
    }

    private String buildBlock1(BasicHeaderBlock block) {
        String applicationId = oneOf(block == null ? null : block.getApplicationId(), "F");
        String serviceId = padDigits(oneOf(block == null ? null : block.getServiceId(), "01"), 2);
        String logicalTerminal = normalizeLogicalTerminal(block == null ? null : block.getLogicalTerminal());
        String sessionNumber = padDigits(oneOf(block == null ? null : block.getSessionNumber(), "0000"), 4);
        String sequenceNumber = padDigits(oneOf(block == null ? null : block.getSequenceNumber(), "000000"), 6);
        return applicationId + serviceId + logicalTerminal + sessionNumber + sequenceNumber;
    }

    private String buildBlock2(ApplicationHeaderBlock block) {
        String io = oneOf(block == null ? null : block.getInputOutputId(), "I").toUpperCase(Locale.ROOT);
        String messageType = padDigits(oneOf(block == null ? null : block.getMessageType(), "103"), 3);
        String receiverLt = normalizeLogicalTerminal(block == null ? null : block.getReceiverLogicalTerminal());
        String priority = oneOf(block == null ? null : block.getPriority(), "N").toUpperCase(Locale.ROOT);
        return io + messageType + receiverLt + priority;
    }

    private String buildBlock3(UserHeaderBlock block) {
        String uetr = normalizeUetr(block == null ? null : block.getUetr());
        return "{121:" + uetr + "}";
    }

    private String buildBlock4(TextBlock block) {
        String tag20 = oneOf(block == null ? null : block.getTag20(), "UNKNOWNREF");
        String tag32A = oneOf(block == null ? null : block.getTag32A(), "000000USD0,00");

        List<String> tag50KLines = normalizePartyLines(block == null ? null : block.getTag50KLines());
        List<String> tag59Lines = normalizePartyLines(block == null ? null : block.getTag59Lines());

        StringBuilder builder = new StringBuilder();
        builder.append(":20:").append(trimToLength(tag20, 16)).append("\r\n");
        builder.append(":32A:").append(trimToLength(tag32A, 24)).append("\r\n");

        builder.append(":50K:").append("\r\n");
        for (String line : tag50KLines) {
            builder.append(trimToLength(line, 35)).append("\r\n");
        }

        String tag52A = oneOf(block == null ? null : block.getTag52A(), "");
        if (!tag52A.isBlank()) {
            builder.append(":52A:").append(trimToLength(tag52A, 12)).append("\r\n");
        }

        String tag57A = oneOf(block == null ? null : block.getTag57A(), "");
        if (!tag57A.isBlank()) {
            builder.append(":57A:").append(trimToLength(tag57A, 12)).append("\r\n");
        }

        builder.append(":59:").append("\r\n");
        for (String line : tag59Lines) {
            builder.append(trimToLength(line, 35)).append("\r\n");
        }

        String tag71A = oneOf(block == null ? null : block.getTag71A(), "");
        if (!tag71A.isBlank()) {
            builder.append(":71A:").append(trimToLength(tag71A, 3)).append("\r\n");
        }

        return stripTrailingCrlf(builder.toString());
    }

    private String buildBlock5(TrailerBlock block,
                               String block1,
                               String block2,
                               String block3,
                               String block4) {
        String payload = block1 + block2 + block3 + block4;

        String checksum = oneOf(block == null ? null : block.getChecksum(), "");
        if (checksum.isBlank()) {
            checksum = digestHex(payload).substring(0, 12);
        }

        String mac = oneOf(block == null ? null : block.getMac(), "");
        if (mac.isBlank()) {
            mac = digestHex("MAC" + payload).substring(0, 16);
        }

        return "{CHK:" + checksum.toUpperCase(Locale.ROOT) + "}{MAC:" + mac.toUpperCase(Locale.ROOT) + "}";
    }

    private String digestHex(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(value.getBytes(StandardCharsets.UTF_8));

            StringBuilder builder = new StringBuilder();
            for (byte current : bytes) {
                builder.append(String.format("%02x", current));
            }
            return builder.toString();
        } catch (Exception ex) {
            return UUID.randomUUID().toString().replace("-", "");
        }
    }

    private String normalizeLogicalTerminal(String value) {
        String normalized = oneOf(value, "BANKBEBBAXXX").replaceAll("\\s+", "").toUpperCase(Locale.ROOT);
        if (normalized.length() >= 12) {
            return normalized.substring(0, 12);
        }
        return String.format(Locale.ROOT, "%-12s", normalized).replace(' ', 'X');
    }

    private String normalizeUetr(String value) {
        String normalized = oneOf(value, "").trim();
        if (normalized.matches("(?i)^[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$")) {
            return normalized.toLowerCase(Locale.ROOT);
        }
        return UUID.randomUUID().toString();
    }

    private List<String> normalizePartyLines(List<String> lines) {
        if (lines == null || lines.isEmpty()) {
            return Collections.singletonList("NOTPROVIDED");
        }

        List<String> normalized = new ArrayList<>();
        for (String line : lines) {
            if (line == null) {
                continue;
            }
            String value = line.replaceAll("\\s+", " ").trim();
            if (!value.isEmpty()) {
                normalized.add(value);
            }
            if (normalized.size() == 4) {
                break;
            }
        }

        if (normalized.isEmpty()) {
            return Collections.singletonList("NOTPROVIDED");
        }
        return normalized;
    }

    private String padDigits(String value, int size) {
        String digits = oneOf(value, "").replaceAll("\\D", "");
        if (digits.length() > size) {
            return digits.substring(digits.length() - size);
        }
        return String.format(Locale.ROOT, "%" + size + "s", digits).replace(' ', '0');
    }

    private String trimToLength(String value, int maxLength) {
        if (value == null) {
            return "";
        }
        String normalized = value.replaceAll("\\s+", " ").trim();
        if (normalized.length() <= maxLength) {
            return normalized;
        }
        return normalized.substring(0, maxLength);
    }

    private String oneOf(String value, String fallback) {
        String normalized = Objects.toString(value, "").trim();
        return normalized.isEmpty() ? fallback : normalized;
    }

    private String stripTrailingCrlf(String value) {
        if (value.endsWith("\r\n")) {
            return value.substring(0, value.length() - 2);
        }
        return value;
    }
}

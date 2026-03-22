package com.swiftbridge.converter.mapping.mt103.normalizer;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Component
public class SwiftFieldNormalizer {

    private static final DateTimeFormatter ISO_DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter MT_32A_DATE = DateTimeFormatter.ofPattern("yyMMdd");
    private static final String DEFAULT_REFERENCE = "UNKNOWNREF";
    private static final String DEFAULT_CURRENCY = "USD";
    private static final int ISO_CURRENCY_LENGTH = 3;
    private static final int BIC8_LENGTH = 8;
    private static final int BIC11_LENGTH = 11;

    public String normalizeReference(String reference) {
        String normalized = normalizeText(reference);
        return normalized.isEmpty() ? defaultReference() : normalized;
    }

    private String defaultReference() {
        return DEFAULT_REFERENCE;
    }

    public String normalize32A(String dateIso, String currency, String amountRaw) {
        LocalDate date = normalizeSettlementDate(dateIso);
        String ccy = normalizeCurrency(currency);
        String amount = normalizeAmountForMt(amountRaw);
        return date.format(MT_32A_DATE) + ccy + amount;
    }

    private LocalDate normalizeSettlementDate(String dateIso) {
        return parseDateOrToday(dateIso);
    }

    public String normalizeAmountForMt(String amountRaw) {
        try {
            String normalized = normalizeText(amountRaw).replace(",", ".");
            if (normalized.isEmpty()) {
                return "0,00";
            }
            BigDecimal amount = parseAmount(normalized).stripTrailingZeros();
            String plain = amount.toPlainString();
            return plain.replace('.', ',');
        } catch (Exception ex) {
            return "0,00";
        }
    }

    private BigDecimal parseAmount(String normalizedAmount) {
        return new BigDecimal(normalizedAmount);
    }

    public String normalizeCurrency(String currency) {
        String value = normalizeText(currency).toUpperCase(Locale.ROOT);
        if (value.length() != ISO_CURRENCY_LENGTH) {
            return DEFAULT_CURRENCY;
        }
        return value;
    }

    public String normalizeBic(String bic) {
        String value = normalizeText(bic).toUpperCase(Locale.ROOT).replace(" ", "");
        if (value.length() == BIC8_LENGTH) {
            return value + "XXX";
        }
        if (value.length() == BIC11_LENGTH) {
            return value;
        }
        return "";
    }

    public String mapChargeBearer(String isoChargeBearer) {
        String value = normalizeChargeBearerKey(isoChargeBearer);
        return switch (value) {
            case "SHAR", "SHA" -> "SHA";
            case "DEBT", "OUR" -> "OUR";
            case "CRED", "BEN" -> "BEN";
            default -> "SHA";
        };
    }

    private String normalizeChargeBearerKey(String isoChargeBearer) {
        return normalizeText(isoChargeBearer).toUpperCase(Locale.ROOT);
    }

    public String normalizeText(String value) {
        if (value == null) {
            return "";
        }
        return value.replaceAll("\\s+", " ").trim();
    }

    private LocalDate parseDateOrToday(String dateIso) {
        try {
            String normalized = normalizeText(dateIso);
            if (normalized.isEmpty()) {
                return LocalDate.now();
            }
            return LocalDate.parse(normalized, ISO_DATE);
        } catch (Exception ex) {
            return LocalDate.now();
        }
    }
}

package com.swiftbridge.orchestrator.service.Impl;

import org.springframework.stereotype.Service;

import com.swiftbridge.orchestrator.service.ConversionValidator;
import com.swiftbridge.orchestrator.service.FinancialValidationException;
import com.swiftbridge.orchestrator.service.ConversionValidator.ValidationResult;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class FinancialValidationService implements ConversionValidator {

    private static final Pattern TAG_20 = Pattern.compile("(?m)^:20:(.+)$");
    private static final Pattern TAG_32A = Pattern.compile("(?m)^:32A:(\\d{6})([A-Z]{3})([0-9,]+(?:\\.[0-9]+)?)$");
    private static final Pattern TAG_52A = Pattern.compile("(?m)^:52A:([A-Z0-9]{8}(?:[A-Z0-9]{3})?)$");
    private static final Pattern TAG_57A = Pattern.compile("(?m)^:57A:([A-Z0-9]{8}(?:[A-Z0-9]{3})?)$");
    private static final Pattern UETR_BLOCK = Pattern.compile("\\{3:\\{121:([0-9a-fA-F-]{36})}}", Pattern.CASE_INSENSITIVE);
    private static final Pattern UETR_FORMAT = Pattern.compile("(?i)^[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$");

    private static final Set<String> ISO_4217_CODES = Currency.getAvailableCurrencies()
        .stream()
        .map(Currency::getCurrencyCode)
        .collect(Collectors.toSet());

    @Override
    public ValidationResult validate(String mt103) {
        String instructionId = extractFirst(TAG_20, mt103);
        String uetr = extractFirst(UETR_BLOCK, mt103).toLowerCase(Locale.ROOT);

        Matcher amountMatcher = TAG_32A.matcher(mt103);
        if (!amountMatcher.find()) {
            throw new FinancialValidationException(
                "FIN-AMOUNT-FORMAT",
                "Tag :32A: is missing or malformed",
                instructionId,
                uetr
            );
        }

        String currency = amountMatcher.group(2);
        String amountRaw = amountMatcher.group(3);

        validateAmount(amountRaw, instructionId, uetr);
        validateCurrency(currency, instructionId, uetr);
        validateBicTag(TAG_52A, mt103, "FIN-BIC-52A", instructionId, uetr);
        validateBicTag(TAG_57A, mt103, "FIN-BIC-57A", instructionId, uetr);

        if (!uetr.isBlank() && !UETR_FORMAT.matcher(uetr).matches()) {
            throw new FinancialValidationException(
                "FIN-UETR-FORMAT",
                "UETR format is invalid",
                instructionId,
                uetr
            );
        }

        return new ValidationResult(instructionId, uetr);
    }

    private void validateAmount(String amountRaw, String instructionId, String uetr) {
        try {
            BigDecimal amount = new BigDecimal(amountRaw.replace(',', '.'));
            if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                throw new FinancialValidationException(
                    "FIN-AMOUNT-POSITIVE",
                    "Amount must be positive",
                    instructionId,
                    uetr
                );
            }

            if (amount.scale() > 2) {
                throw new FinancialValidationException(
                    "FIN-AMOUNT-SCALE",
                    "Amount must have max 2 decimal places",
                    instructionId,
                    uetr
                );
            }
        } catch (NumberFormatException ex) {
            throw new FinancialValidationException(
                "FIN-AMOUNT-FORMAT",
                "Amount is not a valid number",
                instructionId,
                uetr
            );
        }
    }

    private void validateCurrency(String currency, String instructionId, String uetr) {
        if (currency == null || currency.length() != 3 || !ISO_4217_CODES.contains(currency)) {
            throw new FinancialValidationException(
                "FIN-CURRENCY-ISO4217",
                "Currency must be a valid 3-letter ISO 4217 code",
                instructionId,
                uetr
            );
        }
    }

    private void validateBicTag(Pattern bicPattern,
                                String mt103,
                                String errorCode,
                                String instructionId,
                                String uetr) {
        Matcher matcher = bicPattern.matcher(mt103);
        while (matcher.find()) {
            String bic = matcher.group(1);
            if (bic.length() != 8 && bic.length() != 11) {
                throw new FinancialValidationException(
                    errorCode,
                    "BIC must be 8 or 11 characters",
                    instructionId,
                    uetr
                );
            }
        }
    }

    private String extractFirst(Pattern pattern, String source) {
        Matcher matcher = pattern.matcher(source);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return "";
    }
}

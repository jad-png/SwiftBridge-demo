package com.swiftbridge.converter.mapping.model;

import java.util.List;

public record Pacs008XPathExtractionResult(
    String debtorName,
    String creditorName,
    String interbankSettlementDate,
    String instructionId,
    List<String> debtorNameSwiftLines,
    List<String> creditorNameSwiftLines,
    List<String> debtorAddressSwiftLines,
    List<String> creditorAddressSwiftLines,
    List<String> warnings
) {
    public boolean hasWarnings() {
        return warnings != null && !warnings.isEmpty();
    }

    public boolean hasDebtorSwiftLines() {
        return (debtorNameSwiftLines != null && !debtorNameSwiftLines.isEmpty())
            || (debtorAddressSwiftLines != null && !debtorAddressSwiftLines.isEmpty());
    }

    public boolean hasCreditorSwiftLines() {
        return (creditorNameSwiftLines != null && !creditorNameSwiftLines.isEmpty())
            || (creditorAddressSwiftLines != null && !creditorAddressSwiftLines.isEmpty());
    }
}

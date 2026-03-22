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
}

package com.swiftbridge.converter.mapping.model;

import java.util.List;

public record Pacs008Fields(
    String reference,
    String uetr,
    String amountValue,
    String amountCurrency,
    String settlementDate,
    String debtorName,
    String creditorName,
    String debtorBic,
    String creditorBic,
    String chargeBearer,
    List<String> debtorAddress,
    List<String> creditorAddress
) {
}

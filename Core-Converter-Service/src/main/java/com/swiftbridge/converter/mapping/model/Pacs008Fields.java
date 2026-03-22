package com.swiftbridge.converter.mapping.model;

import java.util.Collections;
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
    public boolean hasDebtorAddress() {
        return debtorAddress != null && !debtorAddress.isEmpty();
    }

    public boolean hasCreditorAddress() {
        return creditorAddress != null && !creditorAddress.isEmpty();
    }

    public List<String> debtorAddressOrEmpty() {
        return debtorAddress == null ? Collections.emptyList() : debtorAddress;
    }

    public List<String> creditorAddressOrEmpty() {
        return creditorAddress == null ? Collections.emptyList() : creditorAddress;
    }
}

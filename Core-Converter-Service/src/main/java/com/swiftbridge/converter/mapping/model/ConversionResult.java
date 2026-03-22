package com.swiftbridge.converter.mapping.model;

import java.util.Collections;
import java.util.List;

public record ConversionResult(
    String mt103,
    List<String> warnings
) {
    public boolean hasWarnings() {
        return warnings != null && !warnings.isEmpty();
    }

    public List<String> warningsOrEmpty() {
        return warnings == null ? Collections.emptyList() : warnings;
    }
}

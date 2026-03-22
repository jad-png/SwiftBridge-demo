package com.swiftbridge.converter.mapping.model;

import java.util.List;

public record ConversionResult(
    String mt103,
    List<String> warnings
) {
}

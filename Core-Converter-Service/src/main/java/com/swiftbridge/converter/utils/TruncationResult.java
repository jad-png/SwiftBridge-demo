package com.swiftbridge.converter.utils;

import java.util.List;

public record TruncationResult(
    List<String> lines,
    boolean truncated,
    String warning
) {
}

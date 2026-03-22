package com.swiftbridge.converter.utils;

import java.util.Collections;
import java.util.List;

public record TruncationResult(
    List<String> lines,
    boolean truncated,
    String warning
) {
    public static TruncationResult empty() {
        return new TruncationResult(Collections.emptyList(), false, "");
    }

    public static TruncationResult of(List<String> lines) {
        return new TruncationResult(lines, false, "");
    }
}

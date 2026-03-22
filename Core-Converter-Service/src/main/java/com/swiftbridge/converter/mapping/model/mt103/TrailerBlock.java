package com.swiftbridge.converter.mapping.model.mt103;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Locale;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrailerBlock {

    private String checksum;
    private String mac;

    public void validateTrailerFormat() {
        if (checksum != null && !checksum.toUpperCase(Locale.ROOT).matches("[0-9A-F]{12}")) {
            throw new IllegalArgumentException("checksum must be 12 hex characters");
        }
        if (mac != null && !mac.toUpperCase(Locale.ROOT).matches("[0-9A-F]{16}")) {
            throw new IllegalArgumentException("mac must be 16 hex characters");
        }
    }
}

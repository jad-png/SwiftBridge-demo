package com.swiftbridge.converter.mapping.model.mt103;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserHeaderBlock {

    private String uetr;

    public void validateUetr() {
        if (uetr == null || uetr.isBlank()) {
            return;
        }
        try {
            UUID.fromString(uetr);
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("uetr must be a valid UUID", ex);
        }
    }
}

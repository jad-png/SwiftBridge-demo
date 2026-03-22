package com.swiftbridge.converter.mapping.model.mt103;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
import lombok.NoArgsConstructor;

import java.util.Objects;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Mt103Message {

    @NonNull
    private BasicHeaderBlock block1;
    @NonNull
    private ApplicationHeaderBlock block2;
    private UserHeaderBlock block3;
    @NonNull
    private TextBlock block4;
    private TrailerBlock block5;

    public void validateRequiredBlocks() {
        Objects.requireNonNull(block1, "block1 is required");
        Objects.requireNonNull(block2, "block2 is required");
        Objects.requireNonNull(block4, "block4 is required");
    }
}

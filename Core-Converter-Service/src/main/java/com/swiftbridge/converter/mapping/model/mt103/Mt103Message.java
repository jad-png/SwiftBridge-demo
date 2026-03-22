package com.swiftbridge.converter.mapping.model.mt103;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Mt103Message {

    private BasicHeaderBlock block1;
    private ApplicationHeaderBlock block2;
    private UserHeaderBlock block3;
    private TextBlock block4;
    private TrailerBlock block5;
}

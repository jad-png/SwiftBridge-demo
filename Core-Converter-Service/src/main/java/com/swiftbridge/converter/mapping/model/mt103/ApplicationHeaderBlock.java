package com.swiftbridge.converter.mapping.model.mt103;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApplicationHeaderBlock {

    private String inputOutputId;
    private String messageType;
    private String receiverLogicalTerminal;
    private String priority;
}

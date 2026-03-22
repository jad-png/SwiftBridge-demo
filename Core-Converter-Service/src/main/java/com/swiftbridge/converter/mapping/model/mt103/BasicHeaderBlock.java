package com.swiftbridge.converter.mapping.model.mt103;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BasicHeaderBlock {

    private String applicationId;
    private String serviceId;
    private String logicalTerminal;
    private String sessionNumber;
    private String sequenceNumber;
}

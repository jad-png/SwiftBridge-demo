package com.swiftbridge.orchestrator.dto.error;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErrorResponseDTO {

    @JsonProperty("errorCode")
    private String errorCode;

    @JsonProperty("message")
    private String message;

    @JsonProperty("timestamp")
    private String timestamp;
}

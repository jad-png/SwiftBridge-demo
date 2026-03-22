package com.swiftbridge.converter.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;

import java.util.Objects;

@Getter
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class ErrorResponseDTO {

    @JsonProperty("errorCode")
    @NonNull
    private String errorCode;

    @JsonProperty("message")
    @NonNull
    private String message;

    @JsonProperty("timestamp")
    private String timestamp;

    public void validate() {
        Objects.requireNonNull(errorCode, "errorCode is required");
        Objects.requireNonNull(message, "message is required");
    }
}

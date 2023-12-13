package com.acme.simple.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

public record TemperatureReading(double value, @NotNull Instant timestamp) {

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    public String timestampStr() {
        return timestamp.toString();
    }
}

package com.acme.simple.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;

public record AppInfo(String name, Instant currentTime) {

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    public String currentTimeStr() {
        return currentTime.toString();
    }
}

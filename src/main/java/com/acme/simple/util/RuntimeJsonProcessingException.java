package com.acme.simple.util;

import com.fasterxml.jackson.core.JsonProcessingException;

import java.util.Objects;

public class RuntimeJsonProcessingException extends RuntimeException {

    public RuntimeJsonProcessingException(String message, JsonProcessingException cause) {
        super(message, Objects.requireNonNull(cause));
    }

    public RuntimeJsonProcessingException(JsonProcessingException cause) {
        this(null, cause);
    }

    @Override
    public synchronized JsonProcessingException getCause() {
        return (JsonProcessingException) super.getCause();
    }
}

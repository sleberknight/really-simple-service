package com.acme.simple.util;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@UtilityClass
public class JacksonHelpers {

    public static void configureObjectMapper(ObjectMapper mapper, boolean serializeTimestampsAsMillis) {
        if (serializeTimestampsAsMillis) {
            LOG.info("Configure application ObjectMapper with [READ|WRITE}_DATE_TIMESTAMPS_AS_NANOSECONDS=false");
            mapper.configure(DeserializationFeature.READ_DATE_TIMESTAMPS_AS_NANOSECONDS, false);
            mapper.configure(SerializationFeature.WRITE_DATE_TIMESTAMPS_AS_NANOSECONDS, false);
        } else {
            LOG.info("Using default serialization configuration. " +
                            "READ_DATE_TIMESTAMPS_AS_NANOSECONDS: {}, WRITE_DATE_TIMESTAMPS_AS_NANOSECONDS: {}",
                    mapper.isEnabled(DeserializationFeature.READ_DATE_TIMESTAMPS_AS_NANOSECONDS),
                    mapper.isEnabled(SerializationFeature.WRITE_DATE_TIMESTAMPS_AS_NANOSECONDS));
        }
    }
}

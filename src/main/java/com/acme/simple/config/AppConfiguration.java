package com.acme.simple.config;

import io.dropwizard.core.Configuration;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AppConfiguration extends Configuration {
    private String appName;
    private boolean serializeTimestampsAsMillis;
}

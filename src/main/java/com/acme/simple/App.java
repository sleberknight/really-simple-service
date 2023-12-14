package com.acme.simple;

import ch.qos.logback.classic.Level;
import com.acme.simple.config.AppConfiguration;
import com.acme.simple.resource.SimpleResource;
import com.acme.simple.resource.TemperatureResource;
import com.acme.simple.util.JacksonHelpers;
import io.dropwizard.core.Application;
import io.dropwizard.core.setup.Environment;
import lombok.extern.slf4j.Slf4j;
import org.glassfish.jersey.CommonProperties;

@Slf4j
public class App extends Application<AppConfiguration> {

    public static void main(String[] args) throws Exception {
        new App().run(args);
    }

    @Override
    protected Level bootstrapLogLevel() {
        return Level.INFO;
    }

    @Override
    public void run(AppConfiguration configuration, Environment environment) {
        var mapper = environment.getObjectMapper();
        JacksonHelpers.configureObjectMapper(mapper, configuration.isSerializeTimestampsAsMillis());

        var jersey = environment.jersey();
        jersey.register(new SimpleResource(configuration));
        jersey.register(new TemperatureResource(configuration));

        if (configuration.isDisableJerseyFeatureAutoDiscovery()) {
            LOG.warn("Disabling auto discovery globally in Jersey on client/server.");
            jersey.getResourceConfig().property(CommonProperties.FEATURE_AUTO_DISCOVERY_DISABLE, true);
        }
    }

}

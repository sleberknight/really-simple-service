package com.acme.simple.client;

import com.acme.simple.model.TemperatureReading;
import com.acme.simple.util.JacksonHelpers;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.dropwizard.jersey.jackson.JacksonFeature;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Entity;
import net.sourceforge.argparse4j.ArgumentParsers;
import net.sourceforge.argparse4j.inf.ArgumentParserException;
import net.sourceforge.argparse4j.inf.Namespace;

import lombok.extern.slf4j.Slf4j;
import org.glassfish.jersey.CommonProperties;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Map;
import java.util.random.RandomGenerator;

@Slf4j
public class SubmitReadingClient {

    public static void main(String[] args) throws IOException {
        var config = parseArgs(args);
        var action = config.action();
        var serializeTimestampsAsMillis = config.serializeTimestampsAsMillis();

        LOG.info("Using action: {}", action);
        LOG.info("Using serializeTimestampsAsMillis: {}", serializeTimestampsAsMillis);

        // Create object mapper, register Java 8 date/time support, and configure
        // timestamp serialization
        var mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        JacksonHelpers.configureObjectMapper(mapper, serializeTimestampsAsMillis);

        // Configure the Jersey Client with Jackson support using custom mapper
        // and Dropwizard's JacksonFeature
        var jacksonFeature = new JacksonFeature(mapper);
        var client = ClientBuilder.newBuilder()
                .property(CommonProperties.FEATURE_AUTO_DISCOVERY_DISABLE, true)
                .build();
        client.register(jacksonFeature);

        try {
            var randomTemp = RandomGenerator.getDefault().nextDouble(27.0, 50.0);
            var reading = new TemperatureReading(randomTemp, Instant.now());

            try (var response = client.target("http://localhost:8080/temps")
                    .path(action)
                    .request("application/json")
                    .post(Entity.json(reading))) {

                var entity = response.readEntity(String.class);

                LOG.info("Response status: {}", response.getStatus());
                LOG.info("Received response entity: {}", entity);
            }
        } finally {
            client.close();
        }
    }

    private record Config(String action, boolean serializeTimestampsAsMillis) {
    }

    private static Config parseArgs(String[] args) throws IOException {
        var parser = ArgumentParsers.newFor("SubmitReadingClient")
                .build()
                .defaultHelp(true)
                .description("Submit or validate a temperature reading");

        parser.addArgument("-a", "--action")
                .required(false)
                .choices("submit", "validate")
                .setDefault("submit")
                .help("Submit or validate a reading. Note that validation is server-side using the server's timestamp serialization configuration.");

        parser.addArgument("-s", "--serialization")
                .required(false)
                .choices("millis", "nanos", "from-config-file")
                .setDefault("from-config-file")
                .help("How to configure timestamp serialization (applies only to client)");

        String action = null;
        boolean serializeTimestampsAsMillis = false;
        try {
            Namespace ns = parser.parseArgs(args);
            var actionOpt = ns.getString("action");
            action = switch (actionOpt) {
                case "submit" -> "new";
                case "validate" -> "validateReading";
                default -> "new";
            };

            var serializationOpt = ns.getString("serialization");
            serializeTimestampsAsMillis = switch (serializationOpt) {
                case "millis" -> true;
                case "nanos" -> false;
                case "from-config-file" -> readTimestampSerializationConfig();
                default -> true;
            };
        } catch (ArgumentParserException e) {
            parser.handleError(e);
            System.exit(1);
        }

        return new Config(action, serializeTimestampsAsMillis);
    }

    @SuppressWarnings("unchecked")
    private static boolean readTimestampSerializationConfig() throws IOException {
        var configYaml = Files.readString(Path.of("config.yml"));
        var yaml = new Yaml();
        var config = yaml.loadAs(configYaml, Map.class);
        return (Boolean) config.getOrDefault("serializeTimestampsAsMillis", true);
    }
}

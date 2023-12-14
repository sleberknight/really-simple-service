package com.acme.simple.client;

import com.acme.simple.model.TemperatureReading;
import com.acme.simple.util.JacksonHelpers;
import com.acme.simple.util.RuntimeJsonProcessingException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.dropwizard.jersey.jackson.JacksonFeature;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.GenericType;
import lombok.extern.slf4j.Slf4j;
import net.sourceforge.argparse4j.ArgumentParsers;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.ArgumentParserException;
import org.glassfish.jersey.CommonProperties;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Map;
import java.util.random.RandomGenerator;

@Slf4j
public class SubmitReadingClient {

    private static final String CHOICE_SUBMIT = "submit";
    private static final String CHOICE_CONFIG_FILE = "from-config-file";
    private static final String CHOICE_VALIDATE = "validate";

    public static void main(String[] args) {
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

                var entity = response.readEntity(new GenericType<Map<String, Object>>() {
                });
                var prettyEntity = mapper.writerWithDefaultPrettyPrinter()
                        .writeValueAsString(entity);

                LOG.info("Path: {}", action);
                LOG.info("Response status: {}", response.getStatus());
                LOG.info("Received response entity:\n{}", prettyEntity);
            } catch (JsonProcessingException e) {
                throw new RuntimeJsonProcessingException(e);
            }
        } finally {
            client.close();
        }
    }

    private record Config(String action, boolean serializeTimestampsAsMillis) {
    }

    private static Config parseArgs(String[] args) {
        var parser = ArgumentParsers.newFor("SubmitReadingClient")
                .build()
                .defaultHelp(true)
                .description("Submit or validate a temperature reading");

        parser.addArgument("-a", "--action")
                .required(false)
                .choices(CHOICE_SUBMIT, CHOICE_VALIDATE)
                .setDefault(CHOICE_SUBMIT)
                .help("Submit or validate a reading. Note that validation is server-side using the server's timestamp serialization configuration.");

        parser.addArgument("-s", "--serialization")
                .required(false)
                .choices("millis", "nanos", CHOICE_CONFIG_FILE)
                .setDefault(CHOICE_CONFIG_FILE)
                .help("How to configure timestamp serialization (applies only to client)");

        try {
            return parseArgs(args, parser);
        } catch (ArgumentParserException e) {
            parser.handleError(e);
            System.exit(1);
            throw new IllegalStateException();
        }
    }

    private static Config parseArgs(String[] args, ArgumentParser parser)
            throws ArgumentParserException {

        var namespace = parser.parseArgs(args);

        var actionArg = namespace.getString("action");
        var action = actionArg.equals(CHOICE_VALIDATE) ? "validateReading" : "new";

        var serializationArg = namespace.getString("serialization");
        boolean serializeTimestampsAsMillis = switch (serializationArg) {
            case "nanos" -> false;
            case CHOICE_CONFIG_FILE -> readTimestampSerializationConfig();
            default -> true;  // millis
        };

        return new Config(action, serializeTimestampsAsMillis);
    }

    @SuppressWarnings("unchecked")
    private static boolean readTimestampSerializationConfig() {
        try {
            var configYaml = Files.readString(Path.of("config.yml"));
            var yaml = new Yaml();
            var config = yaml.loadAs(configYaml, Map.class);
            return (Boolean) config.getOrDefault("serializeTimestampsAsMillis", true);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}

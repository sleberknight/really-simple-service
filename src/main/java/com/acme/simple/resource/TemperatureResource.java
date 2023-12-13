package com.acme.simple.resource;

import static java.util.Comparator.comparing;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

import com.acme.simple.config.AppConfiguration;
import com.acme.simple.model.TemperatureReading;
import com.acme.simple.util.JacksonHelpers;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Path("/temps")
@Produces(MediaType.APPLICATION_JSON)
@Slf4j
public class TemperatureResource {

    private final Deque<TemperatureReading> temperatureReadings;
    private final AppConfiguration configuration;

    public TemperatureResource(AppConfiguration configuration) {
        this.configuration = configuration;

        temperatureReadings = new ArrayDeque<>();

        // An initial "database" of sample readings
        temperatureReadings.addFirst(new TemperatureReading(35.0, Instant.now()));
        temperatureReadings.addFirst(new TemperatureReading(34.9, Instant.now().minusSeconds(60)));
    }

    @GET
    @Path("/latest")
    public Response latestReading() {
        var reading = temperatureReadings.peekFirst();
        if (isNull(reading)) {
            LOG.warn("No readings found. Returning bogus reading.");
            return Response.ok(new TemperatureReading(0.0, Instant.now())).build();
        }
        return Response.ok(reading).build();
    }

    @GET
    @Path("/recent")
    public Response recentReadings(@QueryParam("count") @DefaultValue("5") int count) {
        var recent = temperatureReadings.stream()
                .sorted(comparing(TemperatureReading::timestamp).reversed())
                .limit(count)
                .toList();
        return Response.ok(recent).build();
    }

    @GET
    @Path("/numReadings")
    public Response numReadings() {
        var now = Instant.now();
        var entity = Map.of(
                "asOf", now,
                "asOfStr", now.toString(),
                "count", temperatureReadings.size()
        );
        return Response.ok(entity).build();
    }

    @POST
    @Path("/new")
    public Response newReading(@NotNull @Valid TemperatureReading newReading) {
        temperatureReadings.addFirst(newReading);
        var now = Instant.now();
        var entity = Map.of(
                "recordedAt", now,
                "recordedAtStr", now.toString(),
                "reading", newReading
        );
        return Response.ok(entity).build();
    }

    @POST
    @Path("/validateReading")
    public Response validateReading(@NotBlank String json) {
        LOG.info("Received JSON: {}", json);
        var validationResult = validate(json);
        var entity = buildValidationEntity(validationResult);
        return Response.ok(entity).build();
    }

    public record ReadingValidationResult(boolean valid,
                                          @Nullable TemperatureReading reading,
                                          @Nullable Exception exception) {

        public static ReadingValidationResult validResult(TemperatureReading reading) {
            return new ReadingValidationResult(true, reading, null);
        }

        public static ReadingValidationResult invalidResult(Exception e) {
            return new ReadingValidationResult(false, null, e);
        }
    }

    private ReadingValidationResult validate(String json) {
        try {
            var mapper = new ObjectMapper();
            JacksonHelpers.configureObjectMapper(mapper, configuration.isSerializeTimestampsAsMillis());
            mapper.registerModule(new JavaTimeModule());

            var temperatureReading = mapper.readValue(json, TemperatureReading.class);
            return ReadingValidationResult.validResult(temperatureReading);
        } catch (JsonProcessingException e) {
            LOG.info("Input could not be deserialized", e);
            return ReadingValidationResult.invalidResult(e);
        }
    }

    private static Map<String, Object> buildValidationEntity(ReadingValidationResult result) {
        var entity = new HashMap<String, Object>();
        entity.put("valid", result.valid());
        entity.put("reading", result.reading());

        var exception = result.exception();
        if (nonNull(exception)) {
            var maybeCause = Optional.ofNullable(exception.getCause());
            var exceptionInfo = Map.of(
                    "type", exception.getClass().getName(),
                    "message", exception.getMessage(),
                    "causeType", maybeCause.map(throwable -> throwable.getClass().getName()).orElse(""),
                    "causeMessage", maybeCause.map(Throwable::getMessage).orElse("")
            );
            entity.put("exceptionInfo", exceptionInfo);
        }
        return entity;
    }

}

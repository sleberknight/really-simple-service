package com.acme.simple.resource;

import com.acme.simple.config.AppConfiguration;
import com.acme.simple.model.AppInfo;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.time.Instant;

@Path("/simple")
@Produces(MediaType.APPLICATION_JSON)
public class SimpleResource {
    private final AppConfiguration configuration;

    public SimpleResource(AppConfiguration configuration) {
        this.configuration = configuration;
    }

    @GET
    @Path("/appInfo")
    public Response appInfo() {
        var now = Instant.now();
        var appName = configuration.getAppName();
        var entity = new AppInfo(appName, now);
        return Response.ok(entity).build();
    }
}

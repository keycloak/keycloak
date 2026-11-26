package org.keycloak.testframework.tests.providers.single;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.keycloak.services.resource.AccountResourceProvider;

public class ProviderWithExtraResourcesProvider implements AccountResourceProvider {

    @Override
    public ProviderWithExtraResourcesProvider getResource() {
        return this;
    }

    @GET
    @Produces(MediaType.TEXT_HTML)
    public Response getMainPage() {
        return Response.ok().entity("<html><head><title>Account</title></head><body><h1>Custom Account Console</h1></body></html>").build();
    }

    @Override
    public void close() {
    }
}

package org.keycloak.tests.providers.rest;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Response;

import org.keycloak.models.KeycloakSession;
import org.keycloak.services.ErrorPage;
import org.keycloak.services.resource.RealmResourceProvider;

public class ErrorTestingRealmResourceProvider implements RealmResourceProvider {

    private final KeycloakSession session;

    ErrorTestingRealmResourceProvider(KeycloakSession session) {
        this.session = session;
    }

    @Override
    public Object getResource() {
        return this;
    }

    @GET
    @Path("display-error-message")
    public Response displayErrorMessage(@QueryParam("message") String message) {
        return ErrorPage.error(session, session.getContext().getAuthenticationSession(),
                Response.Status.BAD_REQUEST, message == null ? "" : message);
    }

    @GET
    @Path("uncaught-error")
    public Response uncaughtError() {
        throw new RuntimeException("Uncaught error");
    }

    @Override
    public void close() {
    }
}

package org.keycloak.tests.providers.rest;

import java.io.IOException;
import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import jakarta.enterprise.inject.spi.CDI;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.keycloak.models.AbstractKeycloakTransaction;
import org.keycloak.models.KeycloakSession;
import org.keycloak.services.ErrorPage;
import org.keycloak.services.resource.RealmResourceProvider;

public class ErrorTestingRealmResourceProvider implements RealmResourceProvider {

    private static final Map<String, String> TRANSACTION_OUTCOMES = new ConcurrentHashMap<>();

    private final KeycloakSession session;

    ErrorTestingRealmResourceProvider(KeycloakSession session) {
        this.session = session;
    }

    @Override
    public Object getResource() {
        // The locator runs before request-body deserialization, so input errors must roll this back too.
        if (!session.getContext().getUri().getPath().endsWith("/transaction")) {
            trackTransaction(session.getContext().getUri().getQueryParameters().getFirst("transaction"));
        }
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

    @POST
    @Path("json")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public JsonInput json(JsonInput input) {
        return input;
    }

    @GET
    @Path("json/throwing-getter")
    @Produces(MediaType.APPLICATION_JSON)
    public ThrowingGetter throwingGetter() {
        return new ThrowingGetter();
    }

    @GET
    @Path("json/invalid-definition")
    @Produces(MediaType.APPLICATION_JSON)
    public EmptyBean invalidDefinition() {
        return new EmptyBean();
    }

    @POST
    @Path("json/invalid-definition")
    @Consumes(MediaType.APPLICATION_JSON)
    public void invalidInputDefinition(UndefinedInput input) {
        throw new AssertionError("An undefined input type must not reach the resource");
    }

    @GET
    @Path("json/transaction")
    @Produces(MediaType.TEXT_PLAIN)
    public String transactionOutcome(@QueryParam("transaction") String transaction) {
        return TRANSACTION_OUTCOMES.remove(transaction);
    }

    @GET
    @Path("json/legacy-mapper")
    @Produces(MediaType.APPLICATION_JSON)
    public String legacyMapper() throws IOException {
        // Extension consumers must still be able to resolve the legacy CDI producer.
        var mapper = CDI.current().select(com.fasterxml.jackson.databind.ObjectMapper.class).get();
        return mapper.writeValueAsString(Map.of("optional", Optional.of("value"), "date", LocalDate.of(2026, 1, 2)));
    }

    private void trackTransaction(String transaction) {
        if (transaction != null) {
            session.getTransactionManager().enlistAfterCompletion(new AbstractKeycloakTransaction() {
                @Override
                protected void commitImpl() {
                    TRANSACTION_OUTCOMES.put(transaction, "committed");
                }

                @Override
                protected void rollbackImpl() {
                    TRANSACTION_OUTCOMES.put(transaction, "rolled-back");
                }
            });
        }
    }

    public static class JsonInput {
        public int count;
    }

    public static class ThrowingGetter {
        public String getValue() {
            throw new IllegalStateException("Private serialization failure detail");
        }
    }

    public static class EmptyBean {
    }

    public interface UndefinedInput {
        String getValue();
    }

    @Override
    public void close() {
    }
}

package org.keycloak.testframework.util;

import java.util.Arrays;
import java.util.stream.Collectors;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

import org.junit.jupiter.api.Assertions;

/**
 * Utilities for the Keycloak Java Admin client
 */
public class ApiUtil {

    /**
     * Several POST endpoints in Keycloak Admin API does not return the created resource in the response; but rather
     * returns a location header instead, making it harder to get the generated ID of a newly created resource. This
     * method parses the location header and returns the ID of the created resource, as well as closing the JAX-RS
     * response.
     *
     * @param response the response from a POST request, for example creating a new user in a realm
     * @return the ID of the created resource, for example the UUID of a new user
     */
    public static String getCreatedId(Response response) {
        try (response) {
            Assertions.assertEquals(201, response.getStatus());
            String path = response.getLocation().getPath();
            return path.substring(path.lastIndexOf('/') + 1);
        }
    }

    /**
     * Verify that a response has one of the expected statuses, and throw otherwise, including the actual status and
     * the response body in the message. Several Admin API endpoints return a {@link Response} rather than throwing on
     * a failure, which would otherwise let a failed create, update or delete pass unnoticed.
     * <p>
     * The response is not closed and its entity is left untouched, so this can be called inside a try-with-resources
     * block that goes on to read the entity. If the check fails the entity is consumed to include the body in the
     * error message, but the exception makes that unobservable to the caller.
     *
     * @param response the response to verify
     * @param context what was attempted, phrased to follow "Failed to ", for example
     *                <code>"update organization 'acme'"</code>
     * @param expected the accepted statuses
     * @throws IllegalStateException if the response has none of the expected statuses
     */
    public static void expectStatus(Response response, String context, Status... expected) {
        for (Status status : expected) {
            if (status.getStatusCode() == response.getStatus()) {
                return;
            }
        }
        throw new IllegalStateException("Failed to %s: expected status %s, but was %d. Response: %s".formatted(
                context,
                Arrays.stream(expected).map(s -> String.valueOf(s.getStatusCode())).collect(Collectors.joining(" or ")),
                response.getStatus(),
                readBody(response)));
    }

    private static String readBody(Response response) {
        try {
            return response.hasEntity() ? response.readEntity(String.class) : "<no body>";
        } catch (RuntimeException e) {
            return "<unreadable body: " + e.getMessage() + ">";
        }
    }

}

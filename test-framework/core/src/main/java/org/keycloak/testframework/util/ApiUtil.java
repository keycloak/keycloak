package org.keycloak.testframework.util;

import jakarta.ws.rs.core.Response;

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

}

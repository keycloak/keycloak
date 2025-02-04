package org.keycloak.testframework.util;

import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.Assertions;

public class ApiUtil {

    public static String handleCreatedResponse(Response response) {
        try (response) {
            Assertions.assertEquals(201, response.getStatus());
            String path = response.getLocation().getPath();
            return path.substring(path.lastIndexOf('/') + 1);
        }
    }

}

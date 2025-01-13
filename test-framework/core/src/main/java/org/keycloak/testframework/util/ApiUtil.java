package org.keycloak.testframework.util;

import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.Assertions;

public class ApiUtil {

    public static String handleCreatedResponse(Response response) {
        Assertions.assertEquals(201, response.getStatus());
        String path = response.getLocation().getPath();
        String uuid = path.substring(path.lastIndexOf('/') + 1);
        response.close();
        return uuid;
    }

}

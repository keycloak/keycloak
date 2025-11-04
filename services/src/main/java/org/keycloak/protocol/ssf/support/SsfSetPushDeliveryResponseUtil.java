package org.keycloak.protocol.ssf.support;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

public class SsfSetPushDeliveryResponseUtil {

    public static WebApplicationException newSsfSetPushDeliveryFailureResponse(Response.Status status, String errorCode, String errorMessage) {
        Response response = Response.status(status)
                .type(MediaType.APPLICATION_JSON)
                .entity(new SsfSetPushDeliveryFailureResponse(errorCode, errorMessage))
                .build();
        return new WebApplicationException(response);
    }
}

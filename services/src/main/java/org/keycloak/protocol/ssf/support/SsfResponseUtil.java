package org.keycloak.protocol.ssf.support;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

public class SsfResponseUtil {

    public static WebApplicationException newSharedSignalFailureResponse(Response.Status status, String errorCode, String errorMessage) {
        Response response = Response.status(status)
                .type(MediaType.APPLICATION_JSON)
                .entity(new SsfFailureResponse(errorCode, errorMessage))
                .build();
        return new WebApplicationException(response);
    }
}

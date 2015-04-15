package org.keycloak.services;

import org.keycloak.OAuth2Constants;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.HashMap;
import java.util.Map;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class ErrorResponseException extends WebApplicationException {

    private final String error;
    private final String errorDescription;
    private final Response.Status status;

    public ErrorResponseException(String error, String errorDescription, Response.Status status) {
        this.error = error;
        this.errorDescription = errorDescription;
        this.status = status;
    }

    @Override
    public Response getResponse() {
        Map<String, String> e = new HashMap<String, String>();
        e.put(OAuth2Constants.ERROR, error);
        if (errorDescription != null) {
            e.put(OAuth2Constants.ERROR_DESCRIPTION, errorDescription);
        }
        return Response.status(status).entity(e).type(MediaType.APPLICATION_JSON_TYPE).build();
    }

}

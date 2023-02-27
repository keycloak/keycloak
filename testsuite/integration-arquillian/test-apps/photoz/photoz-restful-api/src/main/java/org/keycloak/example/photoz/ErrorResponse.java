package org.keycloak.example.photoz;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import java.util.HashMap;
import java.util.Map;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public class ErrorResponse extends WebApplicationException {

    private final Response.Status status;

    public ErrorResponse(String message) {
        this(message, Response.Status.INTERNAL_SERVER_ERROR);
    }

    public ErrorResponse(String message, Response.Status status) {
        super(message, status);
        this.status = status;
    }

    @Override
    public Response getResponse() {
        Map<String, String> errorResponse = new HashMap<>();

        errorResponse.put("message", getMessage());

        return Response.status(status).entity(errorResponse).build();
    }
}

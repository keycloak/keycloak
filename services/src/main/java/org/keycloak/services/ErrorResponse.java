package org.keycloak.services;

import org.keycloak.representations.idm.ErrorRepresentation;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class ErrorResponse {

    public static Response exists(String message) {
        ErrorRepresentation error = new ErrorRepresentation();
        error.setErrorMessage(message);
        return Response.status(Response.Status.CONFLICT).entity(error).type(MediaType.APPLICATION_JSON).build();
    }

    public static Response error(String message, Response.Status status) {
        ErrorRepresentation error = new ErrorRepresentation();
        error.setErrorMessage(message);
        return Response.status(status).entity(error).type(MediaType.APPLICATION_JSON).build();
    }

}

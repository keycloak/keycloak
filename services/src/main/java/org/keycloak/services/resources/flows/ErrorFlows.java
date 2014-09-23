package org.keycloak.services.resources.flows;

import org.keycloak.representations.idm.ErrorRepresentation;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class ErrorFlows {

    public Response exists(String message) {
        ErrorRepresentation error = new ErrorRepresentation();
        error.setErrorMessage(message);
        return Response.status(Response.Status.CONFLICT).entity(error).type(MediaType.APPLICATION_JSON).build();
    }

    public Response error(String message, Response.Status status) {
        ErrorRepresentation error = new ErrorRepresentation();
        error.setErrorMessage(message);
        return Response.status(status).entity(error).type(MediaType.APPLICATION_JSON).build();
    }



}

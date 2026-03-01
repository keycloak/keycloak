package org.keycloak.services.resources.admin;

import jakarta.ws.rs.OPTIONS;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;

import org.keycloak.services.cors.Cors;

/**
 * Created by st on 21/03/17.
 */
public class AdminCorsPreflightService {

    /**
     * CORS preflight
     *
     * @return
     */
    @Path("{any:.*}")
    @OPTIONS
    public Response preflight() {
        return Cors.builder().preflight().allowedMethods("GET", "PUT", "POST", "DELETE").auth().add(Response.ok());
    }

}

package org.keycloak.services.resources.account;

import jakarta.ws.rs.OPTIONS;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;

import org.keycloak.services.cors.Cors;

/**
 * Created by st on 21/03/17.
 */
public class CorsPreflightService {

    /**
     * CORS preflight
     *
     * @return
     */
    @Path("{any:.*}")
    @OPTIONS
    public Response preflight() {
        return Cors.builder().auth().allowedMethods("GET", "POST", "DELETE", "PUT", "HEAD", "OPTIONS").preflight()
                .add(Response.ok());
    }

}

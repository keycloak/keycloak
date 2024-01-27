package org.keycloak.services.resources.admin;

import org.keycloak.http.HttpRequest;
import org.keycloak.services.cors.Cors;

import jakarta.ws.rs.OPTIONS;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;

/**
 * Created by st on 21/03/17.
 */
public class AdminCorsPreflightService {

    private HttpRequest request;

    public AdminCorsPreflightService(HttpRequest request) {
        this.request = request;
    }

    /**
     * CORS preflight
     *
     * @return
     */
    @Path("{any:.*}")
    @OPTIONS
    public Response preflight() {
        return Cors.add(request, Response.ok()).preflight().allowedMethods("GET", "PUT", "POST", "DELETE").auth().build();
    }

}

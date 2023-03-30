package org.keycloak.services.resources.account;

import org.keycloak.http.HttpRequest;
import org.keycloak.services.resources.Cors;

import javax.ws.rs.OPTIONS;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;

/**
 * Created by st on 21/03/17.
 */
public class CorsPreflightService {

    private final HttpRequest request;

    public CorsPreflightService(HttpRequest request) {
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
        Cors cors = Cors.add(request, Response.ok()).auth().allowedMethods("GET", "POST", "DELETE", "PUT", "HEAD", "OPTIONS").preflight();
        return cors.build();
    }

}

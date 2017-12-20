package org.keycloak.services.resources.admin;

import org.jboss.resteasy.spi.HttpRequest;
import org.keycloak.services.resources.Cors;

import javax.ws.rs.OPTIONS;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;

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

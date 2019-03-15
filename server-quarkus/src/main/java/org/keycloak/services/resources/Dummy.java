package org.keycloak.services.resources;

import javax.ws.rs.GET;
import javax.ws.rs.Path;

/**
 * Quarkus doesn't pick up the Application if there's no JAX-RS endpoints
 */
@Path("/dummy")
public class Dummy {

    // ...and doesn't load Resteasy providers unless there is at least one resource method
    @GET
    public String hello() {
        return "Hello World!";
    }

}

package org.keycloak.services.resources;

import javax.ws.rs.Path;

/**
 * Quarkus doesn't pick up the Application if there's no JAX-RS endpoints
 */
@Path("/dummy")
public class Dummy {

}

package org.keycloak.admin.client.resource;

import org.keycloak.representations.idm.UserRepresentation;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.util.List;

public interface KeycloakUsers {

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public List<UserRepresentation> search(@QueryParam("username")  String username,
                                           @QueryParam("firstName") String firstName,
                                           @QueryParam("lastName")  String lastName,
                                           @QueryParam("email")     String email);

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public List<UserRepresentation> search(@QueryParam("search") String search);

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public void create(UserRepresentation userRepresentation);

    @Path("{username}")
    public KeycloakUser get(@PathParam("username") String username);

}

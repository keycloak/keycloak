package org.keycloak.admin.client.resource;

import org.keycloak.representations.idm.UserRepresentation;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import java.util.List;

public interface UsersResource {

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public List<UserRepresentation> search(@QueryParam("username") String username,
                                           @QueryParam("firstName") String firstName,
                                           @QueryParam("lastName") String lastName,
                                           @QueryParam("email") String email,
                                           @QueryParam("first") Integer firstResult,
                                           @QueryParam("max") Integer maxResults);

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public List<UserRepresentation> search(@QueryParam("search") String search,
                                           @QueryParam("first") Integer firstResult,
                                           @QueryParam("max") Integer maxResults);

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public void create(UserRepresentation userRepresentation);

    @Path("{username}")
    public UserResource get(@PathParam("username") String username);

}

package org.keycloak.admin.client.service.interfaces;

import org.keycloak.representations.idm.UserRepresentation;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;

public interface UsersService {

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
    public Response create(UserRepresentation userRepresentation);

    @Path("{username}")
    public UserService get(@PathParam("username") String username);

}

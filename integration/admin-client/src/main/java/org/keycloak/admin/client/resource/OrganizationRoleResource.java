package org.keycloak.admin.client.resource;

import java.util.List;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;

public interface OrganizationRoleResource {

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    RoleRepresentation getRole();

    @DELETE
    void deleteRole();

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    Response updateRole(RoleRepresentation rep);

    @GET
    @Path("users")
    @Produces(MediaType.APPLICATION_JSON)
    List<UserRepresentation> getUsersInRole();

    @PUT
    @Path("users/{userId}")
    void assignRoleToUser(@PathParam("userId") String userId);

    @DELETE
    @Path("users/{userId}")
    void removeRoleFromUser(@PathParam("userId") String userId);
}

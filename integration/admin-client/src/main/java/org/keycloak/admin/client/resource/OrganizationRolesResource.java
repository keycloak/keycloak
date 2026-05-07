package org.keycloak.admin.client.resource;

import java.util.List;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.keycloak.representations.idm.RoleRepresentation;

public interface OrganizationRolesResource {

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    Response createRole(RoleRepresentation rep);

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    List<RoleRepresentation> getRoles(@QueryParam("search") String search,
                                      @QueryParam("first") Integer first,
                                      @QueryParam("max") Integer max);

    @Path("{role-id}")
    OrganizationRoleResource getRoleById(@PathParam("role-id") String id);
}

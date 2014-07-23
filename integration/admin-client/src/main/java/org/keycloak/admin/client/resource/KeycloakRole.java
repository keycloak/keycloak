package org.keycloak.admin.client.resource;

import org.keycloak.representations.idm.RoleRepresentation;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.util.List;
import java.util.Set;

/**
 * @author rodrigo.sasaki@icarros.com.br
 */
public interface KeycloakRole {

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public RoleRepresentation toRepresentation();

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    public void update(RoleRepresentation roleRepresentation);

    @DELETE
    public void remove();

    @GET
    @Path("composites")
    @Produces(MediaType.APPLICATION_JSON)
    public Set<RoleRepresentation> getChildren();

    @GET
    @Path("composites/realm")
    @Produces(MediaType.APPLICATION_JSON)
    public Set<RoleRepresentation> getRealmLevelChildren();

    @GET
    @Path("composites/application/{appName}")
    @Produces(MediaType.APPLICATION_JSON)
    public Set<RoleRepresentation> getApplicationLevelChildren(@PathParam("appName") String appName);

    @POST
    @Path("composites")
    @Consumes(MediaType.APPLICATION_JSON)
    public void addChildren(List<RoleRepresentation> rolesToAdd);

    @DELETE
    @Path("composites")
    @Consumes(MediaType.APPLICATION_JSON)
    public void removeChildren(List<RoleRepresentation> rolesToRemove);

}

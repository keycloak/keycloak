package org.keycloak.admin.client.resource;

import org.keycloak.representations.idm.RoleRepresentation;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.util.List;
import java.util.Set;

/**
 * @author rodrigo.sasaki@icarros.com.br
 */
public interface RoleResource {

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

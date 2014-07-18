package org.keycloak.admin.client.service.interfaces;

import org.keycloak.representations.idm.RoleRepresentation;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;
import java.util.Set;

/**
 * @author rodrigo.sasaki@icarros.com.br
 */
public interface ApplicationRolesService {

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public List<RoleRepresentation> list();

    @GET
    @Path("{roleName}")
    @Produces(MediaType.APPLICATION_JSON)
    public RoleRepresentation getRepresentation(@PathParam("roleName") String roleName);

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response create(RoleRepresentation roleRepresentation);

    @PUT
    @Path("{roleName}")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response update(@PathParam("roleName") String roleName, RoleRepresentation roleRepresentation);

    @DELETE
    @Path("{roleName}")
    public void remove(@PathParam("roleName") String roleName);

    @GET
    @Path("{roleName}/composites")
    @Produces(MediaType.APPLICATION_JSON)
    public Set<RoleRepresentation> getChildren(@PathParam("roleName") String roleName);

    @GET
    @Path("{roleName}/composites/realm")
    @Produces(MediaType.APPLICATION_JSON)
    public Set<RoleRepresentation> getRealmLevelChildren(@PathParam("roleName") String roleName);

    @GET
    @Path("{roleName}/composites/application/{appName}")
    @Produces(MediaType.APPLICATION_JSON)
    public Set<RoleRepresentation> getApplicationLevelChildren(@PathParam("roleName") String roleName, @PathParam("appName") String appName);

    @POST
    @Path("{roleName}/composites")
    @Consumes(MediaType.APPLICATION_JSON)
    public void addChildren(@PathParam("roleName") String roleName, List<RoleRepresentation> rolesToAdd);

    @DELETE
    @Path("{roleName}/composites")
    @Consumes(MediaType.APPLICATION_JSON)
    public void removeChildren(@PathParam("roleName") String roleName, List<RoleRepresentation> rolesToRemove);

}

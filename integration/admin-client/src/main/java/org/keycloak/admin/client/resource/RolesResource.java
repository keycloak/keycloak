package org.keycloak.admin.client.resource;

import org.keycloak.representations.idm.RoleRepresentation;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.util.List;

/**
 * @author rodrigo.sasaki@icarros.com.br
 */
public interface RolesResource {

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public List<RoleRepresentation> list();

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public void create(RoleRepresentation roleRepresentation);

    @Path("{roleName}")
    public RoleResource get(@PathParam("roleName") String roleName);

    @Path("{role-name}")
    @DELETE
    public void deleteRole(final @PathParam("role-name") String roleName);

}

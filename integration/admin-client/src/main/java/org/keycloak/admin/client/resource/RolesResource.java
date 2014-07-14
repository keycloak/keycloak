package org.keycloak.admin.client.resource;

import org.keycloak.representations.idm.RoleRepresentation;

import javax.ws.rs.*;
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


}

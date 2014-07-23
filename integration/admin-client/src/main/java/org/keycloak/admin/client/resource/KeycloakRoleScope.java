package org.keycloak.admin.client.resource;

import org.keycloak.representations.idm.RoleRepresentation;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.util.List;

/**
 * @author rodrigo.sasaki@icarros.com.br
 */
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public interface KeycloakRoleScope {

    @GET
    public List<RoleRepresentation> listAll();

    @GET
    @Path("available")
    public List<RoleRepresentation> listAvailable();

    @GET
    @Path("composite")
    public List<RoleRepresentation> listEffective();

    @POST
    public void add(List<RoleRepresentation> rolesToAdd);

    @DELETE
    public void remove(List<RoleRepresentation> rolesToRemove);

}

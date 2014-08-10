package org.keycloak.admin.client.resource;

import org.keycloak.representations.idm.RoleRepresentation;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.util.List;

/**
 * @author rodrigo.sasaki@icarros.com.br
 */
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public interface RoleScopeResource {

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

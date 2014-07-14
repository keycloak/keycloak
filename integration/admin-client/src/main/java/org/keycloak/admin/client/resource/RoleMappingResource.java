package org.keycloak.admin.client.resource;

import org.keycloak.representations.idm.MappingsRepresentation;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;

/**
 * @author rodrigo.sasaki@icarros.com.br
 */
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public interface RoleMappingResource {

    @GET
    public MappingsRepresentation getAll();

    @Path("realm")
    public RoleScopeResource realmLevel();

    @Path("applications/{appName}")
    public RoleScopeResource applicationLevel(@PathParam("appName") String appName);

}

package org.keycloak.admin.client.service.interfaces;

import org.keycloak.representations.idm.MappingsRepresentation;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;

/**
 * @author rodrigo.sasaki@icarros.com.br
 */
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public interface UserRoleMappingsService {

    @GET
    public MappingsRepresentation getMappings();

    @Path("realm")
    public UserRolesService realmLevel();

    @Path("applications/{appName}")
    public UserRolesService applicationLevel(@PathParam("appName") String appName);

}

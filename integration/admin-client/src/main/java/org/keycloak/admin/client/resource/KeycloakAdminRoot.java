package org.keycloak.admin.client.resource;

import javax.ws.rs.Consumes;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.MediaType;

/**
 * @author rodrigo.sasaki@icarros.com.br
 */
@Path("/admin")
@Consumes(MediaType.APPLICATION_JSON)
public interface KeycloakAdminRoot {

    @Path("/realms/{realm}")
    public KeycloakRealm realm(@PathParam("realm") String realm);


}

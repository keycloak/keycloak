package org.keycloak.admin.client.service.interfaces;

import javax.ws.rs.Consumes;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.MediaType;

/**
 * @author rodrigo.sasaki@icarros.com.br
 */
@Path("/admin")
@Consumes(MediaType.APPLICATION_JSON)
public interface AdminRoot {

    @Path("/realms/{realm}")
    public RealmService realm(@PathParam("realm") String realm);


}

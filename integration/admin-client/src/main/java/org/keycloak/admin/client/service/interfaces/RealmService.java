package org.keycloak.admin.client.service.interfaces;

import javax.ws.rs.Path;

/**
 * @author rodrigo.sasaki@icarros.com.br
 */
public interface RealmService {

    @Path("/applications")
    public ApplicationsService applications();

    @Path("/users")
    public UsersService users();

}

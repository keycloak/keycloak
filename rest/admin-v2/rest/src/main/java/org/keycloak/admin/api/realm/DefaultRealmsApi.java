package org.keycloak.admin.api.realm;

import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;

import org.keycloak.models.KeycloakSession;
import org.keycloak.services.resources.admin.RealmsAdminResource;

public class DefaultRealmsApi implements RealmsApi {
    private final KeycloakSession session;
    private final RealmsAdminResource realmsAdminResource;

    public DefaultRealmsApi(KeycloakSession session, RealmsAdminResource realmsAdminResource) {
        this.session = session;
        this.realmsAdminResource = realmsAdminResource;
    }

    @Path("{name}")
    @Override
    public RealmApi realm(@PathParam("name") String name) {
        var realmAdmin = realmsAdminResource.getRealmAdmin(name);
        return new DefaultRealmApi(session, realmAdmin);
    }

}

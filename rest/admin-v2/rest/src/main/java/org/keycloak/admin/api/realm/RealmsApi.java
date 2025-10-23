package org.keycloak.admin.api.realm;


import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import org.keycloak.provider.Provider;

public interface RealmsApi extends Provider {

    @Path("{name}")
    RealmApi realm(@PathParam("name") String name);
}

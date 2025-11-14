package org.keycloak.admin.api.realm;

import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;

public interface RealmsApi {

    @Path("{name}")
    RealmApi realm(@PathParam("name") String name);
}

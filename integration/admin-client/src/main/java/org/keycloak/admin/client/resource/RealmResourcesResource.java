package org.keycloak.admin.client.resource;

import jakarta.ws.rs.Path;

public interface RealmResourcesResource {

    @Path("policies")
    RealmResourcePolicies policies();
}

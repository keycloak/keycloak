package org.keycloak.realm.resources.policies.admin.resource;

import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.Path;
import org.keycloak.common.Profile;
import org.keycloak.common.Profile.Feature;
import org.keycloak.models.KeycloakSession;

public class RealmResourcesResource {

    private final KeycloakSession session;

    public RealmResourcesResource(KeycloakSession session) {
        if (!Profile.isFeatureEnabled(Feature.RESOURCE_LIFECYCLE)) {
            throw new NotFoundException();
        }
        this.session = session;
    }

    @Path("policies")
    public RealmResourcePoliciesResource policies() {
        return new RealmResourcePoliciesResource(session);
    }
}

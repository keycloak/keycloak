package org.keycloak.test.framework.realm;

import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.representations.idm.RealmRepresentation;

public class ManagedRealm {

    private final String baseUrl;
    private final RealmRepresentation createdRepresentation;
    private final RealmResource realmResource;

    public ManagedRealm(String baseUrl, RealmRepresentation createdRepresentation, RealmResource realmResource) {
        this.baseUrl = baseUrl;
        this.createdRepresentation = createdRepresentation;
        this.realmResource = realmResource;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public String getName() {
        return createdRepresentation.getRealm();
    }

    public RealmResource admin() {
        return realmResource;
    }

}

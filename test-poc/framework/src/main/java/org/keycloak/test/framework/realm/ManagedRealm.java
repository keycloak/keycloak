package org.keycloak.test.framework.realm;

import org.keycloak.admin.client.resource.RealmResource;

public class ManagedRealm {

    private final String baseUrl;
    private final String name;
    private final RealmResource realmResource;

    public ManagedRealm(String baseUrl, String name, RealmResource realmResource) {
        this.baseUrl = baseUrl;
        this.name = name;
        this.realmResource = realmResource;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public String getName() {
        return name;
    }

    public RealmResource admin() {
        return realmResource;
    }

}

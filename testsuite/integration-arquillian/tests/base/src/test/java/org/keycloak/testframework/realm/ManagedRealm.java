package org.keycloak.testframework.realm;

import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.testsuite.AbstractKeycloakTest;

public class ManagedRealm {

    private final AbstractKeycloakTest test;
    private final String realmName;

    public ManagedRealm(AbstractKeycloakTest test, String realmName) {
        this.test = test;
        this.realmName = realmName;
    }

    public String getName() {
        return realmName;
    }

    public RealmResource admin() {
        return test.getAdminClient().realm(realmName);
    }

}

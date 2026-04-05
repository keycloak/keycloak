package org.keycloak.tests.scim.tck;

import org.keycloak.testframework.realm.RealmConfig;
import org.keycloak.testframework.realm.RealmConfigBuilder;

public class ScimRealmConfig implements RealmConfig {

    @Override
    public RealmConfigBuilder configure(RealmConfigBuilder realm) {
        return realm.scimEnabled(true);
    }
}

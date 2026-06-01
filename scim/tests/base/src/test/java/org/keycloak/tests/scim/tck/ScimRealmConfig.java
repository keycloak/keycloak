package org.keycloak.tests.scim.tck;

import org.keycloak.testframework.realm.RealmBuilder;
import org.keycloak.testframework.realm.RealmConfig;

public class ScimRealmConfig implements RealmConfig {

    @Override
    public RealmBuilder configure(RealmBuilder realm) {
        return realm.scimEnabled(true);
    }
}

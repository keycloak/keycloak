package org.keycloak.testframework.realm;

public class DefaultRealmConfig implements RealmConfig {

    @Override
    public RealmConfigBuilder configure(RealmConfigBuilder realm) {
        return realm;
    }

}

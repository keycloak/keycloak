package org.keycloak.testframework.realm;

public class DefaultRealmConfig implements RealmConfig {

    @Override
    public RealmBuilder configure(RealmBuilder realm) {
        return realm;
    }

}

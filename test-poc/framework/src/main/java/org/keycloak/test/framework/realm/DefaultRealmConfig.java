package org.keycloak.test.framework.realm;

import org.keycloak.representations.idm.RealmRepresentation;

public class DefaultRealmConfig implements RealmConfig {

    @Override
    public RealmRepresentation getRepresentation() {
        return new RealmRepresentation();
    }

}

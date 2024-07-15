package org.keycloak.test.framework.realm;

import org.keycloak.representations.idm.UserRepresentation;

public class DefaultUserConfig implements UserConfig {

    @Override
    public UserRepresentation getRepresentation() {
        return new UserRepresentation();
    }

}

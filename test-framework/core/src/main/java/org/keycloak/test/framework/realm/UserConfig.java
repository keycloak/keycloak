package org.keycloak.test.framework.realm;

import org.keycloak.representations.idm.UserRepresentation;

public interface UserConfig {

    UserRepresentation getRepresentation();

    default UserConfigBuilder builder() {
        return new UserConfigBuilder();
    }

}

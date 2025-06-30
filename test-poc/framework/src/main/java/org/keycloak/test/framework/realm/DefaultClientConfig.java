package org.keycloak.test.framework.realm;

import org.keycloak.representations.idm.ClientRepresentation;

public class DefaultClientConfig implements ClientConfig {

    @Override
    public ClientRepresentation getRepresentation() {
        return new ClientRepresentation();
    }

}

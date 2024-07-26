package org.keycloak.test.framework.realm;

import org.keycloak.representations.idm.ClientRepresentation;

public interface ClientConfig {

    ClientRepresentation getRepresentation();

    default ClientConfigBuilder builder() {
        return new ClientConfigBuilder();
    }

}

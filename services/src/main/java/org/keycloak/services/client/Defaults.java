package org.keycloak.services.client;

import org.keycloak.representations.admin.v2.ClientRepresentation;

public class Defaults {

    public ClientRepresentation applyDefaults(ClientRepresentation client) {
        // More clever and comprehensive logic goes here.
        if (client.getEnabled() == null) {
            client.setEnabled(true);
        }
        return client;
    }

}

package org.keycloak.testsuite.updaters;

import java.io.Closeable;
import java.io.IOException;

import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.representations.idm.ClientRepresentation;

public class ClientAttributeUpdater implements Closeable {

    private final ClientResource client;
    private final ClientRepresentation original;
    private final ClientRepresentation updated;

    public static ClientAttributeUpdater forClient(Keycloak adminClient, String realmName, String clientId) {
        String id = adminClient.realm(realmName).clients().findByClientId(clientId).get(0).getId();
        return new ClientAttributeUpdater(adminClient.realm(realmName).clients().get(id));
    }

    public ClientAttributeUpdater(ClientResource client) {
        this.client = client;
        this.original = client.toRepresentation();
        this.updated = client.toRepresentation();
        if (this.updated.getAttributes() == null) {
            this.updated.setAttributes(new java.util.HashMap<>());
        }
    }

    public ClientAttributeUpdater setAttribute(String name, String value) {
        updated.getAttributes().put(name, value);
        return this;
    }

    public ClientAttributeUpdater update() {
        client.update(updated);
        return this;
    }

    @Override
    public void close() throws IOException {
        client.update(original);
    }
}

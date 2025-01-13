package org.keycloak.testframework.realm;

import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.representations.idm.ClientRepresentation;

public class ManagedClient {

    private final ClientRepresentation createdRepresentation;
    private final ClientResource clientResource;

    public ManagedClient(ClientRepresentation createdRepresentation, ClientResource clientResource) {
        this.createdRepresentation = createdRepresentation;
        this.clientResource = clientResource;
    }

    public String getId() {
        return createdRepresentation.getId();
    }

    public String getClientId() {
        return createdRepresentation.getClientId();
    }

    public String getSecret() {
        return createdRepresentation.getSecret();
    }

    public ClientResource admin() {
        return clientResource;
    }

}

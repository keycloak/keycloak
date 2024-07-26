package org.keycloak.test.framework.realm;

import org.keycloak.representations.idm.ClientRepresentation;

public class ClientConfigBuilder {

    private final ClientRepresentation representation;

    public ClientConfigBuilder() {
        this.representation = new ClientRepresentation();
        this.representation.setEnabled(true);
    }

    public ClientConfigBuilder clientId(String clientId) {
        representation.setClientId(clientId);
        return this;
    }

    public ClientConfigBuilder secret(String secret) {
        representation.setSecret(secret);
        return this;
    }

    public ClientConfigBuilder redirectUris(String... redirectUris) {
        representation.setRedirectUris(Collections.combine(representation.getRedirectUris(), redirectUris));
        return this;
    }

    public ClientConfigBuilder serviceAccount() {
        representation.setServiceAccountsEnabled(true);
        return this;
    }

    public ClientRepresentation build() {
        return representation;
    }

}

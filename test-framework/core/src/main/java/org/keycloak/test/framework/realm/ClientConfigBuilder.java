package org.keycloak.test.framework.realm;

import org.keycloak.representations.idm.ClientRepresentation;

public class ClientConfigBuilder {

    private final ClientRepresentation rep;

    private ClientConfigBuilder(ClientRepresentation rep) {
        this.rep = rep;
    }

    public static ClientConfigBuilder create() {
        ClientRepresentation rep = new ClientRepresentation();
        rep.setEnabled(true);
        return new ClientConfigBuilder(rep);
    }

    public static ClientConfigBuilder update(ClientRepresentation rep) {
        return new ClientConfigBuilder(rep);
    }

    public ClientConfigBuilder enabled(boolean enabled) {
        rep.setEnabled(enabled);
        return this;
    }

    public ClientConfigBuilder clientId(String clientId) {
        rep.setClientId(clientId);
        return this;
    }

    public ClientConfigBuilder secret(String secret) {
        rep.setSecret(secret);
        return this;
    }

    public ClientConfigBuilder redirectUris(String... redirectUris) {
        rep.setRedirectUris(Collections.combine(rep.getRedirectUris(), redirectUris));
        return this;
    }

    public ClientConfigBuilder serviceAccount() {
        rep.setServiceAccountsEnabled(true);
        return this;
    }

    public ClientConfigBuilder directAccessGrants() {
        rep.setDirectAccessGrantsEnabled(true);
        return this;
    }

    public ClientConfigBuilder authorizationServices() {
        rep.setAuthorizationServicesEnabled(true);
        return this;
    }

    public ClientRepresentation build() {
        return rep;
    }

}

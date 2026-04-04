package org.keycloak.testframework.admin;

import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;

public class AdminClientBuilder {

    private final AdminClientFactory adminClientFactory;
    private final KeycloakBuilder delegate;
    private boolean close = false;

    public AdminClientBuilder(AdminClientFactory adminClientFactory, KeycloakBuilder delegate) {
        this.adminClientFactory = adminClientFactory;
        this.delegate = delegate;
    }

    public AdminClientBuilder realm(String realm) {
        delegate.realm(realm);
        return this;
    }

    public AdminClientBuilder grantType(String grantType) {
        delegate.grantType(grantType);
        return this;
    }

    public AdminClientBuilder username(String username) {
        delegate.username(username);
        return this;
    }

    public AdminClientBuilder password(String password) {
        delegate.password(password);
        return this;
    }

    public AdminClientBuilder clientId(String clientId) {
        delegate.clientId(clientId);
        return this;
    }

    public AdminClientBuilder scope(String scope) {
        delegate.scope(scope);
        return this;
    }

    public AdminClientBuilder clientSecret(String clientSecret) {
        delegate.clientSecret(clientSecret);
        return this;
    }

    public AdminClientBuilder authorization(String accessToken) {
        delegate.authorization(accessToken);
        return this;
    }

    public AdminClientBuilder autoClose() {
        this.close = true;
        return this;
    }

    public Keycloak build() {
        Keycloak keycloak = delegate.build();
        if (close) {
            adminClientFactory.addToClose(keycloak);
        }
        return keycloak;
    }
}

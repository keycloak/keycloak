package org.keycloak.testframework.admin;

import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;

import java.util.ArrayList;
import java.util.List;


public class KeycloakAdminClientFactory {

    private final List<Keycloak> adminClients = new ArrayList<>();

    private final String serverUrl;

    public KeycloakAdminClientFactory(String serverUrl) {
        this.serverUrl = serverUrl;
    }

    public Keycloak create(AutoClose autoClose, String realm, String clientId, String clientSecret, String username, String password) {
        KeycloakBuilder clientBuilder = KeycloakBuilder.builder()
                .serverUrl(serverUrl)
                .realm(realm)
                .grantType(OAuth2Constants.PASSWORD)
                .clientId(clientId).clientSecret(clientSecret)
                .username(username).password(password);

        Keycloak adminClient = clientBuilder.build();

        if (autoClose == AutoClose.Automatic) {
            adminClients.add(adminClient);
        }
        return adminClient;
    }

    public Keycloak create(AutoClose autoClose, String realm, String clientId, String clientSecret) {
        KeycloakBuilder clientBuilder = KeycloakBuilder.builder()
                .serverUrl(serverUrl)
                .realm(realm)
                .grantType(OAuth2Constants.CLIENT_CREDENTIALS)
                .clientId(clientId).clientSecret(clientSecret);

        Keycloak adminClient = clientBuilder.build();

        if (autoClose == AutoClose.Automatic) {
            adminClients.add(adminClient);
        }
        return adminClient;
    }

    public enum AutoClose {
        Automatic,
        Manual
    }

    public void close() {
        adminClients.forEach(Keycloak::close);
    }

}

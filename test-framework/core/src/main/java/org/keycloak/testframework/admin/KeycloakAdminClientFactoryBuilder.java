package org.keycloak.testframework.admin;

import org.keycloak.admin.client.Config;
import org.keycloak.testframework.realm.ManagedClient;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.ManagedUser;

import java.util.Map;

public class KeycloakAdminClientFactoryBuilder {

    private String serverUrl;
    private String grantType;
    private Map<String, String> adminClientSettings;
    private KeycloakAdminClientFactory.DependencyFetcher<ManagedRealm> dependencyFetcherRealm;
    private KeycloakAdminClientFactory.DependencyFetcher<ManagedClient> dependencyFetcherClient;
    private KeycloakAdminClientFactory.DependencyFetcher<ManagedUser> dependencyFetcherUser;


    public KeycloakAdminClientFactoryBuilder serverUrl(String serverUrl) {
        this.serverUrl = serverUrl;
        return this;
    }

    public KeycloakAdminClientFactoryBuilder grantType(String grantType) {
        Config.checkGrantType(grantType);
        this.grantType = grantType;
        return this;
    }

    public KeycloakAdminClientFactoryBuilder adminClientSettings(Map<String, String> adminClientSettings) {
        // todo
        this.adminClientSettings = adminClientSettings;
        return this;
    }

    public KeycloakAdminClientFactoryBuilder dependencyFetcherRealm(KeycloakAdminClientFactory.DependencyFetcher<ManagedRealm> dependencyFetcherRealm) {
        this.dependencyFetcherRealm = dependencyFetcherRealm;
        return this;
    }

    public KeycloakAdminClientFactoryBuilder dependencyFetcherClient(KeycloakAdminClientFactory.DependencyFetcher<ManagedClient> dependencyFetcherClient) {
        this.dependencyFetcherClient = dependencyFetcherClient;
        return this;
    }

    public KeycloakAdminClientFactoryBuilder dependencyFetcherUser(KeycloakAdminClientFactory.DependencyFetcher<ManagedUser> dependencyFetcherUser) {
        this.dependencyFetcherUser = dependencyFetcherUser;
        return this;
    }

    private KeycloakAdminClientFactoryBuilder() {

    }

    /**
     * Returns a new KeycloakAdminClientFactory builder.
     */
    public static KeycloakAdminClientFactoryBuilder builder() {
        return new KeycloakAdminClientFactoryBuilder();
    }

    public KeycloakAdminClientFactory build() {
        return new KeycloakAdminClientFactory(serverUrl, grantType, adminClientSettings, dependencyFetcherRealm, dependencyFetcherClient, dependencyFetcherUser);
    }
}

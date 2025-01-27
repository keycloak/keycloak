package org.keycloak.testframework.admin;

import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.keycloak.testframework.config.Config;
import org.keycloak.testframework.realm.ManagedClient;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.ManagedUser;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;


public class KeycloakAdminClientFactory {

    private List<Keycloak> adminClients = new ArrayList<>();

    private final String serverUrl;
    private final String grantType;
    private Map<String, String> adminClientSettings; // todo
    private KeycloakAdminClientFactory.DependencyFetcher<ManagedRealm> dependencyFetcherRealm;
    private KeycloakAdminClientFactory.DependencyFetcher<ManagedClient> dependencyFetcherClient;
    private KeycloakAdminClientFactory.DependencyFetcher<ManagedUser> dependencyFetcherUser;

    public KeycloakAdminClientFactory(String serverUrl,
                                      String grantType,
                                      Map<String, String> adminClientSettings,
                                      KeycloakAdminClientFactory.DependencyFetcher<ManagedRealm> dependencyFetcherRealm, DependencyFetcher<ManagedClient> dependencyFetcherClient, DependencyFetcher<ManagedUser> dependencyFetcherUser) {
        this.serverUrl = serverUrl;
        this.grantType = grantType;
        this.adminClientSettings = adminClientSettings;
        this.dependencyFetcherRealm = dependencyFetcherRealm;
        this.dependencyFetcherClient = dependencyFetcherClient;
        this.dependencyFetcherUser = dependencyFetcherUser;
    }

    public Keycloak createMaster() {
        KeycloakBuilder clientBuilder = KeycloakBuilder.builder()
                .serverUrl(serverUrl)
                .grantType(grantType);

        clientBuilder.realm("master").clientId(Config.getAdminClientId()).clientSecret(Config.getAdminClientSecret());
        Keycloak adminClient = clientBuilder.build();
        adminClients.add(adminClient);
        return adminClient;
    }

    public Keycloak create(String realmRef, String clientRef, String userRef) {
        KeycloakBuilder clientBuilder = createBuilder(realmRef);

        ManagedClient managedClient = dependencyFetcherClient.getDependency(ManagedClient.class, clientRef);
        clientBuilder.clientId(managedClient.getClientId()).clientSecret(managedClient.getSecret());

        ManagedUser managedUser = dependencyFetcherUser.getDependency(ManagedUser.class, userRef);
        clientBuilder.username(managedUser.getUsername())
                .password(managedUser.getPassword())
                .grantType(OAuth2Constants.PASSWORD);


        Keycloak adminClient = clientBuilder.build();
        adminClients.add(adminClient);
        return adminClient;
    }

    public Keycloak create(String realmRef, String clientRef) {
        KeycloakBuilder clientBuilder = createBuilder(realmRef);

        ManagedClient managedClient = dependencyFetcherClient.getDependency(ManagedClient.class, clientRef);
        clientBuilder.clientId(managedClient.getClientId()).clientSecret(managedClient.getSecret());

        Keycloak adminClient = clientBuilder.build();
        adminClients.add(adminClient);
        return adminClient;
    }

    private KeycloakBuilder createBuilder(String realmRef) {
        ManagedRealm managedRealm = dependencyFetcherRealm.getDependency(ManagedRealm.class, realmRef);

        return KeycloakBuilder.builder()
                .serverUrl(serverUrl)
                .grantType(grantType)
                .realm(managedRealm.getName());
    }

    public void close() {
        adminClients.forEach(Keycloak::close);
    }

    public interface DependencyFetcher<D> {
        D getDependency(Class<D> clazz, String ref);
    }
}

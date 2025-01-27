package org.keycloak.testframework.admin;

import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testframework.TestFrameworkException;
import org.keycloak.testframework.annotations.InjectAdminClientFactory;
import org.keycloak.testframework.config.Config;
import org.keycloak.testframework.injection.InstanceContext;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.ManagedUser;

import java.util.ArrayList;
import java.util.List;


public class KeycloakAdminClientFactory {

    private final List<Keycloak> adminClients = new ArrayList<>();

    private final InstanceContext<KeycloakAdminClientFactory, InjectAdminClientFactory> instanceContext;
    private final String serverUrl;
    private final String grantType;

    public KeycloakAdminClientFactory(InstanceContext<KeycloakAdminClientFactory, InjectAdminClientFactory> instanceContext, String serverUrl, String grantType) {
        this.instanceContext = instanceContext;
        this.serverUrl = serverUrl;
        this.grantType = grantType;
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

    public Keycloak create(String realmRef, String clientId, String user) {
        ManagedRealm managedRealm = instanceContext.getDependency(ManagedRealm.class, realmRef);

        KeycloakBuilder clientBuilder = createBuilder(managedRealm.getName());
        RealmResource realmRes = managedRealm.admin();

        setClient(clientBuilder, realmRes, clientId);
        setUser(clientBuilder, realmRes, user);

        Keycloak adminClient = clientBuilder.build();
        adminClients.add(adminClient);
        return adminClient;
    }

    public Keycloak create(String realmRef, String clientId) {
        ManagedRealm managedRealm = instanceContext.getDependency(ManagedRealm.class, realmRef);

        KeycloakBuilder clientBuilder = createBuilder(managedRealm.getName());
        RealmResource realmRes = managedRealm.admin();

        setClient(clientBuilder, realmRes, clientId);

        Keycloak adminClient = clientBuilder.build();
        adminClients.add(adminClient);
        return adminClient;
    }

    private KeycloakBuilder createBuilder(String realm) {
        return KeycloakBuilder.builder()
                .serverUrl(serverUrl)
                .grantType(grantType)
                .realm(realm);
    }

    private void setClient(KeycloakBuilder clientBuilder, RealmResource realmRes, String clientId) {
        ClientRepresentation clientRep = realmRes.clients().findByClientId(clientId).stream()
                .findFirst().orElseThrow(() -> new TestFrameworkException("Client " + clientId + " not found in managed realm"));
        clientBuilder.clientId(clientId).clientSecret(clientRep.getSecret());
    }

    private void setUser(KeycloakBuilder clientBuilder, RealmResource realmRes, String user) {
        UserRepresentation userRep = realmRes.users().search(user).stream()
                .findFirst().orElseThrow(() -> new TestFrameworkException("User " + user + " not found in managed realm"));
        String password = ManagedUser.getPassword(userRep);
        clientBuilder.username(userRep.getUsername()).password(password);
        clientBuilder.grantType(OAuth2Constants.PASSWORD);
    }

    public void close() {
        adminClients.forEach(Keycloak::close);
    }

}

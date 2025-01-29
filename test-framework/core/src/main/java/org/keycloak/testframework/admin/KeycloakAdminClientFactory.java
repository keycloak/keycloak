package org.keycloak.testframework.admin;

import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testframework.TestFrameworkException;
import org.keycloak.testframework.annotations.InjectAdminClientFactory;
import org.keycloak.testframework.injection.InstanceContext;
import org.keycloak.testframework.realm.ManagedRealm;

import java.util.ArrayList;
import java.util.List;


public class KeycloakAdminClientFactory {

    private final List<Keycloak> adminClients = new ArrayList<>();

    private final InstanceContext<KeycloakAdminClientFactory, InjectAdminClientFactory> instanceContext;
    private final String serverUrl;

    public KeycloakAdminClientFactory(InstanceContext<KeycloakAdminClientFactory, InjectAdminClientFactory> instanceContext, String serverUrl) {
        this.instanceContext = instanceContext;
        this.serverUrl = serverUrl;
    }

    public Keycloak create(String realmRef, String clientId, String user, String password) {
        ManagedRealm managedRealm = instanceContext.getDependency(ManagedRealm.class, realmRef);

        KeycloakBuilder clientBuilder = createBuilder(managedRealm.getName());
        clientBuilder.grantType(OAuth2Constants.PASSWORD);
        RealmResource realmRes = managedRealm.admin();

        setClient(clientBuilder, realmRes, clientId);
        setUser(clientBuilder, realmRes, user, password);

        Keycloak adminClient = clientBuilder.build();
        adminClients.add(adminClient);
        return adminClient;
    }

    public Keycloak create(String realmRef, String clientId) {
        ManagedRealm managedRealm = instanceContext.getDependency(ManagedRealm.class, realmRef);

        KeycloakBuilder clientBuilder = createBuilder(managedRealm.getName());
        clientBuilder.grantType(OAuth2Constants.CLIENT_CREDENTIALS);
        RealmResource realmRes = managedRealm.admin();

        setClient(clientBuilder, realmRes, clientId);

        Keycloak adminClient = clientBuilder.build();
        adminClients.add(adminClient);
        return adminClient;
    }

    private KeycloakBuilder createBuilder(String realm) {
        return KeycloakBuilder.builder()
                .serverUrl(serverUrl)
                .realm(realm);
    }

    private void setClient(KeycloakBuilder clientBuilder, RealmResource realmRes, String clientId) {
        ClientRepresentation clientRep = realmRes.clients().findByClientId(clientId).stream()
                .findFirst().orElseThrow(() -> new TestFrameworkException("Client " + clientId + " not found in managed realm"));
        clientBuilder.clientId(clientId).clientSecret(clientRep.getSecret());
    }

    private void setUser(KeycloakBuilder clientBuilder, RealmResource realmRes, String user, String password) {
        UserRepresentation userRep = realmRes.users().search(user).stream()
                .findFirst().orElseThrow(() -> new TestFrameworkException("User " + user + " not found in managed realm"));
        clientBuilder.username(userRep.getUsername()).password(password);
    }

    public void close() {
        adminClients.forEach(Keycloak::close);
    }

}

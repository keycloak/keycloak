package org.keycloak.tests.authz.services.uma;

import java.util.List;
import java.util.Map;
import java.util.Set;

import jakarta.ws.rs.core.Response.Status;

import org.keycloak.authorization.client.AuthzClient;
import org.keycloak.authorization.client.Configuration;
import org.keycloak.authorization.client.resource.ProtectionResource;
import org.keycloak.authorization.client.util.HttpResponseException;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.authorization.ResourceRepresentation;
import org.keycloak.representations.idm.authorization.ResourceServerRepresentation;
import org.keycloak.testframework.annotations.InjectClient;
import org.keycloak.testframework.annotations.InjectHttpClient;
import org.keycloak.testframework.annotations.InjectKeycloakUrls;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.injection.LifeCycle;
import org.keycloak.testframework.realm.ClientConfig;
import org.keycloak.testframework.realm.ClientConfigBuilder;
import org.keycloak.testframework.realm.ManagedClient;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.RealmConfig;
import org.keycloak.testframework.realm.RealmConfigBuilder;
import org.keycloak.testframework.server.KeycloakUrls;

import org.apache.http.client.HttpClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@KeycloakIntegrationTest
public class ProtectionApiTest {

    private static final String RESOURCE_SERVER_ID = "resource-server";

    @InjectRealm(config = TestRealmConfig.class, lifecycle = LifeCycle.METHOD)
    ManagedRealm realm;

    @InjectClient(config = TestResourceServerConfig.class, lifecycle = LifeCycle.METHOD)
    ManagedClient resourceServer;

    @InjectKeycloakUrls
    KeycloakUrls keycloakUrls;

    @InjectHttpClient
    HttpClient httpClient;

    private AuthzClient authzClient;

    @BeforeEach
    public void enableRemoteResourceManagement() {
        RoleRepresentation umaProtectionRole = resourceServer.admin().roles().get("uma_protection").toRepresentation();
        for (String username : List.of("alice", "marta")) {
            String userId = realm.admin().users().search(username).get(0).getId();
            realm.admin().users().get(userId).roles().clientLevel(resourceServer.getId()).add(List.of(umaProtectionRole));
        }

        ResourceServerRepresentation settings = resourceServer.admin().authorization().getSettings();
        settings.setAllowRemoteResourceManagement(true);
        resourceServer.admin().authorization().update(settings);

        authzClient = AuthzClient.create(new Configuration(keycloakUrls.getBase(), realm.getName(), resourceServer.getClientId(),
                Map.of("secret", resourceServer.getSecret()), httpClient));
    }

    @Test
    public void testRejectsClientSuppliedIdWithoutChangingExistingResource() {
        ProtectionResource alice = authzClient.protection("alice", "password");
        ResourceRepresentation existing = alice.resource().create(new ResourceRepresentation("alice-resource", Set.of()));

        ResourceRepresentation registration = new ResourceRepresentation("overwritten-resource", Set.of());
        registration.setId(existing.getId());

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> authzClient.protection("marta", "password").resource().create(registration));
        assertEquals(Status.BAD_REQUEST.getStatusCode(), ((HttpResponseException) exception.getCause()).getStatusCode());
        assertEquals("alice-resource", alice.resource().findById(existing.getId()).getName());

        ResourceRepresentation created = authzClient.protection("marta", "password").resource()
                .create(new ResourceRepresentation("marta-resource", Set.of()));
        assertNotNull(created.getId());
    }

    public static class TestRealmConfig implements RealmConfig {

        @Override
        public RealmConfigBuilder configure(RealmConfigBuilder realm) {
            realm.addUser("alice").name("Alice", "Test").email("alice@example.org").emailVerified(true).password("password");
            realm.addUser("marta").name("Marta", "Test").email("marta@example.org").emailVerified(true).password("password");
            return realm;
        }
    }

    public static class TestResourceServerConfig implements ClientConfig {

        @Override
        public ClientConfigBuilder configure(ClientConfigBuilder client) {
            return client.clientId(RESOURCE_SERVER_ID)
                    .secret("secret")
                    .authorizationServicesEnabled(true)
                    .directAccessGrantsEnabled(true);
        }
    }
}

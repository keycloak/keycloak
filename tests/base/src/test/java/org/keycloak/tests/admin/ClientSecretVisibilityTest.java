package org.keycloak.tests.admin;

import java.util.List;

import jakarta.ws.rs.ForbiddenException;

import org.keycloak.admin.client.Keycloak;
import org.keycloak.models.AdminRoles;
import org.keycloak.models.Constants;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.ComponentRepresentation;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.testframework.admin.AdminClientFactory;
import org.keycloak.testframework.annotations.InjectAdminClientFactory;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.realm.ClientBuilder;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.RealmBuilder;
import org.keycloak.testframework.realm.RealmConfig;
import org.keycloak.testframework.realm.UserBuilder;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.fail;

@KeycloakIntegrationTest
public class ClientSecretVisibilityTest {

    private static final String VIEW_ONLY_USER = "view-only-user";
    private static final String MANAGE_USER = "manage-user";
    private static final String PASSWORD = "password";
    private static final String CONFIDENTIAL_CLIENT_ID = "test-confidential";
    private static final String CONFIDENTIAL_CLIENT_SECRET = "test-secret-value";

    @InjectRealm(config = SecretVisibilityRealmConfig.class)
    ManagedRealm realm;

    @InjectAdminClientFactory
    AdminClientFactory adminClientFactory;

    private String confidentialClientUuid;

    @BeforeEach
    public void setUp() {
        confidentialClientUuid = realm.admin()
                .clients().findByClientId(CONFIDENTIAL_CLIENT_ID)
                .get(0).getId();
    }

    @Test
    public void viewOnly_getClient_secretIsMasked() {
        try (Keycloak viewClient = createViewOnlyClient()) {
            ClientRepresentation rep = viewClient.realm(realm.getName())
                    .clients().get(confidentialClientUuid)
                    .toRepresentation();
            assertThat(rep.getSecret(), is(not(CONFIDENTIAL_CLIENT_SECRET)));
            assertThat(rep.getSecret(), anyOf(nullValue(), is(ComponentRepresentation.SECRET_VALUE)));
        }
    }

    @Test
    public void viewOnly_getClients_secretsAreMasked() {
        try (Keycloak viewClient = createViewOnlyClient()) {
            List<ClientRepresentation> clients = viewClient.realm(realm.getName())
                    .clients().findAll();

            ClientRepresentation target = clients.stream()
                    .filter(c -> CONFIDENTIAL_CLIENT_ID.equals(c.getClientId()))
                    .findFirst()
                    .orElseThrow(() -> new AssertionError(
                            "Confidential client not found in list"));

            assertThat(target.getSecret(), is(not(CONFIDENTIAL_CLIENT_SECRET)));
            assertThat(target.getSecret(),
                    anyOf(nullValue(), is(ComponentRepresentation.SECRET_VALUE)));
        }
    }

    @Test
    public void viewOnly_getClientRotatedSecret_returns403() {
        try (Keycloak viewClient = createViewOnlyClient()) {
            try {
                viewClient.realm(realm.getName())
                        .clients().get(confidentialClientUuid)
                        .getClientRotatedSecret();
                fail("Expected ForbiddenException for view-only user"
                        + " on getClientRotatedSecret()");
            } catch (ForbiddenException expected) {
                // Expected -- 403 before the not-found check
            } catch (jakarta.ws.rs.NotFoundException e) {
                fail("Got 404 instead of 403 — auth check is too permissive");
            }
        }
    }

    @Test
    public void viewOnly_getClientSecret_returns403() {
        try (Keycloak viewClient = createViewOnlyClient()) {
            try {
                viewClient.realm(realm.getName())
                        .clients().get(confidentialClientUuid)
                        .getSecret();
                fail("Expected ForbiddenException for view-only user on getClientSecret()");
            } catch (ForbiddenException expected) {
                // Expected -- view-only users must not access the dedicated secret endpoint
            }
        }
    }

    @Test
    public void manageRole_getClient_secretIsVisible() {
        try (Keycloak manageClient = createManageClient()) {
            ClientRepresentation rep = manageClient.realm(realm.getName())
                    .clients().get(confidentialClientUuid).toRepresentation();

            assertThat(rep.getSecret(), is(notNullValue()));
            assertThat(rep.getSecret(), is(not(ComponentRepresentation.SECRET_VALUE)));
        }
    }

    @Test
    public void manageRole_getClients_secretsAreVisible() {
        try (Keycloak manageClient = createManageClient()) {
            List<ClientRepresentation> clients = manageClient.realm(realm.getName())
                    .clients().findAll();

            ClientRepresentation target = clients.stream()
                    .filter(c -> CONFIDENTIAL_CLIENT_ID.equals(c.getClientId()))
                    .findFirst()
                    .orElseThrow(() -> new AssertionError(
                            "Confidential client not found in list"));

            assertThat(target.getSecret(), is(notNullValue()));
            assertThat(target.getSecret(), is(not(ComponentRepresentation.SECRET_VALUE)));
        }
    }

    @Test
    public void manageRole_getClientSecret_returnsSecret() {
        try (Keycloak manageClient = createManageClient()) {
            CredentialRepresentation secret = manageClient.realm(realm.getName())
                    .clients().get(confidentialClientUuid)
                    .getSecret();

            assertThat(secret, is(notNullValue()));
            assertThat(secret.getValue(), is(notNullValue()));
            assertThat(secret.getValue(), is(not(ComponentRepresentation.SECRET_VALUE)));
        }
    }

    @Test
    public void publicClient_getClient_secretIsNull() {
        List<ClientRepresentation> clients = realm.admin().clients().findAll();

        ClientRepresentation publicClient = clients.stream()
                .filter(c -> "test-public".equals(c.getClientId()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Public client not found"));

        assertThat(publicClient.getSecret(), is(nullValue()));
    }

    // --- Realm Configuration ---

    public static class SecretVisibilityRealmConfig implements RealmConfig {
        @Override
        public RealmBuilder configure(RealmBuilder realm) {
            realm.clients(ClientBuilder.create(CONFIDENTIAL_CLIENT_ID)
                    .secret(CONFIDENTIAL_CLIENT_SECRET)
                    .directAccessGrantsEnabled(true));

            // Public client for regression test
            realm.clients(ClientBuilder.create("test-public")
                          .publicClient());

            // User with only view-clients role
            realm.users(UserBuilder.create(VIEW_ONLY_USER)
                    .password(PASSWORD)
                    .email("viewonly@localhost")
                    .firstName("View")
                    .lastName("Only")
                    .clientRoles(Constants.REALM_MANAGEMENT_CLIENT_ID,
                            AdminRoles.VIEW_CLIENTS));

            // User with manage-clients role
            realm.users(UserBuilder.create(MANAGE_USER)
                    .password(PASSWORD)
                    .email("manage@localhost")
                    .firstName("Manage")
                    .lastName("Clients")
                    .clientRoles(Constants.REALM_MANAGEMENT_CLIENT_ID,
                            AdminRoles.MANAGE_CLIENTS));

            return realm;
        }
    }

    private Keycloak createViewOnlyClient() {
        return adminClientFactory.create()
                .realm(realm.getName())
                .username(VIEW_ONLY_USER)
                .password(PASSWORD)
                .clientId(Constants.ADMIN_CLI_CLIENT_ID)
                .build();
    }

    private Keycloak createManageClient() {
        return adminClientFactory.create()
                .realm(realm.getName())
                .username(MANAGE_USER)
                .password(PASSWORD)
                .clientId(Constants.ADMIN_CLI_CLIENT_ID)
                .build();
    }
}

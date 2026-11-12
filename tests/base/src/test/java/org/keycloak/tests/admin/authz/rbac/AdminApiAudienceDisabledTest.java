package org.keycloak.tests.admin.authz.rbac;

import jakarta.ws.rs.core.Response;

import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.models.AdminRoles;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.testframework.admin.AdminClientFactory;
import org.keycloak.testframework.annotations.InjectAdminClientFactory;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.server.KeycloakServerConfig;
import org.keycloak.testframework.server.KeycloakServerConfigBuilder;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@KeycloakIntegrationTest(config = AdminApiAudienceDisabledTest.AudienceCheckDisabledConfig.class)
public class AdminApiAudienceDisabledTest extends AbstractAdminRBACTest {

    private static final String REALM_NAME = "admin-api-audience-disabled";
    private static final String USERNAME = "test-admin";
    private static final String CUSTOM_CLIENT_ID = "custom-admin-client";

    @InjectAdminClientFactory
    AdminClientFactory adminClientFactory;

    @Test
    public void testClientWithoutScopeIsAcceptedWhenCheckDisabled() {
        RealmResource realm = createRealm(adminClient, REALM_NAME);
        try {
            createUser(realm, USERNAME);
            grantRealmManagementRole(realm, USERNAME, AdminRoles.VIEW_REALM);
            createCustomClient(realm);
            try (Keycloak client = adminClientFactory.create()
                    .realm(REALM_NAME)
                    .clientId(CUSTOM_CLIENT_ID)
                    .username(USERNAME)
                    .password("password")
                    .build()) {
                assertEquals(REALM_NAME, client.realm(REALM_NAME).toRepresentation().getRealm());
            }
        } finally {
            realm.remove();
        }
    }

    private void createCustomClient(RealmResource realm) {
        ClientRepresentation client = new ClientRepresentation();
        client.setClientId(CUSTOM_CLIENT_ID);
        client.setPublicClient(true);
        client.setDirectAccessGrantsEnabled(true);
        client.setStandardFlowEnabled(false);
        client.setFullScopeAllowed(true);
        try (Response response = realm.clients().create(client)) {
            assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus());
        }
    }

    public static class AudienceCheckDisabledConfig implements KeycloakServerConfig {
        @Override
        public KeycloakServerConfigBuilder configure(KeycloakServerConfigBuilder config) {
            return config.option("spi-admin--audience-check-enabled", "false");
        }
    }
}

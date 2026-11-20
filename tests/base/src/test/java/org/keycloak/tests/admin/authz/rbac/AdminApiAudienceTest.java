package org.keycloak.tests.admin.authz.rbac;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jakarta.ws.rs.NotAuthorizedException;
import jakarta.ws.rs.core.Response;

import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.admin.client.resource.ClientScopeResource;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.models.AdminRoles;
import org.keycloak.models.Constants;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.ProtocolMapperRepresentation;
import org.keycloak.testframework.admin.AdminClientBuilder;
import org.keycloak.testframework.admin.AdminClientFactory;
import org.keycloak.testframework.annotations.InjectAdminClientFactory;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.oauth.OAuthClient;
import org.keycloak.testframework.oauth.annotations.InjectOAuthClient;
import org.keycloak.testsuite.util.oauth.AccessTokenResponse;
import org.keycloak.util.TokenUtil;

import org.junit.jupiter.api.Test;

import static org.keycloak.tests.utils.admin.AdminApiUtil.addAdminApiClientScope;
import static org.keycloak.tests.utils.admin.AdminApiUtil.getAdminApiClientScopeId;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@KeycloakIntegrationTest
public class AdminApiAudienceTest extends AbstractAdminRBACTest {

    private static final String REALM_NAME = "admin-api-audience";
    private static final String USERNAME = "test-admin";
    private static final String CUSTOM_CLIENT_ID = "custom-admin-client";
    private static final String WRONG_AUDIENCE = "https://wrong.example.com/admin/realms/other";

    @InjectAdminClientFactory
    AdminClientFactory adminClientFactory;

    @InjectOAuthClient(realmRef = "master")
    OAuthClient oauth;

    @Test
    public void testDefaultAdminCliClientHasAudience() {
        withRealm((realm) -> {
            try (Keycloak client = createClient(Constants.ADMIN_CLI_CLIENT_ID)) {
                assertEquals(REALM_NAME, client.realm(REALM_NAME).toRepresentation().getRealm());
            }
        });
    }

    @Test
    public void testCustomClientWithoutScopeIsRejected() {
        withRealm((realm) -> {
            createCustomClient(realm, false);
            try (Keycloak client = createClient(CUSTOM_CLIENT_ID)) {
                assertThrows(NotAuthorizedException.class, () -> client.realm(REALM_NAME).toRepresentation(),
                        "Token without the kc-admin-api audience must be rejected by the Admin API");
            }
        });
    }

    @Test
    public void testCustomClientWithScopeIsAccepted() {
        withRealm((realm) -> {
            createCustomClient(realm, false);
            addAdminApiClientScope(realm, CUSTOM_CLIENT_ID);
            try (Keycloak client = createClient(CUSTOM_CLIENT_ID)) {
                assertEquals(REALM_NAME, client.realm(REALM_NAME).toRepresentation().getRealm());
            }
        });
    }

    @Test
    public void testCustomClientWithScopeAndLightweightTokenIsAccepted() {
        withRealm((realm) -> {
            createCustomClient(realm, true);
            addAdminApiClientScope(realm, CUSTOM_CLIENT_ID);
            try (Keycloak client = createClient(CUSTOM_CLIENT_ID)) {
                assertEquals(REALM_NAME, client.realm(REALM_NAME).toRepresentation().getRealm());
            }
        });
    }

    @Test
    public void testCustomClientWithOptionalScopeRequiresRequestingIt() {
        withRealm((realm) -> {
            createCustomClient(realm, false);
            addAdminApiClientScope(realm, CUSTOM_CLIENT_ID, false);

            try (Keycloak client = createClient(CUSTOM_CLIENT_ID, null)) {
                assertThrows(NotAuthorizedException.class, () -> client.realm(REALM_NAME).toRepresentation(),
                        "Token without the requested kc-admin-api optional scope must be rejected by the Admin API");
            }

            try (Keycloak client = createClient(CUSTOM_CLIENT_ID, Constants.KEYCLOAK_ADMIN_API_SCOPE)) {
                assertEquals(REALM_NAME, client.realm(REALM_NAME).toRepresentation().getRealm());
            }
        });
    }

    @Test
    public void testTokenWithAudienceButWithoutScopeIsRejected() {
        withRealm((realm) -> {
            createCustomClientWithAudienceMapper(realm);
            try (Keycloak client = createClient(CUSTOM_CLIENT_ID)) {
                assertThrows(NotAuthorizedException.class, () -> client.realm(REALM_NAME).toRepresentation(),
                        "Token carrying the audience but lacking the kc-admin-api scope must be rejected by the Admin API");
            }
        });
    }

    @Test
    public void testTokenWithScopeButWithoutAudienceIsRejected() {
        withRealm((realm) -> assertScopeWithoutValidAudienceIsRejected(realm, false, false));
    }

    @Test
    public void testTokenWithScopeButWithoutAudienceIsRejectedWithLightweightToken() {
        withRealm((realm) -> assertScopeWithoutValidAudienceIsRejected(realm, true, false));
    }

    @Test
    public void testTokenWithScopeButWrongAudienceIsRejected() {
        withRealm((realm) -> assertScopeWithoutValidAudienceIsRejected(realm, false, true));
    }

    private void assertScopeWithoutValidAudienceIsRejected(RealmResource realm, boolean lightweightToken, boolean wrongAudience) {
        createCustomClient(realm, lightweightToken);
        // Drop the audience mapper so the token still carries the kc-admin-api scope but not the Admin API audience.
        removeAudienceMapperFromAdminApiScope(realm);
        addAdminApiClientScope(realm, CUSTOM_CLIENT_ID);
        if (wrongAudience) {
            addWrongAudienceMapper(realm);
        }

        AccessTokenResponse tokenResponse = oauth.realm(REALM_NAME).client(CUSTOM_CLIENT_ID)
                .doPasswordGrantRequest(USERNAME, "password");
        AccessToken token = oauth.verifyToken(tokenResponse.getAccessToken());

        assertTrue(TokenUtil.hasScope(token.getScope(), Constants.KEYCLOAK_ADMIN_API_SCOPE),
                "The token must carry the kc-admin-api scope");
        assertFalse(hasAdminApiAudience(token),
                "The token must not carry the Admin API audience");
        if (wrongAudience) {
            assertTrue(Arrays.asList(token.getAudience()).contains(WRONG_AUDIENCE),
                    "The token must carry the forged audience");
        }

        try (Keycloak client = adminClientFactory.create().realm(REALM_NAME)
                .authorization(tokenResponse.getAccessToken()).build()) {
            assertThrows(NotAuthorizedException.class, () -> client.realm(REALM_NAME).toRepresentation(),
                    "Token carrying the kc-admin-api scope but lacking the Admin API audience must be rejected by the Admin API");
        }
    }

    @Test
    public void testRefreshedTokenKeepsAudienceAndScope() {
        withRealm((realm) -> {
            createCustomClient(realm, false);
            addAdminApiClientScope(realm, CUSTOM_CLIENT_ID);

            AccessTokenResponse tokenResponse = oauth.realm(REALM_NAME).client(CUSTOM_CLIENT_ID)
                    .doPasswordGrantRequest(USERNAME, "password");
            AccessToken initialToken = oauth.verifyToken(tokenResponse.getAccessToken());
            assertTrue(TokenUtil.hasScope(initialToken.getScope(), Constants.KEYCLOAK_ADMIN_API_SCOPE),
                    "The initial token must carry the kc-admin-api scope");
            assertTrue(initialToken.getAudience() != null && initialToken.getAudience().length > 0,
                    "The initial token must carry the Admin API audience");

            AccessTokenResponse refreshResponse = oauth.realm(REALM_NAME).client(CUSTOM_CLIENT_ID)
                    .doRefreshTokenRequest(tokenResponse.getRefreshToken());
            AccessToken refreshedToken = oauth.verifyToken(refreshResponse.getAccessToken());

            assertTrue(TokenUtil.hasScope(refreshedToken.getScope(), Constants.KEYCLOAK_ADMIN_API_SCOPE),
                    "The refreshed token must keep the kc-admin-api scope");
            assertEquals(Set.of(initialToken.getAudience()), Set.of(refreshedToken.getAudience()),
                    "The refreshed token must keep the same audience");

            try (Keycloak client = adminClientFactory.create().realm(REALM_NAME)
                    .authorization(refreshResponse.getAccessToken()).build()) {
                assertEquals(REALM_NAME, client.realm(REALM_NAME).toRepresentation().getRealm(),
                        "The refreshed token must still be accepted by the Admin API");
            }
        });
    }

    private void withRealm(java.util.function.Consumer<RealmResource> test) {
        RealmResource realm = createRealm(adminClient, REALM_NAME);
        try {
            createUser(realm, USERNAME);
            grantRealmManagementRole(realm, USERNAME, AdminRoles.VIEW_REALM);
            test.accept(realm);
        } finally {
            realm.remove();
        }
    }

    private void createCustomClient(RealmResource realm, boolean lightweightToken) {
        ClientRepresentation client = new ClientRepresentation();
        client.setClientId(CUSTOM_CLIENT_ID);
        client.setPublicClient(true);
        client.setDirectAccessGrantsEnabled(true);
        client.setStandardFlowEnabled(false);
        client.setFullScopeAllowed(true);
        if (lightweightToken) {
            client.setAttributes(Map.of(Constants.USE_LIGHTWEIGHT_ACCESS_TOKEN_ENABLED, "true"));
        }
        try (Response response = realm.clients().create(client)) {
            assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus());
        }
    }

    private void createCustomClientWithAudienceMapper(RealmResource realm) {
        ClientRepresentation client = new ClientRepresentation();
        client.setClientId(CUSTOM_CLIENT_ID);
        client.setPublicClient(true);
        client.setDirectAccessGrantsEnabled(true);
        client.setStandardFlowEnabled(false);
        client.setFullScopeAllowed(true);

        ProtocolMapperRepresentation mapper = new ProtocolMapperRepresentation();
        mapper.setName("forged-admin-api-audience");
        mapper.setProtocol("openid-connect");
        mapper.setProtocolMapper("oidc-admin-api-audience-mapper");
        mapper.setConfig(Map.of(
                "access.token.claim", "true",
                "introspection.token.claim", "true",
                "lightweight.claim", "true"));
        client.setProtocolMappers(List.of(mapper));

        try (Response response = realm.clients().create(client)) {
            assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus());
        }
    }

    private void removeAudienceMapperFromAdminApiScope(RealmResource realm) {
        ClientScopeResource scope = realm.clientScopes().get(getAdminApiClientScopeId(realm));
        scope.getProtocolMappers().getMappers().stream()
                .filter(mapper -> "oidc-admin-api-audience-mapper".equals(mapper.getProtocolMapper()))
                .forEach(mapper -> scope.getProtocolMappers().delete(mapper.getId()));
    }

    private void addWrongAudienceMapper(RealmResource realm) {
        ClientResource client = realm.clients().get(findClientByClientId(realm));

        ProtocolMapperRepresentation mapper = new ProtocolMapperRepresentation();
        mapper.setName("wrong-audience");
        mapper.setProtocol("openid-connect");
        mapper.setProtocolMapper("oidc-audience-mapper");
        mapper.setConfig(Map.of(
                "included.custom.audience", WRONG_AUDIENCE,
                "access.token.claim", "true",
                "introspection.token.claim", "true",
                "lightweight.claim", "true"));

        try (Response response = client.getProtocolMappers().createMapper(mapper)) {
            assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus());
        }
    }

    private String findClientByClientId(RealmResource realm) {
        return realm.clients().findByClientId(CUSTOM_CLIENT_ID).get(0).getId();
    }

    private boolean hasAdminApiAudience(AccessToken token) {
        String[] audience = token.getAudience();
        if (audience == null) {
            return false;
        }
        return Arrays.stream(audience)
                .anyMatch(aud -> aud != null && aud.endsWith("/admin/realms/" + REALM_NAME));
    }

    private Keycloak createClient(String clientId) {
        return createClient(clientId, null);
    }

    private Keycloak createClient(String clientId, String scope) {
        AdminClientBuilder builder = adminClientFactory.create()
                .realm(REALM_NAME)
                .clientId(clientId)
                .username(USERNAME)
                .password("password");
        if (scope != null) {
            builder.scope(scope);
        }
        return builder.build();
    }
}

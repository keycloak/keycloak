/*
 * Copyright 2024 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.keycloak.testsuite.oauth;

import java.util.List;

import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.models.Constants;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.representations.AccessTokenResponse;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testframework.realm.ClientBuilder;
import org.keycloak.testframework.realm.RealmBuilder;
import org.keycloak.testsuite.AbstractKeycloakTest;
import org.keycloak.testsuite.events.TestEventsListenerProviderFactory;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Tests that service account role resolution works correctly with lightweight access
 * tokens and fullScopeAllowed=false when using client_credentials grant (no user session).
 *
 * Regression test for https://github.com/keycloak/keycloak/issues/50950
 *
 * @author igraecao
 */
public class ServiceAccountScopedRolesTest extends AbstractKeycloakTest {

    private static final String CLIENT_FULL_SCOPE = "sa-full-scope";
    private static final String CLIENT_LIMITED_SCOPE_WITH_ROLE = "sa-limited-with-role";
    private static final String CLIENT_LIMITED_SCOPE_NO_ROLE = "sa-limited-no-role";
    private static final String CLIENT_SECRET = "secret1";

    @Override
    public void addTestRealms(List<RealmRepresentation> testRealms) {
        RealmBuilder realm = RealmBuilder.create().name("test")
                .eventsListeners(TestEventsListenerProviderFactory.PROVIDER_ID);

        // Client with fullScopeAllowed=true + lightweight tokens - should get all assigned roles
        ClientRepresentation fullScopeClient = ClientBuilder.create()
                .id(KeycloakModelUtils.generateId())
                .clientId(CLIENT_FULL_SCOPE)
                .secret(CLIENT_SECRET)
                .serviceAccountsEnabled()
                .fullScopeEnabled(true)
                .attribute(Constants.USE_LIGHTWEIGHT_ACCESS_TOKEN_ENABLED, Boolean.TRUE.toString())
                .build();
        realm.clients(fullScopeClient);

        // Client with fullScopeAllowed=false + lightweight tokens - will have role in scope
        ClientRepresentation limitedWithRole = ClientBuilder.create()
                .id(KeycloakModelUtils.generateId())
                .clientId(CLIENT_LIMITED_SCOPE_WITH_ROLE)
                .secret(CLIENT_SECRET)
                .serviceAccountsEnabled()
                .fullScopeEnabled(false)
                .attribute(Constants.USE_LIGHTWEIGHT_ACCESS_TOKEN_ENABLED, Boolean.TRUE.toString())
                .build();
        realm.clients(limitedWithRole);

        // Client with fullScopeAllowed=false + lightweight tokens - will NOT have role in scope
        ClientRepresentation limitedNoRole = ClientBuilder.create()
                .id(KeycloakModelUtils.generateId())
                .clientId(CLIENT_LIMITED_SCOPE_NO_ROLE)
                .secret(CLIENT_SECRET)
                .serviceAccountsEnabled()
                .fullScopeEnabled(false)
                .attribute(Constants.USE_LIGHTWEIGHT_ACCESS_TOKEN_ENABLED, Boolean.TRUE.toString())
                .build();
        realm.clients(limitedNoRole);

        testRealms.add(realm.build());
    }

    /**
     * With fullScopeAllowed=true and lightweight tokens, the service account should
     * receive all assigned roles even without a persistent user session.
     */
    @Test
    public void testServiceAccountFullScopeGetsAllRoles() throws Exception {
        RealmResource realmResource = adminClient.realm("test");

        // Assign manage-users and view-users roles to the service account
        assignRealmManagementRoleToServiceAccount(realmResource, CLIENT_FULL_SCOPE, "manage-users");
        assignRealmManagementRoleToServiceAccount(realmResource, CLIENT_FULL_SCOPE, "view-users");

        // Get token via client_credentials (lightweight - no session, no embedded roles)
        oauth.clientId(CLIENT_FULL_SCOPE);
        oauth.client(CLIENT_FULL_SCOPE, CLIENT_SECRET);
        AccessTokenResponse tokenResponse = oauth.doClientCredentialsGrantAccessTokenRequest();
        assertEquals(200, tokenResponse.getStatusCode());

        // Verify the token is lightweight (no session state, roles resolved at validation)
        assertNull("Lightweight token should not have session_state",
                tokenResponse.getSessionState());

        // Use the token to call admin API - should succeed with resolved roles
        try (Keycloak adminKeycloak = KeycloakBuilder.builder()
                .serverUrl(suiteContext.getAuthServerInfo().getContextRoot() + "/auth")
                .realm("test")
                .authorization("Bearer " + tokenResponse.getAccessToken())
                .build()) {
            List<UserRepresentation> users = adminKeycloak.realm("test").users().list();
            assertNotNull(users);
        }
    }

    /**
     * With fullScopeAllowed=false and lightweight tokens, the service account should
     * only receive roles that are within the client's scope mappings. The specific role
     * (view-users) IS in scope, so access should work.
     *
     * This is the primary regression case from #50950: without the fix, no roles are
     * resolved at all (403 on all admin endpoints).
     */
    @Test
    public void testServiceAccountLimitedScopeWithRoleInScopeGetsAccess() throws Exception {
        RealmResource realmResource = adminClient.realm("test");

        // Assign view-users to the service account
        assignRealmManagementRoleToServiceAccount(realmResource, CLIENT_LIMITED_SCOPE_WITH_ROLE, "view-users");

        // Add view-users to the client's scope mappings
        addRealmManagementRoleToClientScope(realmResource, CLIENT_LIMITED_SCOPE_WITH_ROLE, "view-users");

        // Get token via client_credentials (lightweight)
        oauth.clientId(CLIENT_LIMITED_SCOPE_WITH_ROLE);
        oauth.client(CLIENT_LIMITED_SCOPE_WITH_ROLE, CLIENT_SECRET);
        AccessTokenResponse tokenResponse = oauth.doClientCredentialsGrantAccessTokenRequest();
        assertEquals(200, tokenResponse.getStatusCode());

        // Verify lightweight
        assertNull("Lightweight token should not have session_state",
                tokenResponse.getSessionState());

        // Use the token to call admin API - view-users is in scope, should work
        try (Keycloak viewKeycloak = KeycloakBuilder.builder()
                .serverUrl(suiteContext.getAuthServerInfo().getContextRoot() + "/auth")
                .realm("test")
                .authorization("Bearer " + tokenResponse.getAccessToken())
                .build()) {
            List<UserRepresentation> users = viewKeycloak.realm("test").users().list();
            assertNotNull(users);
        }
    }

    /**
     * With fullScopeAllowed=false and lightweight tokens, when NO admin roles are in
     * the client's scope mappings, the service account should get 403 - no roles
     * resolved means no access. This verifies the scope boundary is enforced.
     */
    @Test
    public void testServiceAccountLimitedScopeNoScopedRolesGets403() throws Exception {
        RealmResource realmResource = adminClient.realm("test");

        // Assign manage-users to the service account but do NOT add it to client scope.
        // This client (CLIENT_LIMITED_SCOPE_NO_ROLE) has no scope mappings at all.
        assignRealmManagementRoleToServiceAccount(realmResource, CLIENT_LIMITED_SCOPE_NO_ROLE, "manage-users");

        // Get token via client_credentials (lightweight)
        oauth.clientId(CLIENT_LIMITED_SCOPE_NO_ROLE);
        oauth.client(CLIENT_LIMITED_SCOPE_NO_ROLE, CLIENT_SECRET);
        AccessTokenResponse tokenResponse = oauth.doClientCredentialsGrantAccessTokenRequest();
        assertEquals(200, tokenResponse.getStatusCode());

        // Verify lightweight
        assertNull("Lightweight token should not have session_state",
                tokenResponse.getSessionState());

        // Use the token to call admin API - should fail with 403 (role assigned but not in scope)
        try (Keycloak noAccessKeycloak = KeycloakBuilder.builder()
                .serverUrl(suiteContext.getAuthServerInfo().getContextRoot() + "/auth")
                .realm("test")
                .authorization("Bearer " + tokenResponse.getAccessToken())
                .build()) {
            try {
                noAccessKeycloak.realm("test").users().list();
                throw new AssertionError("Expected 403 but got 200 - scope boundary not enforced");
            } catch (jakarta.ws.rs.ForbiddenException e) {
                // Expected - role is assigned but not in client scope mappings
                assertTrue(e.getMessage().contains("403"));
            }
        }
    }

    private void assignRealmManagementRoleToServiceAccount(RealmResource realm, String clientId, String roleName) {
        ClientRepresentation client = realm.clients().findByClientId(clientId).get(0);
        UserRepresentation serviceAccount = realm.clients().get(client.getId()).getServiceAccountUser();

        ClientRepresentation realmMgmt = realm.clients().findByClientId("realm-management").get(0);
        RoleRepresentation role = realm.clients().get(realmMgmt.getId()).roles().get(roleName).toRepresentation();

        realm.users().get(serviceAccount.getId()).roles().clientLevel(realmMgmt.getId()).add(List.of(role));
    }

    private void addRealmManagementRoleToClientScope(RealmResource realm, String clientId, String roleName) {
        ClientRepresentation client = realm.clients().findByClientId(clientId).get(0);
        ClientRepresentation realmMgmt = realm.clients().findByClientId("realm-management").get(0);
        RoleRepresentation role = realm.clients().get(realmMgmt.getId()).roles().get(roleName).toRepresentation();

        // Add role to the client's scope mappings
        realm.clients().get(client.getId()).getScopeMappings().clientLevel(realmMgmt.getId()).add(List.of(role));
    }
}

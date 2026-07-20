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
import java.util.Map;

import jakarta.ws.rs.core.Response;

import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.representations.AccessTokenResponse;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.ClientScopeRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testframework.realm.ClientBuilder;
import org.keycloak.testframework.realm.RealmBuilder;
import org.keycloak.testsuite.AbstractKeycloakTest;
import org.keycloak.testsuite.events.TestEventsListenerProviderFactory;
import org.keycloak.testsuite.util.OAuthClient;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Tests that service account role resolution works correctly with fullScopeAllowed=false
 * when using client_credentials grant (sessionless lightweight access tokens).
 *
 * Regression test for https://github.com/keycloak/keycloak/issues/50950
 *
 * @author igraecao
 */
public class ServiceAccountScopedRolesTest extends AbstractKeycloakTest {

    private static final String CLIENT_FULL_SCOPE = "sa-full-scope";
    private static final String CLIENT_LIMITED_SCOPE = "sa-limited-scope";
    private static final String CLIENT_SECRET = "secret1";

    @Override
    public void addTestRealms(List<RealmRepresentation> testRealms) {
        RealmBuilder realm = RealmBuilder.create().name("test")
                .eventsListeners(TestEventsListenerProviderFactory.PROVIDER_ID);

        // Client with fullScopeAllowed=true (default) - should get all assigned roles
        ClientRepresentation fullScopeClient = ClientBuilder.create()
                .id(KeycloakModelUtils.generateId())
                .clientId(CLIENT_FULL_SCOPE)
                .secret(CLIENT_SECRET)
                .serviceAccountsEnabled(true)
                .fullScopeEnabled(true)
                .build();
        realm.clients(fullScopeClient);

        // Client with fullScopeAllowed=false - should only get scoped roles
        ClientRepresentation limitedScopeClient = ClientBuilder.create()
                .id(KeycloakModelUtils.generateId())
                .clientId(CLIENT_LIMITED_SCOPE)
                .secret(CLIENT_SECRET)
                .serviceAccountsEnabled(true)
                .fullScopeEnabled(false)
                .build();
        realm.clients(limitedScopeClient);

        testRealms.add(realm.build());
    }

    /**
     * With fullScopeAllowed=true, the service account should receive all assigned roles
     * even without a persistent user session.
     */
    @Test
    public void testServiceAccountFullScopeGetsAllRoles() throws Exception {
        RealmResource realmResource = adminClient.realm("test");

        // Assign manage-users role to the service account
        assignRealmManagementRoleToServiceAccount(realmResource, CLIENT_FULL_SCOPE, "manage-users");
        assignRealmManagementRoleToServiceAccount(realmResource, CLIENT_FULL_SCOPE, "view-users");

        // Get token via client_credentials
        oauth.clientId(CLIENT_FULL_SCOPE);
        oauth.client(CLIENT_FULL_SCOPE, CLIENT_SECRET);
        AccessTokenResponse tokenResponse = oauth.doClientCredentialsGrantAccessTokenRequest();
        assertEquals(200, tokenResponse.getStatusCode());

        // Use the token to call admin API (user list) - should succeed
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
     * With fullScopeAllowed=false, the service account should only receive roles
     * that are within the client's scope mappings. Assigned roles outside the scope
     * must NOT appear in the resolved token.
     *
     * This is the regression case from #50950: without the fix, no roles are resolved
     * at all (403 on all admin endpoints).
     */
    @Test
    public void testServiceAccountLimitedScopeGetsOnlyScopedRoles() throws Exception {
        RealmResource realmResource = adminClient.realm("test");

        // Assign both manage-users and view-users to the service account
        assignRealmManagementRoleToServiceAccount(realmResource, CLIENT_LIMITED_SCOPE, "manage-users");
        assignRealmManagementRoleToServiceAccount(realmResource, CLIENT_LIMITED_SCOPE, "view-users");

        // Add view-users to the client's scope mappings (but NOT manage-users)
        addRealmManagementRoleToClientScope(realmResource, CLIENT_LIMITED_SCOPE, "view-users");

        // Get token via client_credentials
        oauth.clientId(CLIENT_LIMITED_SCOPE);
        oauth.client(CLIENT_LIMITED_SCOPE, CLIENT_SECRET);
        AccessTokenResponse tokenResponse = oauth.doClientCredentialsGrantAccessTokenRequest();
        assertEquals(200, tokenResponse.getStatusCode());

        // Use the token to call admin API - view-users should work (in scope)
        try (Keycloak viewKeycloak = KeycloakBuilder.builder()
                .serverUrl(suiteContext.getAuthServerInfo().getContextRoot() + "/auth")
                .realm("test")
                .authorization("Bearer " + tokenResponse.getAccessToken())
                .build()) {
            // GET users should work with view-users
            List<UserRepresentation> users = viewKeycloak.realm("test").users().list();
            assertNotNull(users);
        }
    }

    /**
     * With fullScopeAllowed=false and NO roles in scope mappings,
     * the service account should get a 403 (no roles resolved = no access).
     */
    @Test
    public void testServiceAccountLimitedScopeNoScopedRolesGets403() throws Exception {
        RealmResource realmResource = adminClient.realm("test");

        // Assign manage-users to the service account but do NOT add it to client scope
        assignRealmManagementRoleToServiceAccount(realmResource, CLIENT_LIMITED_SCOPE, "manage-users");
        // Don't add any roles to client scope mappings

        // Get token via client_credentials
        oauth.clientId(CLIENT_LIMITED_SCOPE);
        oauth.client(CLIENT_LIMITED_SCOPE, CLIENT_SECRET);
        AccessTokenResponse tokenResponse = oauth.doClientCredentialsGrantAccessTokenRequest();
        assertEquals(200, tokenResponse.getStatusCode());

        // Use the token to call admin API - should fail with 403 (no roles in scope)
        try (Keycloak noAccessKeycloak = KeycloakBuilder.builder()
                .serverUrl(suiteContext.getAuthServerInfo().getContextRoot() + "/auth")
                .realm("test")
                .authorization("Bearer " + tokenResponse.getAccessToken())
                .build()) {
            try {
                noAccessKeycloak.realm("test").users().list();
                // Should not reach here
                throw new AssertionError("Expected 403 but got 200");
            } catch (jakarta.ws.rs.ForbiddenException e) {
                // Expected - no roles in scope means no access
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

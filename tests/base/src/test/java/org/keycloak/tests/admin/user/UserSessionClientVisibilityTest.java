/*
 * Copyright 2026 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.tests.admin.user;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.BearerAuthFilter;
import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.admin.client.resource.ScopePermissionsResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.authorization.fgap.AdminPermissionsSchema;
import org.keycloak.models.AdminRoles;
import org.keycloak.models.Constants;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.representations.idm.UserSessionRepresentation;
import org.keycloak.representations.idm.authorization.ScopePermissionRepresentation;
import org.keycloak.representations.idm.authorization.UserPolicyRepresentation;
import org.keycloak.testframework.annotations.InjectAdminClient;
import org.keycloak.testframework.annotations.InjectKeycloakUrls;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.oauth.OAuthClient;
import org.keycloak.testframework.oauth.annotations.InjectOAuthClient;
import org.keycloak.testframework.realm.ClientBuilder;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.RealmBuilder;
import org.keycloak.testframework.realm.RealmConfig;
import org.keycloak.testframework.realm.UserBuilder;
import org.keycloak.testframework.server.KeycloakUrls;
import org.keycloak.tests.admin.authz.fgap.PermissionTestUtils;
import org.keycloak.tests.utils.admin.AdminApiUtil;
import org.keycloak.testsuite.util.oauth.AccessTokenResponse;
import org.keycloak.util.JsonSerialization;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Viewing a user's sessions and consents only requires permission to view the user, so the clients those responses
 * reveal must be filtered by the caller's per-client view permission.
 */
@KeycloakIntegrationTest
public class UserSessionClientVisibilityTest {

    private static final String TARGET_USER = "session-owner";
    private static final String VISIBLE_CLIENT = "visible-app";
    private static final String HIDDEN_CLIENT = "hidden-app";
    private static final String CLIENT_SECRET = "secret";

    @InjectRealm(config = SessionVisibilityRealmConfig.class)
    ManagedRealm managedRealm;

    @InjectOAuthClient
    OAuthClient oauth;

    @InjectAdminClient(ref = "fgapViewer", mode = InjectAdminClient.Mode.MANAGED_REALM, client = "myclient", user = "fgap-viewer")
    Keycloak fgapViewer;

    @InjectAdminClient(ref = "fullViewer", mode = InjectAdminClient.Mode.MANAGED_REALM, client = "myclient", user = "full-viewer")
    Keycloak fullViewer;

    @InjectKeycloakUrls
    KeycloakUrls keycloakUrls;

    private String targetUserId;
    private String visibleClientUuid;
    private String hiddenClientUuid;

    @BeforeEach
    public void createSessions() {
        targetUserId = managedRealm.admin().users().search(TARGET_USER, true).get(0).getId();
        visibleClientUuid = AdminApiUtil.findClientByClientId(managedRealm.admin(), VISIBLE_CLIENT).toRepresentation().getId();
        hiddenClientUuid = AdminApiUtil.findClientByClientId(managedRealm.admin(), HIDDEN_CLIENT).toRepresentation().getId();

        removeSessions(managedRealm.admin().users().get(targetUserId));
        managedRealm.cleanup().add(r -> removeSessions(r.users().get(targetUserId)));

        // one regular and one offline session per client
        for (String clientId : List.of(VISIBLE_CLIENT, HIDDEN_CLIENT)) {
            for (String scope : new String[] { null, OAuth2Constants.OFFLINE_ACCESS }) {
                AccessTokenResponse response = oauth.client(clientId, CLIENT_SECRET).scope(scope)
                        .doPasswordGrantRequest(TARGET_USER, "password");
                assertEquals(200, response.getStatusCode(), response.getErrorDescription());
            }
        }

        grantViewClient("fgap-viewer", VISIBLE_CLIENT);
    }

    private static void removeSessions(UserResource user) {
        user.logout();
        // logout leaves offline sessions in place
        for (String clientId : List.of(VISIBLE_CLIENT, HIDDEN_CLIENT)) {
            try {
                user.revokeConsent(clientId);
            } catch (NotFoundException ignored) {
                // no offline token for this client
            }
        }
    }

    @Test
    public void sessionsHideClientsWithoutViewPermission() {
        Set<String> clients = sessionClients(fgapViewer.realm(managedRealm.getName()).users().get(targetUserId).getUserSessions());

        assertTrue(clients.contains(visibleClientUuid));
        assertFalse(clients.contains(hiddenClientUuid), "sessions must not reveal clients the admin cannot view");
    }

    @Test
    public void sessionsShowAllClientsWithViewClients() {
        Set<String> clients = sessionClients(fullViewer.realm(managedRealm.getName()).users().get(targetUserId).getUserSessions());

        assertTrue(clients.containsAll(Set.of(visibleClientUuid, hiddenClientUuid)));
    }

    @Test
    public void offlineSessionsRequireClientViewPermission() {
        UserResource user = fgapViewer.realm(managedRealm.getName()).users().get(targetUserId);

        List<UserSessionRepresentation> visible = user.getOfflineSessions(visibleClientUuid);
        assertEquals(1, visible.size());
        assertEquals(Set.of(visibleClientUuid), visible.get(0).getClients().keySet());

        assertThrows(ForbiddenException.class, () -> user.getOfflineSessions(hiddenClientUuid));
    }

    @Test
    public void offlineSessionsShowAllClientsWithViewClients() {
        UserResource user = fullViewer.realm(managedRealm.getName()).users().get(targetUserId);

        assertEquals(1, user.getOfflineSessions(visibleClientUuid).size());
        assertEquals(1, user.getOfflineSessions(hiddenClientUuid).size());
    }

    @Test
    public void consentsHideClientsWithoutViewPermission() {
        List<Map<String, Object>> consents = fgapViewer.realm(managedRealm.getName()).users().get(targetUserId).getConsents();

        assertEquals(Set.of(VISIBLE_CLIENT), consentClientIds(consents));
        assertEquals(visibleClientUuid, offlineTokenClient(consents.get(0)));
    }

    @Test
    public void consentsShowAllClientsWithViewClients() {
        List<Map<String, Object>> consents = fullViewer.realm(managedRealm.getName()).users().get(targetUserId).getConsents();

        assertEquals(Set.of(VISIBLE_CLIENT, HIDDEN_CLIENT), consentClientIds(consents));
    }

    @Test
    public void uiExtSessionsHideClientsWithoutViewPermission() throws IOException {
        Set<String> clients = uiExtSessionClients(fgapViewer);

        assertTrue(clients.contains(visibleClientUuid));
        assertFalse(clients.contains(hiddenClientUuid), "ui-ext sessions must not reveal clients the admin cannot view");
    }

    @Test
    public void uiExtSessionsShowAllClientsWithViewClients() throws IOException {
        Set<String> clients = uiExtSessionClients(fullViewer);

        assertTrue(clients.containsAll(Set.of(visibleClientUuid, hiddenClientUuid)));
    }

    private static Set<String> sessionClients(List<UserSessionRepresentation> sessions) {
        assertFalse(sessions.isEmpty());
        return sessions.stream()
                .flatMap(s -> s.getClients().keySet().stream())
                .collect(Collectors.toSet());
    }

    private static Set<String> consentClientIds(List<Map<String, Object>> consents) {
        return consents.stream()
                .map(consent -> (String) consent.get("clientId"))
                .collect(Collectors.toSet());
    }

    @SuppressWarnings("unchecked")
    private static String offlineTokenClient(Map<String, Object> consent) {
        List<Map<String, String>> grants = (List<Map<String, String>>) consent.get("additionalGrants");
        assertEquals(1, grants.size());
        return grants.get(0).get("client");
    }

    private Set<String> uiExtSessionClients(Keycloak admin) throws IOException {
        Client httpClient = Keycloak.getClientProvider().newRestEasyClient(null, null, true);
        try (Response response = httpClient.target(keycloakUrls.getBaseUrl().toString())
                .path("admin").path("realms").path(managedRealm.getName())
                .path("ui-ext").path("sessions")
                .queryParam("max", 100)
                .register(new BearerAuthFilter(admin.tokenManager()))
                .request(MediaType.APPLICATION_JSON)
                .get()) {
            assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
            JsonNode sessions = JsonSerialization.readValue(response.readEntity(String.class), JsonNode.class);

            Set<String> clients = new HashSet<>();
            for (JsonNode session : sessions) {
                if (targetUserId.equals(session.get("userId").asText())) {
                    session.get("clients").fieldNames().forEachRemaining(clients::add);
                }
            }
            return clients;
        } finally {
            httpClient.close();
        }
    }

    private void grantViewClient(String username, String clientId) {
        ClientResource permissionsClient = AdminApiUtil.findClientByClientId(managedRealm.admin(), Constants.ADMIN_PERMISSIONS_CLIENT_ID);
        String userId = managedRealm.admin().users().search(username, true).get(0).getId();
        String clientUuid = AdminApiUtil.findClientByClientId(managedRealm.admin(), clientId).toRepresentation().getId();

        UserPolicyRepresentation policy = PermissionTestUtils.createUserPolicy(managedRealm, permissionsClient,
                KeycloakModelUtils.generateId(), userId);
        ScopePermissionRepresentation permission = PermissionTestUtils.createPermission(permissionsClient, clientUuid,
                AdminPermissionsSchema.CLIENTS_RESOURCE_TYPE, Set.of(AdminPermissionsSchema.VIEW), policy);
        // removing the policy may already have removed the permission
        managedRealm.cleanup().add(r -> {
            ScopePermissionsResource permissions = r.clients().get(permissionsClient.toRepresentation().getId())
                    .authorization().permissions().scope();
            if (permissions.findByName(permission.getName()) != null) {
                permissions.findById(permission.getId()).remove();
            }
        });
    }

    public static class SessionVisibilityRealmConfig implements RealmConfig {

        @Override
        public RealmBuilder configure(RealmBuilder realm) {
            realm.adminPermissionsEnabled(true);

            realm.clients(ClientBuilder.create("myclient")
                            .secret("mysecret")
                            .directAccessGrantsEnabled(true),
                    ClientBuilder.create(VISIBLE_CLIENT)
                            .secret(CLIENT_SECRET)
                            .directAccessGrantsEnabled(true),
                    ClientBuilder.create(HIDDEN_CLIENT)
                            .secret(CLIENT_SECRET)
                            .directAccessGrantsEnabled(true));

            realm.users(UserBuilder.create(TARGET_USER)
                            .password("password")
                            .name("Session", "Owner")
                            .email("session-owner@localhost")
                            .emailVerified(true)
                            .realmRoles(OAuth2Constants.OFFLINE_ACCESS),
                    UserBuilder.create("fgap-viewer")
                            .password("password")
                            .name("Fgap", "Viewer")
                            .email("fgap-viewer@localhost")
                            .emailVerified(true)
                            .clientRoles(Constants.REALM_MANAGEMENT_CLIENT_ID, AdminRoles.VIEW_REALM, AdminRoles.VIEW_USERS),
                    UserBuilder.create("full-viewer")
                            .password("password")
                            .name("Full", "Viewer")
                            .email("full-viewer@localhost")
                            .emailVerified(true)
                            .clientRoles(Constants.REALM_MANAGEMENT_CLIENT_ID, AdminRoles.VIEW_REALM, AdminRoles.VIEW_USERS,
                                    AdminRoles.VIEW_CLIENTS));

            return realm;
        }
    }
}

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
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.MediaType;

import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.BearerAuthFilter;
import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.admin.client.resource.ScopePermissionsResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.authorization.fgap.AdminPermissionsSchema;
import org.keycloak.models.AdminRoles;
import org.keycloak.models.ClientModel;
import org.keycloak.models.Constants;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserConsentModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.representations.idm.UserSessionRepresentation;
import org.keycloak.representations.idm.authorization.ScopePermissionRepresentation;
import org.keycloak.representations.idm.authorization.UserPolicyRepresentation;
import org.keycloak.services.managers.UserConsentManager;
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
import org.keycloak.testframework.remote.runonserver.InjectRunOnServer;
import org.keycloak.testframework.remote.runonserver.RunOnServerClient;
import org.keycloak.testframework.server.KeycloakUrls;
import org.keycloak.testframework.util.ApiUtil;
import org.keycloak.tests.admin.authz.fgap.PermissionTestUtils;
import org.keycloak.tests.utils.admin.AdminApiUtil;
import org.keycloak.testsuite.util.oauth.AccessTokenResponse;
import org.keycloak.testsuite.util.oauth.AuthorizationEndpointResponse;
import org.keycloak.util.JsonSerialization;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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
    private static final String REMOVED_CLIENT = "removed-app";
    private static final String CLIENT_SECRET = "secret";
    // served by the test framework's callback app, and registered on both clients in the realm config
    private static final String OAUTH_CALLBACK_URL = "http://localhost:8500/callback/oauth";

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

    @InjectRunOnServer
    RunOnServerClient runOnServer;

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

        // a single SSO session shared by the visible and the hidden client, then offline tokens for both from it, so the
        // regular and the offline session each contain both clients
        oauth.redirectUri(OAUTH_CALLBACK_URL);
        oauth.scope(null);
        oauth.client(VISIBLE_CLIENT, CLIENT_SECRET);
        exchangeCode(oauth.doLogin(TARGET_USER, "password"));
        joinSsoSession(HIDDEN_CLIENT);
        // a login that only requests offline access does not keep an SSO session, so offline tokens come last
        oauth.scope(OAuth2Constants.OFFLINE_ACCESS);
        joinSsoSession(VISIBLE_CLIENT);
        joinSsoSession(HIDDEN_CLIENT);

        grantView("fgap-viewer", targetUserId, AdminPermissionsSchema.USERS_RESOURCE_TYPE);
        grantView("fgap-viewer", visibleClientUuid, AdminPermissionsSchema.CLIENTS_RESOURCE_TYPE);
    }

    private void joinSsoSession(String clientId) {
        oauth.client(clientId, CLIENT_SECRET);
        oauth.openLoginForm();
        exchangeCode(oauth.parseLoginResponse());
    }

    private void exchangeCode(AuthorizationEndpointResponse authorization) {
        assertTrue(authorization.isSuccess(), authorization.getErrorDescription());
        AccessTokenResponse response = oauth.doAccessTokenRequest(authorization.getCode());
        assertEquals(200, response.getStatusCode(), response.getErrorDescription());
    }

    private void createHiddenOnlySessions() {
        removeSessions(managedRealm.admin().users().get(targetUserId));

        oauth.scope(null);
        oauth.client(HIDDEN_CLIENT, CLIENT_SECRET);
        exchangeCode(oauth.doLogin(TARGET_USER, "password"));

        oauth.scope(OAuth2Constants.OFFLINE_ACCESS);
        joinSsoSession(HIDDEN_CLIENT);
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
    public void delegatedViewerCanOnlyViewGrantedUserAndClient() {
        var realm = fgapViewer.realm(managedRealm.getName());
        String otherUserId = managedRealm.admin().users().search("full-viewer", true).get(0).getId();

        assertEquals(TARGET_USER, realm.users().get(targetUserId).toRepresentation().getUsername());
        assertThrows(ForbiddenException.class, () -> realm.users().get(otherUserId).toRepresentation());
        assertEquals(VISIBLE_CLIENT, realm.clients().get(visibleClientUuid).toRepresentation().getClientId());
        assertThrows(ForbiddenException.class, () -> realm.clients().get(hiddenClientUuid).toRepresentation());
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
    public void sessionsOmitHiddenOnlySession() {
        createHiddenOnlySessions();

        UserResource fullUser = fullViewer.realm(managedRealm.getName()).users().get(targetUserId);
        assertEquals(Set.of(hiddenClientUuid), sessionClients(fullUser.getUserSessions()));

        UserResource delegatedUser = fgapViewer.realm(managedRealm.getName()).users().get(targetUserId);
        assertTrue(delegatedUser.getUserSessions().isEmpty(),
                "a session used only by a hidden client must not reveal its existence");
    }

    @Test
    public void offlineSessionsRequireClientViewPermission() {
        UserResource user = fgapViewer.realm(managedRealm.getName()).users().get(targetUserId);

        List<UserSessionRepresentation> visible = user.getOfflineSessions(visibleClientUuid);
        assertEquals(1, visible.size());
        assertEquals(Set.of(visibleClientUuid), visible.get(0).getClients().keySet());

        assertThrows(ForbiddenException.class, () -> user.getOfflineSessions(hiddenClientUuid));
        // an unknown client must look the same as a hidden one, so client ids cannot be probed
        assertThrows(ForbiddenException.class, () -> user.getOfflineSessions(KeycloakModelUtils.generateId()));
    }

    @Test
    public void offlineSessionsShowAllClientsWithViewClients() {
        UserResource user = fullViewer.realm(managedRealm.getName()).users().get(targetUserId);

        assertEquals(1, user.getOfflineSessions(visibleClientUuid).size());
        assertEquals(1, user.getOfflineSessions(hiddenClientUuid).size());
        assertThrows(NotFoundException.class, () -> user.getOfflineSessions(KeycloakModelUtils.generateId()));
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
    public void persistedConsentsHideClientsWithoutViewPermission() {
        createPersistedConsents();
        List<Map<String, Object>> allConsents = fullViewer.realm(managedRealm.getName()).users().get(targetUserId).getConsents();
        assertEquals(Set.of(VISIBLE_CLIENT, HIDDEN_CLIENT), consentClientIds(allConsents));
        allConsents.forEach(UserSessionClientVisibilityTest::assertConsentMetadata);

        List<Map<String, Object>> visibleConsents = fgapViewer.realm(managedRealm.getName()).users().get(targetUserId).getConsents();

        assertEquals(List.of(allConsents.stream().filter(c -> VISIBLE_CLIENT.equals(c.get("clientId"))).findFirst().orElseThrow()),
                visibleConsents);
        assertEquals(visibleClientUuid, offlineTokenClient(visibleConsents.get(0)));
    }

    @Test
    public void persistedConsentsShowAllClientsWithViewClients() {
        createPersistedConsents();
        List<Map<String, Object>> consents = fullViewer.realm(managedRealm.getName()).users().get(targetUserId).getConsents();

        assertEquals(2, consents.size());
        assertEquals(Set.of(VISIBLE_CLIENT, HIDDEN_CLIENT), consentClientIds(consents));
        for (Map<String, Object> consent : consents) {
            assertConsentMetadata(consent);
            assertEquals(VISIBLE_CLIENT.equals(consent.get("clientId")) ? visibleClientUuid : hiddenClientUuid,
                    offlineTokenClient(consent));
        }
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

    @Test
    public void uiExtRegularSessionsOmitHiddenOnlySession() throws IOException {
        createHiddenOnlySessions();

        JsonNode fullSessions = uiExtSessions(fullViewer, null, "REGULAR", null);
        assertEquals(1, targetSessionCount(fullSessions));
        assertEquals(Set.of(hiddenClientUuid), uiExtSessionClients(fullSessions));
        assertEquals(0, targetSessionCount(uiExtSessions(fgapViewer, null, "REGULAR", null)),
                "a hidden-only regular session must not appear in the realm session list");
    }

    @Test
    public void uiExtOfflineSessionsOmitHiddenOnlySession() throws IOException {
        createHiddenOnlySessions();

        JsonNode fullSessions = uiExtSessions(fullViewer, null, "OFFLINE", null);
        assertEquals(1, targetSessionCount(fullSessions));
        assertEquals(Set.of(hiddenClientUuid), uiExtSessionClients(fullSessions));
        assertEquals(0, targetSessionCount(uiExtSessions(fgapViewer, null, "OFFLINE", null)),
                "a hidden-only offline session must not disclose an inaccessible client's offline grant");
    }

    /**
     * Removing a client leaves the sessions it was used in without clients. Such a session reveals no client, so it
     * must stay listed for every admin that can view the user, rather than disappear along with hidden-only sessions.
     */
    @Test
    public void sessionWithoutClientsStaysListed() throws IOException {
        createSessionWithoutClients();

        for (Keycloak admin : List.of(fullViewer, fgapViewer)) {
            List<UserSessionRepresentation> sessions = admin.realm(managedRealm.getName()).users().get(targetUserId).getUserSessions();
            assertEquals(1, sessions.size());
            assertTrue(sessions.get(0).getClients().isEmpty());

            JsonNode uiExtSessions = uiExtSessions(admin, null, "REGULAR", null);
            assertEquals(1, targetSessionCount(uiExtSessions));
            assertTrue(uiExtSessionClients(uiExtSessions).isEmpty());
        }
    }

    private void createSessionWithoutClients() {
        removeSessions(managedRealm.admin().users().get(targetUserId));

        String clientUuid = ApiUtil.getCreatedId(managedRealm.admin().clients().create(ClientBuilder.create(REMOVED_CLIENT)
                .secret(CLIENT_SECRET)
                .redirectUris(OAUTH_CALLBACK_URL)
                .build()));
        oauth.scope(null);
        oauth.client(REMOVED_CLIENT, CLIENT_SECRET);
        exchangeCode(oauth.doLogin(TARGET_USER, "password"));

        managedRealm.admin().clients().get(clientUuid).remove();
    }

    @Test
    public void uiExtSessionSearchDoesNotRevealHiddenClientUsage() throws IOException {
        for (String type : List.of("REGULAR", "OFFLINE")) {
            for (String clientUuid : new String[] { null, visibleClientUuid }) {
                JsonNode hiddenSearch = uiExtSessions(fgapViewer, clientUuid, type, HIDDEN_CLIENT);
                assertTrue(hiddenSearch.isEmpty(), "search must not match a hidden client in " + type + " sessions");

                JsonNode visibleSearch = uiExtSessions(fgapViewer, clientUuid, type, VISIBLE_CLIENT);
                assertEquals(1, visibleSearch.size());
                assertEquals(Set.of(visibleClientUuid), uiExtSessionClients(visibleSearch));

                JsonNode fullSearch = uiExtSessions(fullViewer, clientUuid, type, HIDDEN_CLIENT);
                assertEquals(1, fullSearch.size());
                assertEquals(Set.of(visibleClientUuid, hiddenClientUuid), uiExtSessionClients(fullSearch));
            }
        }
    }

    @Test
    public void uiExtClientSessionsHideOtherClientsWithoutViewPermission() throws IOException {
        for (String type : List.of("REGULAR", "OFFLINE")) {
            JsonNode sessions = uiExtSessions(fgapViewer, visibleClientUuid, type, null);

            assertEquals(1, sessions.size());
            assertEquals(Set.of(visibleClientUuid), uiExtSessionClients(sessions));
            assertThrows(ForbiddenException.class, () -> uiExtSessions(fgapViewer, hiddenClientUuid, type, null));
        }
    }

    @Test
    public void uiExtClientSessionsShowOtherClientsWithViewClients() throws IOException {
        for (String type : List.of("REGULAR", "OFFLINE")) {
            for (String clientUuid : List.of(visibleClientUuid, hiddenClientUuid)) {
                JsonNode sessions = uiExtSessions(fullViewer, clientUuid, type, null);

                assertEquals(1, sessions.size());
                assertEquals(Set.of(visibleClientUuid, hiddenClientUuid), uiExtSessionClients(sessions));
            }
        }
    }

    @Test
    public void clientSessionsHideOtherClientsWithoutViewPermission() {
        ClientResource client = fgapViewer.realm(managedRealm.getName()).clients().get(visibleClientUuid);

        assertEquals(Set.of(visibleClientUuid), sessionClients(client.getUserSessions(0, 100)),
                "client sessions must not reveal other clients the admin cannot view");
        assertEquals(Set.of(visibleClientUuid), sessionClients(client.getOfflineUserSessions(0, 100)),
                "client offline sessions must not reveal other clients the admin cannot view");
    }

    @Test
    public void clientSessionsShowOtherClientsWithViewClients() {
        ClientResource client = fullViewer.realm(managedRealm.getName()).clients().get(visibleClientUuid);

        assertEquals(Set.of(visibleClientUuid, hiddenClientUuid), sessionClients(client.getUserSessions(0, 100)));
        assertEquals(Set.of(visibleClientUuid, hiddenClientUuid), sessionClients(client.getOfflineUserSessions(0, 100)));
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

    private void createPersistedConsents() {
        runOnServer.run(session -> {
            RealmModel realm = session.getContext().getRealm();
            UserModel user = session.users().getUserByUsername(realm, TARGET_USER);
            for (String clientId : List.of(VISIBLE_CLIENT, HIDDEN_CLIENT)) {
                ClientModel client = realm.getClientByClientId(clientId);
                UserConsentModel consent = new UserConsentModel(client);
                consent.addGrantedClientScope(client.getClientScopes(true).get("profile"));
                UserConsentManager.addConsent(session, realm, user, consent);
            }
        });
    }

    private static void assertConsentMetadata(Map<String, Object> consent) {
        assertEquals(List.of("profile"), consent.get("grantedClientScopes"));
        assertNotNull(consent.get("createdDate"));
        assertNotNull(consent.get("lastUpdatedDate"));
    }

    private Set<String> uiExtSessionClients(Keycloak admin) throws IOException {
        return uiExtSessionClients(uiExtSessions(admin, null, "ALL", null));
    }

    private Set<String> uiExtSessionClients(JsonNode sessions) {
        Set<String> clients = new HashSet<>();
        for (JsonNode session : sessions) {
            if (targetUserId.equals(session.get("userId").asText())) {
                session.get("clients").fieldNames().forEachRemaining(clients::add);
            }
        }
        return clients;
    }

    private int targetSessionCount(JsonNode sessions) {
        int count = 0;
        for (JsonNode session : sessions) {
            if (targetUserId.equals(session.get("userId").asText())) {
                count++;
            }
        }
        return count;
    }

    private JsonNode uiExtSessions(Keycloak admin, String clientUuid, String type, String search) throws IOException {
        Client httpClient = Keycloak.getClientProvider().newRestEasyClient(null, null, true);
        try {
            WebTarget target = httpClient.target(keycloakUrls.getBaseUrl().toString())
                    .path("admin").path("realms").path(managedRealm.getName())
                    .path("ui-ext").path("sessions");
            if (clientUuid != null) {
                target = target.path("client").queryParam("clientId", clientUuid);
            }
            target = target.queryParam("max", 100).queryParam("type", type);
            if (search != null) {
                target = target.queryParam("search", search);
            }
            String response = target.register(new BearerAuthFilter(admin.tokenManager()))
                    .request(MediaType.APPLICATION_JSON).get(String.class);
            JsonNode sessions = JsonSerialization.readValue(response, JsonNode.class);
            assertTrue(sessions.isArray());
            return sessions;
        } finally {
            httpClient.close();
        }
    }

    private void grantView(String username, String resourceId, String resourceType) {
        ClientResource permissionsClient = AdminApiUtil.findClientByClientId(managedRealm.admin(), Constants.ADMIN_PERMISSIONS_CLIENT_ID);
        String userId = managedRealm.admin().users().search(username, true).get(0).getId();

        UserPolicyRepresentation policy = PermissionTestUtils.createUserPolicy(managedRealm, permissionsClient,
                KeycloakModelUtils.generateId(), userId);
        ScopePermissionRepresentation permission = PermissionTestUtils.createPermission(permissionsClient, resourceId,
                resourceType, Set.of(AdminPermissionsSchema.VIEW), policy);
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
                            .redirectUris(OAUTH_CALLBACK_URL),
                    ClientBuilder.create(HIDDEN_CLIENT)
                            .secret(CLIENT_SECRET)
                            .redirectUris(OAUTH_CALLBACK_URL));

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
                            .clientRoles(Constants.REALM_MANAGEMENT_CLIENT_ID, AdminRoles.VIEW_REALM),
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

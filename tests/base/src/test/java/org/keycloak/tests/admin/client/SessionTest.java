/*
 * Copyright 2016 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @author tags. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package org.keycloak.tests.admin.client;

import java.util.Arrays;
import java.util.List;

import jakarta.ws.rs.core.Response;

import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.models.AdminRoles;
import org.keycloak.models.Constants;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.representations.idm.UserSessionRepresentation;
import org.keycloak.testframework.admin.AdminClientFactory;
import org.keycloak.testframework.annotations.InjectAdminClientFactory;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.InjectUser;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.oauth.OAuthClient;
import org.keycloak.testframework.oauth.annotations.InjectOAuthClient;
import org.keycloak.testframework.realm.ClientBuilder;
import org.keycloak.testframework.realm.CredentialBuilder;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.ManagedUser;
import org.keycloak.testframework.realm.RealmBuilder;
import org.keycloak.testframework.realm.RealmConfig;
import org.keycloak.testframework.realm.UserBuilder;
import org.keycloak.testframework.realm.UserConfig;
import org.keycloak.testframework.ui.annotations.InjectPage;
import org.keycloak.testframework.ui.page.LoginPage;
import org.keycloak.testframework.util.ApiUtil;
import org.keycloak.tests.suites.DatabaseTest;
import org.keycloak.tests.utils.admin.AdminApiUtil;
import org.keycloak.testsuite.util.AccountHelper;
import org.keycloak.testsuite.util.oauth.AccessTokenResponse;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 *
 * @author Stan Silvert ssilvert@redhat.com (C) 2016 Red Hat Inc.
 */
@KeycloakIntegrationTest
public class SessionTest {

    // Dedicated public direct-access-grant client used only to authenticate the role-restricted admin
    // clients. Kept separate from "test-app" so admin logins do not pollute the user-session lists under test.
    private static final String ADMIN_AUTH_CLIENT_ID = "test-admin-client";

    @InjectRealm(config = SessionTestRealmConfig.class)
    ManagedRealm managedRealm;

    @InjectUser(config = SessionTestUserConfig.class)
    ManagedUser user;

    @InjectOAuthClient
    OAuthClient oauth;

    @InjectAdminClientFactory
    AdminClientFactory adminClientFactory;

    @InjectPage
    LoginPage loginPage;

    @Test
    @DatabaseTest
    public void testGetAppSessionCount() {
        ClientResource accountClient = AdminApiUtil.findClientByClientId(managedRealm.admin(), "test-app");
        int sessionCount = accountClient.getApplicationSessionCount().get("count");
        assertEquals(0, sessionCount);

        oauth.openLoginForm();
        loginPage.fillLogin(user.getUsername(), user.getPassword());
        loginPage.submit();

        sessionCount = accountClient.getApplicationSessionCount().get("count");
        assertEquals(1, sessionCount);

        AccountHelper.logout(managedRealm.admin(), user.getUsername());

        sessionCount = accountClient.getApplicationSessionCount().get("count");
        assertEquals(0, sessionCount);
    }

    @Test
    @DatabaseTest
    public void testGetUserSessions() {
        ClientResource account = AdminApiUtil.findClientByClientId(managedRealm.admin(), "test-app");

        oauth.openLoginForm();
        loginPage.fillLogin(user.getUsername(), user.getPassword());
        loginPage.submit();

        List<UserSessionRepresentation> sessions = account.getUserSessions(0, 5);
        assertEquals(1, sessions.size());

        UserSessionRepresentation rep = sessions.get(0);

        UserRepresentation testUserRep = user.admin().toRepresentation();
        assertEquals(testUserRep.getId(), rep.getUserId());
        assertEquals(testUserRep.getUsername(), rep.getUsername());

        String clientId = account.toRepresentation().getId();
        assertEquals("test-app", rep.getClients().get(clientId));
        assertNotNull(rep.getIpAddress());
        assertTrue(rep.getLastAccess() > 0);
        assertTrue(rep.getStart() > 0);
        assertFalse(rep.isRememberMe());

        AccountHelper.logout(managedRealm.admin(), user.getUsername());
    }

    @Test
    public void testGetUserSessionsWithRememberMe() {
        managedRealm.updateWithCleanup(r -> r.setRememberMe(true));

        oauth.openLoginForm();
        loginPage.rememberMe(true);
        loginPage.fillLogin(user.getUsername(), user.getPassword());
        loginPage.submit();

        ClientResource account = AdminApiUtil.findClientByClientId(managedRealm.admin(), "test-app");
        List<UserSessionRepresentation> sessions = account.getUserSessions(0, 5);
        assertEquals(1, sessions.size());

        UserSessionRepresentation rep = sessions.get(0);
        assertTrue(rep.isRememberMe());

        AccountHelper.logout(managedRealm.admin(), user.getUsername());
    }

    @Test
    @DatabaseTest
    public void testGetUserSessionsHidesUserInfoWithoutViewUsers() {
        Keycloak viewClientsOnly = createRealmAdmin("view-clients-admin", AdminRoles.VIEW_CLIENTS);
        Keycloak viewClientsAndUsers = createRealmAdmin("view-clients-users-admin", AdminRoles.VIEW_CLIENTS, AdminRoles.VIEW_USERS);

        // Create a user session via direct grant
        AccessTokenResponse tokenResponse = oauth.doPasswordGrantRequest(user.getUsername(), user.getPassword());
        assertEquals(200, tokenResponse.getStatusCode());

        try {
            String clientUuid = AdminApiUtil.findClientByClientId(managedRealm.admin(), "test-app").toRepresentation().getId();
            UserRepresentation testUserRep = user.admin().toRepresentation();

            // Caller WITHOUT view-users: the session is still returned, but userId/username are masked
            UserSessionRepresentation masked = getSingleUserSession(viewClientsOnly, clientUuid);
            assertEquals("(hidden)", masked.getUserId());
            assertEquals("(hidden)", masked.getUsername());
            // Non-identifying fields must remain intact
            assertEquals("test-app", masked.getClients().get(clientUuid));
            assertNotNull(masked.getIpAddress());
            assertTrue(masked.getStart() > 0);

            // Caller WITH view-users: real values are exposed
            UserSessionRepresentation full = getSingleUserSession(viewClientsAndUsers, clientUuid);
            assertEquals(testUserRep.getId(), full.getUserId());
            assertEquals(testUserRep.getUsername(), full.getUsername());
        } finally {
            AccountHelper.logout(managedRealm.admin(), user.getUsername());
        }
    }

    @Test
    @DatabaseTest
    public void testGetOfflineUserSessionsHidesUserInfoWithoutViewUsers() {
        Keycloak viewClientsOnly = createRealmAdmin("offline-view-clients-admin", AdminRoles.VIEW_CLIENTS);
        Keycloak viewClientsAndUsers = createRealmAdmin("offline-view-clients-users-admin", AdminRoles.VIEW_CLIENTS, AdminRoles.VIEW_USERS);

        // Create an offline user session via direct grant requesting offline_access
        oauth.scope(OAuth2Constants.OFFLINE_ACCESS);
        try {
            AccessTokenResponse tokenResponse = oauth.doPasswordGrantRequest(user.getUsername(), user.getPassword());
            assertEquals(200, tokenResponse.getStatusCode());

            String clientUuid = AdminApiUtil.findClientByClientId(managedRealm.admin(), "test-app").toRepresentation().getId();
            UserRepresentation testUserRep = user.admin().toRepresentation();

            // Caller WITHOUT view-users: the offline session is still returned, but userId/username are masked
            UserSessionRepresentation masked = getSingleOfflineUserSession(viewClientsOnly, clientUuid);
            assertEquals("(hidden)", masked.getUserId());
            assertEquals("(hidden)", masked.getUsername());
            assertEquals("test-app", masked.getClients().get(clientUuid));

            // Caller WITH view-users: real values are exposed
            UserSessionRepresentation full = getSingleOfflineUserSession(viewClientsAndUsers, clientUuid);
            assertEquals(testUserRep.getId(), full.getUserId());
            assertEquals(testUserRep.getUsername(), full.getUsername());
        } finally {
            oauth.scope(null);
            user.admin().logout();
        }
    }

    private UserSessionRepresentation getSingleUserSession(Keycloak adminClient, String clientUuid) {
        List<UserSessionRepresentation> sessions = adminClient.realm(managedRealm.getName())
                .clients().get(clientUuid).getUserSessions(0, 5);
        assertEquals(1, sessions.size());
        return sessions.get(0);
    }

    private UserSessionRepresentation getSingleOfflineUserSession(Keycloak adminClient, String clientUuid) {
        List<UserSessionRepresentation> sessions = adminClient.realm(managedRealm.getName())
                .clients().get(clientUuid).getOfflineUserSessions(0, 5);
        assertEquals(1, sessions.size());
        return sessions.get(0);
    }

    /**
     * Creates a realm user holding the given {@code realm-management} client roles and returns an admin client
     * authenticated as that user, so permission boundaries can be exercised. The user and client are registered
     * for cleanup.
     */
    private Keycloak createRealmAdmin(String username, String... realmManagementRoles) {
        UserRepresentation adminUser = UserBuilder.create()
                .username(username)
                .name(username, username)
                .email(username + "@localhost.com")
                .emailVerified(true)
                .enabled(true)
                .build();

        String userId;
        try (Response response = managedRealm.admin().users().create(adminUser)) {
            userId = ApiUtil.getCreatedId(response);
        }
        managedRealm.cleanup().add(r -> r.users().delete(userId));
        managedRealm.admin().users().get(userId).resetPassword(CredentialBuilder.password("password").build());

        String realmManagementUuid = managedRealm.admin().clients()
                .findByClientId(Constants.REALM_MANAGEMENT_CLIENT_ID).get(0).getId();
        List<RoleRepresentation> roles = Arrays.stream(realmManagementRoles)
                .map(roleName -> managedRealm.admin().clients().get(realmManagementUuid).roles().get(roleName).toRepresentation())
                .toList();
        managedRealm.admin().users().get(userId).roles().clientLevel(realmManagementUuid).add(roles);

        return adminClientFactory.create()
                .realm(managedRealm.getName())
                .username(username)
                .password("password")
                .clientId(ADMIN_AUTH_CLIENT_ID)
                .grantType(OAuth2Constants.PASSWORD)
                .autoClose()
                .build();
    }

    private static class SessionTestRealmConfig implements RealmConfig {

        @Override
        public RealmBuilder configure(RealmBuilder realm) {
            return realm.clients(ClientBuilder.create(ADMIN_AUTH_CLIENT_ID)
                    .publicClient(true)
                    .directAccessGrantsEnabled(true));
        }
    }

    private static class SessionTestUserConfig implements UserConfig {

        @Override
        public UserBuilder configure(UserBuilder config) {
            return config.username("user")
                    .password("password")
                    .name("Session", "User")
                    .email("session@user.com")
                    .emailVerified(true);
        }
    }
}

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

import java.util.List;

import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.representations.idm.UserSessionRepresentation;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.InjectUser;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.oauth.OAuthClient;
import org.keycloak.testframework.oauth.annotations.InjectOAuthClient;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.ManagedUser;
import org.keycloak.testframework.realm.UserConfig;
import org.keycloak.testframework.realm.UserConfigBuilder;
import org.keycloak.testframework.ui.annotations.InjectPage;
import org.keycloak.testframework.ui.page.LoginPage;
import org.keycloak.tests.utils.admin.AdminApiUtil;
import org.keycloak.testsuite.util.AccountHelper;

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

    @InjectRealm
    ManagedRealm managedRealm;

    @InjectUser(config = SessionTestUserConfig.class)
    ManagedUser user;

    @InjectOAuthClient
    OAuthClient oauth;

    @InjectPage
    LoginPage loginPage;

    @Test
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

    private static class SessionTestUserConfig implements UserConfig {

        @Override
        public UserConfigBuilder configure(UserConfigBuilder config) {
            return config.username("user")
                    .password("password")
                    .name("Session", "User")
                    .email("session@user.com")
                    .emailVerified(true);
        }
    }
}

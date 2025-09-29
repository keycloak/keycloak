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

package org.keycloak.testsuite.admin.client;

import org.junit.Before;
import org.junit.Test;
import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.events.admin.OperationType;
import org.keycloak.events.admin.ResourceType;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.representations.idm.UserSessionRepresentation;
import org.keycloak.testsuite.AbstractClientTest;
import org.keycloak.testsuite.util.AccountHelper;
import org.keycloak.testsuite.util.AdminEventPaths;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.keycloak.testsuite.auth.page.AuthRealm.TEST;

/**
 *
 * @author Stan Silvert ssilvert@redhat.com (C) 2016 Red Hat Inc.
 */
public class SessionTest extends AbstractClientTest {

    @Before
    public void init() {
        // make user test user exists in test realm
        createTestUserWithAdminClient();
        getCleanup().addUserId(testUser.getId());

        assertAdminEvents.assertEvent(getRealmId(), OperationType.CREATE, AdminEventPaths.userResourcePath(testUser.getId()), ResourceType.USER);
        assertAdminEvents.assertEvent(getRealmId(), OperationType.ACTION, AdminEventPaths.userResetPasswordPath(testUser.getId()), ResourceType.USER);

        createAppClientInRealm(testRealmResource().toRepresentation().getRealm());
    }

    @Override
    public void setDefaultPageUriParameters() {
        super.setDefaultPageUriParameters();
        loginPage.setAuthRealm(TEST);
    }

    @Test
    public void testGetAppSessionCount() {
        ClientResource accountClient = findClientResourceById("test-app");
        int sessionCount = accountClient.getApplicationSessionCount().get("count");
        assertEquals(0, sessionCount);

        oauth.openLoginForm();
        loginPage.form().login(testUser);

        sessionCount = accountClient.getApplicationSessionCount().get("count");
        assertEquals(1, sessionCount);

        AccountHelper.logout(testRealmResource(), testUser.getUsername());

        sessionCount = accountClient.getApplicationSessionCount().get("count");
        assertEquals(0, sessionCount);
    }

    @Test
    public void testGetUserSessions() {
        //List<java.util.Map<String, String>> stats = this.testRealmResource().getClientSessionStats();
        ClientResource account = findClientResourceById("test-app");

        oauth.openLoginForm();
        loginPage.form().login(testUser);

        List<UserSessionRepresentation> sessions = account.getUserSessions(0, 5);
        assertEquals(1, sessions.size());

        UserSessionRepresentation rep = sessions.get(0);

        UserRepresentation testUserRep = getFullUserRep(testUser.getUsername());
        assertEquals(testUserRep.getId(), rep.getUserId());
        assertEquals(testUserRep.getUsername(), rep.getUsername());

        String clientId = account.toRepresentation().getId();
        assertEquals("test-app", rep.getClients().get(clientId));
        assertNotNull(rep.getIpAddress());
        assertNotNull(rep.getLastAccess());
        assertNotNull(rep.getStart());
        assertFalse(rep.isRememberMe());

        AccountHelper.logout(testRealmResource(), testUser.getUsername());
    }

    @Test
    public void testGetUserSessionsWithRememberMe() {
        RealmRepresentation realm = adminClient.realm(TEST).toRepresentation();
        realm.setRememberMe(true);
        adminClient.realm(TEST).update(realm);

        oauth.openLoginForm();
        loginPage.form().rememberMe(true);
        loginPage.form().login(testUser);

        ClientResource account = findClientResourceById("test-app");
        List<UserSessionRepresentation> sessions = account.getUserSessions(0, 5);
        assertEquals(1, sessions.size());

        UserSessionRepresentation rep = sessions.get(0);
        assertTrue(rep.isRememberMe());

        AccountHelper.logout(testRealmResource(), testUser.getUsername());
    }
}

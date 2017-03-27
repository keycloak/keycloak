/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.testsuite.model;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserManager;
import org.keycloak.models.UserModel;
import org.keycloak.sessions.AuthenticationSessionModel;
import org.keycloak.sessions.CommonClientSessionModel;
import org.keycloak.testsuite.rule.KeycloakRule;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class AuthenticationSessionProviderTest {

    @ClassRule
    public static KeycloakRule kc = new KeycloakRule();

    private KeycloakSession session;
    private RealmModel realm;

    @Before
    public void before() {
        session = kc.startSession();
        realm = session.realms().getRealm("test");
        session.users().addUser(realm, "user1").setEmail("user1@localhost");
        session.users().addUser(realm, "user2").setEmail("user2@localhost");
    }

    @After
    public void after() {
        resetSession();
        session.sessions().removeUserSessions(realm);
        UserModel user1 = session.users().getUserByUsername("user1", realm);
        UserModel user2 = session.users().getUserByUsername("user2", realm);

        UserManager um = new UserManager(session);
        if (user1 != null) {
            um.removeUser(realm, user1);
        }
        if (user2 != null) {
            um.removeUser(realm, user2);
        }
        kc.stopSession(session, true);
    }

    private void resetSession() {
        kc.stopSession(session, true);
        session = kc.startSession();
        realm = session.realms().getRealm("test");
    }

    @Test
    public void testLoginSessionsCRUD() {
        ClientModel client1 = realm.getClientByClientId("test-app");
        UserModel user1 = session.users().getUserByUsername("user1", realm);

        AuthenticationSessionModel authSession = session.authenticationSessions().createAuthenticationSession(realm, client1);

        authSession.setAction("foo");
        authSession.setTimestamp(100);

        resetSession();

        // Ensure session is here
        authSession = session.authenticationSessions().getAuthenticationSession(realm, authSession.getId());
        testLoginSession(authSession, client1.getId(), null, "foo");
        Assert.assertEquals(100, authSession.getTimestamp());

        // Update and commit
        authSession.setAction("foo-updated");
        authSession.setTimestamp(200);
        authSession.setAuthenticatedUser(session.users().getUserByUsername("user1", realm));

        resetSession();

        // Ensure session was updated
        authSession = session.authenticationSessions().getAuthenticationSession(realm, authSession.getId());
        testLoginSession(authSession, client1.getId(), user1.getId(), "foo-updated");
        Assert.assertEquals(200, authSession.getTimestamp());

        // Remove and commit
        session.authenticationSessions().removeAuthenticationSession(realm, authSession);

        resetSession();

        // Ensure session was removed
        Assert.assertNull(session.authenticationSessions().getAuthenticationSession(realm, authSession.getId()));

    }

    @Test
    public void testLoginSessionRestart() {
        ClientModel client1 = realm.getClientByClientId("test-app");
        UserModel user1 = session.users().getUserByUsername("user1", realm);

        AuthenticationSessionModel authSession = session.authenticationSessions().createAuthenticationSession(realm, client1);

        authSession.setAction("foo");
        authSession.setTimestamp(100);

        authSession.setAuthenticatedUser(user1);
        authSession.setAuthNote("foo", "bar");
        authSession.setClientNote("foo2", "bar2");
        authSession.setExecutionStatus("123", CommonClientSessionModel.ExecutionStatus.SUCCESS);

        resetSession();

        client1 = realm.getClientByClientId("test-app");
        authSession = session.authenticationSessions().getAuthenticationSession(realm, authSession.getId());
        authSession.restartSession(realm, client1);

        resetSession();

        authSession = session.authenticationSessions().getAuthenticationSession(realm, authSession.getId());
        testLoginSession(authSession, client1.getId(), null, null);
        Assert.assertTrue(authSession.getTimestamp() > 0);

        Assert.assertTrue(authSession.getClientNotes().isEmpty());
        Assert.assertNull(authSession.getAuthNote("foo2"));
        Assert.assertTrue(authSession.getExecutionStatus().isEmpty());

    }

    private void testLoginSession(AuthenticationSessionModel authSession, String expectedClientId, String expectedUserId, String expectedAction) {
        Assert.assertEquals(expectedClientId, authSession.getClient().getId());

        if (expectedUserId == null) {
            Assert.assertNull(authSession.getAuthenticatedUser());
        } else {
            Assert.assertEquals(expectedUserId, authSession.getAuthenticatedUser().getId());
        }

        if (expectedAction == null) {
            Assert.assertNull(authSession.getAction());
        } else {
            Assert.assertEquals(expectedAction, authSession.getAction());
        }
    }
}

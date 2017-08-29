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
import org.keycloak.common.util.Time;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserManager;
import org.keycloak.models.UserModel;
import org.keycloak.services.managers.ClientManager;
import org.keycloak.services.managers.RealmManager;
import org.keycloak.sessions.AuthenticationSessionModel;
import org.keycloak.sessions.CommonClientSessionModel;
import org.keycloak.testsuite.rule.KeycloakRule;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

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
        testAuthenticationSession(authSession, client1.getId(), null, "foo");
        Assert.assertEquals(100, authSession.getTimestamp());

        // Update and commit
        authSession.setAction("foo-updated");
        authSession.setTimestamp(200);
        authSession.setAuthenticatedUser(session.users().getUserByUsername("user1", realm));

        resetSession();

        // Ensure session was updated
        authSession = session.authenticationSessions().getAuthenticationSession(realm, authSession.getId());
        testAuthenticationSession(authSession, client1.getId(), user1.getId(), "foo-updated");
        Assert.assertEquals(200, authSession.getTimestamp());

        // Remove and commit
        session.authenticationSessions().removeAuthenticationSession(realm, authSession);

        resetSession();

        // Ensure session was removed
        Assert.assertNull(session.authenticationSessions().getAuthenticationSession(realm, authSession.getId()));

    }

    @Test
    public void testAuthenticationSessionRestart() {
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
        testAuthenticationSession(authSession, client1.getId(), null, null);
        Assert.assertTrue(authSession.getTimestamp() > 0);

        Assert.assertTrue(authSession.getClientNotes().isEmpty());
        Assert.assertNull(authSession.getAuthNote("foo2"));
        Assert.assertTrue(authSession.getExecutionStatus().isEmpty());

    }


    @Test
    public void testExpiredAuthSessions() {
        try {
            realm.setAccessCodeLifespan(10);
            realm.setAccessCodeLifespanUserAction(10);
            realm.setAccessCodeLifespanLogin(30);

            // Login lifespan is largest
            String authSessionId = session.authenticationSessions().createAuthenticationSession(realm, realm.getClientByClientId("test-app")).getId();
            resetSession();

            Time.setOffset(25);
            session.authenticationSessions().removeExpired(realm);
            resetSession();

            assertNotNull(session.authenticationSessions().getAuthenticationSession(realm, authSessionId));

            Time.setOffset(35);
            session.authenticationSessions().removeExpired(realm);
            resetSession();

            assertNull(session.authenticationSessions().getAuthenticationSession(realm, authSessionId));

            // User action is largest
            realm.setAccessCodeLifespanUserAction(40);

            Time.setOffset(0);
            authSessionId = session.authenticationSessions().createAuthenticationSession(realm, realm.getClientByClientId("test-app")).getId();
            resetSession();

            Time.setOffset(35);
            session.authenticationSessions().removeExpired(realm);
            resetSession();

            assertNotNull(session.authenticationSessions().getAuthenticationSession(realm, authSessionId));

            Time.setOffset(45);
            session.authenticationSessions().removeExpired(realm);
            resetSession();

            assertNull(session.authenticationSessions().getAuthenticationSession(realm, authSessionId));

            // Access code is largest
            realm.setAccessCodeLifespan(50);

            Time.setOffset(0);
            authSessionId = session.authenticationSessions().createAuthenticationSession(realm, realm.getClientByClientId("test-app")).getId();
            resetSession();

            Time.setOffset(45);
            session.authenticationSessions().removeExpired(realm);
            resetSession();

            assertNotNull(session.authenticationSessions().getAuthenticationSession(realm, authSessionId));

            Time.setOffset(55);
            session.authenticationSessions().removeExpired(realm);
            resetSession();

            assertNull(session.authenticationSessions().getAuthenticationSession(realm, authSessionId));
        } finally {
            Time.setOffset(0);

            realm.setAccessCodeLifespan(60);
            realm.setAccessCodeLifespanUserAction(300);
            realm.setAccessCodeLifespanLogin(1800);

        }
    }


    @Test
    public void testOnRealmRemoved() {
        RealmModel fooRealm = session.realms().createRealm("foo-realm");
        ClientModel fooClient = fooRealm.addClient("foo-client");

        String authSessionId = session.authenticationSessions().createAuthenticationSession(realm, realm.getClientByClientId("test-app")).getId();
        String authSessionId2 = session.authenticationSessions().createAuthenticationSession(fooRealm, fooClient).getId();

        resetSession();

        new RealmManager(session).removeRealm(session.realms().getRealmByName("foo-realm"));

        resetSession();

        AuthenticationSessionModel authSession = session.authenticationSessions().getAuthenticationSession(realm, authSessionId);
        testAuthenticationSession(authSession, realm.getClientByClientId("test-app").getId(), null, null);
        Assert.assertNull(session.authenticationSessions().getAuthenticationSession(realm, authSessionId2));
    }

    @Test
    public void testOnClientRemoved() {
        String authSessionId = session.authenticationSessions().createAuthenticationSession(realm, realm.getClientByClientId("test-app")).getId();
        String authSessionId2 = session.authenticationSessions().createAuthenticationSession(realm, realm.getClientByClientId("third-party")).getId();

        String testAppClientUUID = realm.getClientByClientId("test-app").getId();

        resetSession();

        new ClientManager(new RealmManager(session)).removeClient(realm, realm.getClientByClientId("third-party"));

        resetSession();

        AuthenticationSessionModel authSession = session.authenticationSessions().getAuthenticationSession(realm, authSessionId);
        testAuthenticationSession(authSession, testAppClientUUID, null, null);
        Assert.assertNull(session.authenticationSessions().getAuthenticationSession(realm, authSessionId2));

        // Revert client
        realm.addClient("third-party");
    }


    private void testAuthenticationSession(AuthenticationSessionModel authSession, String expectedClientId, String expectedUserId, String expectedAction) {
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

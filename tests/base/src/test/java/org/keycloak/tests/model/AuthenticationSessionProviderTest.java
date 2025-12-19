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

package org.keycloak.tests.model;

import java.util.concurrent.atomic.AtomicReference;

import org.keycloak.common.util.Time;
import org.keycloak.infinispan.util.InfinispanUtils;
import org.keycloak.models.ClientModel;
import org.keycloak.models.Constants;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.models.utils.ResetTimeOffsetEvent;
import org.keycloak.services.managers.ClientManager;
import org.keycloak.services.managers.RealmManager;
import org.keycloak.sessions.AuthenticationSessionModel;
import org.keycloak.sessions.CommonClientSessionModel;
import org.keycloak.sessions.RootAuthenticationSessionModel;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.injection.LifeCycle;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.RealmConfig;
import org.keycloak.testframework.realm.RealmConfigBuilder;
import org.keycloak.testframework.remote.annotations.TestOnServer;
import org.keycloak.testframework.remote.runonserver.InjectRunOnServer;
import org.keycloak.testframework.remote.runonserver.RunOnServerClient;
import org.keycloak.tests.utils.infinispan.InfinispanTimeUtil;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assume.assumeFalse;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
@KeycloakIntegrationTest
public class AuthenticationSessionProviderTest {

    @InjectRealm(config = AuthenticationSessionProviderRealm.class, lifecycle = LifeCycle.METHOD)
    ManagedRealm realm;

    @InjectRunOnServer
    RunOnServerClient runOnServer;

    @TestOnServer
    public void testLoginSessionsCRUD(KeycloakSession session) {
        AtomicReference<String> rootAuthSessionID = new AtomicReference<>();
        AtomicReference<String> tabID = new AtomicReference<>();
        final int timestamp = Time.currentTime();

        KeycloakModelUtils.runJobInTransaction(session.getKeycloakSessionFactory(), (KeycloakSession sessionCRUD1) -> {
            KeycloakSession currentSession = sessionCRUD1;
            RealmModel realm = currentSession.realms().getRealmByName("test");
            currentSession.getContext().setRealm(realm);

            ClientModel client1 = realm.getClientByClientId("test-app");

            RootAuthenticationSessionModel rootAuthSession = currentSession.authenticationSessions().createRootAuthenticationSession(realm);
            rootAuthSessionID.set(rootAuthSession.getId());

            AuthenticationSessionModel authSession = rootAuthSession.createAuthenticationSession(client1);
            tabID.set(authSession.getTabId());

            authSession.setAction("foo");
            rootAuthSession.setTimestamp(timestamp);
        });

        KeycloakModelUtils.runJobInTransaction(session.getKeycloakSessionFactory(), (KeycloakSession sessionCRUD2) -> {
            KeycloakSession currentSession = sessionCRUD2;
            RealmModel realm = currentSession.realms().getRealmByName("test");
            currentSession.getContext().setRealm(realm);

            ClientModel client1 = realm.getClientByClientId("test-app");

            // Ensure currentSession is here
            RootAuthenticationSessionModel rootAuthSession = currentSession.authenticationSessions().getRootAuthenticationSession(realm, rootAuthSessionID.get());
            AuthenticationSessionModel authSession = rootAuthSession.getAuthenticationSession(client1, tabID.get());
            testAuthenticationSession(authSession, client1.getId(), null, "foo");

            assertThat(rootAuthSession.getTimestamp(), is(timestamp));

            // Update and commit
            authSession.setAction("foo-updated");
            rootAuthSession.setTimestamp(timestamp + 1000);
            authSession.setAuthenticatedUser(currentSession.users().getUserByUsername(realm, "user1"));
        });

        KeycloakModelUtils.runJobInTransaction(session.getKeycloakSessionFactory(), (KeycloakSession sessionCRUD3) -> {
            KeycloakSession currentSession = sessionCRUD3;
            RealmModel realm = currentSession.realms().getRealmByName("test");
            currentSession.getContext().setRealm(realm);
            UserModel user1 = currentSession.users().getUserByUsername(realm, "user1");

            // Ensure currentSession was updated
            RootAuthenticationSessionModel rootAuthSession = currentSession.authenticationSessions().getRootAuthenticationSession(realm, rootAuthSessionID.get());
            ClientModel client1 = realm.getClientByClientId("test-app");
            AuthenticationSessionModel authSession = rootAuthSession.getAuthenticationSession(client1, tabID.get());

            testAuthenticationSession(authSession, client1.getId(), user1.getId(), "foo-updated");

            assertThat(rootAuthSession.getTimestamp(), is(timestamp + 1000));

            // Remove and commit
            currentSession.authenticationSessions().removeRootAuthenticationSession(realm, rootAuthSession);
        });

        KeycloakModelUtils.runJobInTransaction(session.getKeycloakSessionFactory(), (KeycloakSession sessionCRUD4) -> {
            KeycloakSession currentSession = sessionCRUD4;
            RealmModel realm = currentSession.realms().getRealmByName("test");
            currentSession.getContext().setRealm(realm);

            // Ensure currentSession was removed
            assertThat(currentSession.authenticationSessions().getRootAuthenticationSession(realm, rootAuthSessionID.get()), nullValue());
        });
    }

    @TestOnServer
    public void testAuthenticationSessionRestart(KeycloakSession session) {
        AtomicReference<String> parentAuthSessionID = new AtomicReference<>();
        AtomicReference<String> tabID = new AtomicReference<>();
        final int timestamp = Time.currentTime();

        KeycloakModelUtils.runJobInTransaction(session.getKeycloakSessionFactory(), (KeycloakSession sessionRestart1) -> {
            KeycloakSession currentSession = sessionRestart1;
            RealmModel realm = currentSession.realms().getRealmByName("test");
            currentSession.getContext().setRealm(realm);

            ClientModel client1 = realm.getClientByClientId("test-app");
            UserModel user1 = currentSession.users().getUserByUsername(realm, "user1");

            AuthenticationSessionModel authSession = currentSession.authenticationSessions().createRootAuthenticationSession(realm)
                    .createAuthenticationSession(client1);

            parentAuthSessionID.set(authSession.getParentSession().getId());
            tabID.set(authSession.getTabId());

            authSession.setAction("foo");
            authSession.getParentSession().setTimestamp(timestamp);

            authSession.setAuthenticatedUser(user1);
            authSession.setAuthNote("foo", "bar");
            authSession.setClientNote("foo2", "bar2");
            authSession.setExecutionStatus("123", CommonClientSessionModel.ExecutionStatus.SUCCESS);
        });

        KeycloakModelUtils.runJobInTransaction(session.getKeycloakSessionFactory(), (KeycloakSession sessionRestart2) -> {
            KeycloakSession currentSession = sessionRestart2;
            RealmModel realm = currentSession.realms().getRealmByName("test");
            currentSession.getContext().setRealm(realm);

            // Test restart root authentication session
            ClientModel client1 = realm.getClientByClientId("test-app");
            AuthenticationSessionModel authSession = currentSession.authenticationSessions().getRootAuthenticationSession(realm, parentAuthSessionID.get())
                    .getAuthenticationSession(client1, tabID.get());
            authSession.getParentSession().restartSession(realm);
        });

        KeycloakModelUtils.runJobInTransaction(session.getKeycloakSessionFactory(), (KeycloakSession sessionRestart3) -> {
            KeycloakSession currentSession = sessionRestart3;
            RealmModel realm = currentSession.realms().getRealmByName("test");
            currentSession.getContext().setRealm(realm);

            ClientModel client1 = realm.getClientByClientId("test-app");

            RootAuthenticationSessionModel rootAuthSession = currentSession.authenticationSessions().getRootAuthenticationSession(realm, parentAuthSessionID.get());

            assertThat(rootAuthSession.getAuthenticationSession(client1, tabID.get()), nullValue());
            assertThat(rootAuthSession.getTimestamp() > 0, is(true));
        });
    }

    @TestOnServer
    public void testExpiredAuthSessions(KeycloakSession session) {
        assumeFalse(InfinispanUtils.isRemoteInfinispan());
        InfinispanTimeUtil.enableTestingTimeService(session);
        AtomicReference<String> authSessionID = new AtomicReference<>();

        try {
            KeycloakModelUtils.runJobInTransaction(session.getKeycloakSessionFactory(), mainSession -> {
                try {
                    // AccessCodeLifespan = 10 ; AccessCodeLifespanUserAction = 10 ; AccessCodeLifespanLogin = 30
                    setAccessCodeLifespan(mainSession, 10, 10, 30);

                    createAuthSession(mainSession, authSessionID);
                    testExpiredOffset(mainSession, 25, false, authSessionID.get());
                    testExpiredOffset(mainSession, 35, true, authSessionID.get());

                    // AccessCodeLifespan = Not set ; AccessCodeLifespanUserAction = 10 ; AccessCodeLifespanLogin = Not set
                    setAccessCodeLifespan(mainSession, -1, 40, -1);

                    createAuthSession(mainSession, authSessionID);
                    testExpiredOffset(mainSession, 35, false, authSessionID.get());
                    testExpiredOffset(mainSession, 45, true, authSessionID.get());

                    // AccessCodeLifespan = 50 ; AccessCodeLifespanUserAction = Not set ; AccessCodeLifespanLogin = Not set
                    setAccessCodeLifespan(mainSession, 50, -1, -1);

                    createAuthSession(mainSession, authSessionID);
                    testExpiredOffset(mainSession, 45, false, authSessionID.get());
                    testExpiredOffset(mainSession, 55, true, authSessionID.get());

                } finally {
                    Time.setOffset(0);
                    session.getKeycloakSessionFactory().publish(new ResetTimeOffsetEvent());
                    setAccessCodeLifespan(mainSession, 60, 300, 1800);
                }
            });
        } finally {
            InfinispanTimeUtil.disableTestingTimeService(session);
        }
    }

    @TestOnServer
    public void testOnRealmRemoved(KeycloakSession session) {
        AtomicReference<String> authSessionID = new AtomicReference<>();
        AtomicReference<String> authSessionID2 = new AtomicReference<>();

        KeycloakModelUtils.runJobInTransaction(session.getKeycloakSessionFactory(), (KeycloakSession sesRealmRemoved1) -> {
            KeycloakSession currentSession = sesRealmRemoved1;
            RealmModel realm = currentSession.realms().getRealmByName("test");
            RealmModel fooRealm = currentSession.realms().createRealm("foo-realm");
            fooRealm.setDefaultRole(currentSession.roles().addRealmRole(fooRealm, Constants.DEFAULT_ROLES_ROLE_PREFIX  + "-" + fooRealm.getName()));
            fooRealm.setAccessCodeLifespanLogin(1800);
            fooRealm.addClient("foo-client");

            authSessionID.set(currentSession.authenticationSessions().createRootAuthenticationSession(realm).getId());
            authSessionID2.set(currentSession.authenticationSessions().createRootAuthenticationSession(fooRealm).getId());
        });

        KeycloakModelUtils.runJobInTransaction(session.getKeycloakSessionFactory(), (KeycloakSession sesRealmRemoved2) -> {
            KeycloakSession currentSession = sesRealmRemoved2;
            RealmModel fooRealm = currentSession.realms().getRealmByName("foo-realm");
            currentSession.getContext().setRealm(fooRealm);
            new RealmManager(currentSession).removeRealm(fooRealm);
        });

        KeycloakModelUtils.runJobInTransaction(session.getKeycloakSessionFactory(), (KeycloakSession sesRealmRemoved3) -> {
            KeycloakSession currentSession = sesRealmRemoved3;
            RealmModel realm = currentSession.realms().getRealmByName("test");

            RootAuthenticationSessionModel authSession = currentSession.authenticationSessions().getRootAuthenticationSession(realm, authSessionID.get());

            assertThat(authSession, notNullValue());
            assertThat(currentSession.authenticationSessions().getRootAuthenticationSession(realm, authSessionID2.get()), nullValue());
        });
    }

    @TestOnServer
    public void testOnClientRemoved(KeycloakSession session) {
        AtomicReference<String> tab1ID = new AtomicReference<>();
        AtomicReference<String> tab2ID = new AtomicReference<>();
        AtomicReference<String> authSessionID = new AtomicReference<>();

        KeycloakModelUtils.runJobInTransaction(session.getKeycloakSessionFactory(), (KeycloakSession sesRealmRemoved1) -> {
            KeycloakSession currentSession = sesRealmRemoved1;
            RealmModel realm = currentSession.realms().getRealmByName("test");
            currentSession.getContext().setRealm(realm);

            authSessionID.set(currentSession.authenticationSessions().createRootAuthenticationSession(realm).getId());

            AuthenticationSessionModel authSession1 = currentSession.authenticationSessions().getRootAuthenticationSession(realm, authSessionID.get()).createAuthenticationSession(realm.getClientByClientId("test-app"));
            AuthenticationSessionModel authSession2 = currentSession.authenticationSessions().getRootAuthenticationSession(realm, authSessionID.get()).createAuthenticationSession(realm.getClientByClientId("third-party"));
            tab1ID.set(authSession1.getTabId());
            tab2ID.set(authSession2.getTabId());

            authSession1.setAuthNote("foo", "bar");
            authSession2.setAuthNote("foo", "baz");
        });

        KeycloakModelUtils.runJobInTransaction(session.getKeycloakSessionFactory(), (KeycloakSession sesRealmRemoved1) -> {
            KeycloakSession currentSession = sesRealmRemoved1;
            RealmModel realm = currentSession.realms().getRealmByName("test");
            currentSession.getContext().setRealm(realm);

            RootAuthenticationSessionModel rootAuthSession = currentSession.authenticationSessions().getRootAuthenticationSession(realm, authSessionID.get());

            assertThat(rootAuthSession.getAuthenticationSessions().size(), is(2));
            assertThat(rootAuthSession.getAuthenticationSession(realm.getClientByClientId("test-app"), tab1ID.get()).getAuthNote("foo"), is("bar"));
            assertThat(rootAuthSession.getAuthenticationSession(realm.getClientByClientId("third-party"), tab2ID.get()).getAuthNote("foo"), is("baz"));

            new ClientManager(new RealmManager(currentSession)).removeClient(realm, realm.getClientByClientId("third-party"));
        });

        KeycloakModelUtils.runJobInTransaction(session.getKeycloakSessionFactory(), (KeycloakSession sesRealmRemoved1) -> {
            KeycloakSession currentSession = sesRealmRemoved1;
            RealmModel realm = currentSession.realms().getRealmByName("test");
            currentSession.getContext().setRealm(realm);
            RootAuthenticationSessionModel rootAuthSession = currentSession.authenticationSessions().getRootAuthenticationSession(realm, authSessionID.get());

            assertThat(rootAuthSession.getAuthenticationSession(realm.getClientByClientId("test-app"), tab1ID.get()).getAuthNote("foo"), is("bar"));
            assertThat(rootAuthSession.getAuthenticationSession(realm.getClientByClientId("third-party"), tab2ID.get()), nullValue());

            // Revert client
            realm.addClient("third-party");
        });
    }

    private void testAuthenticationSession(AuthenticationSessionModel authSession, String expectedClientId, String expectedUserId, String expectedAction) {
        assertThat(authSession.getClient().getId(), is(expectedClientId));

        if (expectedUserId == null) {
            assertThat(authSession.getAuthenticatedUser(), nullValue());
        } else {
            assertThat(authSession.getAuthenticatedUser().getId(), is(expectedUserId));
        }

        if (expectedAction == null) {
            assertThat(authSession.getAction(), nullValue());
        } else {
            assertThat(authSession.getAction(), is(expectedAction));
        }
    }

    private void createAuthSession(KeycloakSession session, AtomicReference<String> authSessionID) {

        KeycloakModelUtils.runJobInTransaction(session.getKeycloakSessionFactory(), (KeycloakSession createAuthSession) -> {
            KeycloakSession currentSession = createAuthSession;
            RealmModel realm = currentSession.realms().getRealmByName("test");

            Time.setOffset(0);
            authSessionID.set(currentSession.authenticationSessions().createRootAuthenticationSession(realm).getId());
        });
    }

    private void testExpiredOffset(KeycloakSession session, int offset, boolean isSessionNull, String authSessionID) {

        KeycloakModelUtils.runJobInTransaction(session.getKeycloakSessionFactory(), (KeycloakSession sessionExp) -> {
            KeycloakSession currentSession = sessionExp;
            RealmModel realm = currentSession.realms().getRealmByName("test");

            Time.setOffset(offset);
            currentSession.authenticationSessions().removeExpired(realm);
        });

        KeycloakModelUtils.runJobInTransaction(session.getKeycloakSessionFactory(), (KeycloakSession sessionExpVerify) -> {
            KeycloakSession currentSession = sessionExpVerify;
            RealmModel realm = currentSession.realms().getRealmByName("test");

            if (isSessionNull)
                assertThat(currentSession.authenticationSessions().getRootAuthenticationSession(realm, authSessionID), nullValue());
            else
                assertThat(currentSession.authenticationSessions().getRootAuthenticationSession(realm, authSessionID), notNullValue());
        });
    }

    // If parameter is -1, then the parameter won't change.
    private void setAccessCodeLifespan(KeycloakSession session, int lifespan, int lifespanUserAction, int lifespanLogin) {

        KeycloakModelUtils.runJobInTransaction(session.getKeycloakSessionFactory(), (KeycloakSession sessionLifespan) -> {
            KeycloakSession currentSession = sessionLifespan;
            RealmModel realm = currentSession.realms().getRealmByName("test");

            if (lifespan != -1)
                realm.setAccessCodeLifespan(lifespan);

            if (lifespanUserAction != -1)
                realm.setAccessCodeLifespanUserAction(lifespanUserAction);

            if (lifespanLogin != -1)
                realm.setAccessCodeLifespanLogin(lifespanLogin);
        });
    }

    private static final class AuthenticationSessionProviderRealm implements RealmConfig {

        @Override
        public RealmConfigBuilder configure(RealmConfigBuilder realm) {
            realm.name("test");
            realm.addUser("user1").email("user1@localhost");
            realm.addUser("user2").email("user2@localhost");
            realm.addClient("test-app");
            realm.addClient("third-party");
            return realm;
        }
    }

}

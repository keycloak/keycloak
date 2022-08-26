/*
 * Copyright 2021 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.testsuite.model.session;

import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Test;
import org.keycloak.common.util.Time;
import org.keycloak.models.ClientModel;
import org.keycloak.models.Constants;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.sessions.AuthenticationSessionModel;
import org.keycloak.sessions.AuthenticationSessionProvider;
import org.keycloak.sessions.RootAuthenticationSessionModel;
import org.keycloak.testsuite.model.KeycloakModelTest;
import org.keycloak.testsuite.model.RequireProvider;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.keycloak.testsuite.model.session.UserSessionPersisterProviderTest.createClients;

/**
 * @author <a href="mailto:mkanis@redhat.com">Martin Kanis</a>
 */
@RequireProvider(value = AuthenticationSessionProvider.class)
public class AuthenticationSessionTest extends KeycloakModelTest {

    private String realmId;

    @Override
    public void createEnvironment(KeycloakSession s) {
        RealmModel realm = s.realms().createRealm("test");
        realm.setDefaultRole(s.roles().addRealmRole(realm, Constants.DEFAULT_ROLES_ROLE_PREFIX + "-" + realm.getName()));
        realm.setAccessCodeLifespanLogin(1800);

        this.realmId = realm.getId();

        createClients(s, realm);
    }

    @Override
    public void cleanEnvironment(KeycloakSession s) {
        s.realms().removeRealm(realmId);
    }

    @Test
    public void testLimitAuthSessions() {
        AtomicReference<String> rootAuthSessionId = new AtomicReference<>();
        List<String> tabIds = withRealm(realmId, (session, realm) -> {
            RootAuthenticationSessionModel ras = session.authenticationSessions().createRootAuthenticationSession(realm);
            rootAuthSessionId.set(ras.getId());
            ClientModel client = realm.getClientByClientId("test-app");
            return IntStream.range(0, 300)
                    .mapToObj(i -> {
                        Time.setOffset(i);
                        return ras.createAuthenticationSession(client);
                    })
                    .map(AuthenticationSessionModel::getTabId)
                    .collect(Collectors.toList());
        });

        String tabId = withRealm(realmId, (session, realm) -> {
            RootAuthenticationSessionModel ras = session.authenticationSessions().getRootAuthenticationSession(realm, rootAuthSessionId.get());
            ClientModel client = realm.getClientByClientId("test-app");

            // create 301st auth session
            return ras.createAuthenticationSession(client).getTabId();
        });

        withRealm(realmId, (session, realm) -> {
            RootAuthenticationSessionModel ras = session.authenticationSessions().getRootAuthenticationSession(realm, rootAuthSessionId.get());
            ClientModel client = realm.getClientByClientId("test-app");

            assertThat(ras.getAuthenticationSessions(), Matchers.aMapWithSize(300));

            Assert.assertEquals(tabId, ras.getAuthenticationSession(client, tabId).getTabId());

            // assert the first authentication session was deleted
            Assert.assertNull(ras.getAuthenticationSession(client, tabIds.get(0)));

            return null;
        });
    }

    @Test
    public void testAuthSessions() {
        AtomicReference<String> rootAuthSessionId = new AtomicReference<>();
        List<String> tabIds = withRealm(realmId, (session, realm) -> {
            RootAuthenticationSessionModel rootAuthSession = session.authenticationSessions().createRootAuthenticationSession(realm);
            rootAuthSessionId.set(rootAuthSession.getId());

            ClientModel client = realm.getClientByClientId("test-app");
            return IntStream.range(0, 5)
                    .mapToObj(i -> {
                        AuthenticationSessionModel authSession = rootAuthSession.createAuthenticationSession(client);
                        authSession.setExecutionStatus("username", AuthenticationSessionModel.ExecutionStatus.ATTEMPTED);
                        authSession.setAuthNote("foo", "bar");
                        authSession.setClientNote("foo", "bar");
                        return authSession;
                    })
                    .map(AuthenticationSessionModel::getTabId)
                    .collect(Collectors.toList());
        });

        withRealm(realmId, (session, realm) -> {
            RootAuthenticationSessionModel rootAuthSession = session.authenticationSessions().getRootAuthenticationSession(realm, rootAuthSessionId.get());
            Assert.assertNotNull(rootAuthSession);
            Assert.assertEquals(rootAuthSessionId.get(), rootAuthSession.getId());

            ClientModel client = realm.getClientByClientId("test-app");
            tabIds.forEach(tabId -> {
                AuthenticationSessionModel authSession = rootAuthSession.getAuthenticationSession(client, tabId);
                Assert.assertNotNull(authSession);

                Assert.assertEquals(AuthenticationSessionModel.ExecutionStatus.ATTEMPTED, authSession.getExecutionStatus().get("username"));
                Assert.assertEquals("bar", authSession.getAuthNote("foo"));
                Assert.assertEquals("bar", authSession.getClientNote("foo"));
            });

            // remove first two auth sessions
            rootAuthSession.removeAuthenticationSessionByTabId(tabIds.get(0));
            rootAuthSession.removeAuthenticationSessionByTabId(tabIds.get(1));

            return null;
        });

        withRealm(realmId, (session, realm) -> {
            RootAuthenticationSessionModel rootAuthSession = session.authenticationSessions().getRootAuthenticationSession(realm, rootAuthSessionId.get());
            Assert.assertNotNull(rootAuthSession);
            Assert.assertEquals(rootAuthSessionId.get(), rootAuthSession.getId());

            assertThat(rootAuthSession.getAuthenticationSessions(), Matchers.aMapWithSize(3));

            Assert.assertNull(rootAuthSession.getAuthenticationSessions().get(tabIds.get(0)));
            Assert.assertNull(rootAuthSession.getAuthenticationSessions().get(tabIds.get(1)));
            IntStream.range(2,4).mapToObj(i -> rootAuthSession.getAuthenticationSessions().get(tabIds.get(i))).forEach(Assert::assertNotNull);

            session.authenticationSessions().removeRootAuthenticationSession(realm, rootAuthSession);

            return null;
        });

        withRealm(realmId, (session, realm) -> {
            RootAuthenticationSessionModel rootAuthSession = session.authenticationSessions().getRootAuthenticationSession(realm, rootAuthSessionId.get());
            Assert.assertNull(rootAuthSession);

            return null;
        });
    }

    @Test
    public void testRemoveExpiredAuthSessions() {
        AtomicReference<String> rootAuthSessionId = new AtomicReference<>();
        withRealm(realmId, (session, realm) -> {
            RootAuthenticationSessionModel rootAuthSession = session.authenticationSessions().createRootAuthenticationSession(realm);
            ClientModel client = realm.getClientByClientId("test-app");
            rootAuthSession.createAuthenticationSession(client);
            rootAuthSessionId.set(rootAuthSession.getId());

            return null;
        });

        withRealm(realmId, (session, realm) -> {
            RootAuthenticationSessionModel rootAuthSession = session.authenticationSessions().getRootAuthenticationSession(realm, rootAuthSessionId.get());
            Assert.assertNotNull(rootAuthSession);

            Time.setOffset(1900);

            return null;
        });

        withRealm(realmId, (session, realm) -> {
            RootAuthenticationSessionModel rootAuthSession = session.authenticationSessions().getRootAuthenticationSession(realm, rootAuthSessionId.get());
            Assert.assertNull(rootAuthSession);

            return null;
        });
    }
}

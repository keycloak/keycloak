/*
 * Copyright 2022 Red Hat, Inc. and/or its affiliates
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

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import org.keycloak.models.AuthenticatedClientSessionModel;
import org.keycloak.models.ClientModel;
import org.keycloak.models.Constants;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.models.UserSessionProvider;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.testsuite.model.KeycloakModelTest;
import org.keycloak.testsuite.model.RequireProvider;

import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.aMapWithSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.startsWith;

@RequireProvider(UserSessionProvider.class)
public class UserSessionConcurrencyTest extends KeycloakModelTest {

    private String realmId;
    private static final int CLIENTS_COUNT = 10;

    @Override
    public void createEnvironment(KeycloakSession s) {
        RealmModel realm = createRealm(s, "test");
        s.getContext().setRealm(realm);
        realm.setDefaultRole(s.roles().addRealmRole(realm, Constants.DEFAULT_ROLES_ROLE_PREFIX + "-" + realm.getName()));
        realm.setSsoSessionIdleTimeout(1800);
        realm.setSsoSessionMaxLifespan(36000);
        realm.setClientSessionIdleTimeout(500);
        this.realmId = realm.getId();

        s.users().addUser(realm, "user1").setEmail("user1@localhost");
        s.users().addUser(realm, "user2").setEmail("user2@localhost");

        for (int i = 0; i < CLIENTS_COUNT; i++) {
            s.clients().addClient(realm, "client" + i);
        }
    }

    @Override
    public void cleanEnvironment(KeycloakSession s) {
        RealmModel realm = s.realms().getRealm(realmId);
        s.getContext().setRealm(realm);
        s.realms().removeRealm(realmId);
    }

    @Override
    protected boolean isUseSameKeycloakSessionFactoryForAllThreads() {
        return true;
    }

    @Test
    public void testConcurrentNotesChange() throws InterruptedException {
        // Create user session
        String uId = withRealm(this.realmId, (session, realm) -> session.sessions().createUserSession(null, realm, session.users().getUserByUsername(realm, "user1"), "user1", "127.0.0.1", "form", true, null, null, UserSessionModel.SessionPersistenceState.PERSISTENT)).getId();

        // Create/Update client session's notes concurrently
        CountDownLatch cdl = new CountDownLatch(20 * CLIENTS_COUNT);
        IntStream.range(0, 20 * CLIENTS_COUNT).parallel()
                .forEach(i -> inComittedTransaction(i, (session, n) -> { try {
                    RealmModel realm = session.realms().getRealm(realmId);
                    session.getContext().setRealm(realm);
                    ClientModel client = realm.getClientByClientId("client" + (n % CLIENTS_COUNT));

                    UserSessionModel uSession = session.sessions().getUserSession(realm, uId);
                    AuthenticatedClientSessionModel cSession = uSession.getAuthenticatedClientSessionByClient(client.getId());
                    if (cSession == null) {
                        cSession = session.sessions().createClientSession(realm, client, uSession);
                    }

                    cSession.setNote(OIDCLoginProtocol.STATE_PARAM, "state-" + n);

                    return null;
                } finally {
                    cdl.countDown();
                }}));

        assertThat(cdl.await(10, TimeUnit.SECONDS), is(true));

        withRealm(this.realmId, (session, realm) -> {
            UserSessionModel uSession = session.sessions().getUserSession(realm, uId);
            assertThat(uSession.getAuthenticatedClientSessions(), aMapWithSize(CLIENTS_COUNT));

            for (int i = 0; i < CLIENTS_COUNT; i++) {
                ClientModel client = realm.getClientByClientId("client" + (i % CLIENTS_COUNT));
                AuthenticatedClientSessionModel cSession = uSession.getAuthenticatedClientSessionByClient(client.getId());

                assertThat(cSession.getNote(OIDCLoginProtocol.STATE_PARAM), startsWith("state-"));
            }

            return null;
        });

        inComittedTransaction(session -> {
            RealmModel realm = session.realms().getRealm(realmId);
            session.getContext().setRealm(realm);
            session.realms().removeRealm(realmId);
        });
    }
}

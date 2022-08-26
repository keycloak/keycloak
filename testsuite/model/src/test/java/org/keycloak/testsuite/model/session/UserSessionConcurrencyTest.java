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

import org.junit.Test;
import org.keycloak.models.AuthenticatedClientSessionModel;
import org.keycloak.models.ClientModel;
import org.keycloak.models.Constants;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakTransaction;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.models.UserSessionProvider;
import org.keycloak.models.map.storage.ModelEntityUtil;
import org.keycloak.models.map.storage.hotRod.HotRodMapStorageProviderFactory;
import org.keycloak.models.map.storage.hotRod.connections.HotRodConnectionProvider;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.testsuite.model.KeycloakModelTest;
import org.keycloak.testsuite.model.RequireProvider;

import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;
import java.util.stream.IntStream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.aMapWithSize;
import static org.hamcrest.Matchers.anEmptyMap;
import static org.hamcrest.Matchers.startsWith;
import static org.keycloak.utils.LockObjectsForModification.lockUserSessionsForModification;


@RequireProvider(UserSessionProvider.class)
public class UserSessionConcurrencyTest extends KeycloakModelTest {

    private String realmId;
    private static final int CLIENTS_COUNT = 10;

    private static final Lock SYNC_USESSION = new ReentrantLock();
    private static final ThreadLocal<Boolean> wasWriting = ThreadLocal.withInitial(() -> false);
    private final boolean isHotRodStore = HotRodMapStorageProviderFactory.PROVIDER_ID.equals(CONFIG.getConfig().get("userSessions.map.storage.provider"));

    @Override
    public void createEnvironment(KeycloakSession s) {
        RealmModel realm = s.realms().createRealm("test");
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
    protected boolean isUseSameKeycloakSessionFactoryForAllThreads() {
        return true;
    }

    @Test
    public void testConcurrentNotesChange() {
        // Create user session
        String uId = withRealm(this.realmId, (session, realm) -> session.sessions().createUserSession(realm, session.users().getUserByUsername(realm, "user1"), "user1", "127.0.0.1", "form", true, null, null)).getId();

        // Create/Update client session's notes concurrently
        IntStream.range(0, 200 * CLIENTS_COUNT).parallel()
                .forEach(i -> inComittedTransaction(i, (session, n) -> {
                    RealmModel realm = session.realms().getRealm(realmId);
                    ClientModel client = realm.getClientByClientId("client" + (n % CLIENTS_COUNT));

                    // THIS SHOULD BE REMOVED AS PART OF ISSUE https://github.com/keycloak/keycloak/issues/13273
                    // Without this lock more threads can create client session but only one of them is referenced from
                    // user session. All others that are not referenced are basically lost and should not be created.
                    // In other words, this lock is to make sure only one thread creates client session, all other
                    // should use client session created by the first thread
                    //
                    // This is basically the same as JpaMapKeycloakTransaction#read method is doing after calling lockUserSessionsForModification() method
                    if (isHotRodStore) {
                        SYNC_USESSION.lock();
                    }

                    UserSessionModel uSession = lockUserSessionsForModification(session, () -> session.sessions().getUserSession(realm, uId));
                    AuthenticatedClientSessionModel cSession = uSession.getAuthenticatedClientSessionByClient(client.getId());
                    if (cSession == null) {
                        wasWriting.set(true);
                        cSession = session.sessions().createClientSession(realm, client, uSession);
                    }

                    cSession.setNote(OIDCLoginProtocol.STATE_PARAM, "state-" + n);

                    if (isHotRodStore) {
                        releaseLockOnTransactionCommit(session, SYNC_USESSION);
                    }

                    return null;
                }));

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

        inComittedTransaction((Consumer<KeycloakSession>) session -> session.realms().removeRealm(realmId));
        if (isHotRodStore) {
            inComittedTransaction(session -> {
                HotRodConnectionProvider provider = session.getProvider(HotRodConnectionProvider.class);
                Map<?,?> remoteCache = provider.getRemoteCache(ModelEntityUtil.getModelName(UserSessionModel.class));

                assertThat(remoteCache, anEmptyMap());
            });
        }
    }

    private void releaseLockOnTransactionCommit(KeycloakSession session, Lock l) {
        session.getTransactionManager().enlistAfterCompletion(new KeycloakTransaction() {
            @Override
            public void begin() {

            }

            @Override
            public void commit() {
                // THIS IS WORKAROUND FOR MISSING https://github.com/keycloak/keycloak/issues/13280
                // It happens that calling remoteCache.put() in one thread and remoteCache.get() in another thread after
                // releasing the l lock is so fast that changes are not yet present in the Infinispan server, to avoid
                // this we need to leverage HotRod transactions that makes sure the changes are propagated to Infinispan
                // server in commit phase
                //
                // In other words, we need to give Infinispan some time to process put request before we let other
                // threads query client session created in this transaction
                if (isHotRodStore && wasWriting.get()) {
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                    wasWriting.set(false);
                }
                l.unlock();
            }

            @Override
            public void rollback() {
                l.unlock();
            }

            @Override
            public void setRollbackOnly() {

            }

            @Override
            public boolean getRollbackOnly() {
                return false;
            }

            @Override
            public boolean isActive() {
                return false;
            }
        });
    }
}

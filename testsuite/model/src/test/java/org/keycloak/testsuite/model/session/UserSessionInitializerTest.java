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

package org.keycloak.testsuite.model.session;

import org.infinispan.client.hotrod.RemoteCache;
import org.junit.Assert;
import org.junit.Test;
import org.keycloak.common.util.Time;
import org.keycloak.connections.infinispan.InfinispanConnectionProvider;
import org.keycloak.models.AuthenticatedClientSessionModel;
import org.keycloak.models.ClientModel;
import org.keycloak.models.Constants;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RealmProvider;
import org.keycloak.models.UserManager;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserProvider;
import org.keycloak.models.UserSessionModel;
import org.keycloak.models.UserSessionProvider;
import org.keycloak.models.session.UserSessionPersisterProvider;
import org.keycloak.services.managers.UserSessionManager;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicReferenceArray;
import java.util.stream.Collectors;

import org.keycloak.testsuite.model.KeycloakModelTest;
import org.keycloak.testsuite.model.RequireProvider;
import org.keycloak.testsuite.util.HotRodServerRule;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 * @author <a href="mailto:mabartos@redhat.com">Martin Bartos</a>
 * @author <a href="mailto:mkanis@redhat.com">Martin Kanis</a>
 */
@RequireProvider(UserSessionPersisterProvider.class)
@RequireProvider(UserSessionProvider.class)
@RequireProvider(UserProvider.class)
@RequireProvider(RealmProvider.class)
public class UserSessionInitializerTest extends KeycloakModelTest {

    private String realmId;

    @Override
    public void createEnvironment(KeycloakSession s) {
        RealmModel realm = s.realms().createRealm("test");
        realm.setOfflineSessionIdleTimeout(Constants.DEFAULT_OFFLINE_SESSION_IDLE_TIMEOUT);
        realm.setDefaultRole(s.roles().addRealmRole(realm, Constants.DEFAULT_ROLES_ROLE_PREFIX + "-" + realm.getName()));
        realm.setSsoSessionIdleTimeout(1800);
        realm.setSsoSessionMaxLifespan(36000);
        this.realmId = realm.getId();

        s.users().addUser(realm, "user1").setEmail("user1@localhost");
        s.users().addUser(realm, "user2").setEmail("user2@localhost");

        UserSessionPersisterProviderTest.createClients(s, realm);
    }

    @Override
    public void cleanEnvironment(KeycloakSession s) {
        RealmModel realm = s.realms().getRealm(realmId);
        s.sessions().removeUserSessions(realm);

        UserModel user1 = s.users().getUserByUsername(realm, "user1");
        UserModel user2 = s.users().getUserByUsername(realm, "user2");

        UserManager um = new UserManager(s);
        if (user1 != null) {
            um.removeUser(realm, user1);
        }
        if (user2 != null) {
            um.removeUser(realm, user2);
        }

        s.realms().removeRealm(realmId);
    }

    @Test
    public void testUserSessionInitializer() {
        int started = Time.currentTime();
        AtomicReferenceArray<String> origSessionIds = createSessionsInPersisterOnly();

        inComittedTransaction(session -> {
            RealmModel realm = session.realms().getRealm(realmId);

            // Assert sessions are in
            ClientModel testApp = realm.getClientByClientId("test-app");
            ClientModel thirdparty = realm.getClientByClientId("third-party");

            assertThat("Count of offline sesions for client 'test-app'", session.sessions().getOfflineSessionsCount(realm, testApp), is((long) 3));
            assertThat("Count of offline sesions for client 'third-party'", session.sessions().getOfflineSessionsCount(realm, thirdparty), is((long) 1));

            List<UserSessionModel> loadedSessions = session.sessions().getOfflineUserSessionsStream(realm, testApp, 0, 10)
                    .collect(Collectors.toList());

            assertSessionLoaded(loadedSessions, origSessionIds.get(0), session.users().getUserByUsername(realm, "user1"), "127.0.0.1", started, started, "test-app", "third-party");
            assertSessionLoaded(loadedSessions, origSessionIds.get(1), session.users().getUserByUsername(realm, "user1"), "127.0.0.2", started, started, "test-app");
            assertSessionLoaded(loadedSessions, origSessionIds.get(2), session.users().getUserByUsername(realm, "user2"), "127.0.0.3", started, started, "test-app");
        });
    }

    @Test
    public void testUserSessionInitializerWithDeletingClient() {
        int started = Time.currentTime();
        AtomicReferenceArray<String> origSessionIds = createSessionsInPersisterOnly();

        inComittedTransaction(session -> {
            RealmModel realm = session.realms().getRealm(realmId);

            // Delete one of the clients now
            ClientModel testApp = realm.getClientByClientId("test-app");
            realm.removeClient(testApp.getId());
        });

        reinitializeKeycloakSessionFactory();

        inComittedTransaction(session -> {
            RealmModel realm = session.realms().getRealm(realmId);

            // Assert sessions are in
            ClientModel thirdparty = realm.getClientByClientId("third-party");

            assertThat("Count of offline sesions for client 'third-party'", session.sessions().getOfflineSessionsCount(realm, thirdparty), is((long) 1));
            List<UserSessionModel> loadedSessions = session.sessions().getOfflineUserSessionsStream(realm, thirdparty, 0, 10)
                    .collect(Collectors.toList());

            assertThat("Size of loaded Sessions", loadedSessions.size(), is(1));
            assertSessionLoaded(loadedSessions, origSessionIds.get(0), session.users().getUserByUsername(realm, "user1"), "127.0.0.1", started, started, "third-party");

            // Revert client
            realm.addClient("test-app");
        });

    }

    @Test
    public void testUserSessionPropagationBetweenSites() throws InterruptedException {
        AtomicInteger index = new AtomicInteger();
        AtomicReference<String> siteName = new AtomicReference<>();
        AtomicReference<String> userSessionId = new AtomicReference<>();

        Object lock = new Object();

        Optional<HotRodServerRule> hotRodServerRule = getParameters(HotRodServerRule.class).findFirst();

        inIndependentFactories(4, 30, () -> {
            synchronized (lock) {
                if (index.incrementAndGet() == 1) {
                    // create a user session in the first node
                    UserSessionModel userSessionModel = withRealm(realmId, (session, realm) -> {
                        final UserModel user = session.users().getUserById(realm, "user1");
                        return session.sessions().createUserSession(realm, user, "un1", "ip1", "auth", false, null, null);
                    });
                    userSessionId.set(userSessionModel.getId());

                    // remove the session from the remote cache in case of crossDC setup
                    if (hotRodServerRule.isPresent()) {
                        RemoteCache<Object, Object> sessions = hotRodServerRule.get().getRemoteCacheManager().getCache(InfinispanConnectionProvider.USER_SESSION_CACHE_NAME);
                        sessions.clear();
                    }

                    // make sure the user session is still present in the local cache
                    UserSessionModel userSession = withRealm(realmId, (session, realm) -> session.sessions().getUserSession(realm, userSessionId.get()));
                    Assert.assertEquals(userSessionModel, userSession);

                    siteName.set(CONFIG.scope("connectionsInfinispan", "default").get("siteName", "site-1"));
                }
                else {
                    // try to get the user session at other nodes
                    UserSessionModel userSession = withRealm(realmId, (session, realm) -> session.sessions().getUserSession(realm, userSessionId.get()));

                    if (siteName.get().equals(CONFIG.scope("connectionsInfinispan", "default").get("siteName", "site-1"))) {
                        // the user session should be present in the local cache in nodes that belong to same site as the node which created the session
                        Assert.assertNotNull(userSession);
                        Assert.assertEquals(userSessionId.get(), userSession.getId());
                    } else {
                        Assert.assertNull(userSession);
                    }
                }
            }
        });
    }

    // Create sessions in persister + infinispan, but then delete them from infinispan cache by reinitializing keycloak session factory
    private AtomicReferenceArray createSessionsInPersisterOnly() {
        UserSessionModel[] origSessions = inComittedTransaction(session -> { return UserSessionPersisterProviderTest.createSessions(session, realmId); });
        AtomicReferenceArray<String> res = new AtomicReferenceArray(origSessions.length);

        inComittedTransaction(session -> {
            RealmModel realm = session.realms().getRealm(realmId);
            UserSessionManager sessionManager = new UserSessionManager(session);

            int i = 0;
            for (UserSessionModel origSession : origSessions) {
                UserSessionModel userSession = session.sessions().getUserSession(realm, origSession.getId());
                for (AuthenticatedClientSessionModel clientSession : userSession.getAuthenticatedClientSessions().values()) {
                    sessionManager.createOrUpdateOfflineSession(clientSession, userSession);
                }
                String cs = userSession.getNote(UserSessionModel.CORRESPONDING_SESSION_ID);
                res.set(i++, cs == null ? userSession.getId() : cs);
            }
        });

        reinitializeKeycloakSessionFactory();

        return res;
    }

    private void assertSessionLoaded(List<UserSessionModel> sessions, String id, UserModel user, String ipAddress, int started, int lastRefresh, String... clients) {
        for (UserSessionModel session : sessions) {
            if (session.getId().equals(id)) {
                UserSessionPersisterProviderTest.assertSession(session, user, ipAddress, started, lastRefresh, clients);
                return;
            }
        }
        Assert.fail("Session with ID " + id + " not found in the list");
    }
}


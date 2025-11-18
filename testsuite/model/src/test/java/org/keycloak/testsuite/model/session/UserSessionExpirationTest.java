/*
 * Copyright 2025 Red Hat, Inc. and/or its affiliates
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

import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.keycloak.common.util.Time;
import org.keycloak.connections.infinispan.InfinispanConnectionProvider;
import org.keycloak.events.Details;
import org.keycloak.events.Event;
import org.keycloak.events.EventStoreProvider;
import org.keycloak.events.EventType;
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
import org.keycloak.testsuite.model.HotRodServerRule;
import org.keycloak.testsuite.model.KeycloakModelTest;
import org.keycloak.testsuite.model.RequireProvider;

import org.awaitility.Awaitility;
import org.infinispan.Cache;
import org.junit.Assert;
import org.junit.Test;

import static org.keycloak.connections.infinispan.InfinispanConnectionProvider.OFFLINE_USER_SESSION_CACHE_NAME;
import static org.keycloak.connections.infinispan.InfinispanConnectionProvider.USER_SESSION_CACHE_NAME;
import static org.keycloak.models.utils.SessionTimeoutHelper.PERIODIC_CLEANER_IDLE_TIMEOUT_WINDOW_SECONDS;
import static org.keycloak.testsuite.model.session.UserSessionPersisterProviderTest.createClients;
import static org.keycloak.testsuite.model.session.UserSessionPersisterProviderTest.createSessions;

@RequireProvider(UserSessionPersisterProvider.class)
@RequireProvider(UserSessionProvider.class)
@RequireProvider(UserProvider.class)
@RequireProvider(RealmProvider.class)
@RequireProvider(EventStoreProvider.class)
public class UserSessionExpirationTest extends KeycloakModelTest {

    private static final int IDLE_TIMEOUT = 1800;
    private static final int LIFESPAN_TIMEOUT = 36000;

    private String realmId;

    @Override
    public void createEnvironment(KeycloakSession s) {
        RealmModel realm = createRealm(s, "test");
        realm.setEventsEnabled(true);
        realm.setEnabledEventTypes(Set.of(EventType.USER_SESSION_DELETED.name()));
        s.getContext().setRealm(realm);
        realm.setDefaultRole(s.roles().addRealmRole(realm, Constants.DEFAULT_ROLES_ROLE_PREFIX + "-" + realm.getName()));
        realm.setSsoSessionIdleTimeout(IDLE_TIMEOUT);
        realm.setSsoSessionMaxLifespan(LIFESPAN_TIMEOUT);
        this.realmId = realm.getId();

        s.users().addUser(realm, "user1").setEmail("user1@localhost");
        s.users().addUser(realm, "user2").setEmail("user2@localhost");

        createClients(s, realm);
    }

    @Override
    public void cleanEnvironment(KeycloakSession s) {
        RealmModel realm = s.realms().getRealm(realmId);
        s.getContext().setRealm(realm);
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
    public void testExpirationEvents() {
        UserSessionModel[] userSessions = inComittedTransaction(session -> {
            return createSessions(session, realmId, false);
        });
        Map<String, String> sessionIdAndUsers = Arrays.stream(userSessions)
                .collect(Collectors.toUnmodifiableMap(UserSessionModel::getId, s -> s.getUser().getId()));

        withRealmConsumer(realmId, (session, realm) -> {
            // Time offset is automatically cleaned up in KeycloakModelTest.cleanEnvironment()
            Time.setOffset(IDLE_TIMEOUT + PERIODIC_CLEANER_IDLE_TIMEOUT_WINDOW_SECONDS + 10);
            session.getProvider(UserSessionPersisterProvider.class).removeExpired(realm);

            var hotRodServer = getParameters(HotRodServerRule.class).findFirst();
            if (hotRodServer.isEmpty()) {
                InfinispanConnectionProvider provider = session.getProvider(InfinispanConnectionProvider.class);
                processExpiration(provider.getCache(USER_SESSION_CACHE_NAME));
                processExpiration(provider.getCache(OFFLINE_USER_SESSION_CACHE_NAME));
            } else {
                hotRodServer.get().streamCacheManagers()
                        .forEach(cacheManager -> {
                            processExpiration(cacheManager.getCache(USER_SESSION_CACHE_NAME));
                            processExpiration(cacheManager.getCache(OFFLINE_USER_SESSION_CACHE_NAME));
                        });
            }

            // Infinispan events are async, let's ensure it is stored in the database before proceed.
            Awaitility.await().until(() -> eventsCount(session) == sessionIdAndUsers.size());
        });

        withRealmConsumer(realmId, (session, realm) -> {
            // user session id -> user id
            Map<String, String> eventsData = events(session);
            Assert.assertEquals(sessionIdAndUsers, eventsData);
        });
    }

    private static void processExpiration(Cache<?, ?> cache) {
        cache.getAdvancedCache().getExpirationManager().processExpiration();
    }

    private static long eventsCount(KeycloakSession session) {
        EventStoreProvider provider = session.getProvider(EventStoreProvider.class);
        return provider.createQuery()
                .type(EventType.USER_SESSION_DELETED)
                .getResultStream()
                .filter(event -> Details.EXPIRED_DETAIL.equals(event.getDetails().get(Details.REASON)))
                .count();
    }

    private static Map<String, String> events(KeycloakSession session) {
        EventStoreProvider provider = session.getProvider(EventStoreProvider.class);
        return provider.createQuery()
                .type(EventType.USER_SESSION_DELETED)
                .getResultStream()
                .filter(event -> Details.EXPIRED_DETAIL.equals(event.getDetails().get(Details.REASON)))
                .collect(Collectors.toUnmodifiableMap(Event::getSessionId, Event::getUserId));
    }

}

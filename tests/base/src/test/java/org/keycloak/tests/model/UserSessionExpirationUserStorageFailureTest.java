/*
 * Copyright 2026 Red Hat, Inc. and/or its affiliates
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

import org.keycloak.common.util.Time;
import org.keycloak.component.ComponentModel;
import org.keycloak.connections.infinispan.InfinispanConnectionProvider;
import org.keycloak.events.Details;
import org.keycloak.events.Event;
import org.keycloak.events.EventStoreProvider;
import org.keycloak.events.EventType;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.models.session.UserSessionPersisterProvider;
import org.keycloak.models.sessions.infinispan.listeners.EmbeddedUserSessionExpirationListener;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.models.utils.ResetTimeOffsetEvent;
import org.keycloak.storage.UserStorageProvider;
import org.keycloak.storage.UserStorageProviderModel;
import org.keycloak.storage.UserStorageUtil;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.injection.LifeCycle;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.RealmBuilder;
import org.keycloak.testframework.realm.RealmConfig;
import org.keycloak.testframework.remote.annotations.TestOnServer;
import org.keycloak.testframework.remote.providers.timeoffset.InfinispanTimeUtil;
import org.keycloak.tests.providers.federation.RollbackOnlyUserStorageProviderFactory;

import org.awaitility.Awaitility;
import org.infinispan.util.concurrent.BlockingManager;

import static org.keycloak.models.utils.SessionTimeoutHelper.PERIODIC_CLEANER_IDLE_TIMEOUT_WINDOW_SECONDS;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@KeycloakIntegrationTest(config = CustomProvidersServerConfig.class)
public class UserSessionExpirationUserStorageFailureTest {

    private static final String REALM_NAME = "user-storage-failure";
    private static final int IDLE_TIMEOUT = 1800;
    private static final int LIFESPAN_TIMEOUT = 36000;

    @InjectRealm(config = ExpirationRealmConfig.class, lifecycle = LifeCycle.METHOD)
    ManagedRealm realm;

    @TestOnServer
    public void persistentSessionCleanupSurvivesRollbackOnlyUserLookup(KeycloakSession session) {
        KeycloakSessionFactory factory = session.getKeycloakSessionFactory();
        InfinispanTimeUtil.enableTestingTimeService(session);
        TestEnvironment environment = null;
        try {
            environment = createEnvironment(factory);
            String userSessionId = createPersistentSession(factory, environment);
            setLookupFailure(factory, environment, true);
            TestEnvironment finalEnvironment = environment;

            Time.setOffset(LIFESPAN_TIMEOUT + PERIODIC_CLEANER_IDLE_TIMEOUT_WINDOW_SECONDS + 10);
            KeycloakModelUtils.runJobInTransaction(factory, currentSession -> {
                RealmModel realm = currentSession.realms().getRealm(finalEnvironment.realmId());
                currentSession.getContext().setRealm(realm);
                currentSession.getProvider(UserSessionPersisterProvider.class).removeExpired(realm);
            });

            KeycloakModelUtils.runJobInTransaction(factory, currentSession -> {
                RealmModel realm = currentSession.realms().getRealm(finalEnvironment.realmId());
                currentSession.getContext().setRealm(realm);
                assertNull(currentSession.getProvider(UserSessionPersisterProvider.class)
                        .loadUserSession(realm, userSessionId, false));
                assertExpirationEventWithoutUsername(currentSession, finalEnvironment.userId(), userSessionId);
            });
            assertTrue(RollbackOnlyUserStorageProviderFactory.getRollbackOnlyFailures() > 0);
        } finally {
            reset(factory, session, environment);
        }
    }

    @TestOnServer
    public void infinispanExpirationEventSurvivesRollbackOnlyUserLookup(KeycloakSession session) {
        KeycloakSessionFactory factory = session.getKeycloakSessionFactory();
        TestEnvironment environment = null;
        try {
            environment = createEnvironment(factory);
            setLookupFailure(factory, environment, true);
            String userSessionId = KeycloakModelUtils.generateId();

            BlockingManager blockingManager = session.getProvider(InfinispanConnectionProvider.class).getBlockingManager();
            TestUserSessionExpirationListener listener = new TestUserSessionExpirationListener(factory, blockingManager);
            listener.sendExpirationEventForTest(userSessionId, environment.userId(), environment.realmId());

            TestEnvironment finalEnvironment = environment;
            Awaitility.await().until(() -> KeycloakModelUtils.runJobInTransactionWithResult(factory, currentSession -> {
                RealmModel realm = currentSession.realms().getRealm(finalEnvironment.realmId());
                currentSession.getContext().setRealm(realm);
                return findExpirationEvent(currentSession, userSessionId) != null;
            }));
            KeycloakModelUtils.runJobInTransaction(factory, currentSession -> {
                RealmModel realm = currentSession.realms().getRealm(finalEnvironment.realmId());
                currentSession.getContext().setRealm(realm);
                assertExpirationEventWithoutUsername(currentSession, finalEnvironment.userId(), userSessionId);
            });
            assertTrue(RollbackOnlyUserStorageProviderFactory.getRollbackOnlyFailures() > 0);
        } finally {
            reset(factory, session, environment);
        }
    }

    private static TestEnvironment createEnvironment(KeycloakSessionFactory factory) {
        RollbackOnlyUserStorageProviderFactory.resetRollbackOnlyFailures();
        return KeycloakModelUtils.runJobInTransactionWithResult(factory, session -> {
            RealmModel realm = session.realms().getRealmByName(REALM_NAME);
            session.getContext().setRealm(realm);

            UserStorageProviderModel provider = new UserStorageProviderModel();
            provider.setName("failing-user-storage");
            provider.setParentId(realm.getId());
            provider.setProviderId(RollbackOnlyUserStorageProviderFactory.PROVIDER_ID);
            provider.setProviderType(UserStorageProvider.class.getName());
            String providerId = realm.addComponentModel(provider).getId();

            UserModel user = session.users().getUserByUsername(realm, RollbackOnlyUserStorageProviderFactory.USERNAME);
            return new TestEnvironment(realm.getId(), providerId, user.getId());
        });
    }

    private static String createPersistentSession(KeycloakSessionFactory factory, TestEnvironment environment) {
        return KeycloakModelUtils.runJobInTransactionWithResult(factory, session -> {
            RealmModel realm = session.realms().getRealm(environment.realmId());
            session.getContext().setRealm(realm);
            UserModel user = session.users().getUserByUsername(realm, RollbackOnlyUserStorageProviderFactory.USERNAME);
            assertEquals(environment.userId(), user.getId());
            return session.sessions().createUserSession(null, realm, user, user.getUsername(), "127.0.0.1", "form",
                    false, null, null, UserSessionModel.SessionPersistenceState.PERSISTENT).getId();
        });
    }

    private static void setLookupFailure(KeycloakSessionFactory factory, TestEnvironment environment, boolean enabled) {
        KeycloakModelUtils.runJobInTransaction(factory, session -> {
            RealmModel realm = session.realms().getRealm(environment.realmId());
            session.getContext().setRealm(realm);
            ComponentModel provider = realm.getComponent(environment.providerId());
            provider.getConfig().putSingle("fail", Boolean.toString(enabled));
            realm.updateComponent(provider);
            UserStorageUtil.userCache(session).evict(realm);
        });
    }

    private static void reset(KeycloakSessionFactory factory, KeycloakSession session, TestEnvironment environment) {
        Time.setOffset(0);
        factory.publish(new ResetTimeOffsetEvent());
        InfinispanTimeUtil.disableTestingTimeService(session);
        if (environment != null) {
            setLookupFailure(factory, environment, false);
        }
    }

    private static void assertExpirationEventWithoutUsername(KeycloakSession session, String userId, String userSessionId) {
        Event event = findExpirationEvent(session, userSessionId);
        assertNotNull(event);
        assertEquals(userId, event.getUserId());
        assertNull(event.getDetails().get(Details.USERNAME));
    }

    private static Event findExpirationEvent(KeycloakSession session, String userSessionId) {
        return session.getProvider(EventStoreProvider.class).createQuery()
                .type(EventType.USER_SESSION_DELETED)
                .getResultStream()
                .filter(event -> userSessionId.equals(event.getSessionId()))
                .filter(event -> Details.USER_SESSION_EXPIRED_REASON.equals(event.getDetails().get(Details.REASON)))
                .findFirst()
                .orElse(null);
    }

    private record TestEnvironment(String realmId, String providerId, String userId) {
    }

    private static final class TestUserSessionExpirationListener extends EmbeddedUserSessionExpirationListener {

        private TestUserSessionExpirationListener(KeycloakSessionFactory factory, BlockingManager blockingManager) {
            super(factory, blockingManager);
        }

        private void sendExpirationEventForTest(String userSessionId, String userId, String realmId) {
            sendExpirationEvent(userSessionId, userId, realmId);
        }
    }

    private static final class ExpirationRealmConfig implements RealmConfig {

        @Override
        public RealmBuilder configure(RealmBuilder realm) {
            return realm.name(REALM_NAME)
                    .eventsEnabled(true)
                    .enabledEventTypes(EventType.USER_SESSION_DELETED.name())
                    .ssoSessionIdleTimeout(IDLE_TIMEOUT)
                    .ssoSessionMaxLifespan(LIFESPAN_TIMEOUT);
        }
    }
}

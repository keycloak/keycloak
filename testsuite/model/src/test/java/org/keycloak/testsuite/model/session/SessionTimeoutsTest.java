/*
 * Copyright 2023 Red Hat, Inc. and/or its affiliates
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

import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.keycloak.common.util.MultiSiteUtils;
import org.keycloak.common.util.Retry;
import org.keycloak.common.util.Time;
import org.keycloak.connections.infinispan.InfinispanConnectionProvider;
import org.keycloak.infinispan.util.InfinispanUtils;
import org.keycloak.models.AuthenticatedClientSessionModel;
import org.keycloak.models.ClientModel;
import org.keycloak.models.Constants;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RealmProvider;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserProvider;
import org.keycloak.models.UserSessionModel;
import org.keycloak.models.UserSessionProvider;
import org.keycloak.models.session.UserSessionPersisterProvider;
import org.keycloak.protocol.oidc.OIDCConfigAttributes;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.services.managers.RealmManager;
import org.keycloak.testsuite.model.HotRodServerRule;
import org.keycloak.testsuite.model.KeycloakModelTest;
import org.keycloak.testsuite.model.RequireProvider;
import org.keycloak.testsuite.model.infinispan.InfinispanTestUtil;

import org.junit.Assert;
import org.junit.Assume;
import org.junit.ClassRule;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.junit.runners.MethodSorters;

/**
 * <p>
 * Test that checks the Infinispan user session provider expires the sessions
 * correctly and does not remain client sessions in memory after user session
 * expiration.</p>
 *
 * @author rmartinc
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@RequireProvider(UserSessionProvider.class)
@RequireProvider(UserProvider.class)
@RequireProvider(RealmProvider.class)
public class SessionTimeoutsTest extends KeycloakModelTest {

    @ClassRule
    public static final TestRule SKIPPED_PROFILES = (base, description) -> {
        // Embedded caches with the Remote Store uses asynchronous code with event listeners, making the test failing randomly.
        // It is a deprecated configuration for cross-site, so we skip the tests.
        Assume.assumeFalse(InfinispanUtils.isEmbeddedInfinispan() && getParameters(HotRodServerRule.class).findFirst().isPresent());
        return base;
    };

    private String realmId;

    @Override
    public void createEnvironment(KeycloakSession s) {
        super.createEnvironment(s);

        RealmModel realm = createRealm(s, "test");
        s.getContext().setRealm(realm);
        realm.setDefaultRole(s.roles().addRealmRole(realm, Constants.DEFAULT_ROLES_ROLE_PREFIX + "-" + realm.getName()));
        this.realmId = realm.getId();

        s.users().addUser(realm, "user1").setEmail("user1@localhost");

        s.clients().addClient(realm, "test-app");
        InfinispanTestUtil.setTestingTimeService(s);
    }

    @Override
    public void cleanEnvironment(KeycloakSession s) {
        InfinispanTestUtil.revertTimeService(s);
        RealmModel realm = s.realms().getRealm(realmId);
        s.getContext().setRealm(realm);
        new RealmManager(s).removeRealm(realm);

        super.cleanEnvironment(s);
    }

    protected static UserSessionModel createUserSession(KeycloakSession session, RealmModel realm, UserModel user, boolean offline) {
        UserSessionModel userSession = session.sessions().createUserSession(UUID.randomUUID().toString(), realm, user, "user1", "127.0.0.1",
                "form", true, null, null, UserSessionModel.SessionPersistenceState.PERSISTENT);
        if (offline) {
            userSession = session.sessions().createOfflineUserSession(userSession);
        }
        return userSession;
    }

    protected static AuthenticatedClientSessionModel createClientSession(KeycloakSession session, String realmId, ClientModel client,
            UserSessionModel userSession, String redirect, String state) {
        RealmModel realm = session.realms().getRealm(realmId);
        AuthenticatedClientSessionModel clientSession = session.sessions().createClientSession(realm, client, userSession);
        if (userSession.isOffline()) {
            clientSession = session.sessions().createOfflineClientSession(clientSession, userSession);
        }
        clientSession.setRedirectUri(redirect);
        if (state != null) {
            clientSession.setNote(OIDCLoginProtocol.STATE_PARAM, state);
        }
        return clientSession;
    }

    protected static UserSessionModel getUserSession(KeycloakSession session, RealmModel realm, String id, boolean offline) {
        return offline
                ? session.sessions().getOfflineUserSession(realm, id)
                : session.sessions().getUserSession(realm, id);
    }

    protected void configureTimeouts(int realmMaxLifespan, int realmIdleTimeout, boolean overrideInClient, boolean lifespan, int clientValue) {
        withRealm(realmId, (session, realm) -> {
            realm.setOfflineSessionMaxLifespanEnabled(true);
            realm.setOfflineSessionMaxLifespan(realmMaxLifespan);
            realm.setOfflineSessionIdleTimeout(realmIdleTimeout);
            realm.setClientOfflineSessionMaxLifespan(realmMaxLifespan);
            realm.setClientOfflineSessionIdleTimeout(realmIdleTimeout);
            realm.setSsoSessionMaxLifespan(realmMaxLifespan);
            realm.setSsoSessionIdleTimeout(realmIdleTimeout);
            realm.setClientSessionMaxLifespan(realmMaxLifespan);
            realm.setClientSessionIdleTimeout(realmIdleTimeout);
            String clientValueString = Integer.toString(clientValue);

            ClientModel client = realm.getClientByClientId("test-app");
            client.removeAttribute(OIDCConfigAttributes.CLIENT_OFFLINE_SESSION_MAX_LIFESPAN);
            client.removeAttribute(OIDCConfigAttributes.CLIENT_OFFLINE_SESSION_IDLE_TIMEOUT);
            client.removeAttribute(OIDCConfigAttributes.CLIENT_SESSION_MAX_LIFESPAN);
            client.removeAttribute(OIDCConfigAttributes.CLIENT_SESSION_IDLE_TIMEOUT);

            if (overrideInClient) {
                if (lifespan) {
                    client.setAttribute(OIDCConfigAttributes.CLIENT_OFFLINE_SESSION_MAX_LIFESPAN, clientValueString);
                    client.setAttribute(OIDCConfigAttributes.CLIENT_SESSION_MAX_LIFESPAN, clientValueString);
                } else {
                    client.setAttribute(OIDCConfigAttributes.CLIENT_OFFLINE_SESSION_IDLE_TIMEOUT, clientValueString);
                    client.setAttribute(OIDCConfigAttributes.CLIENT_SESSION_IDLE_TIMEOUT, clientValueString);
                }
            } else {
                if (lifespan) {
                    realm.setClientOfflineSessionMaxLifespan(clientValue);
                    realm.setClientSessionMaxLifespan(clientValue);
                } else {
                    realm.setClientOfflineSessionIdleTimeout(clientValue);
                    realm.setClientSessionIdleTimeout(clientValue);
                }
            }
            return null;
        });
    }

    protected void testUserClientMaxLifespanSmallerThanSession(boolean offline, boolean overrideInClient) {
        configureTimeouts(3000, 7200, overrideInClient, true, 2000);

        try {
            final String[] sessions = inComittedTransaction(session -> {
                RealmModel realm = session.realms().getRealm(realmId);
                session.getContext().setRealm(realm);

                UserModel user = session.users().getUserByUsername(realm, "user1");
                UserSessionModel userSession = createUserSession(session, realm, user, offline);
                Assert.assertEquals(offline, userSession.isOffline());
                AuthenticatedClientSessionModel clientSession = createClientSession(session, realmId, realm.getClientByClientId("test-app"), userSession, "http://redirect", "state");
                return new String[]{userSession.getId(), clientSession.getId()};
            });

            setTimeOffset(1000);

            withRealm(realmId, (session, realm) -> {
                // check the sessions are created
                ClientModel client = realm.getClientByClientId("test-app");
                UserSessionModel userSession = getUserSession(session, realm, sessions[0], offline);
                Assert.assertNotNull(userSession);
                Assert.assertNotNull(userSession.getAuthenticatedClientSessionByClient(client.getId()));
                return null;
            });

            setTimeOffset(2100);

            sessions[1] = withRealm(realmId, (session, realm) -> {
                // refresh sessions after 2000 => only user session should exist
                session.getContext().setRealm(realm);
                ClientModel client = realm.getClientByClientId("test-app");
                UserSessionModel userSession = getUserSession(session, realm, sessions[0], offline);
                Assert.assertNotNull(userSession);
                Assert.assertNull(userSession.getAuthenticatedClientSessionByClient(client.getId()));
                // recreate client session
                AuthenticatedClientSessionModel clientSession = createClientSession(session, realmId, realm.getClientByClientId("test-app"), userSession, "http://redirect", "state");
                return clientSession.getId();
            });

            setTimeOffset(2500);

            withRealm(realmId, (session, realm) -> {
                // check the sessions are created
                ClientModel client = realm.getClientByClientId("test-app");
                UserSessionModel userSession = getUserSession(session, realm, sessions[0], offline);
                Assert.assertNotNull(userSession);
                Assert.assertNotNull(userSession.getAuthenticatedClientSessionByClient(client.getId()));
                return null;
            });

            setTimeOffset(3100);

            withRealm(realmId, (session, realm) -> {
                // ensure user session is expired after user session expiration
                Assert.assertNull(getUserSession(session, realm, sessions[0], offline));
                return null;
            });
        } finally {
            setTimeOffset(0);
        }
    }

    protected void testUserClientMaxLifespanGreaterThanSession(boolean offline, boolean overrideInClient) {
        configureTimeouts(3000, 7200, overrideInClient, true, 5000);

        try {
            final String[] sessions = withRealm(realmId, (session, realm) -> {
                UserModel user = session.users().getUserByUsername(realm, "user1");
                UserSessionModel userSession = createUserSession(session, realm, user, offline);
                Assert.assertEquals(offline, userSession.isOffline());
                AuthenticatedClientSessionModel clientSession = createClientSession(session, realmId, realm.getClientByClientId("test-app"), userSession, "http://redirect", "state");
                return new String[]{userSession.getId(), clientSession.getId()};
            });

            setTimeOffset(2000);

            withRealm(realmId, (session, realm) -> {
                // check the sessions are created
                ClientModel client = realm.getClientByClientId("test-app");
                UserSessionModel userSession = getUserSession(session, realm, sessions[0], offline);
                Assert.assertNotNull(userSession);
                Assert.assertNotNull(userSession.getAuthenticatedClientSessionByClient(client.getId()));
                return null;
            });

            setTimeOffset(3100);

            withRealm(realmId, (session, realm) -> {
                // ensure user session is expired after user session expiration
                Assert.assertNull(getUserSession(session, realm, sessions[0], offline));
                return null;
            });
        } finally {
            setTimeOffset(0);
        }
    }

    protected void testUserClientIdleTimeoutSmallerThanSession(int refreshTimes, boolean offline, boolean overrideInClient) {
        configureTimeouts(7200, 3000, overrideInClient, false, 2000);

        try {
            final String[] sessions = withRealm(realmId, (session, realm) -> {
                UserModel user = session.users().getUserByUsername(realm, "user1");
                UserSessionModel userSession = createUserSession(session, realm, user, offline);
                Assert.assertEquals(offline, userSession.isOffline());
                AuthenticatedClientSessionModel clientSession = createClientSession(session, realmId, realm.getClientByClientId("test-app"), userSession, "http://redirect", "state");
                return new String[]{userSession.getId(), clientSession.getId()};
            });

            allowXSiteReplication(offline);

            int offset = 0;
            for (int i = 0; i < refreshTimes; i++) {
                offset += 1500;
                setTimeOffset(offset);
                int time = Time.currentTime();
                withRealm(realmId, (session, realm) -> {
                    // refresh sessions before user session expires => both session should exist
                    ClientModel client = realm.getClientByClientId("test-app");
                    UserSessionModel userSession = getUserSession(session, realm, sessions[0], offline);
                    Assert.assertNotNull(userSession);
                    AuthenticatedClientSessionModel clientSession = userSession.getAuthenticatedClientSessionByClient(client.getId());
                    Assert.assertNotNull(clientSession);
                    userSession.setLastSessionRefresh(time);
                    clientSession.setTimestamp(time);
                    return null;
                });

                if (MultiSiteUtils.isPersistentSessionsEnabled()) {
                    // The persistent session will write the update data asynchronously, wait for it to arrive.
                    Retry.executeWithBackoff(iteration -> {
                        withRealm(realmId, (session, realm) -> {
                            UserSessionPersisterProvider provider = session.getProvider(UserSessionPersisterProvider.class);
                            UserSessionModel userSessionModel = provider.loadUserSession(realm, sessions[0], offline);
                            Assert.assertNotNull(userSessionModel);
                            Assert.assertEquals(userSessionModel.getLastSessionRefresh(), time);

                            // refresh sessions before user session expires => both session should exist
                            ClientModel client = realm.getClientByClientId("test-app");
                            AuthenticatedClientSessionModel clientSession = userSessionModel.getAuthenticatedClientSessionByClient(client.getId());
                            Assert.assertNotNull(clientSession);
                            Assert.assertEquals(clientSession.getTimestamp(), time);
                            return null;
                        });
                    }, 10, 10);
                }
            }

            offset += 2100;
            setTimeOffset(offset);
            sessions[1] = withRealm(realmId, (session, realm) -> {
                // refresh sessions after 2000 => only user session should exist, client should be expired by idle
                session.getContext().setRealm(realm);
                ClientModel client = realm.getClientByClientId("test-app");
                UserSessionModel userSession = getUserSession(session, realm, sessions[0], offline);
                Assert.assertNotNull(userSession);
                Assert.assertNull(userSession.getAuthenticatedClientSessionByClient(client.getId()));
                // recreate client session
                AuthenticatedClientSessionModel clientSession = createClientSession(session, realmId, realm.getClientByClientId("test-app"), userSession, "http://redirect", "state");
                return clientSession.getId();
            });

            offset += 3100;
            setTimeOffset(offset);
            withRealm(realmId, (session, realm) -> {
                // ensure user session is expired after user session expiration
                Assert.assertNull(getUserSession(session, realm, sessions[0], offline));
                return null;
            });
            processExpiration(true);
            processExpiration(false);
        } finally {
            setTimeOffset(0);
        }
    }

    @Test
    public void testOfflineUserClientMaxLifespanGreaterThanSession() {
        testUserClientMaxLifespanGreaterThanSession(true, false);
    }

    @Test
    public void testOfflineUserClientMaxLifespanGreaterThanSessionOverrideInClient() {
        testUserClientMaxLifespanGreaterThanSession(true, true);
    }

    @Test
    public void testOfflineUserClientMaxLifespanSmallerThanSession() {
        testUserClientMaxLifespanSmallerThanSession(true, false);
    }

    @Test
    public void testOfflineUserClientMaxLifespanSmallerThanSessionOverrideInClient() {
        testUserClientMaxLifespanSmallerThanSession(true, true);
    }

    @Test
    public void testOfflineUserClientIdleTimeoutSmallerThanSessionNoRefresh() {
        testUserClientIdleTimeoutSmallerThanSession(0, true, false);
    }

    @Test
    public void testOfflineUserClientIdleTimeoutSmallerThanSessionOneRefresh() {
        testUserClientIdleTimeoutSmallerThanSession(1, true, false);
    }

    @Test
    public void testOnlineUserClientMaxLifespanGreaterThanSession() {
        testUserClientMaxLifespanGreaterThanSession(false, false);
    }

    @Test
    public void testOnlineUserClientMaxLifespanGreaterThanSessionOverrideInClient() {
        testUserClientMaxLifespanGreaterThanSession(false, true);
    }

    @Test
    public void testOnlineUserClientMaxLifespanSmallerThanSession() {
        testUserClientMaxLifespanSmallerThanSession(false, false);
    }

    @Test
    public void testOnlineUserClientMaxLifespanSmallerThanSessionOverrideInClient() {
        testUserClientMaxLifespanSmallerThanSession(false, true);
    }

    @Test
    public void testOnlineUserClientIdleTimeoutSmallerThanSessionNoRefresh() {
        testUserClientIdleTimeoutSmallerThanSession(0, false, false);
    }

    @Test
    public void testOnlineUserClientIdleTimeoutSmallerThanSessionOneRefresh() {
        testUserClientIdleTimeoutSmallerThanSession(1, false, false);
    }

    /**
     * This method introduces a delay to allow replication of clientSession cache on site 1 and site 2.
     * Without the delay these test fails from time to time. This has no effect when tests run without remote Infinispan
     * @param offline boolean Indicates where we work with offline sessions
     */
    private void allowXSiteReplication(boolean offline) {
        var hotRodServer = getParameters(HotRodServerRule.class).findFirst();
        if (hotRodServer.isEmpty()) {
            return;
        }

        var cacheName = offline ? InfinispanConnectionProvider.OFFLINE_CLIENT_SESSION_CACHE_NAME : InfinispanConnectionProvider.CLIENT_SESSION_CACHE_NAME;
        var cache1 = hotRodServer.get().getHotRodCacheManager().getCache(cacheName);
        var cache2 = hotRodServer.get().getHotRodCacheManager2().getCache(cacheName);
        eventually(() -> "Wrong cache size. Site1: " + cache1.keySet() + ", Site2: " + cache2.keySet(),
                () -> cache1.size() == cache2.size(), 10000, 10, TimeUnit.MILLISECONDS);
    }

    private void processExpiration(boolean offline) {
        var hotRodServer = getParameters(HotRodServerRule.class).findFirst();
        if (hotRodServer.isEmpty()) {
            return;
        }
        // force expired entries to be removed from memory
        var cacheName = offline ? InfinispanConnectionProvider.OFFLINE_CLIENT_SESSION_CACHE_NAME : InfinispanConnectionProvider.CLIENT_SESSION_CACHE_NAME;
        hotRodServer.get().getHotRodCacheManager().getCache(cacheName).getAdvancedCache().getExpirationManager().processExpiration();
        hotRodServer.get().getHotRodCacheManager2().getCache(cacheName).getAdvancedCache().getExpirationManager().processExpiration();
    }
}

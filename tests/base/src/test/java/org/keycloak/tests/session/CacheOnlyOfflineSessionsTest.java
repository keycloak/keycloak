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
package org.keycloak.tests.session;

import org.keycloak.common.Profile;
import org.keycloak.connections.infinispan.InfinispanConnectionProvider;
import org.keycloak.connections.jpa.JpaConnectionProvider;
import org.keycloak.models.jpa.session.PersistentUserSessionEntity;
import org.keycloak.models.sessions.infinispan.InfinispanUserSessionProvider;
import org.keycloak.models.sessions.infinispan.changes.SessionEntityWrapper;
import org.keycloak.models.sessions.infinispan.entities.AuthenticatedClientSessionEntity;
import org.keycloak.models.sessions.infinispan.entities.EmbeddedClientSessionKey;
import org.keycloak.models.sessions.infinispan.entities.UserSessionEntity;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.InjectUser;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.oauth.OAuthClient;
import org.keycloak.testframework.oauth.annotations.InjectOAuthClient;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.ManagedUser;
import org.keycloak.testframework.remote.runonserver.InjectRunOnServer;
import org.keycloak.testframework.remote.runonserver.RunOnServerClient;
import org.keycloak.testframework.server.KeycloakServerConfig;
import org.keycloak.testframework.server.KeycloakServerConfigBuilder;

import org.infinispan.Cache;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@KeycloakIntegrationTest(config = CacheOnlyOfflineSessionsTest.CacheOnlyServerConfig.class)
public class CacheOnlyOfflineSessionsTest {
    @InjectRealm
    ManagedRealm realm;
    @InjectUser(config = VolatileSessionResurrectionConcurrencyTest.TestUserConfig.class)
    ManagedUser user;
    @InjectOAuthClient(config = VolatileSessionResurrectionConcurrencyTest.OfflineGrantClientConfig.class)
    OAuthClient oauth;
    @InjectRunOnServer
    RunOnServerClient runOnServer;

    @Test
    public void offlineSessionsCanBeCreatedUpdatedListedRefreshedAndRevokedWithoutPersistence() {
        oauth.scope("offline_access");
        var response = oauth.doPasswordGrantRequest(user.getUsername(), "password");
        assertEquals(200, response.getStatusCode());
        var sessionId = oauth.parseRefreshToken(response.getRefreshToken()).getSessionId();
        String realmName = realm.getName();
        runOnServer.run(session -> {
            var model = session.realms().getRealmByName(realmName);
            var parent = session.sessions().getOfflineUserSession(model, sessionId);
            assertNotNull(parent);
            var client = model.getClientByClientId("volatile-marker-test-client");
            var child = parent.getAuthenticatedClientSessionByClient(client.getId());
            assertNotNull(child);
            parent.setNote("cache-only", "parent");
            child.setNote("cache-only", "child");
            parent.setLastSessionRefresh(parent.getLastSessionRefresh() + 1);
            assertNull(((InfinispanUserSessionProvider) session.sessions()).getPersisterLastSessionRefreshStore());
        });
        runOnServer.run(session -> {
            var model = session.realms().getRealmByName(realmName);
            var client = model.getClientByClientId("volatile-marker-test-client");
            var parent = session.sessions().getOfflineUserSession(model, sessionId);
            assertEquals("parent", parent.getNote("cache-only"));
            assertEquals("child", parent.getAuthenticatedClientSessionByClient(client.getId()).getNote("cache-only"));
            assertEquals(1, session.sessions().getOfflineUserSessionsStream(model, parent.getUser()).count());
            assertEquals(1, session.sessions().getOfflineSessionsCount(model, client));
            assertEquals(1L, session.sessions().getActiveClientSessionStats(model, true).get(client.getId()));
            assertEquals(1, session.sessions().readOnlyStreamOfflineUserSessions(model).count());
            assertEquals(1, session.sessions().readOnlyStreamOfflineUserSessions(model, client, 0, 10).count());
            assertNull(session.getProvider(JpaConnectionProvider.class).getEntityManager()
                    .find(PersistentUserSessionEntity.class, new PersistentUserSessionEntity.Key(sessionId, "1")));
            var connections = session.getProvider(InfinispanConnectionProvider.class);
            Cache<String, SessionEntityWrapper<UserSessionEntity>> parents = connections.getCache(InfinispanConnectionProvider.OFFLINE_USER_SESSION_CACHE_NAME);
            Cache<EmbeddedClientSessionKey, SessionEntityWrapper<AuthenticatedClientSessionEntity>> children = connections.getCache(InfinispanConnectionProvider.OFFLINE_CLIENT_SESSION_CACHE_NAME);
            assertTrue(parents.getAdvancedCache().getCacheEntry(sessionId).getLifespan() > 1000);
            assertTrue(children.getAdvancedCache().getCacheEntry(new EmbeddedClientSessionKey(sessionId, client.getId())).getLifespan() > 1000);
            assertEquals(-1, parents.getCacheConfiguration().memory().maxCount());
            assertEquals(2, parents.getCacheConfiguration().clustering().hash().numOwners());
            assertNull(session.sessions().getOfflineUserSession(model, "absent-session"));
            assertNull(parents.get("absent-session"));
        });
        var refreshed = oauth.doRefreshTokenRequest(response.getRefreshToken());
        assertEquals(200, refreshed.getStatusCode());
        assertTrue(oauth.doLogout(refreshed.getRefreshToken()).isSuccess());
        assertEquals(400, oauth.doRefreshTokenRequest(refreshed.getRefreshToken()).getStatusCode());
        oauth.scope(null);
    }

    public static class CacheOnlyServerConfig implements KeycloakServerConfig {
        @Override
        public KeycloakServerConfigBuilder configure(KeycloakServerConfigBuilder config) {
            return config.featuresDisabled(Profile.Feature.PERSISTENT_USER_SESSIONS)
                    .option("spi-user-session-persister--provider", "disabled")
                    .spiOption("user-sessions", "infinispan", "max-cache-lifespan", "1")
                    .spiOption("user-sessions", "infinispan", "offline-session-cache-entry-lifespan-override", "1")
                    .spiOption("user-sessions", "infinispan", "offline-client-session-cache-entry-lifespan-override", "1");
        }
    }
}

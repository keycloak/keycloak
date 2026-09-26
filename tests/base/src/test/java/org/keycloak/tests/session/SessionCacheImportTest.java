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

import org.keycloak.connections.infinispan.InfinispanConnectionProvider;
import org.keycloak.connections.jpa.JpaConnectionProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.UserSessionModel;
import org.keycloak.models.jpa.session.PersistentClientSessionEntity;
import org.keycloak.models.jpa.session.PersistentUserSessionEntity;
import org.keycloak.models.session.UserSessionPersisterProvider;
import org.keycloak.models.sessions.infinispan.changes.SessionEntityWrapper;
import org.keycloak.models.sessions.infinispan.entities.AuthenticatedClientSessionEntity;
import org.keycloak.models.sessions.infinispan.entities.EmbeddedClientSessionKey;
import org.keycloak.models.sessions.infinispan.entities.UserSessionEntity;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.InjectUser;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.oauth.OAuthClient;
import org.keycloak.testframework.oauth.annotations.InjectOAuthClient;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.ManagedUser;
import org.keycloak.testframework.remote.runonserver.InjectRunOnServer;
import org.keycloak.testframework.remote.runonserver.RunOnServerClient;

import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.infinispan.Cache;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

@KeycloakIntegrationTest(config = SessionResurrectionConcurrencyTest.SessionCachingServerConfig.class)
public class SessionCacheImportTest {
    @InjectRealm
    ManagedRealm realm;
    @InjectUser(config = SessionResurrectionConcurrencyTest.TestUserConfig.class)
    ManagedUser user;
    @InjectOAuthClient(config = SessionResurrectionConcurrencyTest.DirectGrantClientConfig.class)
    OAuthClient oauth;
    @InjectRunOnServer
    RunOnServerClient runOnServer;

    @Test
    public void cachedOfflineChildRetainsItsDomainWhenParentIsReloaded() {
        oauth.scope(null);
        var response = oauth.doPasswordGrantRequest(user.getUsername(), "password");
        assertEquals(200, response.getStatusCode());
        String id = oauth.parseRefreshToken(response.getRefreshToken()).getSessionId();
        String realmName = realm.getName();
        createOfflineCopy(id, realmName);
        try {
            runOnServer.run(session -> {
                var model = session.realms().getRealmByName(realmName);
                var client = model.getClientByClientId("marker-test-client");
                var key = new EmbeddedClientSessionKey(id, client.getId());
                assertNotNull(children(session, true).get(key));
                assertFalse(children(session, true).get(key).getEntity().isOffline(), "The cache does not serialize the domain flag");
                parents(session, true).remove(id);
                var parent = session.sessions().getOfflineUserSession(model, id);
                var child = parent.getAuthenticatedClientSessionByClient(client.getId());
                assertNotNull(child);
                child.setNote("offline-import", "updated");
            });
            runOnServer.run(session -> {
                var model = session.realms().getRealmByName(realmName);
                var client = model.getClientByClientId("marker-test-client");
                var persister = session.getProvider(UserSessionPersisterProvider.class);
                var offline = persister.loadUserSession(model, id, true);
                assertEquals("updated", persister.loadClientSession(model, client, offline, true).getNote("offline-import"));
                var online = persister.loadUserSession(model, id, false);
                assertNotNull(online);
                assertNull(persister.loadClientSession(model, client, online, false).getNote("offline-import"));
                var key = new EmbeddedClientSessionKey(id, client.getId());
                assertEquals("updated", children(session, true).get(key).getEntity().getNotes().get("offline-import"));
                assertNull(children(session, false).get(key).getEntity().getNotes().get("offline-import"));
            });
        } finally {
            oauth.doLogout(response.getRefreshToken());
            oauth.scope(null);
        }
    }

    @ParameterizedTest
    @CsvSource({"false,false", "false,true", "true,false", "true,true"})
    public void managedChildCannotBePublishedAfterConcurrentMutation(boolean offline, boolean delete) {
        oauth.scope(null);
        var response = oauth.doPasswordGrantRequest(user.getUsername(), "password");
        assertEquals(200, response.getStatusCode());
        String id = oauth.parseRefreshToken(response.getRefreshToken()).getSessionId();
        String realmName = realm.getName();
        createOfflineCopy(id, realmName);
        try {
            runOnServer.run(session -> {
                var model = session.realms().getRealmByName(realmName);
                var client = model.getClientByClientId("marker-test-client");
                var em = session.getProvider(JpaConnectionProvider.class).getEntityManager();
                var key = databaseKey(id, client.getId(), offline);
                var managed = em.find(PersistentClientSessionEntity.class, key);
                assertNotNull(managed);
                String oldData = managed.getData();
                assertNoManagedParent(session);
                assertFalse(session.getProvider(UserSessionPersisterProvider.class).isUserSessionCacheImportEligible(id, offline));
                var factory = session.getKeycloakSessionFactory();
                KeycloakModelUtils.suspendJtaTransaction(factory, () -> KeycloakModelUtils.runJobInTransaction(factory, other -> {
                    var otherRealm = other.realms().getRealmByName(realmName);
                    other.getContext().setRealm(otherRealm);
                    var otherClient = otherRealm.getClientById(client.getId());
                    var parent = load(other, otherRealm, id, offline);
                    var child = parent.getAuthenticatedClientSessionByClient(otherClient.getId());
                    if (delete) {
                        child.detachFromUserSession();
                    } else {
                        child.setNote("freshness", "committed");
                    }
                }));
                assertSame(managed, em.find(PersistentClientSessionEntity.class, key));
                assertEquals(oldData, managed.getData(), "The reader must retain its earlier first-level-cache snapshot");
                assertNoManagedParent(session);
                evict(session, id, client.getId(), offline);
                var parent = load(session, model, id, offline);
                assertNotNull(parent);
                parent.getAuthenticatedClientSessionByClient(client.getId());
                assertNull(parents(session, offline).get(id), "A managed child alone must prevent parent graph publication");
                assertNull(children(session, offline).get(new EmbeddedClientSessionKey(id, client.getId())), "The old child must remain request-local");
            });
            runOnServer.run(session -> {
                var model = session.realms().getRealmByName(realmName);
                var client = model.getClientByClientId("marker-test-client");
                var parent = load(session, model, id, offline);
                var child = parent.getAuthenticatedClientSessionByClient(client.getId());
                if (delete) {
                    assertNull(child);
                } else {
                    assertNotNull(child);
                    assertEquals("committed", child.getNote("freshness"));
                }
                assertNotNull(parents(session, offline).get(id), "A fresh transaction must still warm the cache");
            });
        } finally {
            oauth.doLogout(response.getRefreshToken());
            oauth.scope(null);
        }
    }

    @ParameterizedTest
    @CsvSource({"false,false", "false,true", "true,false", "true,true"})
    public void unrelatedManagedChildStillPermitsPublication(boolean offline, boolean otherDomain) {
        oauth.scope(null);
        var response = oauth.doPasswordGrantRequest(user.getUsername(), "password");
        var other = oauth.doPasswordGrantRequest(user.getUsername(), "password");
        assertEquals(200, response.getStatusCode());
        assertEquals(200, other.getStatusCode());
        String id = oauth.parseRefreshToken(response.getRefreshToken()).getSessionId();
        String otherId = oauth.parseRefreshToken(other.getRefreshToken()).getSessionId();
        String realmName = realm.getName();
        createOfflineCopy(id, realmName);
        createOfflineCopy(otherId, realmName);
        try {
            runOnServer.run(session -> {
                var model = session.realms().getRealmByName(realmName);
                var client = model.getClientByClientId("marker-test-client");
                var em = session.getProvider(JpaConnectionProvider.class).getEntityManager();
                var key = databaseKey(otherDomain ? id : otherId, client.getId(), otherDomain ? !offline : offline);
                var managed = em.find(PersistentClientSessionEntity.class, key);
                assertNotNull(managed);
                assertNoManagedParent(session);
                assertTrue(session.getProvider(UserSessionPersisterProvider.class).isUserSessionCacheImportEligible(id, offline));
                evict(session, id, client.getId(), offline);
                var parent = load(session, model, id, offline);
                assertNotNull(parent.getAuthenticatedClientSessionByClient(client.getId()));
                assertNotNull(parents(session, offline).get(id));
                assertNotNull(children(session, offline).get(new EmbeddedClientSessionKey(id, client.getId())));
                assertSame(managed, em.find(PersistentClientSessionEntity.class, key));
            });
        } finally {
            oauth.doLogout(response.getRefreshToken());
            oauth.doLogout(other.getRefreshToken());
            oauth.scope(null);
        }
    }

    private void createOfflineCopy(String id, String realmName) {
        runOnServer.run(session -> {
            var model = session.realms().getRealmByName(realmName);
            var client = model.getClientByClientId("marker-test-client");
            var parent = session.sessions().getUserSession(model, id);
            assertNotNull(parent);
            var child = parent.getAuthenticatedClientSessionByClient(client.getId());
            var sibling = model.getClientByClientId("import-test-sibling");
            if (sibling == null) {
                sibling = model.addClient("import-test-sibling");
            }
            var siblingSession = session.sessions().createClientSession(model, sibling, parent);
            var offline = session.sessions().createOfflineUserSession(parent);
            session.sessions().createOfflineClientSession(child, offline);
            // Removing the last offline child also deletes its parent; retain a sibling to isolate freshness.
            session.sessions().createOfflineClientSession(siblingSession, offline);
        });
    }

    private static void assertNoManagedParent(KeycloakSession session) {
        var context = session.getProvider(JpaConnectionProvider.class).getEntityManager()
                .unwrap(SharedSessionContractImplementor.class).getPersistenceContextInternal();
        assertTrue(context.getEntitiesByKey().values().stream().noneMatch(PersistentUserSessionEntity.class::isInstance));
    }

    private static PersistentClientSessionEntity.Key databaseKey(String id, String client, boolean offline) {
        return new PersistentClientSessionEntity.Key(id, client, PersistentClientSessionEntity.LOCAL, PersistentClientSessionEntity.LOCAL, offline ? "1" : "0");
    }

    private static UserSessionModel load(KeycloakSession session, org.keycloak.models.RealmModel realm, String id, boolean offline) {
        return offline ? session.sessions().getOfflineUserSession(realm, id) : session.sessions().getUserSession(realm, id);
    }

    private static void evict(KeycloakSession session, String id, String client, boolean offline) {
        parents(session, offline).remove(id);
        children(session, offline).remove(new EmbeddedClientSessionKey(id, client));
    }

    private static Cache<String, SessionEntityWrapper<UserSessionEntity>> parents(KeycloakSession session, boolean offline) {
        return session.getProvider(InfinispanConnectionProvider.class).getCache(offline ? InfinispanConnectionProvider.OFFLINE_USER_SESSION_CACHE_NAME : InfinispanConnectionProvider.USER_SESSION_CACHE_NAME);
    }

    private static Cache<EmbeddedClientSessionKey, SessionEntityWrapper<AuthenticatedClientSessionEntity>> children(KeycloakSession session, boolean offline) {
        return session.getProvider(InfinispanConnectionProvider.class).getCache(offline ? InfinispanConnectionProvider.OFFLINE_CLIENT_SESSION_CACHE_NAME : InfinispanConnectionProvider.CLIENT_SESSION_CACHE_NAME);
    }
}

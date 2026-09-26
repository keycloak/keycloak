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

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.keycloak.connections.infinispan.InfinispanConnectionProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.sessions.infinispan.InfinispanUserSessionProvider;
import org.keycloak.models.sessions.infinispan.PersistentUserSessionProvider;
import org.keycloak.models.sessions.infinispan.changes.SessionEntityWrapper;
import org.keycloak.models.sessions.infinispan.entities.AuthenticatedClientSessionEntity;
import org.keycloak.models.sessions.infinispan.entities.EmbeddedClientSessionKey;
import org.keycloak.models.sessions.infinispan.entities.UserSessionEntity;
import org.keycloak.testframework.remote.providers.runonserver.RunOnServer;

import org.infinispan.Cache;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Exercises the actual realm-cleanup handler and delayed publication at its cache CAS boundary.
 * The fixtures are cache-only: this does not simulate a database transaction or cluster delivery.
 */
public class RealmSessionCacheRemoval implements RunOnServer {

    private final boolean offline;
    private final boolean persistent;

    public RealmSessionCacheRemoval(boolean offline, boolean persistent) {
        this.offline = offline;
        this.persistent = persistent;
    }

    @Override
    public void run(KeycloakSession session) {
        var infinispan = session.getProvider(InfinispanConnectionProvider.class);
        Cache<String, SessionEntityWrapper<UserSessionEntity>> parents = infinispan.getCache(offline
                ? InfinispanConnectionProvider.OFFLINE_USER_SESSION_CACHE_NAME : InfinispanConnectionProvider.USER_SESSION_CACHE_NAME);
        Cache<EmbeddedClientSessionKey, SessionEntityWrapper<AuthenticatedClientSessionEntity>> children = infinispan.getCache(offline
                ? InfinispanConnectionProvider.OFFLINE_CLIENT_SESSION_CACHE_NAME : InfinispanConnectionProvider.CLIENT_SESSION_CACHE_NAME);
        String realmId = "cleanup-" + UUID.randomUUID();
        String otherRealmId = "other-" + UUID.randomUUID();
        Map<String, SessionEntityWrapper<UserSessionEntity>> parentEntries = new HashMap<>();
        Map<EmbeddedClientSessionKey, SessionEntityWrapper<AuthenticatedClientSessionEntity>> childEntries = new HashMap<>();

        for (String entryRealm : new String[]{realmId, otherRealmId}) {
            for (String parentState : new String[]{"absent", "marker", "complete"}) {
                String parentId = entryRealm + "-" + parentState;
                if (!parentState.equals("absent")) {
                    var entity = new UserSessionEntity(parentId);
                    entity.setRealmId(entryRealm);
                    // Both parent markers and complete parents deliberately omit the child IDs.
                    parentEntries.put(parentId, parentState.equals("marker")
                            ? SessionEntityWrapper.createLoadingMarker(entity) : new SessionEntityWrapper<>(entity));
                }
                for (boolean marker : new boolean[]{false, true}) {
                    var key = new EmbeddedClientSessionKey(parentId, marker ? "loading-client" : "complete-client");
                    var entity = new AuthenticatedClientSessionEntity();
                    entity.setRealmId(entryRealm);
                    if (!marker) {
                        entity.setUserSessionId(parentId);
                        entity.setClientId(key.clientId());
                    }
                    // Real standalone child markers contain the realm ID but no session IDs.
                    childEntries.put(key, marker ? SessionEntityWrapper.createLoadingMarker(entity) : new SessionEntityWrapper<>(entity));
                }
            }
        }

        try {
            parentEntries.forEach((key, value) -> parents.put(key, value, 60, TimeUnit.SECONDS));
            childEntries.forEach((key, value) -> children.put(key, value, 60, TimeUnit.SECONDS));

            var provider = session.sessions();
            assertEquals(persistent, provider instanceof PersistentUserSessionProvider);
            if (provider instanceof PersistentUserSessionProvider persistentProvider) {
                persistentProvider.removeLocalUserSessions(realmId, offline);
            } else {
                ((InfinispanUserSessionProvider) provider).removeLocalUserSessions(realmId, offline);
            }

            parentEntries.forEach((key, value) -> {
                if (realmId.equals(value.getEntity().getRealmId())) {
                    if (value.isLoadingMarker()) {
                        assertFalse(parents.replace(key, value, new SessionEntityWrapper<>(value.getEntity()), 60, TimeUnit.SECONDS),
                                "A delayed parent loader must lose its publication marker");
                    }
                    assertNull(parents.get(key), "Realm cleanup must remove the parent");
                } else {
                    assertEquals(value, parents.get(key), "Another realm's parent must survive");
                }
            });
            childEntries.forEach((key, value) -> {
                if (realmId.equals(value.getEntity().getRealmId())) {
                    if (value.isLoadingMarker()) {
                        var staleSnapshot = new AuthenticatedClientSessionEntity();
                        staleSnapshot.setRealmId(realmId);
                        staleSnapshot.setUserSessionId(key.userSessionId());
                        staleSnapshot.setClientId(key.clientId());
                        assertFalse(children.replace(key, value, new SessionEntityWrapper<>(staleSnapshot), 60, TimeUnit.SECONDS),
                                "A delayed child loader must lose its publication marker: " + key);
                    }
                    assertNull(children.get(key), "Realm cleanup must remove unlisted children: " + key);
                } else {
                    assertEquals(value, children.get(key), "Another realm's child must survive");
                }
            });
        } finally {
            parentEntries.keySet().forEach(parents::remove);
            childEntries.keySet().forEach(children::remove);
        }
    }
}

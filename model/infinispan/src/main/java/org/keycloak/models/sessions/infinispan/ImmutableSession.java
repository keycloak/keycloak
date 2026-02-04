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

package org.keycloak.models.sessions.infinispan;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Stream;

import org.keycloak.common.Profile;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.models.light.LightweightUserAdapter;
import org.keycloak.models.sessions.infinispan.changes.SessionEntityWrapper;
import org.keycloak.models.sessions.infinispan.entities.AuthenticatedClientSessionEntity;
import org.keycloak.models.sessions.infinispan.entities.ClientSessionKey;
import org.keycloak.models.sessions.infinispan.entities.EmbeddedClientSessionKey;
import org.keycloak.models.sessions.infinispan.entities.RemoteAuthenticatedClientSessionEntity;
import org.keycloak.models.sessions.infinispan.entities.RemoteUserSessionEntity;
import org.keycloak.models.sessions.infinispan.entities.UserSessionEntity;
import org.keycloak.models.sessions.infinispan.query.ClientSessionQueries;
import org.keycloak.models.sessions.infinispan.query.QueryHelper;
import org.keycloak.models.sessions.infinispan.util.SessionExpirationPredicates;
import org.keycloak.utils.StreamsUtil;

import org.infinispan.Cache;
import org.infinispan.client.hotrod.RemoteCache;

import static org.keycloak.models.Constants.SESSION_NOTE_LIGHTWEIGHT_USER;

/**
 * Helper class to map a list of user and client sessions, from Infinispan caches, into an immutable session.
 * <p>
 * It copies the data to a new instance to prevent observing changes made by other threads to the underlying cached
 * instances.
 */
public final class ImmutableSession {

    public static void readOnly() {
        throw new UnsupportedOperationException("this instance is read-only");
    }

    public static Stream<UserSessionModel> copyOf(KeycloakSession session,
                                                  Collection<UserSessionEntity> entityList,
                                                  SessionExpirationPredicates expiration,
                                                  Cache<EmbeddedClientSessionKey, SessionEntityWrapper<AuthenticatedClientSessionEntity>> cache) {
        var clientSessionKeys = new HashSet<EmbeddedClientSessionKey>();
        var userSessionMap = new LinkedHashMap<String, ImmutableUserSessionModel>();
        var users = session.users();
        entityList.forEach(entity -> {
            if (entity == null) {
                return;
            }
            if (!Objects.equals(entity.getRealmId(), expiration.realm().getId())) {
                return;
            }
            if (expiration.isUserSessionExpired(entity)) {
                return;
            }
            UserModel user;
            if (Profile.isFeatureEnabled(Profile.Feature.TRANSIENT_USERS) && entity.getNotes().containsKey(SESSION_NOTE_LIGHTWEIGHT_USER)) {
                user = LightweightUserAdapter.fromString(session, expiration.realm(), entity.getNotes().get(SESSION_NOTE_LIGHTWEIGHT_USER));
            } else {
                user = users.getUserById(expiration.realm(), entity.getUser());
            }
            if (user == null) {
                return;
            }
            var copy = new ImmutableUserSessionModel(
                    entity.getId(),
                    expiration.realm(),
                    user,
                    entity.getBrokerSessionId(),
                    entity.getBrokerUserId(),
                    entity.getLoginUsername(),
                    entity.getIpAddress(),
                    entity.getAuthMethod(),
                    new HashMap<>(), // to break cyclic dependency between user and client session
                    Map.copyOf(entity.getNotes()),
                    entity.getState(),
                    entity.getStarted(),
                    entity.getLastSessionRefresh(),
                    entity.isRememberMe(),
                    expiration.offline()
            );
            entity.getClientSessions().forEach(clientId -> clientSessionKeys.add(new EmbeddedClientSessionKey(copy.id(), clientId)));
            userSessionMap.put(copy.id(), copy);
        });

        populateClientSessions(userSessionMap, clientSessionKeys, expiration, cache);
        return userSessionMap.values().stream()
                .map(UserSessionModel.class::cast);
    }

    public static Stream<UserSessionModel> copyOf(KeycloakSession session,
                                                  Collection<RemoteUserSessionEntity> entityList,
                                                  SessionExpirationPredicates expiration,
                                                  RemoteCache<ClientSessionKey, RemoteAuthenticatedClientSessionEntity> cache,
                                                  int batchSize) {
        var userSessionMap = new LinkedHashMap<String, ImmutableUserSessionModel>();
        var users = session.users();
        entityList.forEach(entity -> {
            if (entity == null) {
                return;
            }
            if (!Objects.equals(entity.getRealmId(), expiration.realm().getId())) {
                return;
            }
            if (expiration.isUserSessionExpired(entity)) {
                return;
            }
            UserModel user;
            if (Profile.isFeatureEnabled(Profile.Feature.TRANSIENT_USERS) && entity.getNotes().containsKey(SESSION_NOTE_LIGHTWEIGHT_USER)) {
                user = LightweightUserAdapter.fromString(session, expiration.realm(), entity.getNotes().get(SESSION_NOTE_LIGHTWEIGHT_USER));
            } else {
                user = users.getUserById(expiration.realm(), entity.getUserId());
            }
            if (user == null) {
                return;
            }
            var copy = new ImmutableUserSessionModel(
                    entity.getUserSessionId(),
                    expiration.realm(),
                    user,
                    entity.getBrokerSessionId(),
                    entity.getBrokerUserId(),
                    entity.getLoginUsername(),
                    entity.getIpAddress(),
                    entity.getAuthMethod(),
                    new HashMap<>(), // to break cyclic dependency between user and client session
                    Map.copyOf(entity.getNotes()),
                    entity.getState(),
                    entity.getStarted(),
                    entity.getLastSessionRefresh(),
                    entity.isRememberMe(),
                    expiration.offline()
            );
            userSessionMap.put(copy.id(), copy);
        });


        populateClientSessions(userSessionMap, expiration, cache, batchSize);
        return userSessionMap.values().stream().map(UserSessionModel.class::cast);
    }

    private static void populateClientSessions(Map<String, ImmutableUserSessionModel> userSessionMap, Set<EmbeddedClientSessionKey> clientSessionKeys, SessionExpirationPredicates expirationPredicates, Cache<EmbeddedClientSessionKey, SessionEntityWrapper<AuthenticatedClientSessionEntity>> cache) {
        StreamsUtil.closing(cache.entrySet().stream()
                        .filterKeys(clientSessionKeys))
                .iterator()
                .forEachRemaining(entry -> {
                    var clientSession = entry.getValue().getEntity();
                    var userSession = userSessionMap.get(entry.getKey().userSessionId());
                    var client = expirationPredicates.realm().getClientById(entry.getKey().clientId());
                    if (client == null || userSession == null) {
                        return;
                    }
                    if (expirationPredicates.isClientSessionExpired(clientSession, userSession.rememberMe(), client)) {
                        return;
                    }
                    var copy = new ImmutableClientSession(
                            entry.getKey().toId(),
                            client,
                            userSession,
                            Map.copyOf(clientSession.getNotes()),
                            clientSession.getRedirectUri(),
                            clientSession.getAction(),
                            clientSession.getAuthMethod(),
                            clientSession.getTimestamp(),
                            clientSession.getStarted()
                    );
                    userSession.clientSessions().put(entry.getKey().clientId(), copy);
                });
    }

    private static void populateClientSessions(Map<String, ImmutableUserSessionModel> userSessionMap, SessionExpirationPredicates expirationPredicates, RemoteCache<ClientSessionKey, RemoteAuthenticatedClientSessionEntity> cache, int batchSize) {
        var query = ClientSessionQueries.fetchClientSessions(cache, userSessionMap.keySet());
        QueryHelper.streamAll(query, batchSize, Function.identity()).forEach(entity -> {
            var userSession = userSessionMap.get(entity.getUserSessionId());
            var client = expirationPredicates.realm().getClientById(entity.getClientId());
            if (client == null || userSession == null) {
                return;
            }
            if (expirationPredicates.isClientSessionExpired(entity, userSession.started(), userSession.rememberMe(), client)) {
                return;
            }
            var copy = new ImmutableClientSession(
                    entity.createId(),
                    client,
                    userSession,
                    Map.copyOf(entity.getNotes()),
                    entity.getRedirectUri(),
                    entity.getAction(),
                    entity.getProtocol(),
                    entity.getTimestamp(),
                    entity.getStarted()
            );
            userSession.clientSessions().put(entity.getClientId(), copy);
        });
    }

}

/*
 * Copyright 2024 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.models.sessions.infinispan.query;

import org.keycloak.marshalling.Marshalling;
import org.keycloak.models.sessions.infinispan.entities.ClientSessionKey;
import org.keycloak.models.sessions.infinispan.entities.RemoteAuthenticatedClientSessionEntity;

import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.commons.api.query.Query;

/**
 * Util class with Infinispan Ickle Queries for {@link RemoteAuthenticatedClientSessionEntity}.
 */
public final class ClientSessionQueries {

    private ClientSessionQueries() {
    }

    public static final String CLIENT_SESSION = Marshalling.protoEntity(RemoteAuthenticatedClientSessionEntity.class);

    private static final String FETCH_USER_SESSION_ID = "SELECT e.userSessionId FROM %s as e WHERE e.realmId = :realmId && e.clientId = :clientId ORDER BY e.userSessionId".formatted(CLIENT_SESSION);
    private static final String PER_CLIENT_COUNT = "SELECT e.clientId, count(e.clientId) FROM %s as e GROUP BY e.clientId ORDER BY e.clientId".formatted(CLIENT_SESSION);
    private static final String CLIENT_SESSION_COUNT = "SELECT count(e) FROM %s as e WHERE e.realmId = :realmId && e.clientId = :clientId".formatted(CLIENT_SESSION);
    private static final String FROM_USER_SESSION = "FROM %s as e WHERE e.userSessionId = :userSessionId ORDER BY e.clientId".formatted(CLIENT_SESSION);
    private static final String IDS_FROM_USER_SESSION = "SELECT e.clientId FROM %s as e WHERE e.userSessionId = :userSessionId ORDER BY e.clientId".formatted(CLIENT_SESSION);

    /**
     * Returns a projection with the user session ID for client sessions from the client {@code clientId}.
     */
    public static Query<Object[]> fetchUserSessionIdForClientId(RemoteCache<ClientSessionKey, RemoteAuthenticatedClientSessionEntity> cache, String realmId, String clientId) {
        return cache.<Object[]>query(FETCH_USER_SESSION_ID)
                .setParameter("realmId", realmId)
                .setParameter("clientId", clientId);
    }

    /**
     * Returns a projection with the client ID and its number of active client sessions.
     */
    public static Query<Object[]> activeClientCount(RemoteCache<ClientSessionKey, RemoteAuthenticatedClientSessionEntity> cache) {
        return cache.query(PER_CLIENT_COUNT);
    }

    /**
     * Returns a projection with the sum of all client session belonging to the client ID.
     */
    public static Query<Object[]> countClientSessions(RemoteCache<ClientSessionKey, RemoteAuthenticatedClientSessionEntity> cache, String realmId, String clientId) {
        return cache.<Object[]>query(CLIENT_SESSION_COUNT)
                .setParameter("realmId", realmId)
                .setParameter("clientId", clientId);
    }

    /**
     * Returns a projection with the client session, and the version of all client sessions belonging to the user
     * session ID.
     */
    public static Query<RemoteAuthenticatedClientSessionEntity> fetchClientSessions(RemoteCache<ClientSessionKey, RemoteAuthenticatedClientSessionEntity> cache, String userSessionId) {
        return cache.<RemoteAuthenticatedClientSessionEntity>query(FROM_USER_SESSION)
                .setParameter("userSessionId", userSessionId);
    }

    /**
     * Returns a projection with the client IDs belonging to the user session.
     * <p>
     * The returned array contains a single {@link String} element with the client ID.
     */
    public static Query<Object[]> fetchClientSessionsIds(RemoteCache<ClientSessionKey, RemoteAuthenticatedClientSessionEntity> cache, String userSessionId) {
        return cache.<Object[]>query(IDS_FROM_USER_SESSION)
                .setParameter("userSessionId", userSessionId);
    }


}

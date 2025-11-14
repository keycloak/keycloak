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
import org.keycloak.models.sessions.infinispan.entities.RemoteUserSessionEntity;

import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.commons.api.query.Query;

/**
 * Util class with Infinispan Ickle Queries for {@link RemoteUserSessionEntity}.
 */
public final class UserSessionQueries {

    private UserSessionQueries() {
    }

    public static final String USER_SESSION = Marshalling.protoEntity(RemoteUserSessionEntity.class);

    private static final String BASE_QUERY = "FROM %s as e ".formatted(USER_SESSION);
    private static final String BY_BROKER_SESSION_ID = BASE_QUERY + "WHERE e.realmId = :realmId && e.brokerSessionId = :brokerSessionId ORDER BY e.userSessionId";
    private static final String BY_USER_ID = BASE_QUERY + "WHERE e.realmId = :realmId && e.userId = :userId ORDER BY e.userSessionId";
    private static final String BY_BROKER_USER_ID = BASE_QUERY + "WHERE e.realmId = :realmId && e.brokerUserId = :brokerUserId ORDER BY e.userSessionId";

    /**
     * Returns a projection with the user session, and the version of all user sessions belonging to the broker session
     * ID.
     */
    public static Query<RemoteUserSessionEntity> searchByBrokerSessionId(RemoteCache<String, RemoteUserSessionEntity> cache, String realmId, String brokerSessionId) {
        return cache.<RemoteUserSessionEntity>query(BY_BROKER_SESSION_ID)
                .setParameter("realmId", realmId)
                .setParameter("brokerSessionId", brokerSessionId);
    }

    /**
     * Returns a projection with the user session, and the version of all user sessions belonging to the user ID.
     */
    public static Query<RemoteUserSessionEntity> searchByUserId(RemoteCache<String, RemoteUserSessionEntity> cache, String realmId, String userId) {
        return cache.<RemoteUserSessionEntity>query(BY_USER_ID)
                .setParameter("realmId", realmId)
                .setParameter("userId", userId);
    }

    /**
     * Returns a projection with the user session, and the version of all user sessions belonging to the broker user
     * ID.
     */
    public static Query<RemoteUserSessionEntity> searchByBrokerUserId(RemoteCache<String, RemoteUserSessionEntity> cache, String realmId, String brokerUserId) {
        return cache.<RemoteUserSessionEntity>query(BY_BROKER_USER_ID)
                .setParameter("realmId", realmId)
                .setParameter("brokerUserId", brokerUserId);
    }
}

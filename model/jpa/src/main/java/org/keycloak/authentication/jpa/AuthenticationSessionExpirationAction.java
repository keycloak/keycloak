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

package org.keycloak.authentication.jpa;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.IntConsumer;

import jakarta.persistence.EntityManager;

import org.keycloak.connections.jpa.JpaConnectionProvider;
import org.keycloak.expiration.jpa.ExpirationAction;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.utils.SessionExpiration;

import org.hibernate.jpa.AvailableHints;
import org.jboss.logging.Logger;

import static org.keycloak.authentication.jpa.RootAuthenticationSessionEntity.SESSION_BUCKET_COUNT;

public enum AuthenticationSessionExpirationAction implements ExpirationAction {
    INSTANCE;

    private static final Logger logger = Logger.getLogger(AuthenticationSessionExpirationAction.class);

    private final Map<String, Integer> emptyObservedAt = new ConcurrentHashMap<>();

    @Override
    public boolean removeExpired(KeycloakSession session, String realmId, int currentTime, int maxRemoval, IntConsumer removeCount) {
        var realm = session.realms().getRealm(realmId);
        if (realm == null) {
            return false;
        }
        session.getContext().setRealm(realm);
        var lifespan = SessionExpiration.getAuthSessionLifespan(realm);
        var olderTimestamp = currentTime - lifespan;
        var em = session.getProvider(JpaConnectionProvider.class).getEntityManager();

        if (isRealmEmpty(em, realmId, currentTime, lifespan)) {
            return false;
        }

        boolean hasMore = false;
        int remaining = maxRemoval;
        for (int bucket = 0; bucket < SESSION_BUCKET_COUNT && remaining > 0; bucket++) {
            int bucketLimit = remaining;
            List<String> expiredIds = new ArrayList<>();
            List<String> nearMissIds = new ArrayList<>();

            var rows = em.createNamedQuery("findExpiredRootAuthSessionIdsByRealm", Object[].class)
                    .setParameter("realmId", realmId)
                    .setParameter("sessionBucket", bucket)
                    .setParameter("timestampCoarse", (long) olderTimestamp)
                    .setMaxResults(bucketLimit)
                    .getResultList();

            for (Object[] row : rows) {
                String id = (String) row[0];
                long timestamp = (long) row[1];
                if (timestamp < olderTimestamp) {
                    expiredIds.add(id);
                } else {
                    nearMissIds.add(id);
                }
            }

            if (!expiredIds.isEmpty()) {
                var removed = em.createNamedQuery("deleteExpiredRootAuthSessionByIds")
                        .setParameter("ids", expiredIds)
                        .setParameter("timestamp", (long) olderTimestamp)
                        .executeUpdate();
                removeCount.accept(removed);
                remaining -= removed;
            }

            if (!nearMissIds.isEmpty()) {
                int updated = em.createNamedQuery("setRootAuthSessionTimestampCoarseToExact")
                        .setParameter("ids", nearMissIds)
                        .executeUpdate();
                logger.debugf("Set coarse to exact for %d near-miss auth sessions in realm %s bucket %d", (Object) updated, realmId, bucket);
            }

            if (rows.size() >= bucketLimit) {
                hasMore = true;
            }
        }

        return hasMore;
    }

    /**
     * Checks with a single index lookup if the realm has no authentication sessions. A session created after an empty
     * observation cannot expire before the observation time plus the lifespan, so the check is remembered to skip the
     * per-bucket queries until then. Only the observation time is stored and the deadline is derived from the current
     * realm settings, so changing the lifespan needs no invalidation. The state is local to the node; a node without
     * state performs the regular check.
     */
    private boolean isRealmEmpty(EntityManager em, String realmId, int currentTime, int lifespan) {
        Integer observed = emptyObservedAt.get(realmId);
        // an observation in the future happens if the clock moved backwards, for example, a time offset in tests
        if (observed != null && observed <= currentTime && currentTime < (long) observed + lifespan) {
            return true;
        }
        boolean empty = em.createNamedQuery("findAnyRootAuthSessionIdByRealm", String.class)
                .setParameter("realmId", realmId)
                .setHint(AvailableHints.HINT_READ_ONLY, true)
                .setMaxResults(1)
                .getResultList()
                .isEmpty();
        if (empty) {
            emptyObservedAt.put(realmId, currentTime);
        } else {
            emptyObservedAt.remove(realmId);
        }
        return empty;
    }
}

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
import java.util.function.IntConsumer;

import org.keycloak.connections.jpa.JpaConnectionProvider;
import org.keycloak.expiration.jpa.ExpirationAction;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.utils.SessionExpiration;

import org.jboss.logging.Logger;

import static org.keycloak.authentication.jpa.RootAuthenticationSessionEntity.SESSION_BUCKET_COUNT;

enum AuthenticationSessionExpirationAction implements ExpirationAction {
    INSTANCE;

    private static final Logger logger = Logger.getLogger(AuthenticationSessionExpirationAction.class);

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

        boolean hasMore = false;
        for (int bucket = 0; bucket < SESSION_BUCKET_COUNT; bucket++) {
            List<String> expiredIds = new ArrayList<>();
            List<String> nearMissIds = new ArrayList<>();

            var rows = em.createNamedQuery("findExpiredRootAuthSessionIdsByRealm", Object[].class)
                    .setParameter("realmId", realmId)
                    .setParameter("sessionBucket", bucket)
                    .setParameter("timestampCoarse", (long) olderTimestamp)
                    .setMaxResults(maxRemoval)
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
            }

            if (!nearMissIds.isEmpty()) {
                int updated = em.createNamedQuery("setRootAuthSessionTimestampCoarseToExact")
                        .setParameter("ids", nearMissIds)
                        .executeUpdate();
                logger.debugf("Set coarse to exact for %d near-miss auth sessions in realm %s bucket %d", (Object) updated, realmId, bucket);
            }

            if (rows.size() >= maxRemoval) {
                hasMore = true;
            }
        }

        return hasMore;
    }
}

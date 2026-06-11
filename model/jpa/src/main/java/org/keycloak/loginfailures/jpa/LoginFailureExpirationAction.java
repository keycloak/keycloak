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

package org.keycloak.loginfailures.jpa;

import java.util.function.IntConsumer;

import org.keycloak.connections.jpa.JpaConnectionProvider;
import org.keycloak.expiration.jpa.ExpirationAction;
import org.keycloak.models.KeycloakSession;

public enum LoginFailureExpirationAction implements ExpirationAction {
    INSTANCE;

    @Override
    public boolean removeExpired(KeycloakSession session, String realmId, int currentTime, int maxRemoval, IntConsumer removeCount) {
        var realm = session.realms().getRealm(realmId);
        if (realm == null) {
            return false;
        }
        if (realm.isPermanentLockout() && realm.getMaxTemporaryLockouts() == 0) {
            // If mode is permanent lockout only, the "failure reset time" cannot be configured and login failures should never expire.
            return false;
        }
        // expired if last-failure + max-delta-time < current time
        var expired = currentTime - realm.getMaxDeltaTimeSeconds();
        var em = session.getProvider(JpaConnectionProvider.class).getEntityManager();
        var userIds = em.createNamedQuery("findExpiredLoginFailureUserIdsByRealm", String.class)
                .setParameter("realmId", realmId)
                .setParameter("expire", expired)
                .setMaxResults(maxRemoval)
                .getResultList();
        if (userIds.isEmpty()) {
            return false;
        }
        var removed = em.createNamedQuery("deleteExpiredLoginFailureByRealmAndUserIds")
                .setParameter("realmId", realmId)
                .setParameter("userIds", userIds)
                .setParameter("expire", expired)
                .executeUpdate();
        removeCount.accept(removed);
        return userIds.size() >= maxRemoval;
    }
}

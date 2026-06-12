/*
 * Copyright 2025 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.cluster.jpa;

import java.util.List;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;

import org.keycloak.cluster.ClusterLockStoreProvider;
import org.keycloak.common.util.Time;
import org.keycloak.connections.jpa.JpaConnectionProvider;
import org.keycloak.models.KeycloakSession;

import org.jboss.logging.Logger;

public class JpaClusterLockStoreProvider implements ClusterLockStoreProvider {

    private static final Logger logger = Logger.getLogger(JpaClusterLockStoreProvider.class);

    private static final int EXPIRY_MULTIPLIER = 10;

    private final KeycloakSession session;

    public JpaClusterLockStoreProvider(KeycloakSession session) {
        this.session = session;
    }

    @Override
    public boolean tryLock(String taskKey, int taskTimeoutInSeconds) {
        EntityManager em = getEntityManager();
        long expiresAt = Time.currentTimeMillis() + (long) taskTimeoutInSeconds * EXPIRY_MULTIPLIER * 1000;

        em.createNamedQuery("ClusterLockEntity.insertIfAbsent")
                .setParameter("taskKey", taskKey)
                .setParameter("expiresAt", expiresAt)
                .executeUpdate();

        List<ClusterLockEntity> result = em.createNamedQuery("ClusterLockEntity.tryLock", ClusterLockEntity.class)
                .setParameter("taskKey", taskKey)
                .setLockMode(LockModeType.PESSIMISTIC_WRITE)
                .setHint("jakarta.persistence.lock.timeout", -2)
                .getResultList();
        if (result.isEmpty()) {
            if (logger.isTraceEnabled()) {
                logger.tracef("Lock not acquired for task %s — row locked by another transaction", taskKey);
            }
            return false;
        }

        ClusterLockEntity entity = result.get(0);
        entity.setExpiresAt(expiresAt);
        em.merge(entity);

        if (logger.isTraceEnabled()) {
            logger.tracef("Lock acquired for task %s", taskKey);
        }
        return true;
    }

    @Override
    public void close() {
    }

    private EntityManager getEntityManager() {
        return session.getProvider(JpaConnectionProvider.class).getEntityManager();
    }
}

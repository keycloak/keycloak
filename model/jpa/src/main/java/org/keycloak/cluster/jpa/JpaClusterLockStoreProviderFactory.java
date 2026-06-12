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
import java.util.Set;
import java.util.concurrent.TimeUnit;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;

import org.keycloak.Config;
import org.keycloak.cluster.ClusterLockStoreProvider;
import org.keycloak.cluster.ClusterLockStoreProviderFactory;
import org.keycloak.common.util.Time;
import org.keycloak.connections.jpa.JpaConnectionProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.provider.Provider;
import org.keycloak.services.scheduled.ScheduledTaskRunner;
import org.keycloak.timer.TimerProvider;

import org.jboss.logging.Logger;

public class JpaClusterLockStoreProviderFactory implements ClusterLockStoreProviderFactory {

    private static final Logger logger = Logger.getLogger(JpaClusterLockStoreProviderFactory.class);

    public static final String ID = "jpa";

    private static final long LOCK_CLEANUP_INTERVAL_MS = TimeUnit.MINUTES.toMillis(30);

    @Override
    public ClusterLockStoreProvider create(KeycloakSession session) {
        return new JpaClusterLockStoreProvider(session);
    }

    @Override
    public void init(Config.Scope config) {
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
        KeycloakModelUtils.runJobInTransaction(factory, session -> {
            var runner = new ScheduledTaskRunner(factory, s -> {
                EntityManager em = session.getProvider(JpaConnectionProvider.class).getEntityManager();
                List<ClusterLockEntity> expired = em.createNamedQuery("ClusterLockEntity.selectExpired", ClusterLockEntity.class)
                        .setParameter("now", Time.currentTimeMillis())
                        .setLockMode(LockModeType.PESSIMISTIC_WRITE)
                        .setHint("jakarta.persistence.lock.timeout", -2)
                        .getResultList();
                for (ClusterLockEntity entity : expired) {
                    em.remove(entity);
                }
                if (!expired.isEmpty()) {
                    logger.debugf("Cleaned up %d expired cluster lock row(s)", expired.size());
                }
            });
            TimerProvider timer = session.getProvider(TimerProvider.class);
            timer.schedule(runner, LOCK_CLEANUP_INTERVAL_MS, LOCK_CLEANUP_INTERVAL_MS, "expired-cluster-lock-cleanup");
            logger.debug("Scheduled expired cluster lock cleanup with interval 30 minutes");
        });
    }

    @Override
    public Set<Class<? extends Provider>> dependsOn() {
        return Set.of(TimerProvider.class);
    }

    @Override
    public void close() {
    }

    @Override
    public String getId() {
        return ID;
    }
}

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

package org.keycloak.infinispan.health.impl;

import java.lang.invoke.MethodHandles;
import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;

import org.keycloak.infinispan.health.ClusterHealth;
import org.keycloak.jgroups.protocol.KEYCLOAK_JDBC_PING2;
import org.keycloak.jgroups.protocol.KEYCLOAK_JDBC_PING2.HealthStatus;

import org.infinispan.factories.annotations.Inject;
import org.infinispan.factories.scopes.Scope;
import org.infinispan.factories.scopes.Scopes;
import org.infinispan.remoting.transport.Transport;
import org.infinispan.remoting.transport.jgroups.JGroupsTransport;
import org.infinispan.util.concurrent.BlockingManager;
import org.jboss.logging.Logger;

/**
 * A {@link ClusterHealth} implementation that makes use of {@link KEYCLOAK_JDBC_PING2}.
 * <p>
 * Since each node is registered in the database, it is possible to detect if a partition is happening.
 * <p>
 * The method {@link KEYCLOAK_JDBC_PING2#healthStatus()} contains the algorithm description. If it returns
 * {@link HealthStatus#ERROR}, the healthy state does not change and relies on the Quarkus/Agroal readiness health
 * check. But, if {@link HealthStatus#NO_COORDINATOR} is returned, the state will be changed to unhealthy. The should be
 * a temporary situation as at least one coordinator must be present in the database table.
 *
 * @see KEYCLOAK_JDBC_PING2#healthStatus()
 */
@Scope(Scopes.GLOBAL)
public class JdbcPingClusterHealthImpl implements ClusterHealth {

    private static final Logger logger = Logger.getLogger(MethodHandles.lookup().lookupClass());

    private final ReentrantLock lock = new ReentrantLock();
    private volatile boolean healthy = true;
    private volatile HealthRunner runner;

    @Inject
    public void inject(Transport transport, BlockingManager blockingManager) {
        // hacking to avoid creating fields :)
        if (transport == null) {
            logger.debug("Cluster health check disabled. Local mode");
            return;
        }
        if (!(transport instanceof JGroupsTransport jgrp)) {
            logger.debug("JGroups Transport not found. Unable to check cluster health.");
            return;
        }
        KEYCLOAK_JDBC_PING2 ping = jgrp.getChannel().getProtocolStack().findProtocol(KEYCLOAK_JDBC_PING2.class);
        if (ping == null) {
            logger.warn("Stack 'jdbc-ping' not used. Unable to check cluster health.");
            return;
        }

        logger.debug("Cluster Health check available");
        init(ping, blockingManager.asExecutor("cluster-health"));
    }

    public void init(KEYCLOAK_JDBC_PING2 discovery, Executor executor) {
        runner = new HealthRunner(discovery, executor, this::checkHealth);
    }

    private void checkHealth(KEYCLOAK_JDBC_PING2 ping) {
        assert ping != null;
        if (!lock.tryLock()) {
            // check in progress
            return;
        }
        try {
            var status = ping.healthStatus();
            switch (status) {
                case HEALTHY:
                    logger.debug("Set cluster health status to healthy");
                    healthy = true;
                    break;
                case NO_COORDINATOR:
                    logger.warn("Unable to check the cluster health because no coordinator has been found.");
                    // fallthrough
                case UNHEALTHY:
                    logger.debug("Set cluster health status to unhealthy");
                    healthy = false;
                    break;
                case ERROR:
                    logger.debug("Error querying the database. Skip updating the cluster health status.");
                    break;
            }
        } finally {
            lock.unlock();
        }
    }

    @Override
    public boolean isHealthy() {
        return healthy;
    }

    @Override
    public void triggerClusterHealthCheck() {
        if (runner != null) {
            runner.trigger();
        }
    }

    @Override
    public boolean isSupported() {
        return runner != null;
    }

    private record HealthRunner(KEYCLOAK_JDBC_PING2 discovery, Executor executor, Consumer<KEYCLOAK_JDBC_PING2> check) {

        HealthRunner {
            Objects.requireNonNull(discovery);
            Objects.requireNonNull(executor);
            Objects.requireNonNull(check);
        }

        void trigger() {
            executor.execute(() -> check.accept(discovery));
        }
    }
}

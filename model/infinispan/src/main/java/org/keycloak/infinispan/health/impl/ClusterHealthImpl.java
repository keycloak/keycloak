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
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.infinispan.configuration.global.GlobalConfiguration;
import org.infinispan.factories.KnownComponentNames;
import org.infinispan.factories.annotations.ComponentName;
import org.infinispan.factories.annotations.Inject;
import org.infinispan.factories.annotations.Stop;
import org.infinispan.factories.scopes.Scope;
import org.infinispan.factories.scopes.Scopes;
import org.infinispan.remoting.transport.Transport;
import org.infinispan.remoting.transport.jgroups.JGroupsTransport;
import org.infinispan.util.concurrent.BlockingManager;
import org.jboss.logging.Logger;
import org.keycloak.infinispan.health.ClusterHealth;
import org.keycloak.infinispan.module.configuration.global.KeycloakConfiguration;
import org.keycloak.jgroups.protocol.KEYCLOAK_JDBC_PING2;
import org.keycloak.jgroups.protocol.KEYCLOAK_JDBC_PING2.HealthStatus;

/**
 * A {@link ClusterHealth} implementation that makes use of {@link KEYCLOAK_JDBC_PING2}.
 * <p>
 * Since each node is registered in the database, it is possible to detect if a partition is happening.
 * <p>
 * The method {@link KEYCLOAK_JDBC_PING2#isHealthy()} contains the algorithm description. If it returns
 * {@link HealthStatus#ERROR}, the healthy state does not change and relies on the Quarkus/Agroal readiness health
 * check. But, if {@link HealthStatus#NO_COORDINATOR} is returned, the state will be changed to unhealthy. The should be
 * a temporary situation as at least one coordinator must be present in the database table.
 *
 * @see KEYCLOAK_JDBC_PING2#isHealthy()
 */
@Scope(Scopes.GLOBAL)
public class ClusterHealthImpl implements ClusterHealth {

    private static final Logger logger = Logger.getLogger(MethodHandles.lookup().lookupClass());

    private volatile boolean healthy = true;
    private ScheduledFuture<?> future;

    @Inject
    public void inject(BlockingManager blockingManager,
                       @ComponentName(KnownComponentNames.EXPIRATION_SCHEDULED_EXECUTOR) ScheduledExecutorService scheduledExecutorService,
                       Transport transport,
                       GlobalConfiguration configuration
    ) {
        // hacking to avoid creating fields :)
        var kcConfig = configuration.module(KeycloakConfiguration.class);
        if (kcConfig == null) {
            logger.warn("Keycloak Configuration not found. Unable to check cluster health.");
            return;
        }
        int delayInSeconds = kcConfig.clusterHealthInterval();
        if (delayInSeconds <= 0) {
            logger.debug("Cluster health check disabled.");
            return;
        }
        if (transport == null) {
            logger.debug("Cluster health check disabled. Local mode");
            return;
        }
        if (!(transport instanceof JGroupsTransport jgrp)) {
            logger.warn("JGroups Transport not found. Unable to check cluster health.");
            return;
        }
        KEYCLOAK_JDBC_PING2 ping = jgrp.getChannel().getProtocolStack().findProtocol(KEYCLOAK_JDBC_PING2.class);
        if (ping == null) {
            logger.warn("Stack 'jdbc-ping' not used. Unable to check cluster health.");
            return;
        }

        logger.infof("Starting cluster health schedule task (interval=%s seconds)", delayInSeconds);
        future = scheduledExecutorService.scheduleWithFixedDelay(
                () -> blockingManager.runBlocking(() -> checkHealth(ping), "cluster-health"),
                delayInSeconds, delayInSeconds, TimeUnit.SECONDS);

    }

    private void checkHealth(KEYCLOAK_JDBC_PING2 ping) {
        var status = ping.isHealthy();
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
                // do not change the state
                logger.debug("Error querying the database. Skip updating the cluster health status.");
                break;
        }
    }

    @Override
    public boolean isHealthy() {
        return healthy;
    }

    @Stop
    public void stop() {
        if (future != null) {
            future.cancel(true);
        }
    }

}

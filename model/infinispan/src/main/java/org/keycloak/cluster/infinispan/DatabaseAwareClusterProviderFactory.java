/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.cluster.infinispan;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.keycloak.Config;
import org.keycloak.cluster.ClusterEventStoreProvider;
import org.keycloak.cluster.ClusterProvider;
import org.keycloak.cluster.jpa.PostgresClusterEventListener;
import org.keycloak.common.Profile;
import org.keycloak.connections.infinispan.InfinispanConnectionProvider;
import org.keycloak.connections.infinispan.NodeInfo;
import org.keycloak.connections.jpa.JpaConnectionProvider;
import org.keycloak.connections.jpa.JpaConnectionProviderFactory;
import org.keycloak.infinispan.util.InfinispanUtils;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.provider.Provider;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ProviderConfigurationBuilder;
import org.keycloak.services.scheduled.ScheduledTaskRunner;
import org.keycloak.timer.TimerProvider;
import org.keycloak.tracing.TracingProvider;

import org.infinispan.commons.marshall.Marshaller;
import org.jboss.logging.Logger;

/**
 * This implementation sends information to other clusters via a database table.
 *
 * @author Alexander Schwartz
 */
public class DatabaseAwareClusterProviderFactory extends InfinispanClusterProviderFactory {

    protected static final Logger logger = Logger.getLogger(DatabaseAwareClusterProviderFactory.class);

    private static final long DEFAULT_POLL_INTERVAL_MS = 100;
    private static final long FALLBACK_POLL_INTERVAL_MS = 30_000;

    private volatile NodeInfo nodeInfo;
    private volatile Marshaller protoStreamMarshaller;

    private Long pollIntervalMs;
    private boolean useListenNotify = true;
    private org.keycloak.cluster.jpa.PostgresClusterEventListener pgListener;

    @Override
    public ClusterProvider create(KeycloakSession session) {
        return new DatabaseAwareClusterProvider(super.create(session), session.getKeycloakSessionFactory(),
                nodeInfo, protoStreamMarshaller);
    }

    @Override
    public void init(Config.Scope config) {
        pollIntervalMs = config.getLong("pollInterval");
        if (pollIntervalMs != null) {
            if (pollIntervalMs <= 0) {
                throw new IllegalArgumentException("pollInterval must be a positive number");
            }
            if (pollIntervalMs > 60000) {
                logger.warnf("Polling interval is %d milliseconds. This is longer than 60 seconds, which seems to be too high for a production setting. Please verify.", pollIntervalMs);
            }
        }
        useListenNotify = config.getBoolean("useListenNotify", true);
    }

    @Override
    public List<ProviderConfigProperty> getConfigMetadata() {
        return ProviderConfigurationBuilder.create()
                .property()
                    .name("pollInterval")
                    .type("long")
                    .helpText("Interval in milliseconds between polling the database for new cluster events. "
                            + "When PostgreSQL LISTEN/NOTIFY is active, this becomes the fallback poll interval of " + FALLBACK_POLL_INTERVAL_MS + ".")
                    .defaultValue(DEFAULT_POLL_INTERVAL_MS)
                    .add()
                .property()
                    .name("useListenNotify")
                    .type("boolean")
                    .helpText("When enabled and the database is PostgreSQL, use LISTEN/NOTIFY for near-instant cluster event delivery "
                            + "instead of relying solely on polling. Set to false to disable and use polling only.")
                    .defaultValue(true)
                    .add()
                .build();
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
        KeycloakModelUtils.runJobInTransaction(factory, session -> {
            InfinispanConnectionProvider ispnConnections = session.getProvider(InfinispanConnectionProvider.class);
            nodeInfo = ispnConnections.getNodeInfo();
            this.protoStreamMarshaller = ispnConnections.getMarshaller();

            boolean listenerStarted = false;

            if (useListenNotify) {
                listenerStarted = tryStartListenNotify(factory);
            }

            var pollerTask = new DatabaseClusterEventPollerTask(nodeInfo.clusterName(), protoStreamMarshaller);
            var runner = new ScheduledTaskRunner(factory, pollerTask);
            TimerProvider timer = session.getProvider(TimerProvider.class);
            long interval = pollIntervalMs != null ? pollIntervalMs : listenerStarted ? FALLBACK_POLL_INTERVAL_MS : DEFAULT_POLL_INTERVAL_MS;
            timer.schedule(runner, interval, interval, "cluster-event-poller");

            if (listenerStarted) {
                logger.infof("Using PostgreSQL LISTEN/NOTIFY for cluster events (cluster '%s'), fallback poll interval %d ms",
                        nodeInfo.clusterName(), FALLBACK_POLL_INTERVAL_MS);
            } else {
                logger.infof("Scheduled cluster event poller with interval %d ms for cluster '%s'",
                        interval, nodeInfo.clusterName());
            }
        });
    }

    private boolean tryStartListenNotify(KeycloakSessionFactory factory) {
        JpaConnectionProviderFactory jpaFactory = (JpaConnectionProviderFactory)
                factory.getProviderFactory(JpaConnectionProvider.class);
        if (jpaFactory == null) {
            return false;
        }

        try (Connection testConn = jpaFactory.getConnection()) {
            if (!PostgresClusterEventListener.isSupported(testConn)) {
                return false;
            }
        } catch (SQLException e) {
            logger.warn("Failed to check PostgreSQL support for LISTEN/NOTIFY", e);
            return false;
        }

        pgListener = new PostgresClusterEventListener(jpaFactory::getConnection, () -> processEvents(factory));
        pgListener.start();
        return true;
    }

    private void processEvents(KeycloakSessionFactory factory) {
        TracingProvider tracing = factory.getProviderFactory(TracingProvider.class).create(null);
        try {
            tracing.trace(DatabaseAwareClusterProviderFactory.class, "notified", span -> {
                KeycloakModelUtils.runJobInTransaction(factory, s -> {
                    ClusterEventStoreProvider store = s.getProvider(ClusterEventStoreProvider.class);
                    if (store != null) {
                        DatabaseClusterEventPollerTask.processEvents(s, store, nodeInfo.clusterName(), protoStreamMarshaller);
                    }
                });
            });
        } finally {
            tracing.close();
        }
    }

    @Override
    public void close() {
        super.close();
        if (pgListener != null) {
            pgListener.close();
            pgListener = null;
        }
    }

    @Override
    public String getId() {
        return InfinispanUtils.EMBEDDED_PROVIDER_ID + "-db";
    }

    @Override
    public Set<Class<? extends Provider>> dependsOn() {
        HashSet<Class<? extends Provider>> result = new HashSet<>(super.dependsOn());
        result.add(TimerProvider.class);
        result.add(InfinispanConnectionProvider.class);
        return result;
    }

    @Override
    public boolean isSupported(Config.Scope config) {
        return InfinispanUtils.isEmbeddedInfinispan() &&
                Profile.isFeatureEnabled(Profile.Feature.CACHELESS);
    }

}

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

import java.util.HashSet;
import java.util.Set;

import org.keycloak.Config;
import org.keycloak.cluster.ClusterProvider;
import org.keycloak.common.Profile;
import org.keycloak.connections.infinispan.InfinispanConnectionProvider;
import org.keycloak.connections.infinispan.NodeInfo;
import org.keycloak.infinispan.util.InfinispanUtils;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.provider.Provider;
import org.keycloak.services.scheduled.ScheduledTaskRunner;
import org.keycloak.timer.TimerProvider;

import org.infinispan.commons.marshall.Marshaller;
import org.jboss.logging.Logger;

/**
 * This implementation sends information to other clusters via a database table.
 *
 * @author Alexander Schwartz
 */
public class DatabaseAwareClusterProviderFactory extends InfinispanClusterProviderFactory {

    protected static final Logger logger = Logger.getLogger(DatabaseAwareClusterProviderFactory.class);

    private static final long DEFAULT_POLL_INTERVAL_MS = 2000;

    private volatile NodeInfo nodeInfo;
    private volatile Marshaller protoStreamMarshaller;

    private long pollIntervalMs = DEFAULT_POLL_INTERVAL_MS;

    @Override
    public ClusterProvider create(KeycloakSession session) {
        return new DatabaseAwareClusterProvider(super.create(session), session.getKeycloakSessionFactory(),
                nodeInfo, protoStreamMarshaller);
    }

    @Override
    public void init(Config.Scope config) {
        pollIntervalMs = config.getLong("pollInterval", DEFAULT_POLL_INTERVAL_MS);
        if (pollIntervalMs <= 0) {
            throw new IllegalArgumentException("pollInterval must be a positive number");
        }
        if (pollIntervalMs > 60000) {
            logger.warnf("Polling interval is %d milliseconds. This is longer than 60 seconds, which seems to be too high for a production setting. Please verify.", pollIntervalMs);
        }
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
        KeycloakModelUtils.runJobInTransaction(factory, session -> {
            InfinispanConnectionProvider ispnConnections = session.getProvider(InfinispanConnectionProvider.class);
            nodeInfo = ispnConnections.getNodeInfo();

            this.protoStreamMarshaller = ispnConnections.getMarshaller();
            var pollerTask = new DatabaseClusterEventPollerTask(nodeInfo.clusterName(), protoStreamMarshaller);
            var runner = new ScheduledTaskRunner(factory, pollerTask);
            TimerProvider timer = session.getProvider(TimerProvider.class);
            timer.schedule(runner, pollIntervalMs, pollIntervalMs, "cluster-event-poller");
            logger.infof("Scheduled cluster event poller with interval %d ms for cluster '%s'", pollIntervalMs, nodeInfo.clusterName());
        });
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

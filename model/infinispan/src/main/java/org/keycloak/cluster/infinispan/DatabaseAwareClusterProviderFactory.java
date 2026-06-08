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
import org.keycloak.marshalling.Marshalling;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.provider.Provider;
import org.keycloak.services.scheduled.ScheduledTaskRunner;
import org.keycloak.timer.TimerProvider;

import org.infinispan.commons.marshall.ProtoStreamMarshaller;
import org.jboss.logging.Logger;

/**
 * This implementation is aware of Cross-Data-Center scenario too,
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class DatabaseAwareClusterProviderFactory extends InfinispanClusterProviderFactory {

    protected static final Logger logger = Logger.getLogger(DatabaseAwareClusterProviderFactory.class);

    private static final long DEFAULT_POLL_INTERVAL_MS = 2000;

    private volatile InfinispanClusterProvider clusterProvider;
    private volatile NodeInfo nodeInfo;
    private volatile ProtoStreamMarshaller protoStreamMarshaller;

    private long pollIntervalMs = DEFAULT_POLL_INTERVAL_MS;

    @Override
    public ClusterProvider create(KeycloakSession session) {
        return new DatabaseAwareClusterProvider(super.create(session), session.getKeycloakSessionFactory(),
                nodeInfo, protoStreamMarshaller);
    }

    private ProtoStreamMarshaller createMarshaller() {
        ProtoStreamMarshaller m = new ProtoStreamMarshaller();
        Marshalling.getSchemas().forEach(s -> {
            s.registerSchema(m.getSerializationContext());
            s.registerMarshallers(m.getSerializationContext());
        });
        return m;
    }

    @Override
    public void init(Config.Scope config) {
        pollIntervalMs = config.getLong("pollInterval", DEFAULT_POLL_INTERVAL_MS);
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
        KeycloakModelUtils.runJobInTransaction(factory, session -> {
            InfinispanConnectionProvider ispnConnections = session.getProvider(InfinispanConnectionProvider.class);
            nodeInfo = ispnConnections.getNodeInfo();
            if (nodeInfo.clusterName() == null) {
                logger.warn("No cluster name configured, cross-site event polling disabled");
                return;
            }

            var pollerTask = new DatabaseClusterEventPollerTask(nodeInfo.clusterName(), protoStreamMarshaller);
            var runner = new ScheduledTaskRunner(factory, pollerTask);
            TimerProvider timer = session.getProvider(TimerProvider.class);
            timer.schedule(runner, pollIntervalMs, pollIntervalMs, "cluster-event-poller");
            logger.infof("Scheduled cluster event poller with interval %d ms for cluster '%s'", pollIntervalMs, nodeInfo.clusterName());
            this.protoStreamMarshaller = createMarshaller();
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

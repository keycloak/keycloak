/*
 * Copyright 2021 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.testsuite.model.parameters;

import com.google.common.collect.ImmutableSet;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.keycloak.cluster.infinispan.remote.RemoteInfinispanClusterProviderFactory;
import org.keycloak.connections.infinispan.remote.RemoteLoadBalancerCheckProviderFactory;
import org.keycloak.infinispan.util.InfinispanUtils;
import org.keycloak.models.UserSessionSpi;
import org.keycloak.models.sessions.infinispan.remote.RemoteInfinispanAuthenticationSessionProviderFactory;
import org.keycloak.models.sessions.infinispan.remote.RemoteInfinispanSingleUseObjectProviderFactory;
import org.keycloak.models.sessions.infinispan.remote.RemoteStickySessionEncoderProviderFactory;
import org.keycloak.models.sessions.infinispan.remote.RemoteUserLoginFailureProviderFactory;
import org.keycloak.provider.ProviderFactory;
import org.keycloak.testsuite.model.Config;
import org.keycloak.testsuite.model.HotRodServerRule;
import org.keycloak.testsuite.model.KeycloakModelParameters;

import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

/**
 * Copied from {@link CrossDCInfinispan}.
 * <p>
 * Adds the new provider factories implementation
 */
public class RemoteInfinispan extends KeycloakModelParameters {

    private final HotRodServerRule hotRodServerRule = new HotRodServerRule();

    private static final AtomicInteger NODE_COUNTER = new AtomicInteger();

    private static final String SITE_1_MCAST_ADDR = "228.5.6.7";

    private static final String SITE_2_MCAST_ADDR = "228.6.7.8";

    private final Object lock = new Object();

    static final Set<Class<? extends ProviderFactory>> ALLOWED_FACTORIES = ImmutableSet.<Class<? extends ProviderFactory>>builder()
            .addAll(Infinispan.ALLOWED_FACTORIES)
            .add(RemoteInfinispanClusterProviderFactory.class)
            .add(RemoteInfinispanAuthenticationSessionProviderFactory.class)
            .add(RemoteInfinispanSingleUseObjectProviderFactory.class)
            .add(RemoteStickySessionEncoderProviderFactory.class)
            .add(RemoteLoadBalancerCheckProviderFactory.class)
            .add(RemoteUserLoginFailureProviderFactory.class)
            .build();

    @Override
    public void updateConfig(Config cf) {
        synchronized (lock) {
            NODE_COUNTER.incrementAndGet();
            cf.spi("connectionsInfinispan")
                    .provider("default")
                    .config("embedded", "true")
                    .config("clustered", "true")
                    .config("remoteStoreEnabled", "true")
                    .config("useKeycloakTimeService", "true")
                    .config("remoteStoreSecurityEnabled", "false")
                    .config("nodeName", "node-" + NODE_COUNTER.get())
                    .config("siteName", siteName(NODE_COUNTER.get()))
                    .config("remoteStorePort", siteName(NODE_COUNTER.get()).equals("site-2") ? "11333" : "11222")
                    .config("jgroupsUdpMcastAddr", mcastAddr(NODE_COUNTER.get()))
                    .spi(UserSessionSpi.NAME)
                    .provider(InfinispanUtils.EMBEDDED_PROVIDER_ID)
                    .config("offlineSessionCacheEntryLifespanOverride", "43200")
                    .config("offlineClientSessionCacheEntryLifespanOverride", "43200");
        }
    }

    public RemoteInfinispan() {
        super(Infinispan.ALLOWED_SPIS, ALLOWED_FACTORIES);
    }

    @Override
    public void beforeSuite(Config cf) {
        hotRodServerRule.createEmbeddedHotRodServer(cf.scope("connectionsInfinispan", "default"));
    }

    private static String siteName(int node) {
        return "site-" + (node % 2 == 0 ? 2 : 1);
    }

    private static String mcastAddr(int node) {
        return (node % 2 == 0) ? SITE_2_MCAST_ADDR : SITE_1_MCAST_ADDR;
    }

    @Override
    public <T> Stream<T> getParameters(Class<T> clazz) {
        if (HotRodServerRule.class.isAssignableFrom(clazz)) {
            return Stream.of((T) hotRodServerRule);
        } else {
            return Stream.empty();
        }
    }

    @Override
    public Statement classRule(Statement base, Description description) {
        return hotRodServerRule.apply(base, description);
    }
}

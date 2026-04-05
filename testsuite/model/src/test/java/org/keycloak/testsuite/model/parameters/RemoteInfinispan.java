/*
 * Copyright 2024 Red Hat, Inc. and/or its affiliates
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

import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import org.keycloak.cluster.infinispan.remote.RemoteInfinispanClusterProviderFactory;
import org.keycloak.connections.infinispan.remote.RemoteLoadBalancerCheckProviderFactory;
import org.keycloak.models.sessions.infinispan.remote.RemoteInfinispanAuthenticationSessionProviderFactory;
import org.keycloak.models.sessions.infinispan.remote.RemoteInfinispanSingleUseObjectProviderFactory;
import org.keycloak.models.sessions.infinispan.remote.RemoteStickySessionEncoderProviderFactory;
import org.keycloak.models.sessions.infinispan.remote.RemoteUserLoginFailureProviderFactory;
import org.keycloak.models.sessions.infinispan.remote.RemoteUserSessionProviderFactory;
import org.keycloak.provider.ProviderFactory;
import org.keycloak.spi.infinispan.CacheEmbeddedConfigProviderSpi;
import org.keycloak.spi.infinispan.CacheRemoteConfigProviderSpi;
import org.keycloak.spi.infinispan.impl.embedded.DefaultCacheEmbeddedConfigProviderFactory;
import org.keycloak.spi.infinispan.impl.remote.DefaultCacheRemoteConfigProviderFactory;
import org.keycloak.testsuite.model.Config;
import org.keycloak.testsuite.model.HotRodServerRule;
import org.keycloak.testsuite.model.KeycloakModelParameters;

import com.google.common.collect.ImmutableSet;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

/**
 * Enables RemoteInfinispan and adds all classes needed to connect to remote Infinispan to allowed factories
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
            .add(RemoteUserSessionProviderFactory.class)
            .build();

    @Override
    public void updateConfig(Config cf) {
        synchronized (lock) {
            var nodeCounter = NODE_COUNTER.incrementAndGet();
            var siteName = siteName(nodeCounter);
            cf.spi("connectionsInfinispan")
                    .provider("default")
                    .config("useKeycloakTimeService", "true");
            cf.spi(CacheRemoteConfigProviderSpi.SPI_NAME)
                    .provider(DefaultCacheRemoteConfigProviderFactory.PROVIDER_ID)
                    .config(DefaultCacheRemoteConfigProviderFactory.HOSTNAME, "localhost")
                    .config(DefaultCacheRemoteConfigProviderFactory.PORT, siteName.equals("site-2") ? "11333" : "11222");
            cf.spi(CacheEmbeddedConfigProviderSpi.SPI_NAME)
                    .provider(DefaultCacheEmbeddedConfigProviderFactory.PROVIDER_ID)
                    .config(DefaultCacheEmbeddedConfigProviderFactory.CONFIG, "test-ispn.xml")
                    .config(DefaultCacheEmbeddedConfigProviderFactory.SITE_NAME, siteName(NODE_COUNTER.get()))
                    .config(DefaultCacheEmbeddedConfigProviderFactory.NODE_NAME, "node-" + NODE_COUNTER.incrementAndGet());
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
            return Stream.of(clazz.cast(hotRodServerRule));
        } else {
            return Stream.empty();
        }
    }

    @Override
    public Statement classRule(Statement base, Description description) {
        return hotRodServerRule.apply(base, description);
    }
}

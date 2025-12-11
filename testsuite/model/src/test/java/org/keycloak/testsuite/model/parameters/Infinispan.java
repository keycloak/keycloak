/*
 * Copyright 2020 Red Hat, Inc. and/or its affiliates
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

import org.keycloak.cluster.infinispan.InfinispanClusterProviderFactory;
import org.keycloak.connections.infinispan.InfinispanConnectionProviderFactory;
import org.keycloak.connections.infinispan.InfinispanConnectionSpi;
import org.keycloak.infinispan.util.InfinispanUtils;
import org.keycloak.keys.PublicKeyStorageSpi;
import org.keycloak.keys.infinispan.InfinispanCachePublicKeyProviderFactory;
import org.keycloak.keys.infinispan.InfinispanPublicKeyStorageProviderFactory;
import org.keycloak.models.SingleUseObjectSpi;
import org.keycloak.models.UserLoginFailureSpi;
import org.keycloak.models.UserSessionSpi;
import org.keycloak.models.cache.CachePublicKeyProviderSpi;
import org.keycloak.models.cache.CacheRealmProviderSpi;
import org.keycloak.models.cache.CacheUserProviderSpi;
import org.keycloak.models.cache.authorization.CachedStoreFactorySpi;
import org.keycloak.models.cache.infinispan.InfinispanCacheRealmProviderFactory;
import org.keycloak.models.cache.infinispan.InfinispanUserCacheProviderFactory;
import org.keycloak.models.cache.infinispan.authorization.InfinispanCacheStoreFactoryProviderFactory;
import org.keycloak.models.cache.infinispan.organization.InfinispanOrganizationProviderFactory;
import org.keycloak.models.session.UserSessionPersisterSpi;
import org.keycloak.models.sessions.infinispan.InfinispanAuthenticationSessionProviderFactory;
import org.keycloak.models.sessions.infinispan.InfinispanSingleUseObjectProviderFactory;
import org.keycloak.models.sessions.infinispan.InfinispanUserLoginFailureProviderFactory;
import org.keycloak.models.sessions.infinispan.InfinispanUserSessionProviderFactory;
import org.keycloak.models.sessions.infinispan.transaction.InfinispanTransactionProviderFactory;
import org.keycloak.models.sessions.infinispan.transaction.InfinispanTransactionSpi;
import org.keycloak.provider.ProviderFactory;
import org.keycloak.provider.Spi;
import org.keycloak.sessions.AuthenticationSessionSpi;
import org.keycloak.sessions.StickySessionEncoderProviderFactory;
import org.keycloak.sessions.StickySessionEncoderSpi;
import org.keycloak.spi.infinispan.CacheEmbeddedConfigProviderFactory;
import org.keycloak.spi.infinispan.CacheEmbeddedConfigProviderSpi;
import org.keycloak.spi.infinispan.JGroupsCertificateProviderFactory;
import org.keycloak.spi.infinispan.JGroupsCertificateProviderSpi;
import org.keycloak.spi.infinispan.impl.embedded.DefaultCacheEmbeddedConfigProviderFactory;
import org.keycloak.storage.configuration.ServerConfigStorageProviderFactory;
import org.keycloak.storage.configuration.ServerConfigurationStorageProviderSpi;
import org.keycloak.testsuite.model.Config;
import org.keycloak.testsuite.model.KeycloakModelParameters;
import org.keycloak.timer.TimerProviderFactory;

import com.google.common.collect.ImmutableSet;

/**
 * @author hmlnarik
 */
public class Infinispan extends KeycloakModelParameters {

    private static final AtomicInteger NODE_COUNTER = new AtomicInteger();

    static final Set<Class<? extends Spi>> ALLOWED_SPIS = ImmutableSet.<Class<? extends Spi>>builder()
            .add(AuthenticationSessionSpi.class)
            .add(CacheRealmProviderSpi.class)
            .add(CachedStoreFactorySpi.class)
            .add(CacheUserProviderSpi.class)
            .add(InfinispanConnectionSpi.class)
            .add(StickySessionEncoderSpi.class)
            .add(UserSessionPersisterSpi.class)
            .add(SingleUseObjectSpi.class)
            .add(PublicKeyStorageSpi.class)
            .add(CachePublicKeyProviderSpi.class)
            .add(CacheEmbeddedConfigProviderSpi.class)
            .add(JGroupsCertificateProviderSpi.class)
            .add(ServerConfigurationStorageProviderSpi.class)
            .add(InfinispanTransactionSpi.class)
            .build();

    static final Set<Class<? extends ProviderFactory>> ALLOWED_FACTORIES = ImmutableSet.<Class<? extends ProviderFactory>>builder()
            .add(InfinispanAuthenticationSessionProviderFactory.class)
            .add(InfinispanCacheRealmProviderFactory.class)
            .add(InfinispanCacheStoreFactoryProviderFactory.class)
            .add(InfinispanClusterProviderFactory.class)
            .add(InfinispanConnectionProviderFactory.class)
            .add(InfinispanUserCacheProviderFactory.class)
            .add(InfinispanUserSessionProviderFactory.class)
            .add(InfinispanUserLoginFailureProviderFactory.class)
            .add(InfinispanSingleUseObjectProviderFactory.class)
            .add(StickySessionEncoderProviderFactory.class)
            .add(TimerProviderFactory.class)
            .add(InfinispanPublicKeyStorageProviderFactory.class)
            .add(InfinispanCachePublicKeyProviderFactory.class)
            .add(InfinispanOrganizationProviderFactory.class)
            .add(CacheEmbeddedConfigProviderFactory.class)
            .add(JGroupsCertificateProviderFactory.class)
            .add(ServerConfigStorageProviderFactory.class)
            .add(InfinispanTransactionProviderFactory.class)
            .build();

    @Override
    public void updateConfig(Config cf) {
        cf.spi("connectionsInfinispan")
                .provider("default")
                .config("useKeycloakTimeService", "true")
                .spi(UserLoginFailureSpi.NAME)
                .provider(InfinispanUtils.EMBEDDED_PROVIDER_ID)
                .config("stalledTimeoutInSeconds", "10")
                .spi(UserSessionSpi.NAME)
                .provider(InfinispanUtils.EMBEDDED_PROVIDER_ID)
                .config("sessionPreloadStalledTimeoutInSeconds", "10")
                .config("offlineSessionCacheEntryLifespanOverride", "43200")
                .config("offlineClientSessionCacheEntryLifespanOverride", "43200");
        cf.spi(CacheEmbeddedConfigProviderSpi.SPI_NAME)
                .provider(DefaultCacheEmbeddedConfigProviderFactory.PROVIDER_ID)
                .config(DefaultCacheEmbeddedConfigProviderFactory.CONFIG, "test-ispn.xml")
                .config(DefaultCacheEmbeddedConfigProviderFactory.NODE_NAME, "node-" + NODE_COUNTER.incrementAndGet());

    }

    public Infinispan() {
        super(ALLOWED_SPIS, ALLOWED_FACTORIES);
    }
}

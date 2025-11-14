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
package org.keycloak.crl.infinispan;

import java.security.cert.X509CRL;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

import org.keycloak.Config;
import org.keycloak.connections.infinispan.InfinispanConnectionProvider;
import org.keycloak.crl.CrlStorageProvider;
import org.keycloak.crl.CrlStorageProviderFactory;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ProviderConfigurationBuilder;

import org.infinispan.Cache;

/**
 *
 * @author rmartinc
 */
public class InfinispanCrlStorageProviderFactory implements CrlStorageProviderFactory, InfinispanCrlStorageProvider.SharedData {

    public static final String PROVIDER_ID = "infinispan";

    private volatile Cache<String, X509CRLEntry> crlCache;
    private final Map<String, FutureTask<X509CRL>> tasksInProgress = new ConcurrentHashMap<>();
    private volatile long cacheTime;
    private volatile long minTimeBetweenRequests;

    @Override
    public CrlStorageProvider create(KeycloakSession session) {
        lazyInit(session);
        return new InfinispanCrlStorageProvider(this);
    }

    @Override
    public List<ProviderConfigProperty> getConfigMetadata() {
        return ProviderConfigurationBuilder.create()
                .property()
                    .name("cacheTime")
                    .type("int")
                    .helpText(
                            """
                            Interval in seconds that the CRL is cached. The next update time of the CRL is always a minimum if present.
                            Zero or a negative value means CRL is cached until the next update time specified in the CRL (or infinite if the
                            CRL does not contain the next update).
                            """
                    )
                    .defaultValue(-1)
                    .add()
                .property()
                    .name("minTimeBetweenRequests")
                    .type("int")
                    .helpText(
                            """
                            Minimum interval in seconds between two requests to retrieve the CRL. The CRL is not updated
                            from the URL again until this minimum time has passed since the previous refresh. In theory
                            this option is never used if the CRL is refreshed correctly in the next update time.
                            The interval should be a positive number. Default 10 seconds.
                            """
                    )
                    .defaultValue(10)
                    .add()
                .build();
    }

    @Override
    public void init(Config.Scope config) {
        final long tmpCacheTime = config.getLong("cacheTime", -1L);
        cacheTime = tmpCacheTime > 0? TimeUnit.SECONDS.toMillis(tmpCacheTime) : -1L;

        final long tmpMinTimeBetweenRequests = config.getLong("minTimeBetweenRequests", 10L);
        minTimeBetweenRequests = tmpMinTimeBetweenRequests > 0? TimeUnit.SECONDS.toMillis(tmpMinTimeBetweenRequests) : 10_000L;
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
        // no-op
    }

    @Override
    public void close() {
        // no-op
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    private void lazyInit(KeycloakSession session) {
        if (crlCache == null) {
            synchronized (this) {
                if (crlCache == null) {
                    this.crlCache = session.getProvider(InfinispanConnectionProvider.class).getCache(InfinispanConnectionProvider.CRL_CACHE_NAME);
                }
            }
        }
    }

    @Override
    public Cache<String, X509CRLEntry> cache() {
        return crlCache;
    }

    @Override
    public Map<String, FutureTask<X509CRL>> tasksInProgress() {
        return tasksInProgress;
    }

    @Override
    public long cacheTime() {
        return cacheTime;
    }

    @Override
    public long minTimeBetweenRequests() {
        return minTimeBetweenRequests;
    }
}

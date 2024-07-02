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

package org.keycloak.crls.infinispan;

import org.infinispan.Cache;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.manager.DefaultCacheManager;
import org.jboss.logging.Logger;
import org.keycloak.Config;
import org.keycloak.crls.CrlLoader;
import org.keycloak.crls.CrlStorageProvider;
import org.keycloak.crls.CrlStorageProviderFactory;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.provider.EnvironmentDependentProviderFactory;
import org.keycloak.crls.CrlEntry;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ProviderConfigurationBuilder;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;

import static org.keycloak.connections.infinispan.InfinispanConnectionProvider.CRLS_CACHE_NAME;

/**
 * Crl Storage Provider Factory backed by Infinispan cache
 *
 * @author Joshua Smith
 * @author Scott Tustison
 */
public class InfinispanCrlStorageProviderFactory implements CrlStorageProviderFactory, EnvironmentDependentProviderFactory {

    private static final Logger log = Logger.getLogger(InfinispanCrlStorageProviderFactory.class);

    public static final String PROVIDER_ID = "infinispan";

    public enum CacheExpirationMode {
        NEVER_EXPIRE,
        NEXT_UPDATE,
        MAX_CACHE_TIME
    }

    private volatile Cache<String, CrlEntry> crlCache;

    private DefaultCacheManager cacheManager;

    private final Map<String, Future<CrlEntry>> tasksInProgress = new ConcurrentHashMap<>();

    private CacheExpirationMode cacheExpirationMode;

    private long maxCacheTime;

    @Override
    public CrlStorageProvider create(KeycloakSession session) {
        lazyInit(session);
        return new InfinispanCrlStorageProvider(session, crlCache, tasksInProgress, new CrlLoader(session), cacheExpirationMode, maxCacheTime);
    }

    private void lazyInit(KeycloakSession session) {
        if (crlCache == null) {
            synchronized (this) {
                if (crlCache == null) {
                    cacheManager = new DefaultCacheManager();
                    cacheManager.defineConfiguration(CRLS_CACHE_NAME, new ConfigurationBuilder().build());
                    Cache<String, CrlEntry> cache = cacheManager.getCache(CRLS_CACHE_NAME);
                    cache.addListener(new InfinispanCrlStorageProvider.CrlCacheListener(new InfinispanCrlStorageProvider(session, cache, tasksInProgress, new CrlLoader(session), cacheExpirationMode, maxCacheTime)));
                    crlCache = cache;
                }
            }
        }
    }

    @Override
    public List<ProviderConfigProperty> getConfigMetadata() {
        return ProviderConfigurationBuilder.create()
                .property()
                    .name("cacheExpirationMode")
                    .type("string")
                    .helpText("Controls how cached CRLs are updated. "
                            + "If 'NEVER_EXPIRE', cached CRLs will never expire. "
                            + "If 'NEXT_UPDATE', cached CRLs will expire when their nextUpdate field time has passed. "
                            + "If 'MAX_CACHE_TIME', cached CRLs will expire after an interval defined by the the maxCacheTime configuration.")
                    .defaultValue(CacheExpirationMode.NEVER_EXPIRE.name())
                    .add()
                .property()
                    .name("maxCacheTime")
                    .type("int")
                    .helpText("Maximum time in seconds that CRLs will be stored in the cache. This is to support "
                            + "cases in which CAs may update their CRLs before the nextUpdate time defined in the CRL. "
                            + "Negative values are interpreted as unlimited lifespan.")
                    .defaultValue(-1)
                    .add()
                .build();
    }

    @Override
    public void init(Config.Scope config) {
        String cacheExpirationModeValue = config.get("cacheExpirationMode", CacheExpirationMode.NEVER_EXPIRE.name());
        cacheExpirationMode = CacheExpirationMode.valueOf(cacheExpirationModeValue);

        maxCacheTime = config.getInt("maxCacheTime", -1);

        log.infof("cacheExpirationMode is %s maxCacheTime is %d", cacheExpirationMode, maxCacheTime);
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
        /* noop */
    }

    @Override
    public boolean isSupported() {
        return true;
    }

    @Override
    public void close() {
        if (this.cacheManager != null) {
            try {
                this.cacheManager.close();
            } catch (IOException e) {
                // TODO: Need to do any special handling here?
            }
        }
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }
}

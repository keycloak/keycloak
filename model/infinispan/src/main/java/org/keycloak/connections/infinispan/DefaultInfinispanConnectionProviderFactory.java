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

package org.keycloak.connections.infinispan;

import org.infinispan.configuration.cache.CacheMode;
import org.infinispan.configuration.cache.Configuration;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.configuration.global.GlobalConfigurationBuilder;
import org.infinispan.eviction.EvictionStrategy;
import org.infinispan.eviction.EvictionType;
import org.infinispan.manager.DefaultCacheManager;
import org.infinispan.manager.EmbeddedCacheManager;
import org.infinispan.transaction.LockingMode;
import org.infinispan.transaction.TransactionMode;
import org.infinispan.transaction.lookup.DummyTransactionManagerLookup;
import org.jboss.logging.Logger;
import org.keycloak.Config;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;

import javax.naming.InitialContext;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class DefaultInfinispanConnectionProviderFactory implements InfinispanConnectionProviderFactory {

    protected static final Logger logger = Logger.getLogger(DefaultInfinispanConnectionProviderFactory.class);

    protected Config.Scope config;

    protected EmbeddedCacheManager cacheManager;

    protected boolean containerManaged;

    @Override
    public InfinispanConnectionProvider create(KeycloakSession session) {
        lazyInit();

        return new DefaultInfinispanConnectionProvider(cacheManager);
    }

    @Override
    public void close() {
        if (cacheManager != null && !containerManaged) {
            cacheManager.stop();
        }
        cacheManager = null;
    }

    @Override
    public String getId() {
        return "default";
    }

    @Override
    public void init(Config.Scope config) {
        this.config = config;
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {

    }

    protected void lazyInit() {
        if (cacheManager == null) {
            synchronized (this) {
                if (cacheManager == null) {
                    String cacheContainer = config.get("cacheContainer");
                    if (cacheContainer != null) {
                        initContainerManaged(cacheContainer);
                    } else {
                        initEmbedded();
                    }
                }
            }
        }
    }

    protected void initContainerManaged(String cacheContainerLookup) {
        try {
            cacheManager = (EmbeddedCacheManager) new InitialContext().lookup(cacheContainerLookup);
            containerManaged = true;

            cacheManager.defineConfiguration(InfinispanConnectionProvider.REALM_REVISIONS_CACHE_NAME, getRevisionCacheConfig(true, InfinispanConnectionProvider.REALM_REVISIONS_CACHE_DEFAULT_MAX));
            cacheManager.getCache(InfinispanConnectionProvider.REALM_CACHE_NAME, true);

            long maxEntries = cacheManager.getCache(InfinispanConnectionProvider.USER_CACHE_NAME).getCacheConfiguration().eviction().maxEntries();
            if (maxEntries <= 0) {
                maxEntries = InfinispanConnectionProvider.USER_REVISIONS_CACHE_DEFAULT_MAX;
            }

            cacheManager.defineConfiguration(InfinispanConnectionProvider.USER_REVISIONS_CACHE_NAME, getRevisionCacheConfig(true, maxEntries));
            cacheManager.getCache(InfinispanConnectionProvider.USER_REVISIONS_CACHE_NAME, true);
            cacheManager.getCache(InfinispanConnectionProvider.AUTHORIZATION_CACHE_NAME, true);
            logger.debugv("Using container managed Infinispan cache container, lookup={1}", cacheContainerLookup);
        } catch (Exception e) {
            throw new RuntimeException("Failed to retrieve cache container", e);
        }
    }

    protected void initEmbedded() {
        GlobalConfigurationBuilder gcb = new GlobalConfigurationBuilder();

        boolean clustered = config.getBoolean("clustered", false);
        boolean async = config.getBoolean("async", true);
        boolean allowDuplicateJMXDomains = config.getBoolean("allowDuplicateJMXDomains", true);

        if (clustered) {
            gcb.transport().defaultTransport();
        }
        gcb.globalJmxStatistics().allowDuplicateDomains(allowDuplicateJMXDomains);

        cacheManager = new DefaultCacheManager(gcb.build());
        containerManaged = false;

        logger.debug("Started embedded Infinispan cache container");

        ConfigurationBuilder invalidationConfigBuilder = new ConfigurationBuilder();
        if (clustered) {
            invalidationConfigBuilder.clustering().cacheMode(async ? CacheMode.INVALIDATION_ASYNC : CacheMode.INVALIDATION_SYNC);
        }
        Configuration invalidationCacheConfiguration = invalidationConfigBuilder.build();

        cacheManager.defineConfiguration(InfinispanConnectionProvider.REALM_CACHE_NAME, invalidationCacheConfiguration);
        cacheManager.defineConfiguration(InfinispanConnectionProvider.USER_CACHE_NAME, invalidationCacheConfiguration);

        ConfigurationBuilder sessionConfigBuilder = new ConfigurationBuilder();
        if (clustered) {
            String sessionsMode = config.get("sessionsMode", "distributed");
            if (sessionsMode.equalsIgnoreCase("replicated")) {
                sessionConfigBuilder.clustering().cacheMode(async ? CacheMode.REPL_ASYNC : CacheMode.REPL_SYNC);
            } else if (sessionsMode.equalsIgnoreCase("distributed")) {
                sessionConfigBuilder.clustering().cacheMode(async ? CacheMode.DIST_ASYNC : CacheMode.DIST_SYNC);
            } else {
                throw new RuntimeException("Invalid value for sessionsMode");
            }

            sessionConfigBuilder.clustering().hash()
                    .numOwners(config.getInt("sessionsOwners", 2))
                    .numSegments(config.getInt("sessionsSegments", 60)).build();
        }

        Configuration sessionCacheConfiguration = sessionConfigBuilder.build();
        cacheManager.defineConfiguration(InfinispanConnectionProvider.SESSION_CACHE_NAME, sessionCacheConfiguration);
        cacheManager.defineConfiguration(InfinispanConnectionProvider.OFFLINE_SESSION_CACHE_NAME, sessionCacheConfiguration);
        cacheManager.defineConfiguration(InfinispanConnectionProvider.LOGIN_FAILURE_CACHE_NAME, sessionCacheConfiguration);
        cacheManager.defineConfiguration(InfinispanConnectionProvider.AUTHORIZATION_CACHE_NAME, sessionCacheConfiguration);

        ConfigurationBuilder replicationConfigBuilder = new ConfigurationBuilder();
        if (clustered) {
            replicationConfigBuilder.clustering().cacheMode(async ? CacheMode.REPL_ASYNC : CacheMode.REPL_SYNC);
        }
        Configuration replicationCacheConfiguration = replicationConfigBuilder.build();
        cacheManager.defineConfiguration(InfinispanConnectionProvider.WORK_CACHE_NAME, replicationCacheConfiguration);

        ConfigurationBuilder counterConfigBuilder = new ConfigurationBuilder();
        counterConfigBuilder.invocationBatching().enable()
                .transaction().transactionMode(TransactionMode.TRANSACTIONAL);
        counterConfigBuilder.transaction().transactionManagerLookup(new DummyTransactionManagerLookup());
        counterConfigBuilder.transaction().lockingMode(LockingMode.PESSIMISTIC);

        cacheManager.defineConfiguration(InfinispanConnectionProvider.REALM_REVISIONS_CACHE_NAME, getRevisionCacheConfig(false, InfinispanConnectionProvider.REALM_REVISIONS_CACHE_DEFAULT_MAX));
        cacheManager.getCache(InfinispanConnectionProvider.REALM_CACHE_NAME, true);

        long maxEntries = cacheManager.getCache(InfinispanConnectionProvider.USER_CACHE_NAME).getCacheConfiguration().eviction().maxEntries();
        if (maxEntries <= 0) {
            maxEntries = InfinispanConnectionProvider.USER_REVISIONS_CACHE_DEFAULT_MAX;
        }

        cacheManager.defineConfiguration(InfinispanConnectionProvider.USER_REVISIONS_CACHE_NAME, getRevisionCacheConfig(false, maxEntries));
        cacheManager.getCache(InfinispanConnectionProvider.USER_REVISIONS_CACHE_NAME, true);
    }

    private Configuration getRevisionCacheConfig(boolean managed, long maxEntries) {
        ConfigurationBuilder cb = new ConfigurationBuilder();
        cb.invocationBatching().enable().transaction().transactionMode(TransactionMode.TRANSACTIONAL);

        // Workaround: Use Dummy manager even in managed ( wildfly/eap ) environment. Without this workaround, there is an issue in EAP7 overlay.
        // After start+end revisions batch is left the JTA transaction in committed state. This is incorrect and causes other issues afterwards.
        // TODO: Investigate
        // if (!managed)
            cb.transaction().transactionManagerLookup(new DummyTransactionManagerLookup());

        cb.transaction().lockingMode(LockingMode.PESSIMISTIC);

        cb.eviction().strategy(EvictionStrategy.LRU).type(EvictionType.COUNT).size(maxEntries);
        return cb.build();
    }

}

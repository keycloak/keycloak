/*
 * Copyright 2017 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.adapters.saml.wildfly.infinispan;

import org.keycloak.adapters.saml.AdapterConstants;
import org.keycloak.adapters.spi.SessionIdMapper;
import org.keycloak.adapters.spi.SessionIdMapperUpdater;

import io.undertow.servlet.api.DeploymentInfo;
import java.util.*;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import org.infinispan.Cache;
import org.infinispan.configuration.cache.CacheMode;
import org.infinispan.configuration.cache.Configuration;
import org.infinispan.manager.EmbeddedCacheManager;
import org.infinispan.persistence.manager.PersistenceManager;
import org.infinispan.persistence.remote.RemoteStore;
import org.jboss.logging.Logger;

/**
 *
 * @author hmlnarik
 */
public class InfinispanSessionCacheIdMapperUpdater {

    private static final Logger LOG = Logger.getLogger(InfinispanSessionCacheIdMapperUpdater.class);

    public static final String DEFAULT_CACHE_CONTAINER_JNDI_NAME = "java:jboss/infinispan/container";

    public static SessionIdMapperUpdater addTokenStoreUpdaters(DeploymentInfo deploymentInfo, SessionIdMapper mapper, SessionIdMapperUpdater previousIdMapperUpdater) {
        Map<String, String> initParameters = deploymentInfo.getInitParameters();
        String containerName = initParameters == null ? null : initParameters.get(AdapterConstants.REPLICATION_CONFIG_CONTAINER_PARAM_NAME);
        String cacheName = initParameters == null ? null : initParameters.get(AdapterConstants.REPLICATION_CONFIG_SSO_CACHE_PARAM_NAME);

        if (containerName == null || cacheName == null) {
            LOG.warnv("Cannot determine parameters of SSO cache for deployment {0}.", deploymentInfo.getDeploymentName());

            return previousIdMapperUpdater;
        }

        String cacheContainerLookup = DEFAULT_CACHE_CONTAINER_JNDI_NAME + "/" + containerName;
        String deploymentSessionCacheName = deploymentInfo.getDeploymentName();

        try {
            EmbeddedCacheManager cacheManager = (EmbeddedCacheManager) new InitialContext().lookup(cacheContainerLookup);

            Configuration ssoCacheConfiguration = cacheManager.getCacheConfiguration(cacheName);
            if (ssoCacheConfiguration == null) {
                Configuration cacheConfiguration = cacheManager.getCacheConfiguration(deploymentSessionCacheName);
                if (cacheConfiguration == null) {
                    LOG.debugv("Using default configuration for SSO cache {0}.{1}.", containerName, cacheName);
                    ssoCacheConfiguration = cacheManager.getDefaultCacheConfiguration();
                } else {
                    LOG.debugv("Using distributed HTTP session cache configuration for SSO cache {0}.{1}, configuration taken from cache {2}",
                      containerName, cacheName, deploymentSessionCacheName);
                    ssoCacheConfiguration = cacheConfiguration;
                    cacheManager.defineConfiguration(cacheName, ssoCacheConfiguration);
                }
            } else {
                LOG.debugv("Using custom configuration of SSO cache {0}.{1}.", containerName, cacheName);
            }

            CacheMode ssoCacheMode = ssoCacheConfiguration.clustering().cacheMode();
            if (ssoCacheMode != CacheMode.REPL_ASYNC && ssoCacheMode != CacheMode.REPL_SYNC) {
                LOG.warnv("SSO cache mode is {0}, it is recommended to use replicated mode instead.", ssoCacheConfiguration.clustering().cacheModeString());
            }

            Cache<String, String[]> ssoCache = cacheManager.getCache(cacheName, true);
            final SsoSessionCacheListener listener = new SsoSessionCacheListener(ssoCache, mapper);
            ssoCache.addListener(listener);

            addSsoCacheCrossDcListener(ssoCache, listener);

            LOG.debugv("Added distributed SSO session cache, lookup={0}, cache name={1}", cacheContainerLookup, cacheName);

            LOG.debugv("Adding session listener for SSO session cache, lookup={0}, cache name={1}", cacheContainerLookup, cacheName);
            SsoCacheSessionIdMapperUpdater updater = new SsoCacheSessionIdMapperUpdater(ssoCache, previousIdMapperUpdater);
            deploymentInfo.addSessionListener(updater);

            return updater;
        } catch (NamingException ex) {
            LOG.warnv("Failed to obtain distributed session cache container, lookup={0}", cacheContainerLookup);
            return previousIdMapperUpdater;
        }
    }

    private static void addSsoCacheCrossDcListener(Cache<String, String[]> ssoCache, SsoSessionCacheListener listener) {
        if (ssoCache.getCacheConfiguration().persistence() == null) {
            return;
        }

        final Set<RemoteStore> stores = getRemoteStores(ssoCache);
        if (stores == null || stores.isEmpty()) {
            return;
        }

        LOG.infov("Listening for events on remote stores configured for cache {0}", ssoCache.getName());

        for (RemoteStore store : stores) {
            store.getRemoteCache().addClientListener(listener);
        }
    }

    public static Set<RemoteStore> getRemoteStores(Cache ispnCache) {
        return ispnCache.getAdvancedCache().getComponentRegistry().getComponent(PersistenceManager.class).getStores(RemoteStore.class);
    }
}

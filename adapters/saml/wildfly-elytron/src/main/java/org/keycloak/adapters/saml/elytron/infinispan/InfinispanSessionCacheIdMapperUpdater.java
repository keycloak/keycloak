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
package org.keycloak.adapters.saml.elytron.infinispan;

import java.util.Set;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import jakarta.servlet.ServletContext;

import org.keycloak.adapters.saml.AdapterConstants;
import org.keycloak.adapters.spi.SessionIdMapper;
import org.keycloak.adapters.spi.SessionIdMapperUpdater;

import org.infinispan.Cache;
import org.infinispan.configuration.cache.CacheMode;
import org.infinispan.configuration.cache.Configuration;
import org.infinispan.factories.ComponentRegistry;
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

    public static SessionIdMapperUpdater addTokenStoreUpdaters(ServletContext servletContext, SessionIdMapper mapper, SessionIdMapperUpdater previousIdMapperUpdater) {
        String containerName = servletContext.getInitParameter(AdapterConstants.REPLICATION_CONFIG_CONTAINER_PARAM_NAME);
        String cacheName = servletContext.getInitParameter(AdapterConstants.REPLICATION_CONFIG_SSO_CACHE_PARAM_NAME);

        // the following is based on https://github.com/jbossas/jboss-as/blob/7.2.0.Final/clustering/web-infinispan/src/main/java/org/jboss/as/clustering/web/infinispan/DistributedCacheManagerFactory.java#L116-L122
        String contextPath = servletContext.getContextPath();
        if (contextPath == null || contextPath.isEmpty() || "/".equals(contextPath)) {
            contextPath = "/ROOT";
        }
        String deploymentSessionCacheName = contextPath;

        if (containerName == null || cacheName == null) {
            LOG.warnv("Cannot determine parameters of SSO cache for deployment {0}.", contextPath);

            return previousIdMapperUpdater;
        }

        String cacheContainerLookup = DEFAULT_CACHE_CONTAINER_JNDI_NAME + "/" + containerName;

        try {
            EmbeddedCacheManager cacheManager = (EmbeddedCacheManager) new InitialContext().lookup(cacheContainerLookup);

            Configuration ssoCacheConfiguration = cacheManager.getCacheConfiguration(cacheName);
            if (ssoCacheConfiguration == null) {
                // Fallback to use cache "/my-app-deployment-context" as template
                ssoCacheConfiguration = tryDefineCacheConfigurationFromTemplate(cacheManager, containerName, cacheName, deploymentSessionCacheName);

                if (ssoCacheConfiguration == null) {
                    // Fallback to use cache "my-app-deployment-context.war" as template
                    if (cacheName.lastIndexOf('.') != -1) {
                        String templateName = cacheName.substring(0, cacheName.lastIndexOf('.'));
                        ssoCacheConfiguration = tryDefineCacheConfigurationFromTemplate(cacheManager, containerName, cacheName, templateName);
                    }
                }

                if (ssoCacheConfiguration == null) {
                    // Finally fallback to the cache container default configuration
                    LOG.debugv("Using default configuration for SSO cache {0}.{1}.", containerName, cacheName);
                    ssoCacheConfiguration = cacheManager.getDefaultCacheConfiguration();
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
            SsoSessionCacheListener listener = new SsoSessionCacheListener(ssoCache, mapper);
            ssoCache.addListener(listener);

            addSsoCacheCrossDcListener(ssoCache, listener);

            LOG.debugv("Added distributed SSO session cache, lookup={0}, cache name={1}", cacheContainerLookup, cacheName);

            return new SsoCacheSessionIdMapperUpdater(ssoCache, previousIdMapperUpdater) {
                @Override
                public void close() {
                    ssoCache.stop();
                }
            };
        } catch (NamingException ex) {
            LOG.warnv("Failed to obtain distributed session cache container, lookup={0}", cacheContainerLookup);
            return previousIdMapperUpdater;
        }
    }

    /**
     * Try to define new cache configuration "newCacheName" from the existing configuration "templateCacheName" .
     *
     * @return Newly defined configuration or null in case that definition of new configuration was not successful
     */
    private static Configuration tryDefineCacheConfigurationFromTemplate(EmbeddedCacheManager cacheManager, String containerName, String newCacheName, String templateCacheName) {
        Configuration cacheConfiguration = cacheManager.getCacheConfiguration(templateCacheName);
        if (cacheConfiguration != null) {
            LOG.debugv("Using distributed HTTP session cache configuration for SSO cache {0}.{1}, configuration taken from cache {2}",
                    containerName, newCacheName, templateCacheName);
            return cacheManager.defineConfiguration(newCacheName, cacheConfiguration);
        } else {
            // templateCacheName configuration did not exists, so returning null
            return null;
        }
    }

    private static void addSsoCacheCrossDcListener(Cache<String, String[]> ssoCache, SsoSessionCacheListener listener) {
        if (ssoCache.getCacheConfiguration().persistence() == null) {
            return;
        }

        Set<RemoteStore> stores = getRemoteStores(ssoCache);
        if (stores == null || stores.isEmpty()) {
            return;
        }

        LOG.infov("Listening for events on remote stores configured for cache {0}", ssoCache.getName());

        for (RemoteStore store : stores) {
            store.getRemoteCache().addClientListener(listener);
        }
    }

    public static Set<RemoteStore> getRemoteStores(Cache<?, ?> ispnCache) {
        return ComponentRegistry.componentOf(ispnCache, PersistenceManager.class).getStores(RemoteStore.class);
    }
}

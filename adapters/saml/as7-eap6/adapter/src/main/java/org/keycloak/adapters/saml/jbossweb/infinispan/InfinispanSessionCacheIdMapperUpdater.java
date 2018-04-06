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
package org.keycloak.adapters.saml.jbossweb.infinispan;

import org.keycloak.adapters.saml.AdapterConstants;
import org.keycloak.adapters.spi.SessionIdMapper;
import org.keycloak.adapters.spi.SessionIdMapperUpdater;

import java.util.List;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.ServletContext;
import org.apache.catalina.Context;
import org.infinispan.Cache;
import org.infinispan.configuration.cache.CacheMode;
import org.infinispan.configuration.cache.Configuration;
import org.infinispan.loaders.CacheLoaderManager;
import org.infinispan.loaders.remote.RemoteCacheStore;
import org.infinispan.manager.EmbeddedCacheManager;
import org.jboss.logging.Logger;

/**
 *
 * @author hmlnarik
 */
public class InfinispanSessionCacheIdMapperUpdater {

    private static final Logger LOG = Logger.getLogger(InfinispanSessionCacheIdMapperUpdater.class);

    public static final String DEFAULT_CACHE_CONTAINER_JNDI_NAME = "java:jboss/infinispan/container";

    public static SessionIdMapperUpdater addTokenStoreUpdaters(Context context, SessionIdMapper mapper, SessionIdMapperUpdater previousIdMapperUpdater) {
        ServletContext servletContext = context.getServletContext();
        String containerName = servletContext == null ? null : servletContext.getInitParameter(AdapterConstants.REPLICATION_CONFIG_CONTAINER_PARAM_NAME);
        String cacheName = servletContext == null ? null : servletContext.getInitParameter(AdapterConstants.REPLICATION_CONFIG_SSO_CACHE_PARAM_NAME);

        // the following is based on https://github.com/jbossas/jboss-as/blob/7.2.0.Final/clustering/web-infinispan/src/main/java/org/jboss/as/clustering/web/infinispan/DistributedCacheManagerFactory.java#L116-L122
        String host = context.getParent() == null ? "" : context.getParent().getName();
        String contextPath = context.getPath();
        if ("/".equals(contextPath)) {
            contextPath = "/ROOT";
        }
        String deploymentSessionCacheName = host + contextPath;

        if (containerName == null || cacheName == null || deploymentSessionCacheName == null) {
            LOG.warnv("Cannot determine parameters of SSO cache for deployment {0}.", host + contextPath);

            return previousIdMapperUpdater;
        }

        String cacheContainerLookup = DEFAULT_CACHE_CONTAINER_JNDI_NAME + "/" + containerName;

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

            // Not possible to add listener for cross-DC support because of too old Infinispan in AS 7
            warnIfRemoteStoreIsUsed(ssoCache);

            LOG.debugv("Added distributed SSO session cache, lookup={0}, cache name={1}", cacheContainerLookup, cacheName);

            SsoCacheSessionIdMapperUpdater updater = new SsoCacheSessionIdMapperUpdater(ssoCache, previousIdMapperUpdater);

            return updater;
        } catch (NamingException ex) {
            LOG.warnv("Failed to obtain distributed session cache container, lookup={0}", cacheContainerLookup);
            return previousIdMapperUpdater;
        }
    }

    private static void warnIfRemoteStoreIsUsed(Cache<String, String[]> ssoCache) {
        final List<RemoteCacheStore> stores = getRemoteStores(ssoCache);
        if (stores == null || stores.isEmpty()) {
            return;
        }

        LOG.warnv("Unable to listen for events on remote stores configured for cache {0} (unsupported in this Infinispan limitations), logouts will not be propagated.", ssoCache.getName());
    }

    public static List<RemoteCacheStore> getRemoteStores(Cache ssoCache) {
        return ssoCache.getAdvancedCache().getComponentRegistry().getComponent(CacheLoaderManager.class).getCacheLoaders(RemoteCacheStore.class);
    }
}

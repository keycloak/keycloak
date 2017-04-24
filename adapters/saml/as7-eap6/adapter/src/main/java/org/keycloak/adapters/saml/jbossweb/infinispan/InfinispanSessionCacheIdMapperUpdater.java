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

import org.keycloak.adapters.spi.SessionIdMapper;
import org.keycloak.adapters.spi.SessionIdMapperUpdater;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.ServletContext;
import org.apache.catalina.Context;
import org.infinispan.Cache;
import org.infinispan.configuration.cache.CacheMode;
import org.infinispan.configuration.cache.Configuration;
import org.infinispan.manager.EmbeddedCacheManager;
import org.jboss.logging.Logger;

/**
 *
 * @author hmlnarik
 */
public class InfinispanSessionCacheIdMapperUpdater {

    private static final Logger LOG = Logger.getLogger(InfinispanSessionCacheIdMapperUpdater.class);

    public static final String DEFAULT_CACHE_CONTAINER_JNDI_NAME = "java:jboss/infinispan/container/web";

    private static final String DEPLOYMENT_CACHE_CONTAINER_JNDI_NAME_PARAM_NAME = "keycloak.sessionIdMapperUpdater.infinispan.cacheContainerJndi";
    private static final String DEPLOYMENT_CACHE_NAME_PARAM_NAME = "keycloak.sessionIdMapperUpdater.infinispan.deploymentCacheName";
    private static final String SSO_CACHE_NAME_PARAM_NAME = "keycloak.sessionIdMapperUpdater.infinispan.cacheName";

    public static SessionIdMapperUpdater addTokenStoreUpdaters(Context context, SessionIdMapper mapper, SessionIdMapperUpdater previousIdMapperUpdater) {
       boolean distributable = context.getDistributable();

        if (! distributable) {
            LOG.warnv("Deployment {0} does not use supported distributed session cache mechanism", context.getName());
            return previousIdMapperUpdater;
        }

        ServletContext servletContext = context.getServletContext();
        String cacheContainerLookup = (servletContext != null && servletContext.getInitParameter(DEPLOYMENT_CACHE_CONTAINER_JNDI_NAME_PARAM_NAME) != null)
          ? servletContext.getInitParameter(DEPLOYMENT_CACHE_CONTAINER_JNDI_NAME_PARAM_NAME)
          : DEFAULT_CACHE_CONTAINER_JNDI_NAME;

        // the following is based on https://github.com/jbossas/jboss-as/blob/7.2.0.Final/clustering/web-infinispan/src/main/java/org/jboss/as/clustering/web/infinispan/DistributedCacheManagerFactory.java#L116-L122
        String host = context.getParent() == null ? "" : context.getParent().getName();
        String contextPath = context.getPath();
        if ("/".equals(contextPath)) {
            contextPath = "/ROOT";
        }

        boolean deploymentSessionCacheNamePreset = servletContext != null && servletContext.getInitParameter(DEPLOYMENT_CACHE_NAME_PARAM_NAME) != null;
        String deploymentSessionCacheName = deploymentSessionCacheNamePreset
          ? servletContext.getInitParameter(DEPLOYMENT_CACHE_NAME_PARAM_NAME)
          : host + contextPath;
        boolean ssoCacheNamePreset = servletContext != null && servletContext.getInitParameter(SSO_CACHE_NAME_PARAM_NAME) != null;
        String ssoCacheName = ssoCacheNamePreset
          ? servletContext.getInitParameter(SSO_CACHE_NAME_PARAM_NAME)
          : deploymentSessionCacheName + ".ssoCache";

        try {
            EmbeddedCacheManager cacheManager = (EmbeddedCacheManager) new InitialContext().lookup(cacheContainerLookup);

            Configuration ssoCacheConfiguration = cacheManager.getCacheConfiguration(ssoCacheName);
            if (ssoCacheConfiguration == null) {
                Configuration cacheConfiguration = cacheManager.getCacheConfiguration(deploymentSessionCacheName);
                if (cacheConfiguration == null) {
                    LOG.debugv("Using default cache container configuration for SSO cache. lookup={0}, looked up configuration of cache={1}", cacheContainerLookup, deploymentSessionCacheName);
                    ssoCacheConfiguration = cacheManager.getDefaultCacheConfiguration();
                } else {
                    LOG.debugv("Using distributed HTTP session cache configuration for SSO cache. lookup={0}, configuration taken from cache={1}", cacheContainerLookup, deploymentSessionCacheName);
                    ssoCacheConfiguration = cacheConfiguration;
                    cacheManager.defineConfiguration(ssoCacheName, ssoCacheConfiguration);
                }
            } else {
                LOG.debugv("Using custom configuration for SSO cache. lookup={0}, cache name={1}", cacheContainerLookup, ssoCacheName);
            }

            CacheMode ssoCacheMode = ssoCacheConfiguration.clustering().cacheMode();
            if (ssoCacheMode != CacheMode.REPL_ASYNC && ssoCacheMode != CacheMode.REPL_SYNC) {
                LOG.warnv("SSO cache mode is {0}, it is recommended to use replicated mode instead", ssoCacheConfiguration.clustering().cacheModeString());
            }

            Cache<String, String[]> ssoCache = cacheManager.getCache(ssoCacheName, true);
            ssoCache.addListener(new SsoSessionCacheListener(mapper));

            LOG.debugv("Added distributed SSO session cache, lookup={0}, cache name={1}", cacheContainerLookup, deploymentSessionCacheName);

            SsoCacheSessionIdMapperUpdater updater = new SsoCacheSessionIdMapperUpdater(ssoCache, previousIdMapperUpdater);

            return updater;
        } catch (NamingException ex) {
            LOG.warnv("Failed to obtain distributed session cache container, lookup={0}", cacheContainerLookup);
            return previousIdMapperUpdater;
        }
    }
}

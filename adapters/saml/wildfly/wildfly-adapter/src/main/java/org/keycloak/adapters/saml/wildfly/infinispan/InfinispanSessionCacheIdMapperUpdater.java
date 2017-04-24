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

    public static SessionIdMapperUpdater addTokenStoreUpdaters(DeploymentInfo deploymentInfo, SessionIdMapper mapper, SessionIdMapperUpdater previousIdMapperUpdater) {
       boolean distributable = Objects.equals(
          deploymentInfo.getSessionManagerFactory().getClass().getName(),
          "org.wildfly.clustering.web.undertow.session.DistributableSessionManagerFactory"
        );

        if (! distributable) {
            LOG.warnv("Deployment {0} does not use supported distributed session cache mechanism", deploymentInfo.getDeploymentName());
            return previousIdMapperUpdater;
        }

        Map<String, String> initParameters = deploymentInfo.getInitParameters();
        String cacheContainerLookup = (initParameters != null && initParameters.get(DEPLOYMENT_CACHE_CONTAINER_JNDI_NAME_PARAM_NAME) != null)
          ? initParameters.get(DEPLOYMENT_CACHE_CONTAINER_JNDI_NAME_PARAM_NAME)
          : DEFAULT_CACHE_CONTAINER_JNDI_NAME;
        boolean deploymentSessionCacheNamePreset = initParameters != null && initParameters.get(DEPLOYMENT_CACHE_NAME_PARAM_NAME) != null;
        String deploymentSessionCacheName = deploymentSessionCacheNamePreset
          ? initParameters.get(DEPLOYMENT_CACHE_NAME_PARAM_NAME)
          : deploymentInfo.getDeploymentName();
        boolean ssoCacheNamePreset = initParameters != null && initParameters.get(SSO_CACHE_NAME_PARAM_NAME) != null;
        String ssoCacheName = ssoCacheNamePreset
          ? initParameters.get(SSO_CACHE_NAME_PARAM_NAME)
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
                LOG.debugv("Using custom configuration of SSO cache. lookup={0}, cache name={1}", cacheContainerLookup, ssoCacheName);
            }

            CacheMode ssoCacheMode = ssoCacheConfiguration.clustering().cacheMode();
            if (ssoCacheMode != CacheMode.REPL_ASYNC && ssoCacheMode != CacheMode.REPL_SYNC) {
                LOG.warnv("SSO cache mode is {0}, it is recommended to use replicated mode instead", ssoCacheConfiguration.clustering().cacheModeString());
            }

            Cache<String, String[]> ssoCache = cacheManager.getCache(ssoCacheName, true);
            ssoCache.addListener(new SsoSessionCacheListener(mapper));

            LOG.debugv("Added distributed SSO session cache, lookup={0}, cache name={1}", cacheContainerLookup, deploymentSessionCacheName);

            SsoCacheSessionIdMapperUpdater updater = new SsoCacheSessionIdMapperUpdater(ssoCache, previousIdMapperUpdater);
            deploymentInfo.addSessionListener(updater);

            return updater;
        } catch (NamingException ex) {
            LOG.warnv("Failed to obtain distributed session cache container, lookup={0}", cacheContainerLookup);
            return previousIdMapperUpdater;
        }
    }
}

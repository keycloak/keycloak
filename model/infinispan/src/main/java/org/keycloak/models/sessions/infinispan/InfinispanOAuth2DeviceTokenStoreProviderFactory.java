/*
 * Copyright 2019 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.models.sessions.infinispan;

import org.infinispan.Cache;
import org.infinispan.client.hotrod.Flag;
import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.commons.api.BasicCache;
import org.jboss.logging.Logger;
import org.keycloak.Config;
import org.keycloak.connections.infinispan.InfinispanConnectionProvider;
import org.keycloak.models.*;
import org.keycloak.models.sessions.infinispan.entities.ActionTokenValueEntity;
import org.keycloak.models.sessions.infinispan.util.InfinispanUtil;

import java.util.UUID;
import java.util.function.Supplier;

/**
 * @author <a href="mailto:h2-wada@nri.co.jp">Hiroyuki Wada</a>
 */
public class InfinispanOAuth2DeviceTokenStoreProviderFactory implements OAuth2DeviceTokenStoreProviderFactory {

    private static final Logger LOG = Logger.getLogger(InfinispanOAuth2DeviceTokenStoreProviderFactory.class);

    // Reuse "actionTokens" infinispan cache for now
    private volatile Supplier<BasicCache<String, ActionTokenValueEntity>> codeCache;

    @Override
    public OAuth2DeviceTokenStoreProvider create(KeycloakSession session) {
        lazyInit(session);
        return new InfinispanOAuth2DeviceTokenStoreProvider(session, codeCache);
    }

    private void lazyInit(KeycloakSession session) {
        if (codeCache == null) {
            synchronized (this) {
                if (codeCache == null) {
                    InfinispanConnectionProvider connections = session.getProvider(InfinispanConnectionProvider.class);
                    Cache cache = connections.getCache(InfinispanConnectionProvider.ACTION_TOKEN_CACHE);

                    RemoteCache remoteCache = InfinispanUtil.getRemoteCache(cache);

                    if (remoteCache != null) {
                        LOG.debugf("Having remote stores. Using remote cache '%s' for token of OAuth 2.0 Device Authorization Grant", remoteCache.getName());
                        this.codeCache = () -> {
                            // Doing this way as flag is per invocation
                            return remoteCache.withFlags(Flag.FORCE_RETURN_VALUE);
                        };
                    } else {
                        LOG.debugf("Not having remote stores. Using normal cache '%s' for token of OAuth 2.0 Device Authorization Grant", cache.getName());
                        this.codeCache = () -> {
                            return cache;
                        };
                    }
                }
            }
        }
    }

    @Override
    public void init(Config.Scope config) {

    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {

    }

    @Override
    public void close() {

    }

    @Override
    public String getId() {
        return "infinispan";
    }
}

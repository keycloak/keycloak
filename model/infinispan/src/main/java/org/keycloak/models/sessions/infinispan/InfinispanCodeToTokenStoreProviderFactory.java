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

package org.keycloak.models.sessions.infinispan;

import java.util.UUID;
import java.util.function.Supplier;

import org.infinispan.Cache;
import org.infinispan.client.hotrod.Flag;
import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.commons.api.BasicCache;
import org.jboss.logging.Logger;
import org.keycloak.Config;
import org.keycloak.connections.infinispan.InfinispanConnectionProvider;
import org.keycloak.models.CodeToTokenStoreProvider;
import org.keycloak.models.CodeToTokenStoreProviderFactory;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.sessions.infinispan.entities.ActionTokenValueEntity;
import org.keycloak.models.sessions.infinispan.util.InfinispanUtil;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class InfinispanCodeToTokenStoreProviderFactory implements CodeToTokenStoreProviderFactory {

    private static final Logger LOG = Logger.getLogger(InfinispanCodeToTokenStoreProviderFactory.class);

    // Reuse "actionTokens" infinispan cache for now
    private volatile Supplier<BasicCache<UUID, ActionTokenValueEntity>> codeCache;

    @Override
    public CodeToTokenStoreProvider create(KeycloakSession session) {
        lazyInit(session);
        return new InfinispanCodeToTokenStoreProvider(session, codeCache);
    }

    private void lazyInit(KeycloakSession session) {
        if (codeCache == null) {
            synchronized (this) {
                if (codeCache == null) {
                    InfinispanConnectionProvider connections = session.getProvider(InfinispanConnectionProvider.class);
                    Cache cache = connections.getCache(InfinispanConnectionProvider.ACTION_TOKEN_CACHE);

                    RemoteCache remoteCache = InfinispanUtil.getRemoteCache(cache);

                    if (remoteCache != null) {
                        LOG.debugf("Having remote stores. Using remote cache '%s' for single-use cache of code", remoteCache.getName());
                        this.codeCache = () -> {
                            // Doing this way as flag is per invocation
                            return remoteCache.withFlags(Flag.FORCE_RETURN_VALUE);
                        };
                    } else {
                        LOG.debugf("Not having remote stores. Using normal cache '%s' for single-use cache of code", cache.getName());
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

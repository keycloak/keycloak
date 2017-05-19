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

import org.keycloak.Config;
import org.keycloak.Config.Scope;
import org.keycloak.cluster.ClusterProvider;
import org.keycloak.common.util.Time;
import org.keycloak.connections.infinispan.InfinispanConnectionProvider;
import org.keycloak.models.*;

import org.keycloak.models.cache.infinispan.AddInvalidatedActionTokenEvent;
import org.keycloak.models.cache.infinispan.RemoveActionTokensSpecificEvent;
import org.keycloak.models.sessions.infinispan.entities.ActionTokenValueEntity;
import org.keycloak.models.sessions.infinispan.entities.ActionTokenReducedKey;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import org.infinispan.Cache;
import org.infinispan.context.Flag;

/**
 *
 * @author hmlnarik
 */
public class InfinispanActionTokenStoreProviderFactory implements ActionTokenStoreProviderFactory {

    public static final String ACTION_TOKEN_EVENTS = "ACTION_TOKEN_EVENTS";

    /**
     * If expiration is set to this value, no expiration is set on the corresponding cache entry (hence cache default is honored)
     */
    private static final int DEFAULT_CACHE_EXPIRATION = 0;

    private Config.Scope config;

    @Override
    public ActionTokenStoreProvider create(KeycloakSession session) {
        InfinispanConnectionProvider connections = session.getProvider(InfinispanConnectionProvider.class);
        Cache<ActionTokenReducedKey, ActionTokenValueEntity> actionTokenCache = connections.getCache(InfinispanConnectionProvider.ACTION_TOKEN_CACHE);

        ClusterProvider cluster = session.getProvider(ClusterProvider.class);

        cluster.registerListener(ACTION_TOKEN_EVENTS, event -> {
            if (event instanceof RemoveActionTokensSpecificEvent) {
                RemoveActionTokensSpecificEvent e = (RemoveActionTokensSpecificEvent) event;

                actionTokenCache
                  .getAdvancedCache()
                  .withFlags(Flag.CACHE_MODE_LOCAL, Flag.SKIP_CACHE_LOAD)
                  .keySet()
                  .stream()
                  .filter(k -> Objects.equals(k.getUserId(), e.getUserId()) && Objects.equals(k.getActionId(), e.getActionId()))
                  .forEach(actionTokenCache::remove);
            } else if (event instanceof AddInvalidatedActionTokenEvent) {
                AddInvalidatedActionTokenEvent e = (AddInvalidatedActionTokenEvent) event;

                if (e.getExpirationInSecs() == DEFAULT_CACHE_EXPIRATION) {
                    actionTokenCache.put(e.getKey(), e.getTokenValue());
                } else {
                    actionTokenCache.put(e.getKey(), e.getTokenValue(), e.getExpirationInSecs() - Time.currentTime(), TimeUnit.SECONDS);
                }
            }
        });

        return new InfinispanActionTokenStoreProvider(session, actionTokenCache);
    }

    @Override
    public void init(Scope config) {
        this.config = config;
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

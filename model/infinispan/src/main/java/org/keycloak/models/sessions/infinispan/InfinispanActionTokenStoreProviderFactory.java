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
import org.keycloak.connections.infinispan.InfinispanConnectionProvider;
import org.keycloak.models.*;

import org.keycloak.models.sessions.infinispan.entities.ActionTokenValueEntity;
import org.keycloak.models.sessions.infinispan.entities.ActionTokenReducedKey;
import org.infinispan.Cache;

/**
 *
 * @author hmlnarik
 */
public class InfinispanActionTokenStoreProviderFactory implements ActionTokenStoreProviderFactory {

    private volatile Cache<ActionTokenReducedKey, ActionTokenValueEntity> actionTokenCache;

    public static final String ACTION_TOKEN_EVENTS = "ACTION_TOKEN_EVENTS";

    private Config.Scope config;

    @Override
    public ActionTokenStoreProvider create(KeycloakSession session) {
        return new InfinispanActionTokenStoreProvider(session, this.actionTokenCache);
    }

    @Override
    public void init(Scope config) {
        this.config = config;
    }

    private static Cache<ActionTokenReducedKey, ActionTokenValueEntity> initActionTokenCache(KeycloakSession session) {
        InfinispanConnectionProvider connections = session.getProvider(InfinispanConnectionProvider.class);
        Cache<ActionTokenReducedKey, ActionTokenValueEntity> cache = connections.getCache(InfinispanConnectionProvider.ACTION_TOKEN_CACHE);
        return cache;
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
        Cache<ActionTokenReducedKey, ActionTokenValueEntity> cache = this.actionTokenCache;

        // It is necessary to put the cache initialization here, otherwise the cache would be initialized lazily, that
        // means also listeners will start only after first cache initialization - that would be too late
        if (cache == null) {
            synchronized (this) {
                cache = this.actionTokenCache;
                if (cache == null) {
                    this.actionTokenCache = initActionTokenCache(factory.create());
                }
            }
        }
    }

    @Override
    public void close() {
    }

    @Override
    public String getId() {
        return "infinispan";
    }

}

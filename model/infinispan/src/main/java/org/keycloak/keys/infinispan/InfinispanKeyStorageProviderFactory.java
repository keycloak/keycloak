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

package org.keycloak.keys.infinispan;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.FutureTask;

import org.infinispan.Cache;
import org.jboss.logging.Logger;
import org.keycloak.Config;
import org.keycloak.connections.infinispan.InfinispanConnectionProvider;
import org.keycloak.keys.KeyStorageProvider;
import org.keycloak.keys.KeyStorageProviderFactory;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class InfinispanKeyStorageProviderFactory implements KeyStorageProviderFactory {

    private static final Logger log = Logger.getLogger(InfinispanKeyStorageProviderFactory.class);

    public static final String PROVIDER_ID = "infinispan";

    private Cache<String, PublicKeysEntry> keysCache;

    private final Map<String, FutureTask<PublicKeysEntry>> tasksInProgress = new ConcurrentHashMap<>();

    private int minTimeBetweenRequests;

    @Override
    public KeyStorageProvider create(KeycloakSession session) {
        lazyInit(session);
        return new InfinispanKeyStorageProvider(keysCache, tasksInProgress, minTimeBetweenRequests);
    }

    private void lazyInit(KeycloakSession session) {
        if (keysCache == null) {
            synchronized (this) {
                if (keysCache == null) {
                    this.keysCache = session.getProvider(InfinispanConnectionProvider.class).getCache(InfinispanConnectionProvider.KEYS_CACHE_NAME);
                }
            }
        }
    }

    @Override
    public void init(Config.Scope config) {
        minTimeBetweenRequests = config.getInt("minTimeBetweenRequests", 10);
        log.debugf("minTimeBetweenRequests is %d", minTimeBetweenRequests);
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {

    }

    @Override
    public void close() {

    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }
}

/*
 * Copyright 2021 Red Hat, Inc. and/or its affiliates
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

import org.infinispan.client.hotrod.exceptions.HotRodClientException;
import org.infinispan.commons.api.BasicCache;
import org.jboss.logging.Logger;
import org.keycloak.common.util.Time;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.PushedAuthzRequestStoreProvider;
import org.keycloak.models.sessions.infinispan.entities.ActionTokenValueEntity;
import org.keycloak.models.sessions.infinispan.util.InfinispanUtil;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;


public class InfinispanPushedAuthzRequestStoreProvider implements PushedAuthzRequestStoreProvider {

    public static final Logger logger = Logger.getLogger(InfinispanPushedAuthzRequestStoreProvider.class);

    private final Supplier<BasicCache<UUID, ActionTokenValueEntity>> parDataCache;

    public InfinispanPushedAuthzRequestStoreProvider(KeycloakSession session, Supplier<BasicCache<UUID, ActionTokenValueEntity>> actionKeyCache) {
        this.parDataCache = actionKeyCache;
    }

    @Override
    public void put(UUID key, int lifespanSeconds, Map<String, String> codeData) {
        ActionTokenValueEntity tokenValue = new ActionTokenValueEntity(codeData);

        try {
            BasicCache<UUID, ActionTokenValueEntity> cache = parDataCache.get();
            long lifespanMs = InfinispanUtil.toHotrodTimeMs(cache, Time.toMillis(lifespanSeconds));
            cache.put(key, tokenValue, lifespanMs, TimeUnit.MILLISECONDS);
        } catch (HotRodClientException re) {
            // No need to retry. The hotrod (remoteCache) has some retries in itself in case of some random network error happened.
            if (logger.isDebugEnabled()) {
                logger.debugf(re, "Failed when adding PAR data for redirect URI: %s", key);
            }

            throw re;
        }
    }

    @Override
    public Map<String, String> remove(UUID key) {
        try {
            BasicCache<UUID, ActionTokenValueEntity> cache = parDataCache.get();
            ActionTokenValueEntity existing = cache.remove(key);
            return existing == null ? null : existing.getNotes();
        } catch (HotRodClientException re) {
            // No need to retry. The hotrod (remoteCache) has some retries in itself in case of some random network error happened.
            // In case of lock conflict, we don't want to retry anyway as there was likely an attempt to remove the code from different place.
            if (logger.isDebugEnabled()) {
                logger.debugf(re, "Failed when removing PAR data for redirect URI %s", key);
            }

            return null;
        }
    }


    @Override
    public void close() {

    }
}

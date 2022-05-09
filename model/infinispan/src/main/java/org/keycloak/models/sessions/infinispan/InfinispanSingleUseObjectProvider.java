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

import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import org.infinispan.client.hotrod.exceptions.HotRodClientException;
import org.infinispan.commons.api.BasicCache;
import org.jboss.logging.Logger;
import org.keycloak.common.util.Time;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.SingleUseObjectProvider;
import org.keycloak.models.sessions.infinispan.entities.ActionTokenValueEntity;
import org.keycloak.connections.infinispan.InfinispanUtil;

/**
 * TODO: Check if Boolean can be used as single-use cache argument instead of ActionTokenValueEntity. With respect to other single-use cache usecases like "Revoke Refresh Token" .
 * Also with respect to the usage of streams iterating over "actionTokens" cache (check there are no ClassCastExceptions when casting values directly to ActionTokenValueEntity)
 *
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class InfinispanSingleUseObjectProvider implements SingleUseObjectProvider {

    public static final Logger logger = Logger.getLogger(InfinispanSingleUseObjectProvider.class);

    private final Supplier<BasicCache<String, ActionTokenValueEntity>> tokenCache;
    private final KeycloakSession session;

    public InfinispanSingleUseObjectProvider(KeycloakSession session, Supplier<BasicCache<String, ActionTokenValueEntity>> actionKeyCache) {
        this.session = session;
        this.tokenCache = actionKeyCache;
    }

    @Override
    public void put(String key, long lifespanSeconds, Map<String, String> notes) {
        ActionTokenValueEntity tokenValue = new ActionTokenValueEntity(notes);

        try {
            BasicCache<String, ActionTokenValueEntity> cache = tokenCache.get();
            long lifespanMs = InfinispanUtil.toHotrodTimeMs(cache, Time.toMillis(lifespanSeconds));
            cache.put(key, tokenValue, lifespanMs, TimeUnit.MILLISECONDS);
        } catch (HotRodClientException re) {
            // No need to retry. The hotrod (remoteCache) has some retries in itself in case of some random network error happened.
            if (logger.isDebugEnabled()) {
                logger.debugf(re, "Failed when adding code %s", key);
            }

            throw re;
        }
    }

    @Override
    public Map<String, String> get(String key) {
        BasicCache<String, ActionTokenValueEntity> cache = tokenCache.get();
        ActionTokenValueEntity actionTokenValueEntity = cache.get(key);
        return actionTokenValueEntity != null ? actionTokenValueEntity.getNotes() : null;
    }

    @Override
    public Map<String, String> remove(String key) {
        try {
            BasicCache<String, ActionTokenValueEntity> cache = tokenCache.get();
            ActionTokenValueEntity existing = cache.remove(key);
            return existing == null ? null : existing.getNotes();
        } catch (HotRodClientException re) {
            // No need to retry. The hotrod (remoteCache) has some retries in itself in case of some random network error happened.
            // In case of lock conflict, we don't want to retry anyway as there was likely an attempt to remove the code from different place.
            if (logger.isDebugEnabled()) {
                logger.debugf(re, "Failed when removing code %s", key);
            }

            return null;
        }
    }

    @Override
    public boolean replace(String key, Map<String, String> notes) {
        BasicCache<String, ActionTokenValueEntity> cache = tokenCache.get();
        return cache.replace(key, new ActionTokenValueEntity(notes)) != null;
    }

    @Override
    public boolean putIfAbsent(String key, long lifespanInSeconds) {
        ActionTokenValueEntity tokenValue = new ActionTokenValueEntity(null);

        try {
            BasicCache<String, ActionTokenValueEntity> cache = tokenCache.get();
            long lifespanMs = InfinispanUtil.toHotrodTimeMs(cache, Time.toMillis(lifespanInSeconds));
            ActionTokenValueEntity existing = cache.putIfAbsent(key, tokenValue, lifespanMs, TimeUnit.MILLISECONDS);
            return existing == null;
        } catch (HotRodClientException re) {
            // No need to retry. The hotrod (remoteCache) has some retries in itself in case of some random network error happened.
            // In case of lock conflict, we don't want to retry anyway as there was likely an attempt to use the token from different place.
            logger.debugf(re, "Failed when adding token %s", key);

            return false;
        }

    }

    @Override
    public boolean contains(String key) {
        BasicCache<String, ActionTokenValueEntity> cache = tokenCache.get();
        return cache.containsKey(key);
    }

    @Override
    public void close() {

    }
}

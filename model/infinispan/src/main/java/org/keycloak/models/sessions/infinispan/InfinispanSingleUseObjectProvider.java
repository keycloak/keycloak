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
import org.keycloak.models.sessions.infinispan.entities.SingleUseObjectValueEntity;
import org.keycloak.connections.infinispan.InfinispanUtil;

/**
 * TODO: Check if Boolean can be used as single-use cache argument instead of SingleUseObjectValueEntity. With respect to other single-use cache usecases like "Revoke Refresh Token" .
 * Also with respect to the usage of streams iterating over "actionTokens" cache (check there are no ClassCastExceptions when casting values directly to SingleUseObjectValueEntity)
 *
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class InfinispanSingleUseObjectProvider implements SingleUseObjectProvider {

    public static final Logger logger = Logger.getLogger(InfinispanSingleUseObjectProvider.class);

    private final Supplier<BasicCache<String, SingleUseObjectValueEntity>> singleUseObjectCache;
    private final KeycloakSession session;
    private final InfinispanKeycloakTransaction tx;

    public InfinispanSingleUseObjectProvider(KeycloakSession session, Supplier<BasicCache<String, SingleUseObjectValueEntity>> singleUseObjectCache) {
        this.session = session;
        this.singleUseObjectCache = singleUseObjectCache;
        this.tx = new InfinispanKeycloakTransaction();

        session.getTransactionManager().enlistAfterCompletion(tx);
    }

    @Override
    public void put(String key, long lifespanSeconds, Map<String, String> notes) {
        SingleUseObjectValueEntity tokenValue = new SingleUseObjectValueEntity(notes);
        try {
            BasicCache<String, SingleUseObjectValueEntity> cache = singleUseObjectCache.get();
            tx.put(cache, key, tokenValue, InfinispanUtil.toHotrodTimeMs(cache, Time.toMillis(lifespanSeconds)), TimeUnit.MILLISECONDS);
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
        SingleUseObjectValueEntity singleUseObjectValueEntity;

        BasicCache<String, SingleUseObjectValueEntity> cache = singleUseObjectCache.get();
        singleUseObjectValueEntity = tx.get(cache, key);
        return singleUseObjectValueEntity != null ? singleUseObjectValueEntity.getNotes() : null;
    }

    @Override
    public Map<String, String> remove(String key) {
        try {
            BasicCache<String, SingleUseObjectValueEntity> cache = singleUseObjectCache.get();
            SingleUseObjectValueEntity existing = cache.remove(key);
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
        BasicCache<String, SingleUseObjectValueEntity> cache = singleUseObjectCache.get();
        return cache.replace(key, new SingleUseObjectValueEntity(notes)) != null;
    }

    @Override
    public boolean putIfAbsent(String key, long lifespanInSeconds) {
        SingleUseObjectValueEntity tokenValue = new SingleUseObjectValueEntity(null);
        BasicCache<String, SingleUseObjectValueEntity> cache = singleUseObjectCache.get();

        try {
            long lifespanMs = InfinispanUtil.toHotrodTimeMs(cache, Time.toMillis(lifespanInSeconds));
            SingleUseObjectValueEntity existing = cache.putIfAbsent(key, tokenValue, lifespanMs, TimeUnit.MILLISECONDS);
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
        BasicCache<String, SingleUseObjectValueEntity> cache = singleUseObjectCache.get();
        return cache.containsKey(key);
    }

    @Override
    public void close() {

    }
}

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

import org.keycloak.common.util.Time;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ModelException;
import org.keycloak.models.SingleUseObjectProvider;
import org.keycloak.models.session.RevokedTokenPersisterProvider;
import org.keycloak.models.sessions.infinispan.entities.SingleUseObjectValueEntity;

import org.infinispan.client.hotrod.exceptions.HotRodClientException;
import org.infinispan.commons.api.BasicCache;
import org.jboss.logging.Logger;

/**
 * TODO: Check if Boolean can be used as single-use cache argument instead of SingleUseObjectValueEntity. With respect to other single-use cache usecases like "Revoke Refresh Token" .
 * Also with respect to the usage of streams iterating over "actionTokens" cache (check there are no ClassCastExceptions when casting values directly to SingleUseObjectValueEntity)
 *
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class InfinispanSingleUseObjectProvider implements SingleUseObjectProvider {

    public static final Logger logger = Logger.getLogger(InfinispanSingleUseObjectProvider.class);

    private final KeycloakSession session;
    private final BasicCache<String, SingleUseObjectValueEntity> singleUseObjectCache;
    private final boolean persistRevokedTokens;
    private final InfinispanKeycloakTransaction tx;

    public InfinispanSingleUseObjectProvider(KeycloakSession session, BasicCache<String, SingleUseObjectValueEntity> singleUseObjectCache, boolean persistRevokedTokens, InfinispanKeycloakTransaction tx) {
        this.session = session;
        this.singleUseObjectCache = singleUseObjectCache;
        this.persistRevokedTokens = persistRevokedTokens;
        this.tx = tx;
    }

    @Override
    public void put(String key, long lifespanSeconds, Map<String, String> notes) {
        SingleUseObjectValueEntity tokenValue = new SingleUseObjectValueEntity(notes);
        try {
            tx.put(singleUseObjectCache, key, tokenValue, Time.toMillis(lifespanSeconds), TimeUnit.MILLISECONDS);
        } catch (HotRodClientException re) {
            // No need to retry. The hotrod (remoteCache) has some retries in itself in case of some random network error happened.
            if (logger.isDebugEnabled()) {
                logger.debugf(re, "Failed when adding code %s", key);
            }
            throw re;
        }
        if (persistRevokedTokens && key.endsWith(REVOKED_KEY)) {
            if (!notes.isEmpty()) {
                throw new ModelException("Notes are not supported for revoked tokens");
            }
            session.getProvider(RevokedTokenPersisterProvider.class).revokeToken(key.substring(0, key.length() - REVOKED_KEY.length()), lifespanSeconds);
        }
    }

    @Override
    public Map<String, String> get(String key) {
        if (persistRevokedTokens && key.endsWith(REVOKED_KEY)) {
            throw new ModelException("Revoked tokens can't be retrieved");
        }

        SingleUseObjectValueEntity singleUseObjectValueEntity;

        singleUseObjectValueEntity = tx.get(singleUseObjectCache, key);
        return singleUseObjectValueEntity != null ? singleUseObjectValueEntity.getNotes() : null;
    }

    @Override
    public Map<String, String> remove(String key) {
        if (persistRevokedTokens && key.endsWith(REVOKED_KEY)) {
           throw new ModelException("Revoked tokens can't be removed");
        }

        try {
            SingleUseObjectValueEntity existing = singleUseObjectCache.remove(key);
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
        if (persistRevokedTokens && key.endsWith(REVOKED_KEY)) {
            throw new ModelException("Revoked tokens can't be replaced");
        }

        return singleUseObjectCache.replace(key, new SingleUseObjectValueEntity(notes)) != null;
    }

    @Override
    public boolean putIfAbsent(String key, long lifespanInSeconds) {
        if (persistRevokedTokens && key.endsWith(REVOKED_KEY)) {
            throw new ModelException("Revoked tokens can't be used in putIfAbsent");
        }

        SingleUseObjectValueEntity tokenValue = new SingleUseObjectValueEntity(null);

        try {
            SingleUseObjectValueEntity existing = singleUseObjectCache.putIfAbsent(key, tokenValue, Time.toMillis(lifespanInSeconds), TimeUnit.MILLISECONDS);
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
        return singleUseObjectCache.containsKey(key);
    }

    @Override
    public void close() {

    }
}

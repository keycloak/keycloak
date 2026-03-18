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

import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ModelException;
import org.keycloak.models.SingleUseObjectProvider;
import org.keycloak.models.session.RevokedTokenPersisterProvider;
import org.keycloak.models.sessions.infinispan.entities.SingleUseObjectValueEntity;

import org.infinispan.commons.api.BasicCache;

/**
 * TODO: Check if Boolean can be used as single-use cache argument instead of SingleUseObjectValueEntity. With respect to other single-use cache use cases like "Revoke Refresh Token" .
 * Also with respect to the usage of streams iterating over "actionTokens" cache (check there are no ClassCastExceptions when casting values directly to SingleUseObjectValueEntity)
 *
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class InfinispanSingleUseObjectProvider implements SingleUseObjectProvider {

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
        tx.put(singleUseObjectCache, key, tokenValue, lifespanSeconds, TimeUnit.SECONDS);
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

        SingleUseObjectValueEntity singleUseObjectValueEntity = tx.get(singleUseObjectCache, key);
        return singleUseObjectValueEntity != null ? singleUseObjectValueEntity.getNotes() : null;
    }

    @Override
    public Map<String, String> remove(String key) {
        if (persistRevokedTokens && key.endsWith(REVOKED_KEY)) {
           throw new ModelException("Revoked tokens can't be removed");
        }

        // Using a get-before-remove allows us to return the value even in cases when a state transfer happens in Infinispan
        // where it might not return the value in all cases.
        // This workaround can be removed once https://github.com/infinispan/infinispan/issues/16703 is implemented.
        var data = singleUseObjectCache.get(key);
        if (data == null) {
            return null;
        }
        return singleUseObjectCache.remove(key, data) ? data.getNotes() : null;
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
        SingleUseObjectValueEntity tokenValue = new SingleUseObjectValueEntity(null);
        SingleUseObjectValueEntity existing = singleUseObjectCache.putIfAbsent(key, tokenValue, lifespanInSeconds, TimeUnit.SECONDS);
        if (persistRevokedTokens && key.endsWith(REVOKED_KEY)) {
            session.getProvider(RevokedTokenPersisterProvider.class).revokeToken(key.substring(0, key.length() - REVOKED_KEY.length()), lifespanInSeconds);
        }
        return existing == null;
    }

    @Override
    public boolean contains(String key) {
        return singleUseObjectCache.containsKey(key);
    }

    @Override
    public void close() {

    }
}

/*
 * Copyright 2024 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.models.sessions.infinispan.remote;

import java.lang.invoke.MethodHandles;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import org.keycloak.models.SingleUseObjectProvider;
import org.keycloak.models.sessions.infinispan.entities.SingleUseObjectValueEntity;
import org.keycloak.models.sessions.infinispan.remote.transaction.SingleUseObjectTransaction;

import org.infinispan.client.hotrod.Flag;
import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.exceptions.HotRodClientException;
import org.jboss.logging.Logger;

public class RemoteInfinispanSingleUseObjectProvider implements SingleUseObjectProvider {

    private final static Logger logger = Logger.getLogger(MethodHandles.lookup().lookupClass());
    public static final SingleUseObjectValueEntity REVOKED_TOKEN_VALUE = new SingleUseObjectValueEntity(Collections.emptyMap());

    private final SingleUseObjectTransaction transaction;
    private final RevokeTokenConsumer revokeTokenConsumer;

    public RemoteInfinispanSingleUseObjectProvider(SingleUseObjectTransaction transaction, RevokeTokenConsumer revokeTokenConsumer) {
        this.transaction = Objects.requireNonNull(transaction);
        this.revokeTokenConsumer = Objects.requireNonNull(revokeTokenConsumer);

    }

    @Override
    public void put(String key, long lifespanSeconds, Map<String, String> notes) {
        if (key.endsWith(REVOKED_KEY)) {
            revokeToken(key, lifespanSeconds);
            return;
        }
        transaction.put(key, wrap(notes), lifespanSeconds, TimeUnit.SECONDS);
    }

    @Override
    public Map<String, String> get(String key) {
        return unwrap(transaction.get(key));
    }

    @Override
    public Map<String, String> remove(String key) {
        try {
            return unwrap(withReturnValue().remove(key));
        } catch (HotRodClientException re) {
            // No need to retry. The hotrod (remoteCache) has some retries in itself in case of some random network error happened.
            // In case of lock conflict, we don't want to retry anyway as there was likely an attempt to remove the code from different place.
            logger.debugf(re, "Failed when removing code %s", key);
            return null;
        }
    }

    @Override
    public boolean replace(String key, Map<String, String> notes) {
        return withReturnValue().replace(key, wrap(notes)) != null;
    }

    @Override
    public boolean putIfAbsent(String key, long lifespanInSeconds) {
        try {
            return withReturnValue().putIfAbsent(key, wrap(null), lifespanInSeconds, TimeUnit.SECONDS) == null;
        } catch (HotRodClientException re) {
            // No need to retry. The hotrod (remoteCache) has some retries in itself in case of some random network error happened.
            // In case of lock conflict, we don't want to retry anyway as there was likely an attempt to use the token from different place.
            logger.debugf(re, "Failed when adding token %s", key);
            return false;
        }
    }

    @Override
    public boolean contains(String key) {
        return transaction.getCache().containsKey(key);
    }

    @Override
    public void close() {

    }

    private RemoteCache<String, SingleUseObjectValueEntity> withReturnValue() {
        return transaction.getCache().withFlags(Flag.FORCE_RETURN_VALUE);
    }

    private void revokeToken(String key, long lifespanSeconds) {
        transaction.put(key, REVOKED_TOKEN_VALUE, lifespanSeconds, TimeUnit.SECONDS);
        var token = key.substring(0, key.length() - REVOKED_KEY.length());
        revokeTokenConsumer.onTokenRevoke(token, lifespanSeconds);
    }

    private static Map<String, String> unwrap(SingleUseObjectValueEntity entity) {
        return entity == null ? null : entity.getNotes();
    }

    private static SingleUseObjectValueEntity wrap(Map<String, String> notes) {
        return new SingleUseObjectValueEntity(notes);
    }

    public interface RevokeTokenConsumer {
        void onTokenRevoke(String token, long lifespanSeconds);
    }
}

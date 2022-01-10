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

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import org.infinispan.client.hotrod.exceptions.HotRodClientException;
import org.infinispan.commons.api.BasicCache;
import org.jboss.logging.Logger;
import org.keycloak.common.util.Time;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.SingleUseTokenStoreProvider;
import org.keycloak.models.sessions.infinispan.entities.ActionTokenValueEntity;
import org.keycloak.connections.infinispan.InfinispanUtil;

/**
 * TODO: Check if Boolean can be used as single-use cache argument instead of ActionTokenValueEntity. With respect to other single-use cache usecases like "Revoke Refresh Token" .
 * Also with respect to the usage of streams iterating over "actionTokens" cache (check there are no ClassCastExceptions when casting values directly to ActionTokenValueEntity)
 *
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class InfinispanSingleUseTokenStoreProvider implements SingleUseTokenStoreProvider {

    public static final Logger logger = Logger.getLogger(InfinispanSingleUseTokenStoreProvider.class);

    private final Supplier<BasicCache<String, ActionTokenValueEntity>> tokenCache;
    private final KeycloakSession session;

    public InfinispanSingleUseTokenStoreProvider(KeycloakSession session, Supplier<BasicCache<String, ActionTokenValueEntity>> actionKeyCache) {
        this.session = session;
        this.tokenCache = actionKeyCache;
    }

    @Override
    public boolean putIfAbsent(String tokenId, int lifespanInSeconds) {
        ActionTokenValueEntity tokenValue = new ActionTokenValueEntity(null);

        // Rather keep the items in the cache for a bit longer
        lifespanInSeconds = lifespanInSeconds + 10;

        try {
            BasicCache<String, ActionTokenValueEntity> cache = tokenCache.get();
            long lifespanMs = InfinispanUtil.toHotrodTimeMs(cache, Time.toMillis(lifespanInSeconds));
            ActionTokenValueEntity existing = cache.putIfAbsent(tokenId, tokenValue, lifespanMs, TimeUnit.MILLISECONDS);
            return existing == null;
        } catch (HotRodClientException re) {
            // No need to retry. The hotrod (remoteCache) has some retries in itself in case of some random network error happened.
            // In case of lock conflict, we don't want to retry anyway as there was likely an attempt to use the token from different place.
            logger.debugf(re, "Failed when adding token %s", tokenId);

            return false;
        }

    }

    @Override
    public void close() {

    }
}

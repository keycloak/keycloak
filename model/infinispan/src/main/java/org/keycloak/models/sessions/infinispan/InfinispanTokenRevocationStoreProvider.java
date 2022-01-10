/*
 * Copyright 2020 Red Hat, Inc. and/or its affiliates
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
 *
 */

package org.keycloak.models.sessions.infinispan;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import org.infinispan.client.hotrod.exceptions.HotRodClientException;
import org.infinispan.commons.api.BasicCache;
import org.jboss.logging.Logger;
import org.keycloak.common.util.Time;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.TokenRevocationStoreProvider;
import org.keycloak.models.sessions.infinispan.entities.ActionTokenValueEntity;
import org.keycloak.connections.infinispan.InfinispanUtil;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class InfinispanTokenRevocationStoreProvider implements TokenRevocationStoreProvider {

    public static final Logger logger = Logger.getLogger(InfinispanTokenRevocationStoreProvider.class);

    private final Supplier<BasicCache<String, ActionTokenValueEntity>> tokenCache;
    private final KeycloakSession session;

    // Key in the data, which indicates that token is considered revoked
    private final String REVOKED_KEY = "revoked";

    public InfinispanTokenRevocationStoreProvider(KeycloakSession session, Supplier<BasicCache<String, ActionTokenValueEntity>> tokenCache) {
        this.session = session;
        this.tokenCache = tokenCache;
    }


    @Override
    public void putRevokedToken(String tokenId, long lifespanSeconds) {
        Map<String, String> data = Collections.singletonMap(REVOKED_KEY, "true");
        ActionTokenValueEntity tokenValue = new ActionTokenValueEntity(data);

        try {
            BasicCache<String, ActionTokenValueEntity> cache = tokenCache.get();
            long lifespanMs = InfinispanUtil.toHotrodTimeMs(cache, Time.toMillis(lifespanSeconds + 1));
            cache.put(tokenId, tokenValue, lifespanMs, TimeUnit.MILLISECONDS);
        } catch (HotRodClientException re) {
            // No need to retry. The hotrod (remoteCache) has some retries in itself in case of some random network error happened.
            if (logger.isDebugEnabled()) {
                logger.debugf(re, "Failed when adding revoked token %s", tokenId);
            }

            throw re;
        }
    }


    @Override
    public boolean isRevoked(String tokenId)  {
        try {
            BasicCache<String, ActionTokenValueEntity> cache = tokenCache.get();
            ActionTokenValueEntity existing = cache.get(tokenId);

            if (existing == null) {
                return false;
            }

            return existing.getNotes().containsKey(REVOKED_KEY);
        } catch (HotRodClientException re) {
            // No need to retry. The hotrod (remoteCache) has some retries in itself in case of some random network error happened.
            if (logger.isDebugEnabled()) {
                logger.debugf(re, "Failed when trying to get revoked token %s", tokenId);
            }

            return false;
        }
    }

    @Override
    public void close() {

    }
}

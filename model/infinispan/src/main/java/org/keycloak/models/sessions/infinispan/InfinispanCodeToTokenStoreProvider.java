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

import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import org.infinispan.commons.api.BasicCache;
import org.jboss.logging.Logger;
import org.keycloak.common.util.Retry;
import org.keycloak.models.CodeToTokenStoreProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.sessions.infinispan.entities.ActionTokenValueEntity;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class InfinispanCodeToTokenStoreProvider implements CodeToTokenStoreProvider {

    public static final Logger logger = Logger.getLogger(InfinispanCodeToTokenStoreProvider.class);

    private final Supplier<BasicCache<UUID, ActionTokenValueEntity>> codeCache;
    private final KeycloakSession session;

    public InfinispanCodeToTokenStoreProvider(KeycloakSession session, Supplier<BasicCache<UUID, ActionTokenValueEntity>> actionKeyCache) {
        this.session = session;
        this.codeCache = actionKeyCache;
    }

    @Override
    public boolean putIfAbsent(UUID codeId) {
        ActionTokenValueEntity tokenValue = new ActionTokenValueEntity(null);

        int lifespanInSeconds = session.getContext().getRealm().getAccessCodeLifespan();

        boolean codeAlreadyExists = Retry.call(() -> {

            try {
                BasicCache<UUID, ActionTokenValueEntity> cache = codeCache.get();
                ActionTokenValueEntity existing = cache.putIfAbsent(codeId, tokenValue, lifespanInSeconds, TimeUnit.SECONDS);
                return existing == null;
            } catch (RuntimeException re) {
                if (logger.isDebugEnabled()) {
                    logger.debugf(re, "Failed when adding code %s", codeId);
                }

                // Rethrow the exception. Retry will take care of handle the exception and eventually retry the operation.
                throw re;
            }

        }, 3, 0);

        return codeAlreadyExists;
    }

    @Override
    public void close() {

    }
}

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

import org.infinispan.client.hotrod.exceptions.HotRodClientException;
import org.infinispan.commons.api.BasicCache;
import org.jboss.logging.Logger;
import org.keycloak.common.util.Time;
import org.keycloak.models.AuthenticatedClientSessionModel;
import org.keycloak.models.SamlArtifactSessionMappingModel;
import org.keycloak.models.SamlArtifactSessionMappingStoreProvider;
import org.keycloak.connections.infinispan.InfinispanUtil;

import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;


/**
 * @author mhajas
 */
public class InfinispanSamlArtifactSessionMappingStoreProvider implements SamlArtifactSessionMappingStoreProvider {

    public static final Logger logger = Logger.getLogger(InfinispanSamlArtifactSessionMappingStoreProvider.class);

    private final Supplier<BasicCache<UUID, String[]>> cacheSupplier;

    public InfinispanSamlArtifactSessionMappingStoreProvider(Supplier<BasicCache<UUID, String[]>> actionKeyCache) {
        this.cacheSupplier = actionKeyCache;
    }

    @Override
    public void put(String artifact, int lifespanSeconds, AuthenticatedClientSessionModel clientSessionModel) {
        try {
            BasicCache<UUID, String[]> cache = cacheSupplier.get();
            long lifespanMs = InfinispanUtil.toHotrodTimeMs(cache, Time.toMillis(lifespanSeconds));
            cache.put(UUID.nameUUIDFromBytes(artifact.getBytes(StandardCharsets.UTF_8)), new String[]{artifact, clientSessionModel.getUserSession().getId(), clientSessionModel.getClient().getId()}, lifespanMs, TimeUnit.MILLISECONDS);
        } catch (HotRodClientException re) {
            // No need to retry. The hotrod (remoteCache) has some retries in itself in case of some random network error happened.
            if (logger.isDebugEnabled()) {
                logger.debugf(re, "Failed adding artifact %s", artifact);
            }

            throw re;
        }
    }

    @Override
    public SamlArtifactSessionMappingModel get(String artifact) {
        try {
            BasicCache<UUID, String[]> cache = cacheSupplier.get();
            String[] existing = cache.get(UUID.nameUUIDFromBytes(artifact.getBytes(StandardCharsets.UTF_8)));
            if (existing == null || existing.length != 3) return null;
            if (!artifact.equals(existing[0])) return null; // Check
            return new SamlArtifactSessionMappingModel(existing[1], existing[2]);
        } catch (HotRodClientException re) {
            // No need to retry. The hotrod (remoteCache) has some retries in itself in case of some random network error happened.
            // In case of lock conflict, we don't want to retry anyway as there was likely an attempt to remove the code from different place.
            if (logger.isDebugEnabled()) {
                logger.debugf(re, "Failed when obtaining data for artifact %s", artifact);
            }

            return null;
        }
    }

    @Override
    public void remove(String artifact) {
        try {
            BasicCache<UUID, String[]> cache = cacheSupplier.get();
            if (cache.remove(UUID.nameUUIDFromBytes(artifact.getBytes(StandardCharsets.UTF_8))) == null) {
                logger.debugf("Artifact %s was already removed", artifact);
            }
        } catch (HotRodClientException re) {
            // No need to retry. The hotrod (remoteCache) has some retries in itself in case of some random network error happened.
            // In case of lock conflict, we don't want to retry anyway as there was likely an attempt to remove the code from different place.
            if (logger.isDebugEnabled()) {
                logger.debugf(re, "Failed to remove artifact %s", artifact);
            }
        }
    }

    @Override
    public void close() {

    }
}

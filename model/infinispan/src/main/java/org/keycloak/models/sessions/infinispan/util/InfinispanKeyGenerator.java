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

package org.keycloak.models.sessions.infinispan.util;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;

import org.keycloak.common.util.SecretGenerator;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.sessions.infinispan.changes.CacheHolder;
import org.keycloak.sessions.StickySessionEncoderProvider;

import org.infinispan.Cache;
import org.infinispan.affinity.KeyAffinityService;
import org.infinispan.affinity.KeyAffinityServiceFactory;
import org.infinispan.affinity.KeyGenerator;
import org.jboss.logging.Logger;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 * @deprecated not supported and to be removed. Check {@link CacheHolder#keyGenerator()}
 */
@Deprecated(since = "26.4", forRemoval = true)
public class InfinispanKeyGenerator {

    private static final Logger log = Logger.getLogger(InfinispanKeyGenerator.class);


    private final Map<String, KeyAffinityService> keyAffinityServices = new ConcurrentHashMap<>();


    public String generateKeyString(KeycloakSession session, Cache<String, ?> cache) {
        return generateKey(session, cache, new StringKeyGenerator());
    }


    public UUID generateKeyUUID(KeycloakSession session, Cache<UUID, ?> cache) {
        return generateKey(session, cache, new UUIDKeyGenerator());
    }


    protected <K> K generateKey(KeycloakSession session, Cache<K, ?> cache, KeyGenerator<K> keyGenerator) {
        String cacheName = cache.getName();

        // "wantsLocalKey" is true if route is not attached to the sticky session cookie. Without attached route, We want the key, which will be "owned" by this node.
        // This is needed due the fact that external loadbalancer will attach route corresponding to our node, which will be the owner of the particular key, hence we
        // will be able to lookup key locally.
        boolean wantsLocalKey = !session.getProvider(StickySessionEncoderProvider.class).shouldAttachRoute();

        if (wantsLocalKey && cache.getCacheConfiguration().clustering().cacheMode().isClustered()) {
            KeyAffinityService<K> keyAffinityService = keyAffinityServices.computeIfAbsent(cacheName, s -> {
                KeyAffinityService<K> k = createKeyAffinityService(cache, keyGenerator);
                log.debugf("Registered key affinity service for cache '%s'", cacheName);
                return k;
            });
            return keyAffinityService.getKeyForAddress(cache.getCacheManager().getAddress());
        } else {
            return keyGenerator.getKey();
        }

    }


    private <K> KeyAffinityService<K> createKeyAffinityService(Cache<K, ?> cache, KeyGenerator<K> keyGenerator) {
        // SingleThreadExecutor is recommended due it needs the single thread and leave it in the WAITING state
        return KeyAffinityServiceFactory.newLocalKeyAffinityService(
                cache,
                keyGenerator,
                Executors.newSingleThreadExecutor(),
                16);
    }


    private static class UUIDKeyGenerator implements KeyGenerator<UUID> {

        @Override
        public UUID getKey() {
            return SecretGenerator.getInstance().generateSecureUUID();
        }
    }


    private static class StringKeyGenerator implements KeyGenerator<String> {

        @Override
        public String getKey() {
            return SecretGenerator.getInstance().generateSecureID();
        }
    }
}

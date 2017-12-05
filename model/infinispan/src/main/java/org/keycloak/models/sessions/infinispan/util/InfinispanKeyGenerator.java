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

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;

import org.infinispan.Cache;
import org.infinispan.affinity.KeyAffinityService;
import org.infinispan.affinity.KeyAffinityServiceFactory;
import org.infinispan.affinity.KeyGenerator;
import org.jboss.logging.Logger;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.sessions.StickySessionEncoderProvider;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class InfinispanKeyGenerator {

    private static final Logger log = Logger.getLogger(InfinispanKeyGenerator.class);

    private static final Map<Class, KeyGenerator> SUPPORTED_GENERATORS = new HashMap<>();

    static {
        SUPPORTED_GENERATORS.put(UUID.class, new UUIDKeyGenerator());
        SUPPORTED_GENERATORS.put(String.class, new StringKeyGenerator());
    }


    private final Map<String, KeyAffinityService> keyAffinityServices = new ConcurrentHashMap<>();


    public <K> K generateKey(KeycloakSession session, Cache<K, ?> cache, Class<K> keyType) {
        String cacheName = cache.getName();

        KeyGenerator<K> keyGenerator = getKeyGenerator(keyType);

        // "wantsLocalKey" is true if route is not attached to the sticky session cookie. Without attached route, We want the key, which will be "owned" by this node.
        // This is needed due the fact that external loadbalancer will attach route corresponding to our node, which will be the owner of the particular key, hence we
        // will be able to lookup key locally.
        boolean wantsLocalKey = !session.getProvider(StickySessionEncoderProvider.class).shouldAttachRoute();

        if (wantsLocalKey && cache.getCacheConfiguration().clustering().cacheMode().isClustered()) {
            KeyAffinityService<K> keyAffinityService = keyAffinityServices.get(cacheName);
            if (keyAffinityService == null) {
                keyAffinityService = createKeyAffinityService(cache, keyGenerator);
                keyAffinityServices.put(cacheName, keyAffinityService);

                log.debugf("Registered key affinity service for cache '%s'", cacheName);
            }

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


    private <K> KeyGenerator<K> getKeyGenerator(Class<K> keyType) {
        KeyGenerator<K> keyGenerator = SUPPORTED_GENERATORS.get(keyType);

        if (keyGenerator == null) {
            throw new IllegalArgumentException("Not supported key generator for type: " + keyType);
        }

        return keyGenerator;
    }


    private static class UUIDKeyGenerator implements KeyGenerator<UUID> {

        @Override
        public UUID getKey() {
            return UUID.randomUUID();
        }
    }


    private static class StringKeyGenerator implements KeyGenerator<String> {

        @Override
        public String getKey() {
            return KeycloakModelUtils.generateId();
        }
    }
}

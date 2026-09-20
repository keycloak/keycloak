/*
 * Copyright 2026 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.protocol.oidc;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.keycloak.Config;
import org.keycloak.cache.LocalCache;
import org.keycloak.utils.ScopeUtil;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Tests that {@link OIDCProviderConfig#getMaxLengthForTheParameter(String, boolean)} memoizes its result via the
 * {@link LocalCache} wired in by {@code OIDCLoginProtocolFactory#postInit()}, instead of re-resolving the
 * underlying configuration on every call.
 */
public class OIDCProviderConfigTest {

    @Test
    public void getMaxLengthForTheParameterIsMemoized() {
        CountingScope config = new CountingScope(new HashMap<>());
        OIDCProviderConfig providerConfig = new OIDCProviderConfig(config);
        providerConfig.setMaxLengthCaches(new FakeLocalCache<>(), new FakeLocalCache<>());

        int first = providerConfig.getMaxLengthForTheParameter("state", false);
        int second = providerConfig.getMaxLengthForTheParameter("state", false);
        int third = providerConfig.getMaxLengthForTheParameter("state", false);

        assertEquals(first, second);
        assertEquals(first, third);
        assertEquals("Underlying configuration should only be resolved once for a repeated lookup",
                1, config.getIntInvocations("req-params-max-size--state"));
    }

    @Test
    public void getMaxLengthForTheParameterDoesNotConflateTokenAndNonTokenLookups() {
        CountingScope config = new CountingScope(new HashMap<>());
        OIDCProviderConfig providerConfig = new OIDCProviderConfig(config);
        providerConfig.setMaxLengthCaches(new FakeLocalCache<>(), new FakeLocalCache<>());

        int nonToken = providerConfig.getMaxLengthForTheParameter("subject_token", false);
        int token = providerConfig.getMaxLengthForTheParameter("subject_token", true);
        // Repeating both lookups must still hit the cache and not trigger further resolution
        int nonTokenAgain = providerConfig.getMaxLengthForTheParameter("subject_token", false);
        int tokenAgain = providerConfig.getMaxLengthForTheParameter("subject_token", true);

        assertEquals(OIDCProviderConfig.DEFAULT_REQ_PARAMS_DEFAULT_MAX_SIZE, nonToken);
        assertEquals(OIDCProviderConfig.DEFAULT_REQ_TOKEN_PARAMS_DEFAULT_MAX_SIZE, token);
        assertEquals(nonToken, nonTokenAgain);
        assertEquals(token, tokenAgain);
        // Each distinct (paramName, isTokenParam) combination triggers its own initial resolution (2 total: token/non-token),
        // but repeating either lookup afterwards must be served entirely from the cache with no further resolution.
        assertEquals("Underlying configuration should only be resolved once per (paramName, isTokenParam) combination",
                2, config.getIntInvocations("req-params-max-size--subject_token"));
    }

    @Test
    public void getMaxLengthForTheParameterWorksWithoutACacheWiredIn() {
        // No setMaxLengthCaches() call: simulates constructing OIDCProviderConfig outside of the factory lifecycle.
        CountingScope config = new CountingScope(new HashMap<>());
        OIDCProviderConfig providerConfig = new OIDCProviderConfig(config);

        int value = providerConfig.getMaxLengthForTheParameter("state", false);

        assertEquals(OIDCProviderConfig.DEFAULT_REQ_PARAMS_DEFAULT_MAX_SIZE, value);
    }

    /**
     * A minimal, unbounded {@link LocalCache} backed by a plain {@link HashMap}, sufficient for exercising
     * {@link OIDCProviderConfig}'s caching logic without depending on a real {@code KeycloakSession}/provider setup.
     */
    private static class FakeLocalCache<K, V> implements LocalCache<K, V> {

        private final Map<K, V> map = new HashMap<>();

        @Override
        public V get(K key) {
            return map.get(key);
        }

        @Override
        public void put(K key, V value) {
            map.put(key, value);
        }

        @Override
        public void invalidate(K key) {
            map.remove(key);
        }

        @Override
        public void close() {
            map.clear();
        }
    }

    /**
     * A {@link Config.Scope} that counts, per key, how many times {@link #getInt(String, Integer)} has been called,
     * so that tests can assert the caching layer in {@link OIDCProviderConfig} avoids redundant resolution.
     */
    private static class CountingScope implements Config.Scope {

        private final Config.Scope delegate;
        private final Map<String, AtomicInteger> invocations = new HashMap<>();

        CountingScope(Map<String, String> properties) {
            this.delegate = ScopeUtil.createScope(properties);
        }

        int getIntInvocations(String key) {
            return invocations.getOrDefault(key, new AtomicInteger()).get();
        }

        @Override
        public Integer getInt(String key, Integer defaultValue) {
            invocations.computeIfAbsent(key, k -> new AtomicInteger()).incrementAndGet();
            return delegate.getInt(key, defaultValue);
        }

        @Override
        public String get(String key) {
            return delegate.get(key);
        }

        @Override
        public String get(String key, String defaultValue) {
            return delegate.get(key, defaultValue);
        }

        @Override
        public String[] getArray(String key) {
            return delegate.getArray(key);
        }

        @Override
        public Long getLong(String key, Long defaultValue) {
            return delegate.getLong(key, defaultValue);
        }

        @Override
        public Boolean getBoolean(String key, Boolean defaultValue) {
            return delegate.getBoolean(key, defaultValue);
        }

        @Override
        public Config.Scope scope(String... scope) {
            return delegate.scope(scope);
        }

        @Override
        public Set<String> getPropertyNames() {
            return delegate.getPropertyNames();
        }

        @Override
        public Config.Scope root() {
            return delegate.root();
        }
    }
}

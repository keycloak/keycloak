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
package org.keycloak.connections.infinispan;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.configuration.global.GlobalConfigurationBuilder;
import org.infinispan.manager.DefaultCacheManager;
import org.junit.Assert;
import org.junit.Test;

import static org.keycloak.connections.infinispan.InfinispanConnectionProvider.CLUSTERED_CACHE_NAMES;
import static org.keycloak.connections.infinispan.InfinispanConnectionProvider.LOGIN_FAILURE_CACHE_NAME;

public class EmbeddedOperationalInfoTest {

    @Test
    public void testCacheWithoutConfigurationIsNotReported() throws IOException {
        // As with login-failures:v2: every clustered cache but loginFailures is defined.
        try (DefaultCacheManager cacheManager = new DefaultCacheManager(new GlobalConfigurationBuilder().nonClusteredDefault().build())) {
            for (String name : CLUSTERED_CACHE_NAMES) {
                if (!LOGIN_FAILURE_CACHE_NAME.equals(name)) {
                    cacheManager.defineConfiguration(name, new ConfigurationBuilder().build());
                    cacheManager.getCache(name);
                }
            }

            Map<String, String> info = new HashMap<>();
            DefaultInfinispanConnectionProviderFactory.addEmbeddedOperationalInfo(cacheManager, info);

            Assert.assertFalse(info.containsKey(LOGIN_FAILURE_CACHE_NAME + ":Cache"));
            for (String name : CLUSTERED_CACHE_NAMES) {
                if (!LOGIN_FAILURE_CACHE_NAME.equals(name)) {
                    Assert.assertEquals(name, "HEALTHY", info.get(name + ":Cache"));
                }
            }
            Assert.assertFalse(info.containsValue("FAILED"));
        }
    }
}

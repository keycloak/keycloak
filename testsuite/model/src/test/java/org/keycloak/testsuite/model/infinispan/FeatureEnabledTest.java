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

package org.keycloak.testsuite.model.infinispan;

import java.util.Arrays;

import org.keycloak.common.Profile;
import org.keycloak.common.util.MultiSiteUtils;
import org.keycloak.connections.infinispan.InfinispanConnectionProvider;
import org.keycloak.infinispan.util.InfinispanUtils;
import org.keycloak.testsuite.model.KeycloakModelTest;
import org.keycloak.testsuite.model.RequireProvider;

import org.infinispan.commons.CacheConfigurationException;
import org.junit.Test;

import static org.keycloak.connections.infinispan.InfinispanConnectionProvider.CLUSTERED_CACHE_NAMES;
import static org.keycloak.connections.infinispan.InfinispanConnectionProvider.LOCAL_CACHE_NAMES;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.junit.Assume.assumeFalse;
import static org.junit.Assume.assumeTrue;

/**
 * Checks if the correct embedded or remote cache is started based on {@link org.keycloak.common.Profile.Feature}.
 */
@RequireProvider(InfinispanConnectionProvider.class)
public class FeatureEnabledTest extends KeycloakModelTest {

    @Test
    public void testLocalCaches() {
        inComittedTransaction(session -> {
            var clusterProvider = session.getProvider(InfinispanConnectionProvider.class);
            for (var cacheName : LOCAL_CACHE_NAMES) {
                assertEmbeddedCacheExists(clusterProvider, cacheName);
            }
        });
    }

    @Test
    public void testRemoteCachesOnly() {
        assumeTrue("Clusterless Feature disabled", Profile.isFeatureEnabled(Profile.Feature.CLUSTERLESS) || MultiSiteUtils.isMultiSiteEnabled());
        assertTrue(InfinispanUtils.isRemoteInfinispan());
        assertFalse(InfinispanUtils.isEmbeddedInfinispan());
        inComittedTransaction(session -> {
            var clusterProvider = session.getProvider(InfinispanConnectionProvider.class);
            Arrays.stream(CLUSTERED_CACHE_NAMES).forEach(s -> assertEmbeddedCacheDoesNotExists(clusterProvider, s));
            Arrays.stream(CLUSTERED_CACHE_NAMES).forEach(s -> assertRemoteCacheExists(clusterProvider, s));

        });
    }

    @Test
    public void testEmbeddedCachesOnly() {
        assumeFalse("Multi-Site Feature enabled", MultiSiteUtils.isMultiSiteEnabled());
        assumeFalse("Clusterless Feature enabled", Profile.isFeatureEnabled(Profile.Feature.CLUSTERLESS));
        assertFalse(InfinispanUtils.isRemoteInfinispan());
        assertTrue(InfinispanUtils.isEmbeddedInfinispan());
        inComittedTransaction(session -> {
            var clusterProvider = session.getProvider(InfinispanConnectionProvider.class);
            Arrays.stream(CLUSTERED_CACHE_NAMES).forEach(s -> assertEmbeddedCacheExists(clusterProvider, s));
            Arrays.stream(CLUSTERED_CACHE_NAMES).forEach(s -> assertRemoteCacheCallThrowsException(clusterProvider, s));
        });
    }

    private static void assertEmbeddedCacheExists(InfinispanConnectionProvider provider, String cacheName) {
        assertNotNull(String.format("Embedded cache '%s' should exist", cacheName), provider.getCache(cacheName));
    }

    private static void assertEmbeddedCacheDoesNotExists(InfinispanConnectionProvider provider, String cacheName) {
        try {
            provider.getCache(cacheName);
            fail(String.format("Embedded cache '%s' should not exist", cacheName));
        } catch (CacheConfigurationException expected) {
            // expected
        }
    }

    private static void assertRemoteCacheExists(InfinispanConnectionProvider provider, String cacheName) {
        assertNotNull(String.format("Remote cache '%s' should exist", cacheName), provider.getRemoteCache(cacheName));
    }

    private static void assertRemoteCacheCallThrowsException(InfinispanConnectionProvider provider, String cacheName) {
        try {
            provider.getRemoteCache(cacheName);
            fail(String.format("Remote cache '%s' should not exist", cacheName));
        } catch (IllegalStateException expected) {}
    }

}

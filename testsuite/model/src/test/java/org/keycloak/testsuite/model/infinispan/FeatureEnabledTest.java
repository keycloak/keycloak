package org.keycloak.testsuite.model.infinispan;

import java.util.Arrays;
import java.util.function.Predicate;

import org.infinispan.commons.CacheConfigurationException;
import org.junit.Test;
import org.keycloak.common.Profile;
import org.keycloak.connections.infinispan.InfinispanConnectionProvider;
import org.keycloak.infinispan.util.InfinispanUtils;
import org.keycloak.testsuite.model.KeycloakModelTest;
import org.keycloak.testsuite.model.RequireProvider;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.junit.Assume.assumeFalse;
import static org.junit.Assume.assumeTrue;
import static org.keycloak.connections.infinispan.InfinispanConnectionProvider.ACTION_TOKEN_CACHE;
import static org.keycloak.connections.infinispan.InfinispanConnectionProvider.AUTHENTICATION_SESSIONS_CACHE_NAME;
import static org.keycloak.connections.infinispan.InfinispanConnectionProvider.CLUSTERED_CACHE_NAMES;
import static org.keycloak.connections.infinispan.InfinispanConnectionProvider.LOCAL_CACHE_NAMES;
import static org.keycloak.connections.infinispan.InfinispanConnectionProvider.LOGIN_FAILURE_CACHE_NAME;
import static org.keycloak.connections.infinispan.InfinispanConnectionProvider.WORK_CACHE_NAME;

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
        assumeTrue("Remote-Cache Feature disabled", Profile.isFeatureEnabled(Profile.Feature.REMOTE_CACHE));
        assumeTrue("Multi-Site Feature disabled", Profile.isFeatureEnabled(Profile.Feature.MULTI_SITE));
        assertTrue(InfinispanUtils.isRemoteInfinispan());
        assertFalse(InfinispanUtils.isEmbeddedInfinispan());
        inComittedTransaction(session -> {
            var clusterProvider = session.getProvider(InfinispanConnectionProvider.class);
            assertEmbeddedCacheDoesNotExists(clusterProvider, WORK_CACHE_NAME);
            assertEmbeddedCacheDoesNotExists(clusterProvider, AUTHENTICATION_SESSIONS_CACHE_NAME);
            assertEmbeddedCacheDoesNotExists(clusterProvider, ACTION_TOKEN_CACHE);
            assertEmbeddedCacheDoesNotExists(clusterProvider, LOGIN_FAILURE_CACHE_NAME);

            // TODO [pruivo] all caches eventually won't exists in embedded
            Arrays.stream(CLUSTERED_CACHE_NAMES)
                    .filter(Predicate.not(Predicate.isEqual(WORK_CACHE_NAME)))
                    .filter(Predicate.not(Predicate.isEqual(AUTHENTICATION_SESSIONS_CACHE_NAME)))
                    .filter(Predicate.not(Predicate.isEqual(ACTION_TOKEN_CACHE)))
                    .filter(Predicate.not(Predicate.isEqual(LOGIN_FAILURE_CACHE_NAME)))
                    .forEach(s -> assertEmbeddedCacheExists(clusterProvider, s));

            Arrays.stream(CLUSTERED_CACHE_NAMES).forEach(s -> assertRemoteCacheExists(clusterProvider, s));

        });
    }

    @Test
    public void testRemoteAndEmbeddedCaches() {
        assumeTrue("Multi-Site Feature disabled", Profile.isFeatureEnabled(Profile.Feature.MULTI_SITE));
        assumeFalse("Remote-Cache Feature enabled", Profile.isFeatureEnabled(Profile.Feature.REMOTE_CACHE));
        assertFalse(InfinispanUtils.isRemoteInfinispan());
        assertTrue(InfinispanUtils.isEmbeddedInfinispan());
        inComittedTransaction(session -> {
            var clusterProvider = session.getProvider(InfinispanConnectionProvider.class);
            Arrays.stream(CLUSTERED_CACHE_NAMES).forEach(s -> assertEmbeddedCacheExists(clusterProvider, s));
            Arrays.stream(CLUSTERED_CACHE_NAMES).forEach(s -> assertRemoteCacheExists(clusterProvider, s));
        });
    }

    @Test
    public void testEmbeddedCachesOnly() {
        assumeFalse("Multi-Site Feature enabled", Profile.isFeatureEnabled(Profile.Feature.MULTI_SITE));
        assumeFalse("Remote-Cache Feature enabled", Profile.isFeatureEnabled(Profile.Feature.REMOTE_CACHE));
        assertFalse(InfinispanUtils.isRemoteInfinispan());
        assertTrue(InfinispanUtils.isEmbeddedInfinispan());
        inComittedTransaction(session -> {
            var clusterProvider = session.getProvider(InfinispanConnectionProvider.class);
            Arrays.stream(CLUSTERED_CACHE_NAMES).forEach(s -> assertEmbeddedCacheExists(clusterProvider, s));
            Arrays.stream(CLUSTERED_CACHE_NAMES).forEach(s -> assertRemoteCacheDoesNotExists(clusterProvider, s));
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

    private static void assertRemoteCacheDoesNotExists(InfinispanConnectionProvider provider, String cacheName) {
        assertNull(String.format("Remote cache '%s' should not exist", cacheName), provider.getRemoteCache(cacheName));
    }

}

/*
 * Copyright 2023 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.it.cli.dist;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.keycloak.config.CachingOptions;
import org.keycloak.it.junit5.extension.DistributionTest;
import org.keycloak.it.junit5.extension.RawDistOnly;
import org.keycloak.it.junit5.extension.TestProvider;
import org.keycloak.it.resource.realm.TestRealmResourceTestProvider;
import org.keycloak.it.utils.KeycloakDistribution;

import com.google.common.base.CaseFormat;
import org.infinispan.commons.dataconversion.MediaType;
import org.infinispan.configuration.cache.Configuration;
import org.infinispan.configuration.parsing.ConfigurationBuilderHolder;
import org.infinispan.configuration.parsing.ParserRegistry;
import org.junit.jupiter.api.Test;

import static org.keycloak.connections.infinispan.InfinispanConnectionProvider.CLIENT_SESSION_CACHE_NAME;
import static org.keycloak.connections.infinispan.InfinispanConnectionProvider.CLUSTERED_CACHE_NUM_OWNERS;
import static org.keycloak.connections.infinispan.InfinispanConnectionProvider.OFFLINE_CLIENT_SESSION_CACHE_NAME;
import static org.keycloak.connections.infinispan.InfinispanConnectionProvider.OFFLINE_USER_SESSION_CACHE_NAME;
import static org.keycloak.connections.infinispan.InfinispanConnectionProvider.USER_SESSION_CACHE_NAME;

import static io.restassured.RestAssured.when;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Ryan Emerson <remerson@redhat.com>
 */
@DistributionTest(keepAlive = true, enableTls = true)
@RawDistOnly(reason = "Containers are immutable")
public class ClusterConfigKeepAliveDistTest {
    @Test
    @TestProvider(TestRealmResourceTestProvider.class)
    void testMaxCountApplied(KeycloakDistribution dist) {
        int maxCount = 100;
        Set<String> maxCountCaches = Stream.of(CachingOptions.LOCAL_MAX_COUNT_CACHES, CachingOptions.CLUSTERED_MAX_COUNT_CACHES)
              .flatMap(Arrays::stream)
              .collect(Collectors.toSet());

        StringBuilder sb = new StringBuilder("start-dev --cache=ispn");
        for (String cache : maxCountCaches)
            sb.append(" --").append(CachingOptions.cacheMaxCountProperty(cache)).append("=").append(maxCount);

        String args = sb.toString();
        dist.run(args.split(" "));

        for (String cache : maxCountCaches) {
            Configuration config = getCacheConfiguration(cache);
            assertEquals(maxCount, config.memory().maxCount());
        }
    }

    @Test
    @TestProvider(TestRealmResourceTestProvider.class)
    void testNumOwnersWithPersistentSessions(KeycloakDistribution dist) {
        doNumOwnerTest(dist, false);
    }

    @Test
    @TestProvider(TestRealmResourceTestProvider.class)
    void testNumOwnersWithVolatileSessions(KeycloakDistribution dist) {
        doNumOwnerTest(dist, true);
    }

    @Test
    @TestProvider(TestRealmResourceTestProvider.class)
    void testCheckMinimumNumOwners(KeycloakDistribution dist) {
        List<String> args = new ArrayList<>();
        args.add("start-dev");
        args.add("--cache=ispn");
        args.add("--features-disabled=persistent-user-sessions");

        Arrays.stream(CLUSTERED_CACHE_NUM_OWNERS)
                .map(cache -> CaseFormat.LOWER_CAMEL.to(CaseFormat.LOWER_HYPHEN, cache))
                .map("--spi-cache-embedded--default--%s-owners=1"::formatted)
                .forEach(args::add);
        dist.run(args);

        // forces the numOwner to 2 to prevent data loss.
        assertNumOwner(Arrays.stream(CLUSTERED_CACHE_NUM_OWNERS), 2);
    }

    private void doNumOwnerTest(KeycloakDistribution dist, boolean volatileSessions) {
        final int owners = 5;
        List<String> args = new ArrayList<>();
        args.add("start-dev");
        args.add("--cache=ispn");
        if (volatileSessions) {
            args.add("--features-disabled=persistent-user-sessions");
        }

        Arrays.stream(CLUSTERED_CACHE_NUM_OWNERS)
                .map(cache -> CaseFormat.LOWER_CAMEL.to(CaseFormat.LOWER_HYPHEN, cache))
                .map(cache -> "--spi-cache-embedded--default--%s-owners=%s".formatted(cache, owners))
                .forEach(args::add);
        dist.run(args);

        Stream<String> caches = Arrays.stream(CLUSTERED_CACHE_NUM_OWNERS);
        if (!volatileSessions) {
            Set<String> sessionCaches = Set.of(
                    CLIENT_SESSION_CACHE_NAME,
                    USER_SESSION_CACHE_NAME);
            // filter out session caches, they have numOwner forced to 1.
            caches = caches.filter(Predicate.not(sessionCaches::contains));
            assertNumOwner(sessionCaches.stream(), 1);
        }
        assertNumOwner(caches, owners);
        // offline session caches are not configurable and always have numOwners forced to 1.
        assertNumOwner(Stream.of(OFFLINE_USER_SESSION_CACHE_NAME, OFFLINE_CLIENT_SESSION_CACHE_NAME), 1);
    }

    private static void assertNumOwner(Stream<String> caches, int expectedOwner) {
        caches.map(name -> new CacheOwners(name, getCacheConfiguration(name).clustering().hash().numOwners()))
                .forEach(configuration -> assertEquals(expectedOwner, configuration.owners(), "Wrong numOwner for cache " + configuration.name));
    }

    private static Configuration getCacheConfiguration(String cache) {
        String configJson = when()
                .get("/realms/master/test-resources/cache/" + cache + "/config")
                .thenReturn()
                .getBody()
                .jsonPath()
                .prettyPrint();

        ConfigurationBuilderHolder configHolder = new ParserRegistry().parse(configJson, MediaType.APPLICATION_JSON);
        // Workaround for ISPN-16595
        String cacheName = CaseFormat.LOWER_CAMEL.to(CaseFormat.LOWER_HYPHEN, cache);
        return configHolder.getNamedConfigurationBuilders().get(cacheName).build();
    }

    private record CacheOwners(String name, int owners) {
    }
}

/*
 * Copyright 2025 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.spi.infinispan.impl.embedded;

import java.lang.invoke.MethodHandles;
import java.util.Arrays;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.infinispan.commons.dataconversion.MediaType;
import org.infinispan.configuration.cache.AbstractStoreConfiguration;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.configuration.cache.HashConfiguration;
import org.infinispan.configuration.parsing.ConfigurationBuilderHolder;
import org.infinispan.eviction.EvictionStrategy;
import org.infinispan.transaction.LockingMode;
import org.infinispan.transaction.TransactionMode;
import org.infinispan.transaction.lookup.EmbeddedTransactionManagerLookup;
import org.jboss.logging.Logger;
import org.keycloak.Config;
import org.keycloak.connections.infinispan.InfinispanConnectionProvider;

import static org.keycloak.connections.infinispan.InfinispanConnectionProvider.AUTHORIZATION_CACHE_NAME;
import static org.keycloak.connections.infinispan.InfinispanConnectionProvider.AUTHORIZATION_REVISIONS_CACHE_DEFAULT_MAX;
import static org.keycloak.connections.infinispan.InfinispanConnectionProvider.AUTHORIZATION_REVISIONS_CACHE_NAME;
import static org.keycloak.connections.infinispan.InfinispanConnectionProvider.CLIENT_SESSION_CACHE_NAME;
import static org.keycloak.connections.infinispan.InfinispanConnectionProvider.CRL_CACHE_NAME;
import static org.keycloak.connections.infinispan.InfinispanConnectionProvider.LOCAL_CACHE_NAMES;
import static org.keycloak.connections.infinispan.InfinispanConnectionProvider.LOCAL_MAX_COUNT_CACHES;
import static org.keycloak.connections.infinispan.InfinispanConnectionProvider.OFFLINE_CLIENT_SESSION_CACHE_NAME;
import static org.keycloak.connections.infinispan.InfinispanConnectionProvider.OFFLINE_USER_SESSION_CACHE_NAME;
import static org.keycloak.connections.infinispan.InfinispanConnectionProvider.REALM_CACHE_NAME;
import static org.keycloak.connections.infinispan.InfinispanConnectionProvider.REALM_REVISIONS_CACHE_DEFAULT_MAX;
import static org.keycloak.connections.infinispan.InfinispanConnectionProvider.REALM_REVISIONS_CACHE_NAME;
import static org.keycloak.connections.infinispan.InfinispanConnectionProvider.USER_CACHE_NAME;
import static org.keycloak.connections.infinispan.InfinispanConnectionProvider.USER_REVISIONS_CACHE_DEFAULT_MAX;
import static org.keycloak.connections.infinispan.InfinispanConnectionProvider.USER_REVISIONS_CACHE_NAME;
import static org.keycloak.connections.infinispan.InfinispanConnectionProvider.USER_SESSION_CACHE_NAME;
import static org.keycloak.connections.infinispan.InfinispanConnectionProvider.WORK_CACHE_NAME;

/**
 * Utility class related to the Infinispan cache configuration.
 * <p>
 * This class contains methods to configure caches based on the SPI configuration options, and it provides cache
 * configuration defaults.
 */
public final class CacheConfigurator {

    private static final Logger logger = Logger.getLogger(MethodHandles.lookup().lookupClass());

    // Map with the default cache configuration if the cache is not present in the XML.
    private static final Map<String, Supplier<ConfigurationBuilder>> DEFAULT_CONFIGS = Map.of(CRL_CACHE_NAME, CacheConfigurator::getCrlCacheConfig);
    private static final Supplier<ConfigurationBuilder> TO_NULL = () -> null;
    private static final String MAX_COUNT_SUFFIX = "MaxCount";

    private CacheConfigurator() {
    }

    /**
     * Configures the Infinispan local caches used by Keycloak (e.g., for realm or user data) using the provided
     * Keycloak configuration.
     *
     * @param keycloakConfig The Keycloak configuration.
     * @param holder         The {@link ConfigurationBuilderHolder} where the caches will be defined.
     * @throws IllegalStateException if an Infinispan cache is not defined. This could indicate a missing or incorrect
     *                               configuration.
     */
    public static void configureLocalCaches(Config.Scope keycloakConfig, ConfigurationBuilderHolder holder) {
        logger.debug("Configuring embedded local caches");
        // configure local caches except revision caches
        configureCacheMaxCount(keycloakConfig, holder, Arrays.stream(LOCAL_MAX_COUNT_CACHES));
        // configure revision caches
        configureRevisionCache(holder, REALM_CACHE_NAME, REALM_REVISIONS_CACHE_NAME, REALM_REVISIONS_CACHE_DEFAULT_MAX);
        configureRevisionCache(holder, USER_CACHE_NAME, USER_REVISIONS_CACHE_NAME, USER_REVISIONS_CACHE_DEFAULT_MAX);
        configureRevisionCache(holder, AUTHORIZATION_CACHE_NAME, AUTHORIZATION_REVISIONS_CACHE_NAME, AUTHORIZATION_REVISIONS_CACHE_DEFAULT_MAX);
        // check all caches are defined
        checkCachesExist(holder, Arrays.stream(LOCAL_CACHE_NAMES));
    }

    /**
     * Applies the default Infinispan cache configuration to the {@code holder}, if the cache is not present.
     * <p>
     * Each cache may have its own default configuration.
     *
     * @param holder The {@link ConfigurationBuilderHolder} where the caches will be defined.
     */
    public static void applyDefaultConfiguration(ConfigurationBuilderHolder holder) {
        var configs = holder.getNamedConfigurationBuilders();
        for (var name : InfinispanConnectionProvider.ALL_CACHES_NAME) {
            configs.computeIfAbsent(name, cacheName -> DEFAULT_CONFIGS.getOrDefault(cacheName, TO_NULL).get());
        }
    }

    /**
     * Verifies that all the {@code caches} are defined in the {@code holder}.
     *
     * @param holder The {@link ConfigurationBuilderHolder} where the caches are configured.
     * @param caches The {@link Stream} containing the names of the caches to check.
     * @throws IllegalStateException if one or more Infinispan caches from the provided {@code caches} stream are not
     *                               defined in the {@code holder}. This could indicate a missing or incorrect
     *                               configuration for those specific caches.
     */
    public static void checkCachesExist(ConfigurationBuilderHolder holder, Stream<String> caches) {
        for (var it = caches.iterator(); it.hasNext(); ) {
            var cache = it.next();
            var builder = holder.getNamedConfigurationBuilders().get(cache);
            if (builder == null) {
                throw cacheNotFound(cache);
            }
        }
    }

    /**
     * Validates that the "work" cache is present in the {@code holder} and has a valid configuration.
     *
     * @param holder The {@link ConfigurationBuilderHolder} where the caches are configured.
     * @throws IllegalStateException if the "work" cache is not found in the holder.
     * @throws RuntimeException      if the "work" cache has an invalid configuration. This could include an incorrect
     *                               settings that would prevent the cache from functioning correctly.
     */
    public static void validateWorkCacheConfiguration(ConfigurationBuilderHolder holder) {
        logger.debugf("Validating %s cache configuration", WORK_CACHE_NAME);
        var cacheBuilder = holder.getNamedConfigurationBuilders().get(WORK_CACHE_NAME);
        if (cacheBuilder == null) {
            throw cacheNotFound(WORK_CACHE_NAME);
        }
        if (holder.getGlobalConfigurationBuilder().cacheContainer().transport().getTransport() == null) {
            // non-clustered, Keycloak started in dev mode?
            return;
        }
        var cacheMode = cacheBuilder.clustering().cacheMode();
        if (!cacheMode.isReplicated()) {
            throw new RuntimeException("Unable to start Keycloak. '%s' cache must be replicated but is %s".formatted(WORK_CACHE_NAME, cacheMode.friendlyCacheModeString().toLowerCase()));
        }
    }

    /**
     * Removes clustered caches from the {@code holder}.
     *
     * @param holder The {@link ConfigurationBuilderHolder} where the caches are configured.
     */
    public static void removeClusteredCaches(ConfigurationBuilderHolder holder) {
        logger.debug("Removing clustered caches");
        Arrays.stream(InfinispanConnectionProvider.CLUSTERED_CACHE_NAMES).forEach(holder.getNamedConfigurationBuilders()::remove);
    }

    /**
     * Configures the maximum number of entries for the specified caches, bounding them to this limit and preventing
     * excessive memory usage.
     *
     * @param keycloakConfig The Keycloak configuration, which provides the maximum entry counts for the caches.
     * @param holder         The {@link ConfigurationBuilderHolder} where the caches are configured.
     * @param caches         The {@link Stream} containing the names of the caches to configure with a maximum count.
     * @throws IllegalStateException if an Infinispan cache from the provided {@code caches} stream is not defined in
     *                               the {@code holder}. This could indicate a missing or incorrect configuration.
     */
    public static void configureCacheMaxCount(Config.Scope keycloakConfig, ConfigurationBuilderHolder holder, Stream<String> caches) {
        for (var it = caches.iterator(); it.hasNext(); ) {
            var name = it.next();
            var builder = holder.getNamedConfigurationBuilders().get(name);
            if (builder == null) {
                throw cacheNotFound(name);
            }
            setMemoryMaxCount(keycloakConfig, name, builder);
        }
    }

    /**
     * Configures all the sessions caches when persistent user sessions feature is enabled.
     *
     * @param holder The {@link ConfigurationBuilderHolder} where the caches are configured.
     * @throws IllegalStateException if an Infinispan cache from the provided {@code caches} stream is not defined in
     *                               the {@code holder}. This could indicate a missing or incorrect configuration.
     */
    public static void configureSessionsCachesForPersistentSessions(ConfigurationBuilderHolder holder) {
        logger.debug("Configuring session cache (persistent user sessions)");
        for (var name : Arrays.asList(USER_SESSION_CACHE_NAME, CLIENT_SESSION_CACHE_NAME, OFFLINE_USER_SESSION_CACHE_NAME, OFFLINE_CLIENT_SESSION_CACHE_NAME)) {
            var builder = holder.getNamedConfigurationBuilders().get(name);
            if (builder == null) {
                throw cacheNotFound(name);
            }
            if (builder.memory().maxCount() == -1) {
                logger.infof("Persistent user sessions enabled and no memory limit found in configuration. Setting max entries for %s to 10000 entries.", name);
                builder.memory().maxCount(10000);
            }
            /* The number of owners for these caches then need to be set to `1` to avoid backup owners with inconsistent data.
             As primary owner evicts a key based on its locally evaluated maxCount setting, it wouldn't tell the backup owner about this, and then the backup owner would be left with a soon-to-be-outdated key.
             While a `remove` is forwarded to the backup owner regardless if the key exists on the primary owner, a `computeIfPresent` is not, and it would leave a backup owner with an outdated key.
             With the number of owners set to `1`, there will be no backup owners, so this is the setting to choose with persistent sessions enabled to ensure consistent data in the caches. */
            builder.clustering().hash().numOwners(1);
        }
    }

    /**
     * Configures all the sessions caches when persistent user sessions feature is enabled.
     *
     * @param holder The {@link ConfigurationBuilderHolder} where the caches are configured.
     * @throws IllegalStateException if an Infinispan cache from the provided {@code caches} stream is not defined in
     *                               the {@code holder}. This could indicate a missing or incorrect configuration.
     */
    public static void configureSessionsCachesForVolatileSessions(ConfigurationBuilderHolder holder) {
        logger.debug("Configuring session cache (volatile user sessions)");
        for (var name : Arrays.asList(USER_SESSION_CACHE_NAME, CLIENT_SESSION_CACHE_NAME)) {
            var builder = holder.getNamedConfigurationBuilders().get(name);
            if (builder == null) {
                throw cacheNotFound(name);
            }
            if (builder.memory().maxCount() != -1) {
                logger.infof("Persistent user sessions disabled and memory limit is set. Ignoring cache limits to avoid losing sessions for cache %s.", name);
                builder.memory().maxCount(-1);
            }
            if (builder.clustering().hash().attributes().attribute(HashConfiguration.NUM_OWNERS).get() == 1 &&
                  builder.persistence().stores().stream().noneMatch(p -> p.attributes().attribute(AbstractStoreConfiguration.SHARED).get())
            ) {
                logger.infof("Persistent user sessions disabled with number of owners set to default value 1 for cache %s and no shared persistence store configured. Setting num_owners=2 to avoid data loss.", name);
                builder.clustering().hash().numOwners(2);
            }
        }

        for (var name : Arrays.asList( OFFLINE_USER_SESSION_CACHE_NAME, OFFLINE_CLIENT_SESSION_CACHE_NAME)) {
            var builder = holder.getNamedConfigurationBuilders().get(name);
            if (builder == null) {
                throw cacheNotFound(name);
            }
            if (builder.memory().maxCount() == -1) {
                logger.infof("Offline sessions should have a max count set to avoid excessive memory usage. Setting a default cache limit of 10000 for cache %s.", name);
                builder.memory().maxCount(10000);
            }
            if (builder.clustering().hash().attributes().attribute(HashConfiguration.NUM_OWNERS).get() != 1 &&
                    builder.persistence().stores().stream().noneMatch(p -> p.attributes().attribute(AbstractStoreConfiguration.SHARED).get())
            ) {
                logger.infof("Setting a memory limit implies to have exactly one owne. Setting num_owners=1 to avoid data loss.", name);
                builder.clustering().hash().numOwners(1);
            }
        }
    }

    // private methods below

    private static void configureRevisionCache(ConfigurationBuilderHolder holder, String baseCache, String revisionCache, long defaultMaxEntries) {
        var baseBuilder = holder.getNamedConfigurationBuilders().get(baseCache);
        if (baseBuilder == null) {
            throw cacheNotFound(baseCache);
        }
        var maxCount = baseBuilder.memory().maxCount();
        maxCount = maxCount > 0 ? 2 * maxCount : defaultMaxEntries;
        logger.debugf("Creating revision cache '%s' with max-count %s", revisionCache, maxCount);
        holder.getNamedConfigurationBuilders().put(revisionCache, getRevisionCacheConfig(maxCount));
    }

    private static void setMemoryMaxCount(Config.Scope keycloakConfig, String name, ConfigurationBuilder builder) {
        var maxCount = keycloakConfig.getInt(maxCountConfigKey(name));
        if (maxCount != null) {
            logger.debugf("Overwriting max-count for cache '%s' to %s entries", name, maxCount);
            builder.memory().maxCount(maxCount);
        }
    }

    public static String maxCountConfigKey(String name) {
        return name + MAX_COUNT_SUFFIX;
    }

    private static IllegalStateException cacheNotFound(String cache) {
        return new IllegalStateException("Infinispan cache '%s' not found.".formatted(cache));
    }

    // cache configuration below

    public static ConfigurationBuilder getCrlCacheConfig() {
        var builder = createCacheConfigurationBuilder();
        builder.memory().whenFull(EvictionStrategy.REMOVE).maxCount(InfinispanConnectionProvider.CRL_CACHE_DEFAULT_MAX);
        return builder;
    }

    public static ConfigurationBuilder getRevisionCacheConfig(long maxEntries) {
        var builder = createCacheConfigurationBuilder();
        builder.simpleCache(false);
        builder.invocationBatching().enable().transaction().transactionMode(TransactionMode.TRANSACTIONAL);

        // Use Embedded manager even in managed ( wildfly/eap ) environment. We don't want infinispan to participate in global transaction
        builder.transaction().transactionManagerLookup(new EmbeddedTransactionManagerLookup());

        builder.transaction().lockingMode(LockingMode.PESSIMISTIC);
        if (builder.memory().storage().canStoreReferences()) {
            builder.encoding().mediaType(MediaType.APPLICATION_OBJECT_TYPE);
        }

        builder.memory().whenFull(EvictionStrategy.REMOVE).maxCount(maxEntries);

        return builder;
    }

    public static ConfigurationBuilder createCacheConfigurationBuilder() {
        ConfigurationBuilder builder = new ConfigurationBuilder();

        // need to force the encoding to application/x-java-object to avoid unnecessary conversion of keys/values. See WFLY-14356.
        builder.encoding().mediaType(MediaType.APPLICATION_OBJECT_TYPE);

        // needs to be disabled if transaction is enabled
        builder.simpleCache(true);

        return builder;
    }

}

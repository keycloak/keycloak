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
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import org.keycloak.Config;
import org.keycloak.config.CachingOptions;
import org.keycloak.marshalling.Marshalling;
import org.keycloak.models.sessions.infinispan.entities.LoginFailureEntity;
import org.keycloak.models.sessions.infinispan.entities.RemoteAuthenticatedClientSessionEntity;
import org.keycloak.models.sessions.infinispan.entities.RemoteUserSessionEntity;
import org.keycloak.models.sessions.infinispan.entities.RootAuthenticationSessionEntity;

import org.infinispan.commons.dataconversion.MediaType;
import org.infinispan.configuration.cache.AbstractStoreConfiguration;
import org.infinispan.configuration.cache.BackupConfiguration;
import org.infinispan.configuration.cache.BackupFailurePolicy;
import org.infinispan.configuration.cache.CacheMode;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.configuration.cache.HashConfiguration;
import org.infinispan.configuration.cache.HashConfigurationBuilder;
import org.infinispan.configuration.parsing.ConfigurationBuilderHolder;
import org.infinispan.eviction.EvictionStrategy;
import org.infinispan.transaction.LockingMode;
import org.infinispan.transaction.TransactionMode;
import org.infinispan.transaction.lookup.EmbeddedTransactionManagerLookup;
import org.jboss.logging.Logger;

import static org.keycloak.connections.infinispan.InfinispanConnectionProvider.ACTION_TOKEN_CACHE;
import static org.keycloak.connections.infinispan.InfinispanConnectionProvider.ALL_CACHES_NAME;
import static org.keycloak.connections.infinispan.InfinispanConnectionProvider.AUTHENTICATION_SESSIONS_CACHE_NAME;
import static org.keycloak.connections.infinispan.InfinispanConnectionProvider.AUTHORIZATION_CACHE_NAME;
import static org.keycloak.connections.infinispan.InfinispanConnectionProvider.AUTHORIZATION_REVISIONS_CACHE_DEFAULT_MAX;
import static org.keycloak.connections.infinispan.InfinispanConnectionProvider.AUTHORIZATION_REVISIONS_CACHE_NAME;
import static org.keycloak.connections.infinispan.InfinispanConnectionProvider.CLIENT_SESSION_CACHE_NAME;
import static org.keycloak.connections.infinispan.InfinispanConnectionProvider.CLUSTERED_CACHE_NAMES;
import static org.keycloak.connections.infinispan.InfinispanConnectionProvider.CLUSTERED_CACHE_NUM_OWNERS;
import static org.keycloak.connections.infinispan.InfinispanConnectionProvider.CLUSTERED_MAX_COUNT_CACHES;
import static org.keycloak.connections.infinispan.InfinispanConnectionProvider.CRL_CACHE_DEFAULT_MAX;
import static org.keycloak.connections.infinispan.InfinispanConnectionProvider.CRL_CACHE_NAME;
import static org.keycloak.connections.infinispan.InfinispanConnectionProvider.KEYS_CACHE_DEFAULT_MAX;
import static org.keycloak.connections.infinispan.InfinispanConnectionProvider.KEYS_CACHE_MAX_IDLE_SECONDS;
import static org.keycloak.connections.infinispan.InfinispanConnectionProvider.KEYS_CACHE_NAME;
import static org.keycloak.connections.infinispan.InfinispanConnectionProvider.LOCAL_CACHE_NAMES;
import static org.keycloak.connections.infinispan.InfinispanConnectionProvider.LOCAL_MAX_COUNT_CACHES;
import static org.keycloak.connections.infinispan.InfinispanConnectionProvider.LOGIN_FAILURE_CACHE_NAME;
import static org.keycloak.connections.infinispan.InfinispanConnectionProvider.OFFLINE_CLIENT_SESSION_CACHE_NAME;
import static org.keycloak.connections.infinispan.InfinispanConnectionProvider.OFFLINE_USER_SESSION_CACHE_NAME;
import static org.keycloak.connections.infinispan.InfinispanConnectionProvider.REALM_CACHE_NAME;
import static org.keycloak.connections.infinispan.InfinispanConnectionProvider.REALM_REVISIONS_CACHE_DEFAULT_MAX;
import static org.keycloak.connections.infinispan.InfinispanConnectionProvider.REALM_REVISIONS_CACHE_NAME;
import static org.keycloak.connections.infinispan.InfinispanConnectionProvider.SESSIONS_CACHE_DEFAULT_MAX;
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

    private static final String MAX_COUNT_SUFFIX = "MaxCount";
    private static final String OWNER_SUFFIX = "Owners";
    private static final int STATE_TRANSFER_CHUNK_SIZE = 16;
    private static final int MIN_NUM_OWNERS_REMOTE_CACHE = 2;

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
    public static void applyDefaultConfiguration(ConfigurationBuilderHolder holder, boolean warnMutate) {
        var configs = holder.getNamedConfigurationBuilders();
        boolean userProvidedConfig = false;
        boolean clustered = isClustered(holder);
        for (var name : ALL_CACHES_NAME) {
            var config = configs.get(name);
            if (config == null) {
                configs.put(name, getCacheConfiguration(name, clustered));
            } else if (!userProvidedConfig) {
                userProvidedConfig = true;
            }
        }
        if (warnMutate && userProvidedConfig) {
            logger.warnf("Modifying the default cache configuration in the config file without setting %s=true is deprecated.", CachingOptions.CACHE_CONFIG_MUTATE_PROPERTY);
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
        if (!isClustered(holder)) {
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
        Arrays.stream(CLUSTERED_CACHE_NAMES).forEach(holder.getNamedConfigurationBuilders()::remove);
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
        boolean clustered = isClustered(holder);
        for (var it = caches.iterator(); it.hasNext(); ) {
            var name = it.next();
            var builder = holder.getNamedConfigurationBuilders().get(name);
            if (builder == null) {
                throw cacheNotFound(name);
            }
            var maxCount = keycloakConfig.getLong(maxCountConfigKey(name));
            if (maxCount != null) {
                if (maxCount < 0) {
                    // Prevent users setting an unbounded max-count for any cache that already has a default max-count defined
                    maxCount = getCacheConfiguration(name, clustered).memory().maxCount();
                    if (maxCount > -1)
                        logger.infof("Ignoring unbounded max-count for cache '%s', reverting to default max of %d entries.", name, maxCount);
                } else {
                    logger.debugf("Overwriting max-count for cache '%s' to %s entries", name, maxCount);
                }
                builder.memory().maxCount(maxCount);
            }
        }
    }

    /**
     * Configures all the sessions caches when persistent user sessions feature is enabled.
     *
     * @param holder The {@link ConfigurationBuilderHolder} where the caches are configured.
     * @throws IllegalStateException if an Infinispan cache from the provided {@code caches} stream is not defined in
     *                               the {@code holder}. This could indicate a missing or incorrect configuration.
     */
    public static void configureSessionsCachesForPersistentSessions(Config.Scope keycloakConfig, ConfigurationBuilderHolder holder) {
        logger.debug("Configuring session cache (persistent user sessions)");
        for (var name : CLUSTERED_MAX_COUNT_CACHES) {
            var builder = holder.getNamedConfigurationBuilders().get(name);
            if (builder == null) {
                throw cacheNotFound(name);
            }
            setMemoryMaxCount(keycloakConfig, name, builder);
            if (builder.memory().maxCount() == -1) {
                logger.infof("Persistent user sessions enabled and no memory limit found in configuration. Setting max entries for %s to %d entries.", name, SESSIONS_CACHE_DEFAULT_MAX);
                builder.memory().maxCount(SESSIONS_CACHE_DEFAULT_MAX);
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
    public static void configureSessionsCachesForVolatileSessions(Config.Scope keycloakConfig, ConfigurationBuilderHolder holder) {
        logger.debug("Configuring session cache (volatile user sessions)");
        for (var name : Arrays.asList(USER_SESSION_CACHE_NAME, CLIENT_SESSION_CACHE_NAME)) {
            var builder = holder.getNamedConfigurationBuilders().get(name);
            if (builder == null) {
                throw cacheNotFound(name);
            }

            setMemoryMaxCount(keycloakConfig, name, builder);
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

        for (var name : Arrays.asList(OFFLINE_USER_SESSION_CACHE_NAME, OFFLINE_CLIENT_SESSION_CACHE_NAME)) {
            var builder = holder.getNamedConfigurationBuilders().get(name);
            if (builder == null) {
                throw cacheNotFound(name);
            }

            setMemoryMaxCount(keycloakConfig, name, builder);
            if (builder.memory().maxCount() == -1) {
                logger.infof("Offline sessions should have a max count set to avoid excessive memory usage. Setting a default cache limit of %d for cache %s.", SESSIONS_CACHE_DEFAULT_MAX, name);
                builder.memory().maxCount(SESSIONS_CACHE_DEFAULT_MAX);
            }
            if (builder.clustering().hash().attributes().attribute(HashConfiguration.NUM_OWNERS).get() != 1 &&
                    builder.persistence().stores().stream().noneMatch(p -> p.attributes().attribute(AbstractStoreConfiguration.SHARED).get())
            ) {
                logger.infof("Setting a memory limit implies to have exactly one owner. Setting num_owners=1 to avoid data loss.", name);
                builder.clustering().hash().numOwners(1);
            }
        }
    }

    /**
     * Configures the caches "actionToken", "authenticationSessions", and "loginFailures" with the minimum number of
     * owners to prevent data loss in a single instance crash.
     * <p>
     * The data in those caches only exist in memory, therefore they must have more than one owner configured.
     *
     * @param holder The {@link ConfigurationBuilderHolder} where the caches are configured.
     * @throws IllegalStateException if an Infinispan cache is not defined in the {@code holder}. This could indicate a
     *                               missing or incorrect configuration.
     */
    public static void ensureMinimumOwners(ConfigurationBuilderHolder holder) {
        for (var name : Arrays.asList(
                LOGIN_FAILURE_CACHE_NAME,
                AUTHENTICATION_SESSIONS_CACHE_NAME,
                ACTION_TOKEN_CACHE)) {
            var builder = holder.getNamedConfigurationBuilders().get(name);
            if (builder == null) {
                throw cacheNotFound(name);
            }
            var hashConfig = builder.clustering().hash();
            var owners = hashConfig.attributes().attribute(HashConfiguration.NUM_OWNERS).get();
            if (owners < 2) {
                logger.infof("Setting num_owners=2 (configured value is %s) for cache '%s' to prevent data loss.", owners, name);
                hashConfig.numOwners(2);
            }
        }
    }

    /**
     * Configures (and overwrites) the {@link HashConfigurationBuilder#numOwners(int)} based on the SPI configuration
     * input.
     *
     * @param keycloakConfig The Keycloak configuration, which provides the number owners value for the caches.
     * @param holder         The {@link ConfigurationBuilderHolder} where the caches are configured.
     */
    public static void configureNumOwners(Config.Scope keycloakConfig, ConfigurationBuilderHolder holder) {
        for (var name : CLUSTERED_CACHE_NUM_OWNERS) {
            var builder = holder.getNamedConfigurationBuilders().get(name);
            if (builder == null) {
                throw cacheNotFound(name);
            }
            var owners = keycloakConfig.getInt(numOwnerConfigKey(name));
            if (owners != null) {
                builder.clustering().hash().numOwners(owners);
            }
        }
    }

    /**
     * Creates a {@link ConfigurationBuilder} for a cache in a remote Infinispan cluster.
     * <p>
     * The returned builder is a template based on the provider's default configuration, which can be freely modified by
     * the caller before use.
     *
     * @param cacheName The name of the cache for which to create the configuration.
     * @param config    The provider's base configuration scope, which may contain cache-specific customizations.
     * @param sites     An array of remote site names for cross-site replication backups. If null or empty, cross-site
     *                  replication will be disabled.
     * @return A {@link ConfigurationBuilder} for the specified cache, or {@code null} if no configuration exists for
     * the given {@code cacheName}.
     */
    public static ConfigurationBuilder getRemoteCacheConfiguration(String cacheName, Config.Scope config, String[] sites) {
        return switch (cacheName) {
            case CLIENT_SESSION_CACHE_NAME, OFFLINE_CLIENT_SESSION_CACHE_NAME ->
                    remoteCacheConfigurationBuilder(cacheName, config, sites, RemoteAuthenticatedClientSessionEntity.class);
            case USER_SESSION_CACHE_NAME, OFFLINE_USER_SESSION_CACHE_NAME ->
                    remoteCacheConfigurationBuilder(cacheName, config, sites, RemoteUserSessionEntity.class);
            case AUTHENTICATION_SESSIONS_CACHE_NAME ->
                    remoteCacheConfigurationBuilder(cacheName, config, sites, RootAuthenticationSessionEntity.class);
            case LOGIN_FAILURE_CACHE_NAME ->
                    remoteCacheConfigurationBuilder(cacheName, config, sites, LoginFailureEntity.class);
            case ACTION_TOKEN_CACHE, WORK_CACHE_NAME -> remoteCacheConfigurationBuilder(cacheName, config, sites, null);
            default -> null;
        };
    }

    // private methods below

    private static ConfigurationBuilder remoteCacheConfigurationBuilder(String name, Config.Scope config, String[] sites, Class<?> indexedEntity) {
        var builder = new ConfigurationBuilder();
        builder.clustering().cacheMode(CacheMode.DIST_SYNC);
        builder.clustering().hash().numOwners(Math.max(MIN_NUM_OWNERS_REMOTE_CACHE, config.getInt(numOwnerConfigKey(name), MIN_NUM_OWNERS_REMOTE_CACHE)));
        builder.clustering().stateTransfer().chunkSize(STATE_TRANSFER_CHUNK_SIZE);
        builder.encoding().mediaType(MediaType.APPLICATION_PROTOSTREAM);
        builder.statistics().enable();

        if (indexedEntity != null) {
            builder.indexing().enable().addIndexedEntities(Marshalling.protoEntity(indexedEntity));
        }

        if (sites == null || sites.length == 0) {
            return builder;
        }

        // we need transactions for cross-site to detect deadlock and rollback any changes.
        builder.transaction()
                .transactionMode(TransactionMode.TRANSACTIONAL)
                .useSynchronization(false)
                .lockingMode(LockingMode.PESSIMISTIC);
        for (var site : sites) {
            builder.sites().addBackup()
                    .site(site)
                    .strategy(BackupConfiguration.BackupStrategy.SYNC)
                    .backupFailurePolicy(BackupFailurePolicy.FAIL)
                    .stateTransfer().chunkSize(STATE_TRANSFER_CHUNK_SIZE);
        }
        return builder;
    }

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
        var maxCount = keycloakConfig.getLong(maxCountConfigKey(name));
        if (maxCount != null) {
            builder.memory().maxCount(maxCount);
        }
    }

    public static String maxCountConfigKey(String name) {
        return name + MAX_COUNT_SUFFIX;
    }

    public static String numOwnerConfigKey(String name) {
        return name + OWNER_SUFFIX;
    }

    private static IllegalStateException cacheNotFound(String cache) {
        return new IllegalStateException("Infinispan cache '%s' not found.".formatted(cache));
    }

    // cache configuration below

    public static ConfigurationBuilder getCrlCacheConfig() {
        return getCacheConfiguration(CRL_CACHE_NAME, true);
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

    /**
     * Returns a cache's default configuration.
     * Revision caches are not returned as their configuration depends on their associated cache's configuration.
     */
    public static ConfigurationBuilder getCacheConfiguration(String cacheName, boolean clustered) {
        var builder = new ConfigurationBuilder();
        switch (cacheName) {
            // Distributed Caches
            case CLIENT_SESSION_CACHE_NAME:
            case OFFLINE_CLIENT_SESSION_CACHE_NAME:
                // Groups keys by user session ID.
                if (clustered) {
                    builder.clustering().hash().groups()
                            .enabled()
                            .addGrouper(ClientSessionKeyGrouper.INSTANCE);
                }
            case USER_SESSION_CACHE_NAME:
            case OFFLINE_USER_SESSION_CACHE_NAME:
                if (clustered) {
                    builder.clustering().cacheMode(CacheMode.DIST_SYNC).hash().numOwners(1);
                }
                builder.memory().maxCount(SESSIONS_CACHE_DEFAULT_MAX);
                return builder;
            case ACTION_TOKEN_CACHE:
            case AUTHENTICATION_SESSIONS_CACHE_NAME:
            case LOGIN_FAILURE_CACHE_NAME:
                if (clustered) {
                    builder.clustering().cacheMode(CacheMode.DIST_SYNC);
                }
                builder.encoding().mediaType(MediaType.APPLICATION_OBJECT_TYPE);
                return builder;
            // Local Caches
            case CRL_CACHE_NAME:
                builder.simpleCache(true);
                builder.memory().whenFull(EvictionStrategy.REMOVE).maxCount(CRL_CACHE_DEFAULT_MAX);
                return builder;
            case KEYS_CACHE_NAME:
                builder.simpleCache(true);
                builder.expiration().maxIdle(KEYS_CACHE_MAX_IDLE_SECONDS, TimeUnit.SECONDS);
                builder.memory().whenFull(EvictionStrategy.REMOVE).maxCount(KEYS_CACHE_DEFAULT_MAX);
                return builder;
            case AUTHORIZATION_CACHE_NAME:
            case REALM_CACHE_NAME:
            case USER_CACHE_NAME:
                builder.simpleCache(true);
                builder.memory().whenFull(EvictionStrategy.REMOVE).maxCount(10000);
                return builder;
            // Replicated caches
            case WORK_CACHE_NAME:
                if (clustered) {
                    builder.clustering().cacheMode(CacheMode.REPL_SYNC);
                }
                return builder;
            default:
                return null;
        }
    }

    private static boolean isClustered(ConfigurationBuilderHolder holder) {
        return holder.getGlobalConfigurationBuilder().transport().getTransport() != null;
    }
}

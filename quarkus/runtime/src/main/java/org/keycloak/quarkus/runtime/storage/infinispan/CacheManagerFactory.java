/*
 * Copyright 2021 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.quarkus.runtime.storage.infinispan;

import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Map;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Stream;

import io.micrometer.core.instrument.Metrics;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.configuration.cache.HashConfiguration;
import org.infinispan.configuration.global.ShutdownHookBehavior;
import org.infinispan.configuration.parsing.ConfigurationBuilderHolder;
import org.infinispan.configuration.parsing.ParserRegistry;
import org.infinispan.manager.DefaultCacheManager;
import org.infinispan.manager.EmbeddedCacheManager;
import org.infinispan.metrics.config.MicrometerMeterRegisterConfigurationBuilder;
import org.infinispan.persistence.remote.configuration.RemoteStoreConfigurationBuilder;
import org.jboss.logging.Logger;
import org.keycloak.common.Profile;
import org.keycloak.common.util.MultiSiteUtils;
import org.keycloak.config.CachingOptions;
import org.keycloak.config.MetricsOptions;
import org.keycloak.config.Option;
import org.keycloak.connections.infinispan.InfinispanConnectionProvider;
import org.keycloak.connections.infinispan.InfinispanUtil;
import org.keycloak.infinispan.util.InfinispanUtils;
import org.keycloak.jgroups.JGroupsConfigurator;
import org.keycloak.marshalling.Marshalling;
import org.keycloak.models.KeycloakSession;
import org.keycloak.quarkus.runtime.configuration.Configuration;

import javax.net.ssl.SSLContext;

import static org.keycloak.config.CachingOptions.CACHE_REMOTE_PASSWORD_PROPERTY;
import static org.keycloak.config.CachingOptions.CACHE_REMOTE_USERNAME_PROPERTY;
import static org.keycloak.connections.infinispan.InfinispanConnectionProvider.CLIENT_SESSION_CACHE_NAME;
import static org.keycloak.connections.infinispan.InfinispanConnectionProvider.CLUSTERED_CACHE_NAMES;
import static org.keycloak.connections.infinispan.InfinispanConnectionProvider.CRL_CACHE_NAME;
import static org.keycloak.connections.infinispan.InfinispanConnectionProvider.LOCAL_CACHE_NAMES;
import static org.keycloak.connections.infinispan.InfinispanConnectionProvider.OFFLINE_CLIENT_SESSION_CACHE_NAME;
import static org.keycloak.connections.infinispan.InfinispanConnectionProvider.OFFLINE_USER_SESSION_CACHE_NAME;
import static org.keycloak.connections.infinispan.InfinispanConnectionProvider.USER_AND_CLIENT_SESSION_CACHES;
import static org.keycloak.connections.infinispan.InfinispanConnectionProvider.USER_SESSION_CACHE_NAME;
import static org.keycloak.connections.infinispan.InfinispanConnectionProvider.WORK_CACHE_NAME;

public class CacheManagerFactory {

    public static final Logger logger = Logger.getLogger(CacheManagerFactory.class);
    // Map with the default cache configuration if the cache is not present in the XML.
    private static final Map<String, Supplier<ConfigurationBuilder>> DEFAULT_CONFIGS = Map.of(
            CRL_CACHE_NAME, InfinispanUtil::getCrlCacheConfig
    );
    private static final Supplier<ConfigurationBuilder> TO_NULL = () -> null;

    private volatile EmbeddedCacheManager cacheManager;
    private final JGroupsConfigurator jGroupsConfigurator;

    public CacheManagerFactory(String config) {
        ConfigurationBuilderHolder builder = new ParserRegistry().parse(config);
        jGroupsConfigurator = JGroupsConfigurator.create(builder);
    }

    public EmbeddedCacheManager getOrCreateEmbeddedCacheManager(KeycloakSession keycloakSession) {
        if (cacheManager != null)
            return cacheManager;

        synchronized (this) {
            if (cacheManager == null) {
                cacheManager = startEmbeddedCacheManager(keycloakSession);
            }
        }
        return cacheManager;
    }

    private EmbeddedCacheManager startEmbeddedCacheManager(KeycloakSession session) {
        logger.info("Starting Infinispan embedded cache manager");
        var builder = jGroupsConfigurator.holder();

        // We must disable the Infinispan default ShutdownHook as we manage the EmbeddedCacheManager lifecycle explicitly
        // with #shutdown and multiple calls to EmbeddedCacheManager#stop can lead to Exceptions being thrown
        builder.getGlobalConfigurationBuilder().shutdown().hookBehavior(ShutdownHookBehavior.DONT_REGISTER);

        Marshalling.configure(builder.getGlobalConfigurationBuilder());
        assertAllCachesAreConfigured(builder,
                // skip revision caches, those are defined by DefaultInfinispanConnectionProviderFactory
                Arrays.stream(LOCAL_CACHE_NAMES)
                        .filter(Predicate.not(InfinispanConnectionProvider.REALM_REVISIONS_CACHE_NAME::equals))
                        .filter(Predicate.not(InfinispanConnectionProvider.AUTHORIZATION_REVISIONS_CACHE_NAME::equals))
                        .filter(Predicate.not(InfinispanConnectionProvider.USER_REVISIONS_CACHE_NAME::equals))
        );
        if (InfinispanUtils.isRemoteInfinispan()) {
            var builders = builder.getNamedConfigurationBuilders();
            // remove all distributed caches
            logger.debug("Removing all distributed caches.");
            for (String cacheName : CLUSTERED_CACHE_NAMES) {
               if (hasRemoteStore(builders.get(cacheName))) {
                   logger.warnf("remote-store configuration detected for cache '%s'. Explicit cache configuration ignored when using '%s' or '%s' Features.", cacheName, Profile.Feature.CLUSTERLESS.getKey(), Profile.Feature.MULTI_SITE.getKey());
               }
               builders.remove(cacheName);
            }
            // Disable JGroups, not required when the data is stored in the Remote Cache.
            // The existing caches are local and do not require JGroups to work properly.
            builder.getGlobalConfigurationBuilder().nonClusteredDefault();
        } else {
            // embedded mode!
            assertAllCachesAreConfigured(builder, Arrays.stream(CLUSTERED_CACHE_NAMES));
            if (builder.getNamedConfigurationBuilders().entrySet().stream().anyMatch(c -> c.getValue().clustering().cacheMode().isClustered())) {
                if (jGroupsConfigurator.isLocal()) {
                    throw new RuntimeException("Unable to use clustered cache with local mode.");
                }
            }
            jGroupsConfigurator.configure(session);
            configureCacheMaxCount(builder, CachingOptions.CLUSTERED_MAX_COUNT_CACHES);
            configureSessionsCaches(builder);
            validateWorkCacheConfiguration(builder);
        }
        configureCacheMaxCount(builder, CachingOptions.LOCAL_MAX_COUNT_CACHES);
        checkForRemoteStores(builder);
        configureMetrics(builder);

        return new DefaultCacheManager(builder, isStartEagerly());
    }

    private static void configureMetrics(ConfigurationBuilderHolder holder) {
        if (Configuration.isTrue(MetricsOptions.METRICS_ENABLED)) {
            holder.getGlobalConfigurationBuilder().addModule(MicrometerMeterRegisterConfigurationBuilder.class);
            holder.getGlobalConfigurationBuilder().module(MicrometerMeterRegisterConfigurationBuilder.class).meterRegistry(Metrics.globalRegistry);
            holder.getGlobalConfigurationBuilder().cacheContainer().statistics(true);
            holder.getGlobalConfigurationBuilder().metrics().namesAsTags(true);
            if (Configuration.isTrue(CachingOptions.CACHE_METRICS_HISTOGRAMS_ENABLED)) {
                holder.getGlobalConfigurationBuilder().metrics().histograms(true);
            }
            holder.getNamedConfigurationBuilders().forEach((s, configurationBuilder) -> configurationBuilder.statistics().enabled(true));
        }
    }

    private static boolean isRemoteTLSEnabled() {
        return Configuration.isTrue(CachingOptions.CACHE_REMOTE_TLS_ENABLED);
    }

    private static boolean isRemoteAuthenticationEnabled() {
        return Configuration.getOptionalKcValue(CACHE_REMOTE_USERNAME_PROPERTY).isPresent() ||
                Configuration.getOptionalKcValue(CACHE_REMOTE_PASSWORD_PROPERTY).isPresent();
    }

    private static SSLContext createSSLContext() {
        try {
            // uses the default Java Runtime TrustStore, or the one generated by Keycloak (see org.keycloak.truststore.TruststoreBuilder)
            var sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, null, null);
            return sslContext;
        } catch (NoSuchAlgorithmException | KeyManagementException e) {
            throw new RuntimeException(e);
        }
    }

    private static boolean isStartEagerly() {
        // eagerly starts caches by default
        return Boolean.parseBoolean(System.getProperty("kc.cache-ispn-start-eagerly", Boolean.TRUE.toString()));
    }

    private static int getStartTimeout() {
        return Integer.getInteger("kc.cache-ispn-start-timeout", 120);
    }

    /**
     *
     * RemoteStores were previously used when running Keycloak in the CrossDC environment, and Keycloak code
     * contained a lot of performance optimizations to make this work smoothly.
     * These optimizations are now removed as recommended multi-site setup no longer relies on RemoteStores.
     * A lot of blueprints in the wild may turn into very ineffective setups.
     * <p />
     * For this reason, we need to be more opinionated on what configurations we allow,
     * especially for user and client sessions.
     * This method is responsible for checking the Infinispan configuration used and either change the configuration to
     * more effective when possible or refuse to start with recommendations for users to change their config.
     *
     * @param builder Cache configuration builder
     */
    private static void checkForRemoteStores(ConfigurationBuilderHolder builder) {
        for (String cacheName : USER_AND_CLIENT_SESSION_CACHES) {
            ConfigurationBuilder cacheConfigurationBuilder = builder.getNamedConfigurationBuilders().get(cacheName);

            if (cacheConfigurationBuilder != null && hasRemoteStore(cacheConfigurationBuilder)) {
                if (Profile.isFeatureEnabled(Profile.Feature.PERSISTENT_USER_SESSIONS)) {
                    logger.warnf("Feature %s is enabled and remote store detected for cache '%s'. Remote stores are no longer needed when sessions stored in the database. The configuration will be ignored.", Profile.Feature.PERSISTENT_USER_SESSIONS.getKey(), cacheName);
                    cacheConfigurationBuilder.persistence().stores().removeIf(RemoteStoreConfigurationBuilder.class::isInstance);
                } else {
                    logger.fatalf("Remote stores are not supported for embedded caches storing user and client sessions.%nFor keeping user sessions across restarts, use feature %s which is enabled by default.%nFor multi-site support, enable %s.",
                            Profile.Feature.PERSISTENT_USER_SESSIONS.getKey(), Profile.Feature.MULTI_SITE.getKey());

                    throw new RuntimeException("Remote stores for storing user and client sessions are not supported.");
                }
            }
        }
    }

    private static void configureSessionsCaches(ConfigurationBuilderHolder builder) {
        Stream.of(USER_SESSION_CACHE_NAME, CLIENT_SESSION_CACHE_NAME, OFFLINE_USER_SESSION_CACHE_NAME, OFFLINE_CLIENT_SESSION_CACHE_NAME)
                .forEach(cacheName -> {
                    var configurationBuilder = builder.getNamedConfigurationBuilders().get(cacheName);
                    if (MultiSiteUtils.isPersistentSessionsEnabled()) {
                        if (configurationBuilder.memory().maxCount() == -1) {
                            logger.infof("Persistent user sessions enabled and no memory limit found in configuration. Setting max entries for %s to 10000 entries.", cacheName);
                            configurationBuilder.memory().maxCount(10000);
                        }
                        /* The number of owners for these caches then need to be set to `1` to avoid backup owners with inconsistent data.
                         As primary owner evicts a key based on its locally evaluated maxCount setting, it wouldn't tell the backup owner about this, and then the backup owner would be left with a soon-to-be-outdated key.
                         While a `remove` is forwarded to the backup owner regardless if the key exists on the primary owner, a `computeIfPresent` is not, and it would leave a backup owner with an outdated key.
                         With the number of owners set to `1`, there will be no backup owners, so this is the setting to choose with persistent sessions enabled to ensure consistent data in the caches. */
                        configurationBuilder.clustering().hash().numOwners(1);
                    } else {
                        if (configurationBuilder.memory().maxCount() != -1) {
                            logger.warnf("Persistent user sessions disabled and memory limit found in configuration for cache %s. This might be a misconfiguration! Update your Infinispan configuration to remove this message.", cacheName);
                        }
                        if (configurationBuilder.memory().maxCount() == 10000 && (cacheName.equals(USER_SESSION_CACHE_NAME) || cacheName.equals(CLIENT_SESSION_CACHE_NAME))) {
                            logger.warnf("Persistent user sessions disabled and memory limit is set to default value 10000. Ignoring cache limits to avoid losing sessions for cache %s.", cacheName);
                            configurationBuilder.memory().maxCount(-1);
                        }
                        if (configurationBuilder.clustering().hash().attributes().attribute(HashConfiguration.NUM_OWNERS).get() == 1
                                && configurationBuilder.persistence().stores().isEmpty()) {
                            logger.warnf("Number of owners is one for cache %s, and no persistence is configured. This might be a misconfiguration as you will lose data when a single node is restarted!", cacheName);
                        }
                    }
                });
    }

    private static void configureCacheMaxCount(ConfigurationBuilderHolder holder, String[] caches) {
        for (String cache : caches) {
            var memory = holder.getNamedConfigurationBuilders().get(cache).memory();
            String propKey = CachingOptions.cacheMaxCountProperty(cache);
            Configuration.getOptionalKcValue(propKey)
                  .map(Integer::parseInt)
                  .ifPresent(memory::maxCount);
        }
    }

    private static void assertAllCachesAreConfigured(ConfigurationBuilderHolder holder, Stream<String> caches)  {
        for (var it = caches.iterator() ; it.hasNext() ; ) {
            var cache = it.next();
            var builder = holder.getNamedConfigurationBuilders().get(cache);
            if (builder != null) {
                continue;
            }
            builder = DEFAULT_CONFIGS.getOrDefault(cache, TO_NULL).get();
            if (builder == null) {
                throw new IllegalStateException("Infinispan cache '%s' not found. Make sure it is defined in your XML configuration file.".formatted(cache));
            }
            holder.getNamedConfigurationBuilders().put(cache, builder);
        }
    }

    private static void validateWorkCacheConfiguration(ConfigurationBuilderHolder builder) {
        var cacheBuilder  = builder.getNamedConfigurationBuilders().get(WORK_CACHE_NAME);
        if (cacheBuilder == null) {
            throw new RuntimeException("Unable to start Keycloak. '%s' cache is missing".formatted(WORK_CACHE_NAME));
        }
        if (builder.getGlobalConfigurationBuilder().cacheContainer().transport().getTransport() == null) {
            // non-clustered, Keycloak started in dev mode?
            return;
        }
        var cacheMode = cacheBuilder.clustering().cacheMode();
        if (!cacheMode.isReplicated()) {
            throw new RuntimeException("Unable to start Keycloak. '%s' cache must be replicated but is %s".formatted(WORK_CACHE_NAME, cacheMode.friendlyCacheModeString().toLowerCase()));
        }
    }

    public static String requiredStringProperty(String propertyName) {
        return Configuration.getOptionalKcValue(propertyName).orElseThrow(() -> new RuntimeException("Property " + propertyName + " required but not specified"));
    }

    public static int requiredIntegerProperty(Option<Integer> option) {
        return Configuration.getOptionalIntegerValue(option)
                .orElseThrow(() -> new RuntimeException("Property '%s' required but not specified".formatted(option.getKey())));
    }

    private static boolean hasRemoteStore(ConfigurationBuilder builder) {
        return builder.persistence().stores().stream().anyMatch(RemoteStoreConfigurationBuilder.class::isInstance);
    }
}

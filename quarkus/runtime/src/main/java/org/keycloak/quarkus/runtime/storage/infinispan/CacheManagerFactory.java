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
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Stream;

import io.micrometer.core.instrument.Metrics;
import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.RemoteCacheManager;
import org.infinispan.client.hotrod.RemoteCacheManagerAdmin;
import org.infinispan.client.hotrod.impl.ConfigurationProperties;
import org.infinispan.commons.dataconversion.MediaType;
import org.infinispan.commons.internal.InternalCacheNames;
import org.infinispan.commons.util.concurrent.CompletableFutures;
import org.infinispan.configuration.cache.CacheMode;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.configuration.cache.HashConfiguration;
import org.infinispan.configuration.global.ShutdownHookBehavior;
import org.infinispan.configuration.parsing.ConfigurationBuilderHolder;
import org.infinispan.configuration.parsing.ParserRegistry;
import org.infinispan.manager.DefaultCacheManager;
import org.infinispan.manager.EmbeddedCacheManager;
import org.infinispan.metrics.config.MicrometerMeterRegisterConfigurationBuilder;
import org.infinispan.persistence.remote.configuration.RemoteStoreConfigurationBuilder;
import org.infinispan.protostream.descriptors.FileDescriptor;
import org.infinispan.query.remote.client.ProtobufMetadataManagerConstants;
import org.jboss.logging.Logger;
import org.keycloak.common.Profile;
import org.keycloak.common.util.MultiSiteUtils;
import org.keycloak.config.CachingOptions;
import org.keycloak.config.MetricsOptions;
import org.keycloak.config.Option;
import org.keycloak.connections.infinispan.InfinispanConnectionProvider;
import org.keycloak.connections.infinispan.InfinispanUtil;
import org.keycloak.infinispan.util.InfinispanUtils;
import org.keycloak.marshalling.KeycloakIndexSchemaUtil;
import org.keycloak.marshalling.KeycloakModelSchema;
import org.keycloak.marshalling.Marshalling;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.sessions.infinispan.query.ClientSessionQueries;
import org.keycloak.models.sessions.infinispan.query.UserSessionQueries;
import org.keycloak.models.sessions.infinispan.remote.RemoteInfinispanAuthenticationSessionProviderFactory;
import org.keycloak.models.sessions.infinispan.remote.RemoteUserLoginFailureProviderFactory;
import org.keycloak.quarkus.runtime.configuration.Configuration;
import org.keycloak.quarkus.runtime.storage.infinispan.jgroups.JGroupsConfigurator;

import javax.net.ssl.SSLContext;

import static org.keycloak.config.CachingOptions.CACHE_REMOTE_HOST_PROPERTY;
import static org.keycloak.config.CachingOptions.CACHE_REMOTE_PASSWORD_PROPERTY;
import static org.keycloak.config.CachingOptions.CACHE_REMOTE_PORT_PROPERTY;
import static org.keycloak.config.CachingOptions.CACHE_REMOTE_USERNAME_PROPERTY;
import static org.keycloak.connections.infinispan.InfinispanConnectionProvider.AUTHENTICATION_SESSIONS_CACHE_NAME;
import static org.keycloak.connections.infinispan.InfinispanConnectionProvider.CLIENT_SESSION_CACHE_NAME;
import static org.keycloak.connections.infinispan.InfinispanConnectionProvider.CLUSTERED_CACHE_NAMES;
import static org.keycloak.connections.infinispan.InfinispanConnectionProvider.CRL_CACHE_NAME;
import static org.keycloak.connections.infinispan.InfinispanConnectionProvider.LOCAL_CACHE_NAMES;
import static org.keycloak.connections.infinispan.InfinispanConnectionProvider.LOGIN_FAILURE_CACHE_NAME;
import static org.keycloak.connections.infinispan.InfinispanConnectionProvider.OFFLINE_CLIENT_SESSION_CACHE_NAME;
import static org.keycloak.connections.infinispan.InfinispanConnectionProvider.OFFLINE_USER_SESSION_CACHE_NAME;
import static org.keycloak.connections.infinispan.InfinispanConnectionProvider.USER_SESSION_CACHE_NAME;
import static org.keycloak.connections.infinispan.InfinispanConnectionProvider.WORK_CACHE_NAME;
import static org.keycloak.connections.infinispan.InfinispanConnectionProvider.skipSessionsCacheIfRequired;
import static org.wildfly.security.sasl.util.SaslMechanismInformation.Names.SCRAM_SHA_512;

public class CacheManagerFactory {

    public static final Logger logger = Logger.getLogger(CacheManagerFactory.class);
    // Map with the default cache configuration if the cache is not present in the XML.
    private static final Map<String, Supplier<ConfigurationBuilder>> DEFAULT_CONFIGS = Map.of(
            CRL_CACHE_NAME, InfinispanUtil::getCrlCacheConfig
    );
    private static final Supplier<ConfigurationBuilder> TO_NULL = () -> null;

    private volatile CompletableFuture<EmbeddedCacheManager> cacheManagerFuture;
    private final CompletableFuture<RemoteCacheManager> remoteCacheManagerFuture;
    private final JGroupsConfigurator jGroupsConfigurator;

    public CacheManagerFactory(String config) {
        ConfigurationBuilderHolder builder = new ParserRegistry().parse(config);
        jGroupsConfigurator = JGroupsConfigurator.create(builder);

        if (jGroupsConfigurator.requiresKeycloakSession()) {
            cacheManagerFuture = null;
        } else {
            cacheManagerFuture = CompletableFuture.supplyAsync(() -> startEmbeddedCacheManager(null));
        }

        if (InfinispanUtils.isRemoteInfinispan()) {
            logger.debug("Remote Cache feature is enabled");
            this.remoteCacheManagerFuture = CompletableFuture.supplyAsync(this::startRemoteCacheManager);
        } else {
            logger.debug("Remote Cache feature is disabled");
            this.remoteCacheManagerFuture = CompletableFutures.completedNull();
        }
    }

    public EmbeddedCacheManager getOrCreateEmbeddedCacheManager(KeycloakSession keycloakSession) {
        if (cacheManagerFuture != null)
            return join(cacheManagerFuture);

        if (cacheManagerFuture == null) {
           synchronized (this) {
               if (cacheManagerFuture == null) {
                   cacheManagerFuture = CompletableFuture.completedFuture(startEmbeddedCacheManager(keycloakSession));
              }
           }
        }
        return join(cacheManagerFuture);
    }

    public RemoteCacheManager getOrCreateRemoteCacheManager() {
        return join(remoteCacheManagerFuture);
    }

    private static <T> T join(Future<T> future) {
        try {
            return future.get(getStartTimeout(), TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        } catch (ExecutionException | TimeoutException e) {
            throw new RuntimeException("Failed to start embedded or remote cache manager", e);
        }
    }

    private RemoteCacheManager startRemoteCacheManager() {
        logger.info("Starting Infinispan remote cache manager (Hot Rod Client)");
        String cacheRemoteHost = requiredStringProperty(CACHE_REMOTE_HOST_PROPERTY);
        Integer cacheRemotePort = Configuration.getOptionalKcValue(CACHE_REMOTE_PORT_PROPERTY)
                .map(Integer::parseInt)
                .orElse(ConfigurationProperties.DEFAULT_HOTROD_PORT);

        org.infinispan.client.hotrod.configuration.ConfigurationBuilder builder = new org.infinispan.client.hotrod.configuration.ConfigurationBuilder();
        builder.addServer().host(cacheRemoteHost).port(cacheRemotePort);
        builder.connectionPool().maxActive(16).exhaustedAction(org.infinispan.client.hotrod.configuration.ExhaustedAction.CREATE_NEW);

        if (isRemoteTLSEnabled()) {
            builder.security().ssl()
                    .enable()
                    .sslContext(createSSLContext())
                    .sniHostName(cacheRemoteHost);
        }

        if (isRemoteAuthenticationEnabled()) {
            builder.security().authentication()
                    .enable()
                    .username(requiredStringProperty(CACHE_REMOTE_USERNAME_PROPERTY))
                    .password(requiredStringProperty(CACHE_REMOTE_PASSWORD_PROPERTY))
                    .realm("default")
                    .saslMechanism(SCRAM_SHA_512);
        }

        Marshalling.configure(builder);

        if (shouldCreateRemoteCaches()) {
            createRemoteCaches(builder);
        }

        var remoteCacheManager = new RemoteCacheManager(builder.build());

        // update the schema before trying to access the caches
        updateProtoSchema(remoteCacheManager);

        // establish connection to all caches
        if (isStartEagerly()) {
            skipSessionsCacheIfRequired(Arrays.stream(CLUSTERED_CACHE_NAMES)).forEach(remoteCacheManager::getCache);
        }
        return remoteCacheManager;
    }

    private static void createRemoteCaches(org.infinispan.client.hotrod.configuration.ConfigurationBuilder builder) {
        // fall back for distributed caches if not defined
        logger.warn("Creating remote cache in external Infinispan server. It should not be used in production!");
        var baseConfig = defaultRemoteCacheBuilder().build();

        skipSessionsCacheIfRequired(Arrays.stream(CLUSTERED_CACHE_NAMES))
                .forEach(name -> builder.remoteCache(name).configuration(baseConfig.toStringConfiguration(name)));
    }

    private static ConfigurationBuilder defaultRemoteCacheBuilder() {
        var builder = new ConfigurationBuilder();
        builder.clustering().cacheMode(CacheMode.DIST_SYNC);
        builder.encoding().mediaType(MediaType.APPLICATION_PROTOSTREAM);
        return builder;
    }

    private void updateProtoSchema(RemoteCacheManager remoteCacheManager) {
        var key = KeycloakModelSchema.INSTANCE.getProtoFileName();
        var current = KeycloakModelSchema.INSTANCE.getProtoFile();

        RemoteCache<String, String> protostreamMetadataCache = remoteCacheManager.getCache(InternalCacheNames.PROTOBUF_METADATA_CACHE_NAME);
        var stored = protostreamMetadataCache.getWithMetadata(key);
        if (stored == null) {
            if (protostreamMetadataCache.putIfAbsent(key, current) == null) {
                logger.info("Infinispan ProtoStream schema uploaded for the first time.");
            } else {
                logger.info("Failed to update Infinispan ProtoStream schema. Assumed it was updated by other Keycloak server.");
            }
            checkForProtoSchemaErrors(protostreamMetadataCache);
            return;
        }
        if (Objects.equals(stored.getValue(), current)) {
            logger.info("Infinispan ProtoStream schema is up to date!");
            return;
        }
        if (protostreamMetadataCache.replaceWithVersion(key, current, stored.getVersion())) {
            logger.info("Infinispan ProtoStream schema successful updated.");
            reindexCaches(remoteCacheManager, stored.getValue(), current);
        } else {
            logger.info("Failed to update Infinispan ProtoStream schema. Assumed it was updated by other Keycloak server.");
        }
        checkForProtoSchemaErrors(protostreamMetadataCache);
    }

    private void checkForProtoSchemaErrors(RemoteCache<String, String> protostreamMetadataCache) {
        String errors = protostreamMetadataCache.get(ProtobufMetadataManagerConstants.ERRORS_KEY_SUFFIX);
        if (errors != null) {
            for (String errorFile : errors.split("\n")) {
                logger.errorf("%nThere was an error in proto file: %s%nError message: %s%nCurrent proto schema: %s%n",
                        errorFile,
                        protostreamMetadataCache.get(errorFile + ProtobufMetadataManagerConstants.ERRORS_KEY_SUFFIX),
                        protostreamMetadataCache.get(errorFile));
            }
        }
    }

    private static void reindexCaches(RemoteCacheManager remoteCacheManager, String oldSchema, String newSchema) {
        var oldPS = KeycloakModelSchema.parseProtoSchema(oldSchema);
        var newPS = KeycloakModelSchema.parseProtoSchema(newSchema);
        var admin = remoteCacheManager.administration();

        if (isEntityChanged(oldPS, newPS, RemoteUserLoginFailureProviderFactory.PROTO_ENTITY)) {
            updateSchemaAndReIndexCache(admin, LOGIN_FAILURE_CACHE_NAME);
        }

        if (isEntityChanged(oldPS, newPS, RemoteInfinispanAuthenticationSessionProviderFactory.PROTO_ENTITY)) {
            updateSchemaAndReIndexCache(admin, AUTHENTICATION_SESSIONS_CACHE_NAME);
        }

        if (isEntityChanged(oldPS, newPS, ClientSessionQueries.CLIENT_SESSION)) {
            updateSchemaAndReIndexCache(admin, CLIENT_SESSION_CACHE_NAME);
            updateSchemaAndReIndexCache(admin, OFFLINE_CLIENT_SESSION_CACHE_NAME);
        }

        if (isEntityChanged(oldPS, newPS, UserSessionQueries.USER_SESSION)) {
            updateSchemaAndReIndexCache(admin, USER_SESSION_CACHE_NAME);
            updateSchemaAndReIndexCache(admin, OFFLINE_USER_SESSION_CACHE_NAME);
        }
    }

    private static boolean isEntityChanged(FileDescriptor oldSchema, FileDescriptor newSchema, String entity) {
        var v1 = KeycloakModelSchema.findEntity(oldSchema, entity);
        var v2 = KeycloakModelSchema.findEntity(newSchema, entity);
        return v1.isPresent() && v2.isPresent() && KeycloakIndexSchemaUtil.isIndexSchemaChanged(v1.get(), v2.get());
    }

    private static void updateSchemaAndReIndexCache(RemoteCacheManagerAdmin admin, String cacheName) {
        admin.updateIndexSchema(cacheName);
        admin.reindexCache(cacheName);
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

    private static boolean shouldCreateRemoteCaches() {
        return Boolean.getBoolean("kc.cache-remote-create-caches");
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

    private static void checkForRemoteStores(ConfigurationBuilderHolder builder) {
        if (Profile.isFeatureEnabled(Profile.Feature.CACHE_EMBEDDED_REMOTE_STORE) && Profile.isFeatureEnabled(Profile.Feature.MULTI_SITE)) {
            logger.fatalf("Feature %s is now deprecated.%nFor multi-site (cross-dc) support, enable only %s.",
                    Profile.Feature.CACHE_EMBEDDED_REMOTE_STORE.getKey(), Profile.Feature.MULTI_SITE.getKey());
            throw new RuntimeException("The features " + Profile.Feature.CACHE_EMBEDDED_REMOTE_STORE.getKey() + " and " + Profile.Feature.MULTI_SITE.getKey() + " must not be enabled at the same time.");
        }
        if (Profile.isFeatureEnabled(Profile.Feature.CACHE_EMBEDDED_REMOTE_STORE) && Profile.isFeatureEnabled(Profile.Feature.CLUSTERLESS)) {
            logger.fatalf("Feature %s is now deprecated.%nFor multi-site (cross-dc) support, enable only %s.",
                    Profile.Feature.CACHE_EMBEDDED_REMOTE_STORE.getKey(), Profile.Feature.CLUSTERLESS.getKey());
            throw new RuntimeException("The features " + Profile.Feature.CACHE_EMBEDDED_REMOTE_STORE.getKey() + " and " + Profile.Feature.CLUSTERLESS.getKey() + " must not be enabled at the same time.");
        }
        if (!Profile.isFeatureEnabled(Profile.Feature.CACHE_EMBEDDED_REMOTE_STORE)) {
            if (builder.getNamedConfigurationBuilders().values().stream().anyMatch(CacheManagerFactory::hasRemoteStore)) {
                logger.fatalf("Remote stores are not supported for embedded caches as feature %s is not enabled. This feature is disabled by default as it is now deprecated.%nFor keeping user sessions across restarts, use feature %s which is enabled by default.%nFor multi-site (cross-dc) support, enable %s.",
                        Profile.Feature.CACHE_EMBEDDED_REMOTE_STORE.getKey(), Profile.Feature.PERSISTENT_USER_SESSIONS.getKey(), Profile.Feature.MULTI_SITE.getKey());
                throw new RuntimeException("Remote store is not supported as feature " + Profile.Feature.CACHE_EMBEDDED_REMOTE_STORE.getKey() + " is not enabled.");
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

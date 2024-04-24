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

package org.keycloak.quarkus.runtime.storage.legacy.infinispan;

import io.micrometer.core.instrument.Metrics;
import org.infinispan.client.hotrod.DefaultTemplate;
import org.infinispan.client.hotrod.RemoteCacheManager;
import org.infinispan.client.hotrod.impl.ConfigurationProperties;
import org.infinispan.commons.api.Lifecycle;
import org.infinispan.commons.util.concurrent.CompletableFutures;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.configuration.cache.PersistenceConfigurationBuilder;
import org.infinispan.configuration.global.GlobalConfiguration;
import org.infinispan.configuration.parsing.ConfigurationBuilderHolder;
import org.infinispan.configuration.parsing.ParserRegistry;
import org.infinispan.jboss.marshalling.core.JBossUserMarshaller;
import org.infinispan.manager.DefaultCacheManager;
import org.infinispan.metrics.config.MicrometerMeterRegisterConfigurationBuilder;
import org.infinispan.persistence.remote.configuration.ExhaustedAction;
import org.infinispan.persistence.remote.configuration.RemoteStoreConfigurationBuilder;
import org.infinispan.remoting.transport.jgroups.JGroupsTransport;
import org.jboss.logging.Logger;
import org.jgroups.protocols.TCP_NIO2;
import org.jgroups.protocols.UDP;
import org.jgroups.util.TLS;
import org.jgroups.util.TLSClientAuth;
import org.keycloak.common.Profile;
import org.keycloak.config.CachingOptions;
import org.keycloak.config.MetricsOptions;
import org.keycloak.infinispan.util.InfinispanUtils;
import org.keycloak.quarkus.runtime.configuration.Configuration;

import javax.net.ssl.SSLContext;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.keycloak.config.CachingOptions.CACHE_EMBEDDED_MTLS_KEYSTORE_FILE_PROPERTY;
import static org.keycloak.config.CachingOptions.CACHE_EMBEDDED_MTLS_KEYSTORE_PASSWORD_PROPERTY;
import static org.keycloak.config.CachingOptions.CACHE_EMBEDDED_MTLS_TRUSTSTORE_FILE_PROPERTY;
import static org.keycloak.config.CachingOptions.CACHE_EMBEDDED_MTLS_TRUSTSTORE_PASSWORD_PROPERTY;
import static org.keycloak.config.CachingOptions.CACHE_REMOTE_HOST_PROPERTY;
import static org.keycloak.config.CachingOptions.CACHE_REMOTE_PASSWORD_PROPERTY;
import static org.keycloak.config.CachingOptions.CACHE_REMOTE_PORT_PROPERTY;
import static org.keycloak.config.CachingOptions.CACHE_REMOTE_USERNAME_PROPERTY;
import static org.keycloak.connections.infinispan.InfinispanConnectionProvider.ACTION_TOKEN_CACHE;
import static org.keycloak.connections.infinispan.InfinispanConnectionProvider.AUTHENTICATION_SESSIONS_CACHE_NAME;
import static org.keycloak.connections.infinispan.InfinispanConnectionProvider.CLIENT_SESSION_CACHE_NAME;
import static org.keycloak.connections.infinispan.InfinispanConnectionProvider.CLUSTERED_CACHE_NAMES;
import static org.keycloak.connections.infinispan.InfinispanConnectionProvider.LOGIN_FAILURE_CACHE_NAME;
import static org.keycloak.connections.infinispan.InfinispanConnectionProvider.OFFLINE_CLIENT_SESSION_CACHE_NAME;
import static org.keycloak.connections.infinispan.InfinispanConnectionProvider.OFFLINE_USER_SESSION_CACHE_NAME;
import static org.keycloak.connections.infinispan.InfinispanConnectionProvider.USER_SESSION_CACHE_NAME;
import static org.keycloak.connections.infinispan.InfinispanConnectionProvider.WORK_CACHE_NAME;
import static org.wildfly.security.sasl.util.SaslMechanismInformation.Names.SCRAM_SHA_512;

public class CacheManagerFactory {

    private static final Logger logger = Logger.getLogger(CacheManagerFactory.class);

    private final String config;
    private final CompletableFuture<DefaultCacheManager> cacheManagerFuture;
    private final CompletableFuture<RemoteCacheManager> remoteCacheManagerFuture;

    public CacheManagerFactory(String config) {
        this.config = config;
        this.cacheManagerFuture = CompletableFuture.supplyAsync(this::startEmbeddedCacheManager);
        if (InfinispanUtils.isRemoteInfinispan()) {
            logger.debug("Remote Cache feature is enabled");
            this.remoteCacheManagerFuture = CompletableFuture.supplyAsync(this::startRemoteCacheManager);
        } else {
            logger.debug("Remote Cache feature is disabled");
            this.remoteCacheManagerFuture = CompletableFutures.completedNull();
        }
    }

    public DefaultCacheManager getOrCreateEmbeddedCacheManager() {
        return join(cacheManagerFuture);
    }

    public RemoteCacheManager getOrCreateRemoteCacheManager() {
        return join(remoteCacheManagerFuture);
    }

    public void shutdown() {
        logger.debug("Shutdown embedded and remote cache managers");
        cacheManagerFuture.thenAccept(CacheManagerFactory::close);
        remoteCacheManagerFuture.thenAccept(CacheManagerFactory::close);
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

    private static void close(Lifecycle lifecycle) {
        if (lifecycle != null) {
            lifecycle.stop();
        }
    }

    private RemoteCacheManager startRemoteCacheManager() {
        logger.info("Starting Infinispan remote cache manager (Hot Rod Client)");
        String cacheRemoteHost = requiredStringProperty(CACHE_REMOTE_HOST_PROPERTY);
        Integer cacheRemotePort = Configuration.getOptionalKcValue(CACHE_REMOTE_PORT_PROPERTY)
                .map(Integer::parseInt)
                .orElse(ConfigurationProperties.DEFAULT_HOTROD_PORT);
        String cacheRemoteUsername = requiredStringProperty(CACHE_REMOTE_USERNAME_PROPERTY);
        String cacheRemotePassword = requiredStringProperty(CACHE_REMOTE_PASSWORD_PROPERTY);

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
                    .username(cacheRemoteUsername)
                    .password(cacheRemotePassword)
                    .realm("default")
                    .saslMechanism(SCRAM_SHA_512);
        }

        // TODO replace with protostream
        builder.marshaller(new JBossUserMarshaller());

        if (createRemoteCaches()) {
            // fall back for distributed caches if not defined
            logger.warn("Creating remote cache in external Infinispan server. It should not be used in production!");
            for (String name : CLUSTERED_CACHE_NAMES) {
                builder.remoteCache(name).templateName(DefaultTemplate.DIST_SYNC);
            }
        }

        RemoteCacheManager remoteCacheManager = new RemoteCacheManager(builder.build());

        // establish connection to all caches
        if (isStartEagerly()) {
            Arrays.stream(CLUSTERED_CACHE_NAMES).forEach(remoteCacheManager::getCache);
        }
        return remoteCacheManager;
    }

    private DefaultCacheManager startEmbeddedCacheManager() {
        logger.info("Starting Infinispan embedded cache manager");
        ConfigurationBuilderHolder builder = new ParserRegistry().parse(config);

        if (builder.getNamedConfigurationBuilders().entrySet().stream().anyMatch(c -> c.getValue().clustering().cacheMode().isClustered())) {
            configureTransportStack(builder);
            configureRemoteStores(builder);
        }

        Arrays.stream(CLUSTERED_CACHE_NAMES).forEach(cacheName -> {
            if (!Profile.isFeatureEnabled(Profile.Feature.PERSISTENT_USER_SESSIONS_NO_CACHE) && Profile.isFeatureEnabled(Profile.Feature.PERSISTENT_USER_SESSIONS) &&
                (cacheName.equals(USER_SESSION_CACHE_NAME) || cacheName.equals(CLIENT_SESSION_CACHE_NAME) || cacheName.equals(OFFLINE_USER_SESSION_CACHE_NAME) || cacheName.equals(OFFLINE_CLIENT_SESSION_CACHE_NAME))) {
                ConfigurationBuilder configurationBuilder = builder.getNamedConfigurationBuilders().get(cacheName);
                if (configurationBuilder.memory().maxSize() == null && configurationBuilder.memory().maxCount() == -1) {
                    logger.infof("Persistent user sessions enabled and no memory limit found in configuration. Setting max entries for %s to 10000 entries", cacheName);
                    configurationBuilder.memory().maxCount(10000);
                }
            }
        });

        if (Configuration.isTrue(MetricsOptions.METRICS_ENABLED)) {
            builder.getGlobalConfigurationBuilder().addModule(MicrometerMeterRegisterConfigurationBuilder.class);
            builder.getGlobalConfigurationBuilder().module(MicrometerMeterRegisterConfigurationBuilder.class).meterRegistry(Metrics.globalRegistry);
            builder.getGlobalConfigurationBuilder().cacheContainer().statistics(true);
            builder.getGlobalConfigurationBuilder().metrics().namesAsTags(true);
            if (Configuration.isTrue(CachingOptions.CACHE_METRICS_HISTOGRAMS_ENABLED)) {
                builder.getGlobalConfigurationBuilder().metrics().histograms(true);
            }
            builder.getNamedConfigurationBuilders().forEach((s, configurationBuilder) -> configurationBuilder.statistics().enabled(true));
        }

        // For Infinispan 10, we go with the JBoss marshalling.
        // TODO: This should be replaced later with the marshalling recommended by infinispan. Probably protostream.
        // See https://infinispan.org/docs/stable/titles/developing/developing.html#marshalling for the details
        builder.getGlobalConfigurationBuilder().serialization().marshaller(new JBossUserMarshaller());

        if (InfinispanUtils.isRemoteInfinispan()) {
            var builders = builder.getNamedConfigurationBuilders();
            // remove all distributed caches
            logger.debug("Removing all distributed caches.");
            // TODO [pruivo] remove all distributed caches after all of them are converted
            //DISTRIBUTED_REPLICATED_CACHE_NAMES.forEach(builders::remove);
            builders.remove(WORK_CACHE_NAME);
            builders.remove(AUTHENTICATION_SESSIONS_CACHE_NAME);
            builders.remove(ACTION_TOKEN_CACHE);
            builders.remove(LOGIN_FAILURE_CACHE_NAME);
        }

        return new DefaultCacheManager(builder, isStartEagerly());
    }

    private static boolean isRemoteTLSEnabled() {
        return Configuration.isTrue(CachingOptions.CACHE_REMOTE_TLS_ENABLED);
    }

    private static boolean isRemoteAuthenticationEnabled() {
        return Configuration.getOptionalKcValue(CACHE_REMOTE_USERNAME_PROPERTY).isPresent() ||
                Configuration.getOptionalKcValue(CACHE_REMOTE_PASSWORD_PROPERTY).isPresent();
    }

    private static boolean createRemoteCaches() {
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

    private void configureTransportStack(ConfigurationBuilderHolder builder) {
        String transportStack = Configuration.getRawValue("kc.cache-stack");

        var transportConfig = builder.getGlobalConfigurationBuilder().transport();
        if (transportStack != null && !transportStack.isBlank()) {
            transportConfig.defaultTransport().stack(transportStack);
        }

        if (Configuration.isTrue(CachingOptions.CACHE_EMBEDDED_MTLS_ENABLED_PROPERTY)) {
            validateTlsAvailable(transportConfig.build());
            var tls = new TLS()
                    .enabled(true)
                    .setKeystorePath(requiredStringProperty(CACHE_EMBEDDED_MTLS_KEYSTORE_FILE_PROPERTY))
                    .setKeystorePassword(requiredStringProperty(CACHE_EMBEDDED_MTLS_KEYSTORE_PASSWORD_PROPERTY))
                    .setKeystoreType("pkcs12")
                    .setTruststorePath(requiredStringProperty(CACHE_EMBEDDED_MTLS_TRUSTSTORE_FILE_PROPERTY))
                    .setTruststorePassword(requiredStringProperty(CACHE_EMBEDDED_MTLS_TRUSTSTORE_PASSWORD_PROPERTY))
                    .setTruststoreType("pkcs12")
                    .setClientAuth(TLSClientAuth.NEED)
                    .setProtocols(new String[]{"TLSv1.3"});
            transportConfig.addProperty(JGroupsTransport.SOCKET_FACTORY, tls.createSocketFactory());
            Logger.getLogger(CacheManagerFactory.class).info("MTLS enabled for communications for embedded caches");
        }

        //TODO [pruivo] disable JGroups after all distributed caches are converted
//        if (isCrossSiteEnabled() && isRemoteCacheEnabled()) {
//            logger.debug("Disabling JGroups between Keycloak nodes");
//            builder.getGlobalConfigurationBuilder().nonClusteredDefault();
//        }
    }

    private void validateTlsAvailable(GlobalConfiguration config) {
        var stackName = config.transport().stack();
        if (stackName == null) {
            // unable to validate
            return;
        }
        for (var protocol : config.transport().jgroups().configurator(stackName).getProtocolStack()) {
            var name = protocol.getProtocolName();
            if (name.equals(UDP.class.getSimpleName()) ||
                    name.equals(UDP.class.getName()) ||
                    name.equals(TCP_NIO2.class.getSimpleName()) ||
                    name.equals(TCP_NIO2.class.getName())) {
                throw new RuntimeException("Cache TLS is not available with protocol " + name);
            }
        }

    }

    private void configureRemoteStores(ConfigurationBuilderHolder builder) {
        //if one of remote store command line parameters is defined, some other are required, otherwise assume it'd configured via xml only
        if (Configuration.getOptionalKcValue(CACHE_REMOTE_HOST_PROPERTY).isPresent()) {

            String cacheRemoteHost = requiredStringProperty(CACHE_REMOTE_HOST_PROPERTY);
            Integer cacheRemotePort = Configuration.getOptionalKcValue(CACHE_REMOTE_PORT_PROPERTY)
                    .map(Integer::parseInt)
                    .orElse(ConfigurationProperties.DEFAULT_HOTROD_PORT);

            SSLContext sslContext = createSSLContext();

            Arrays.stream(CLUSTERED_CACHE_NAMES).forEach(cacheName -> {
                if (Profile.isFeatureEnabled(Profile.Feature.PERSISTENT_USER_SESSIONS_NO_CACHE) &&
                    (cacheName.equals(USER_SESSION_CACHE_NAME) || cacheName.equals(CLIENT_SESSION_CACHE_NAME) || cacheName.equals(OFFLINE_USER_SESSION_CACHE_NAME) || cacheName.equals(OFFLINE_CLIENT_SESSION_CACHE_NAME))) {
                    return;
                }
                PersistenceConfigurationBuilder persistenceCB = builder.getNamedConfigurationBuilders().get(cacheName).persistence();

                //if specified via command line -> cannot be defined in the xml file
                if (!persistenceCB.stores().isEmpty()) {
                    throw new RuntimeException(String.format("Remote store for cache '%s' is already configured via CLI parameters. It should not be present in the XML file.", cacheName));
                }

                var storeBuilder = persistenceCB.addStore(RemoteStoreConfigurationBuilder.class);
                storeBuilder
                        .rawValues(true)
                        .shared(true)
                        .segmented(false)
                        .remoteCacheName(cacheName)
                        .connectionPool()
                            .maxActive(16)
                            .exhaustedAction(ExhaustedAction.CREATE_NEW)
                        .addServer()
                        .host(cacheRemoteHost)
                        .port(cacheRemotePort);

                if (isRemoteTLSEnabled()) {
                    storeBuilder.remoteSecurity()
                            .ssl()
                            .enable()
                            .sslContext(sslContext)
                            .sniHostName(cacheRemoteHost);
                }

                if (isRemoteAuthenticationEnabled()) {
                    storeBuilder.remoteSecurity()
                            .authentication()
                            .enable()
                            .username(requiredStringProperty(CACHE_REMOTE_USERNAME_PROPERTY))
                            .password(requiredStringProperty(CACHE_REMOTE_PASSWORD_PROPERTY))
                            .realm("default")
                            .saslMechanism(SCRAM_SHA_512);
                }
            });
        }
    }

    private static String requiredStringProperty(String propertyName) {
        return Configuration.getOptionalKcValue(propertyName).orElseThrow(() -> new RuntimeException("Property " + propertyName + " required but not specified"));
    }
}

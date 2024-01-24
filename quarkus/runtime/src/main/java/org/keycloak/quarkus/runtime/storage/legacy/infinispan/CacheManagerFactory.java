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

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import io.micrometer.core.instrument.Metrics;
import org.infinispan.configuration.global.GlobalConfiguration;
import org.infinispan.configuration.parsing.ConfigurationBuilderHolder;
import org.infinispan.configuration.parsing.ParserRegistry;
import org.infinispan.jboss.marshalling.core.JBossUserMarshaller;
import org.infinispan.manager.DefaultCacheManager;
import org.infinispan.metrics.config.MicrometerMeterRegisterConfigurationBuilder;
import org.infinispan.remoting.transport.jgroups.JGroupsTransport;
import org.jboss.logging.Logger;
import org.jgroups.protocols.TCP_NIO2;
import org.jgroups.protocols.UDP;
import org.jgroups.util.TLS;
import org.jgroups.util.TLSClientAuth;
import org.keycloak.quarkus.runtime.configuration.Configuration;

import static org.keycloak.config.CachingOptions.CACHE_EMBEDDED_MTLS_ENABLED_PROPERTY;
import static org.keycloak.config.CachingOptions.CACHE_EMBEDDED_MTLS_KEYSTORE_FILE_PROPERTY;
import static org.keycloak.config.CachingOptions.CACHE_EMBEDDED_MTLS_KEYSTORE_PASSWORD_PROPERTY;
import static org.keycloak.config.CachingOptions.CACHE_EMBEDDED_MTLS_TRUSTSTORE_FILE_PROPERTY;
import static org.keycloak.config.CachingOptions.CACHE_EMBEDDED_MTLS_TRUSTSTORE_PASSWORD_PROPERTY;

public class CacheManagerFactory {

    private static final Logger logger = Logger.getLogger(CacheManagerFactory.class);

    private String config;
    private final boolean metricsEnabled;
    private DefaultCacheManager cacheManager;
    private Future<DefaultCacheManager> cacheManagerFuture;
    private ExecutorService executor;
    private boolean initialized;

    public CacheManagerFactory(String config, boolean metricsEnabled) {
        this.config = config;
        this.metricsEnabled = metricsEnabled;
        this.executor = createThreadPool();
        this.cacheManagerFuture = executor.submit(this::startCacheManager);
    }

    public DefaultCacheManager getOrCreate() {
        if (cacheManager == null) {
            if (initialized) {
                return null;
            }

            try {
                // for now, we don't have any explicit property for setting the cache start timeout
                return cacheManager = cacheManagerFuture.get(getStartTimeout(), TimeUnit.SECONDS);
            } catch (Exception e) {
                throw new RuntimeException("Failed to start caches", e);
            } finally {
                shutdownThreadPool();
            }
        }

        return cacheManager;
    }

    private ExecutorService createThreadPool() {
        return Executors.newSingleThreadExecutor(r -> new Thread(r, "keycloak-cache-init"));
    }

    private DefaultCacheManager startCacheManager() {
        ConfigurationBuilderHolder builder = new ParserRegistry().parse(config);

        if (builder.getNamedConfigurationBuilders().get("sessions").clustering().cacheMode().isClustered()) {
            configureTransportStack(builder);
        }

        if (metricsEnabled) {
            builder.getGlobalConfigurationBuilder().addModule(MicrometerMeterRegisterConfigurationBuilder.class);
            builder.getGlobalConfigurationBuilder().module(MicrometerMeterRegisterConfigurationBuilder.class).meterRegistry(Metrics.globalRegistry);
        }

        // For Infinispan 10, we go with the JBoss marshalling.
        // TODO: This should be replaced later with the marshalling recommended by infinispan. Probably protostream.
        // See https://infinispan.org/docs/stable/titles/developing/developing.html#marshalling for the details
        builder.getGlobalConfigurationBuilder().serialization().marshaller(new JBossUserMarshaller());

        return new DefaultCacheManager(builder, isStartEagerly());
    }

    private boolean isStartEagerly() {
        // eagerly starts caches by default
        return Boolean.parseBoolean(System.getProperty("kc.cache-ispn-start-eagerly", Boolean.TRUE.toString()));
    }

    private Integer getStartTimeout() {
        return Integer.getInteger("kc.cache-ispn-start-timeout", 120);
    }

    private void shutdownThreadPool() {
        if (executor != null) {
            executor.shutdown();
            try {
                if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
                    executor.shutdownNow();
                    if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
                        Logger.getLogger(CacheManagerFactory.class).warn("Cache init thread pool not terminated");
                    }
                }
            } catch (Exception cause) {
                executor.shutdownNow();
            } finally {
                executor = null;
                cacheManagerFuture = null;
                config = null;
                initialized = true;
            }
        }
    }

    private void configureTransportStack(ConfigurationBuilderHolder builder) {
        String transportStack = Configuration.getRawValue("kc.cache-stack");

        var transportConfig = builder.getGlobalConfigurationBuilder().transport();
        if (transportStack != null && !transportStack.isBlank()) {
            transportConfig.defaultTransport().stack(transportStack);
        }

        if (booleanProperty(CACHE_EMBEDDED_MTLS_ENABLED_PROPERTY)) {
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

    private static boolean booleanProperty(String propertyName) {
        return Configuration.getOptionalKcValue(propertyName).map(Boolean::parseBoolean).orElse(Boolean.FALSE);
    }

    private static String requiredStringProperty(String propertyName) {
        return Configuration.getOptionalKcValue(propertyName).orElseThrow(() -> new RuntimeException("Property " + propertyName + " required but not specified"));
    }
}

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

package org.keycloak.quarkus.runtime;

import static org.keycloak.quarkus.runtime.configuration.Configuration.getBuildTimeProperty;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.infinispan.configuration.parsing.ConfigurationBuilderHolder;
import org.infinispan.configuration.parsing.ParserRegistry;
import org.infinispan.jboss.marshalling.core.JBossUserMarshaller;
import org.infinispan.manager.DefaultCacheManager;
import io.quarkus.smallrye.metrics.runtime.SmallRyeMetricsHandler;
import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;
import org.keycloak.common.Profile;
import org.keycloak.quarkus.runtime.configuration.Configuration;
import org.keycloak.quarkus.runtime.integration.QuarkusKeycloakSessionFactory;
import org.keycloak.quarkus.runtime.storage.database.liquibase.FastServiceLocator;
import org.keycloak.quarkus.runtime.storage.database.liquibase.KeycloakLogger;
import org.keycloak.provider.Provider;
import org.keycloak.provider.ProviderFactory;
import org.keycloak.provider.Spi;
import org.keycloak.quarkus.runtime.storage.infinispan.CacheManagerFactory;

import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.ShutdownContext;
import io.quarkus.runtime.annotations.Recorder;
import liquibase.logging.LogFactory;
import liquibase.servicelocator.ServiceLocator;

@Recorder
public class KeycloakRecorder {

    public void configureLiquibase(Map<String, List<String>> services) {
        LogFactory.setInstance(new LogFactory() {
            final KeycloakLogger logger = new KeycloakLogger();

            @Override
            public liquibase.logging.Logger getLog(String name) {
                return logger;
            }

            @Override
            public liquibase.logging.Logger getLog() {
                return logger;
            }
        });
        
        // we set this property to avoid Liquibase to lookup resources from the classpath and access JAR files
        // we already index the packages we want so Liquibase will still be able to load these services
        // for uber-jar, this is not a problem because everything is inside the JAR, but once we move to fast-jar we'll have performance penalties
        // it seems that v4 of liquibase provides a more smart way of initialization the ServiceLocator that may allow us to remove this
        System.setProperty("liquibase.scan.packages", "org.liquibase.core");
        
        ServiceLocator.setInstance(new FastServiceLocator(services));
    }

    public void configSessionFactory(
            Map<Spi, Map<Class<? extends Provider>, Map<String, Class<? extends ProviderFactory>>>> factories,
            Map<Class<? extends Provider>, String> defaultProviders,
            Map<String, ProviderFactory> preConfiguredProviders,
            Boolean reaugmented) {
        Profile.setInstance(createProfile());
        QuarkusKeycloakSessionFactory.setInstance(new QuarkusKeycloakSessionFactory(factories, defaultProviders, preConfiguredProviders, reaugmented));
    }

    public static Profile createProfile() {
        return new Profile(new Profile.PropertyResolver() {
            @Override 
            public String resolve(String feature) {
                if (feature.startsWith("keycloak.profile.feature")) {
                    feature = feature.replaceAll("keycloak\\.profile\\.feature", "kc\\.features");    
                } else {
                    feature = "kc.features";
                }

                Optional<String> value = getBuildTimeProperty(feature);

                if (value.isEmpty()) {
                    value = getBuildTimeProperty(feature.replaceAll("\\.features\\.", "\\.features-"));
                }
                
                if (value.isPresent()) {
                    return value.get();
                }

                return Configuration.getRawValue(feature);
            }
        });
    }

    public RuntimeValue<CacheManagerFactory> createCacheInitializer(String config, ShutdownContext shutdownContext) {
        try {
            ConfigurationBuilderHolder builder = new ParserRegistry().parse(config);

            if (builder.getNamedConfigurationBuilders().get("sessions").clustering().cacheMode().isClustered()) {
                configureTransportStack(builder);
            }

            // For Infinispan 10, we go with the JBoss marshalling.
            // TODO: This should be replaced later with the marshalling recommended by infinispan. Probably protostream.
            // See https://infinispan.org/docs/stable/titles/developing/developing.html#marshalling for the details
            builder.getGlobalConfigurationBuilder().serialization().marshaller(new JBossUserMarshaller());
            CacheManagerFactory cacheManagerFactory = new CacheManagerFactory(builder);

            shutdownContext.addShutdownTask(new Runnable() {
                @Override
                public void run() {
                    DefaultCacheManager cacheManager = cacheManagerFactory.getOrCreate();

                    if (cacheManager != null) {
                        cacheManager.stop();
                    }
                }
            });

            return new RuntimeValue<>(cacheManagerFactory);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void configureTransportStack(ConfigurationBuilderHolder builder) {
        String transportStack = Configuration.getRawValue("kc.cache-stack");

        if (transportStack != null) {
            builder.getGlobalConfigurationBuilder().transport().defaultTransport()
                    .addProperty("configurationFile", "default-configs/default-jgroups-" + transportStack + ".xml");
        }
    }

    public void registerShutdownHook(ShutdownContext shutdownContext) {
        shutdownContext.addShutdownTask(new Runnable() {
            @Override
            public void run() {
                QuarkusKeycloakSessionFactory.getInstance().close();
            }
        });
    }

    public Handler<RoutingContext> createMetricsHandler(String path) {
        SmallRyeMetricsHandler metricsHandler = new SmallRyeMetricsHandler();
        metricsHandler.setMetricsPath(path);
        return metricsHandler;
    }
}

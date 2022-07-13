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

import java.lang.annotation.Annotation;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

import io.agroal.api.AgroalDataSource;
import io.quarkus.agroal.DataSource;
import io.quarkus.arc.Arc;
import io.quarkus.arc.InstanceHandle;
import io.quarkus.hibernate.orm.runtime.integration.HibernateOrmIntegrationRuntimeInitListener;
import liquibase.Scope;

import org.hibernate.cfg.AvailableSettings;
import org.infinispan.manager.DefaultCacheManager;
import io.quarkus.smallrye.metrics.runtime.SmallRyeMetricsHandler;
import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;

import org.keycloak.Config;
import org.keycloak.common.Profile;
import org.keycloak.quarkus.runtime.configuration.Configuration;
import org.keycloak.quarkus.runtime.configuration.MicroProfileConfigProvider;
import org.keycloak.quarkus.runtime.integration.QuarkusKeycloakSessionFactory;
import org.keycloak.quarkus.runtime.storage.database.liquibase.FastServiceLocator;
import org.keycloak.provider.Provider;
import org.keycloak.provider.ProviderFactory;
import org.keycloak.provider.Spi;
import org.keycloak.quarkus.runtime.storage.legacy.infinispan.CacheManagerFactory;

import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.ShutdownContext;
import io.quarkus.runtime.annotations.Recorder;
import liquibase.servicelocator.ServiceLocator;

@Recorder
public class KeycloakRecorder {

    public void configureLiquibase(Map<String, List<String>> services) {
        ServiceLocator locator = Scope.getCurrentScope().getServiceLocator();
        if (locator instanceof FastServiceLocator)
            ((FastServiceLocator) locator).initServices(services);
    }

    public void configSessionFactory(
            Map<Spi, Map<Class<? extends Provider>, Map<String, Class<? extends ProviderFactory>>>> factories,
            Map<Class<? extends Provider>, String> defaultProviders,
            Map<String, ProviderFactory> preConfiguredProviders,
            Boolean reaugmented) {
        Config.init(new MicroProfileConfigProvider());
        Profile.setInstance(new QuarkusProfile());
        QuarkusKeycloakSessionFactory.setInstance(new QuarkusKeycloakSessionFactory(factories, defaultProviders, preConfiguredProviders, reaugmented));
    }

    public RuntimeValue<CacheManagerFactory> createCacheInitializer(String config, ShutdownContext shutdownContext) {
        try {
            CacheManagerFactory cacheManagerFactory = new CacheManagerFactory(config);

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

    public HibernateOrmIntegrationRuntimeInitListener createUserDefinedUnitListener(String name) {
        return new HibernateOrmIntegrationRuntimeInitListener() {
            @Override
            public void contributeRuntimeProperties(BiConsumer<String, Object> propertyCollector) {
                InstanceHandle<AgroalDataSource> instance = Arc.container().instance(
                        AgroalDataSource.class, new DataSource() {
                            @Override public Class<? extends Annotation> annotationType() {
                                return DataSource.class;
                            }

                            @Override public String value() {
                                return name;
                            }
                        });
                propertyCollector.accept(AvailableSettings.DATASOURCE, instance.get());
            }
        };
    }

    public HibernateOrmIntegrationRuntimeInitListener createDefaultUnitListener() {
        return new HibernateOrmIntegrationRuntimeInitListener() {
            @Override
            public void contributeRuntimeProperties(BiConsumer<String, Object> propertyCollector) {
                propertyCollector.accept(AvailableSettings.DEFAULT_SCHEMA, Configuration.getRawValue("kc.db-schema"));
            }
        };
    }
}

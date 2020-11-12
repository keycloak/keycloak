/*
 * Copyright 2020 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.quarkus.deployment;

import static org.keycloak.configuration.Configuration.getPropertyNames;
import static org.keycloak.configuration.Configuration.getRawValue;

import javax.persistence.spi.PersistenceUnitTransactionType;
import java.io.File;
import java.io.FileOutputStream;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Properties;
import java.util.ServiceLoader;

import io.quarkus.deployment.IsDevelopment;
import io.quarkus.deployment.builditem.HotDeploymentWatchedFileBuildItem;
import io.quarkus.deployment.builditem.IndexDependencyBuildItem;
import io.quarkus.hibernate.orm.deployment.HibernateOrmConfig;
import io.quarkus.smallrye.health.runtime.SmallRyeHealthHandler;
import io.quarkus.vertx.http.deployment.RouteBuildItem;
import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.jpa.boot.spi.PersistenceUnitDescriptor;
import org.jboss.logging.Logger;
import org.jboss.resteasy.spi.ResteasyDeployment;
import org.keycloak.Config;
import org.keycloak.common.Profile;
import org.keycloak.config.ConfigProviderFactory;
import org.keycloak.configuration.Configuration;
import org.keycloak.configuration.KeycloakConfigSourceProvider;
import org.keycloak.configuration.MicroProfileConfigProvider;
import org.keycloak.connections.jpa.DefaultJpaConnectionProviderFactory;
import org.keycloak.connections.jpa.updater.liquibase.LiquibaseJpaUpdaterProviderFactory;
import org.keycloak.connections.jpa.updater.liquibase.conn.DefaultLiquibaseConnectionProvider;
import org.keycloak.provider.EnvironmentDependentProviderFactory;
import org.keycloak.provider.KeycloakDeploymentInfo;
import org.keycloak.provider.Provider;
import org.keycloak.provider.ProviderFactory;
import org.keycloak.provider.ProviderManager;
import org.keycloak.provider.Spi;
import org.keycloak.provider.quarkus.QuarkusRequestFilter;
import org.keycloak.quarkus.KeycloakRecorder;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.hibernate.orm.deployment.PersistenceUnitDescriptorBuildItem;
import io.quarkus.vertx.http.deployment.FilterBuildItem;
import org.keycloak.services.NotFoundHandler;
import org.keycloak.services.ServicesLogger;
import org.keycloak.services.health.KeycloakMetricsHandler;
import org.keycloak.services.resources.KeycloakApplication;
import org.keycloak.transaction.JBossJtaTransactionManagerLookup;
import org.keycloak.util.Environment;

class KeycloakProcessor {

    private static final Logger logger = Logger.getLogger(KeycloakProcessor.class);

    private static final String DEFAULT_HEALTH_ENDPOINT = "/health";

    @BuildStep
    FeatureBuildItem getFeature() {
        return new FeatureBuildItem("keycloak");
    }

    /**
     * <p>Configures the persistence unit for Quarkus.
     *
     * <p>The main reason we have this build step is because we re-use the same persistence unit from {@code keycloak-model-jpa}
     * module, the same used by the Wildfly distribution. The {@code hibernate-orm} extension expects that the dialect is statically
     * set to the persistence unit if there is any from the classpath and we use this method to obtain the dialect from the configuration
     * file so that we can build the application with whatever dialect we want. In addition to the dialect, we should also be 
     * allowed to set any additional defaults that we think that makes sense.
     *
     * @param recorder
     * @param config
     * @param descriptors
     */
    @Record(ExecutionTime.STATIC_INIT)
    @BuildStep
    void configureHibernate(KeycloakRecorder recorder, HibernateOrmConfig config, List<PersistenceUnitDescriptorBuildItem> descriptors) {
        PersistenceUnitDescriptor unit = descriptors.get(0).asOutputPersistenceUnitDefinition().getActualHibernateDescriptor();

        unit.getProperties().setProperty(AvailableSettings.DIALECT, config.defaultPersistenceUnit.dialect.dialect.orElse(null));
        unit.getProperties().setProperty(AvailableSettings.JPA_TRANSACTION_TYPE, PersistenceUnitTransactionType.JTA.name());
        unit.getProperties().setProperty(AvailableSettings.QUERY_STARTUP_CHECKING, Boolean.FALSE.toString());
    }

    /**
     * <p>Load the built-in provider factories during build time so we don't spend time looking up them at runtime. By loading
     * providers at this stage we are also able to perform a more dynamic configuration based on the default providers.
     *
     * <p>User-defined providers are going to be loaded at startup</p>
     *
     * @param recorder
     */
    @Record(ExecutionTime.RUNTIME_INIT)
    @BuildStep
    void configureProviders(KeycloakRecorder recorder) {
        Profile.setInstance(recorder.createProfile());
        Map<Spi, Map<Class<? extends Provider>, Map<String, Class<? extends ProviderFactory>>>> factories = new HashMap<>();
        Map<Class<? extends Provider>, String> defaultProviders = new HashMap<>();

        for (Map.Entry<Spi, Map<Class<? extends Provider>, Map<String, ProviderFactory>>> entry : loadFactories()
                .entrySet()) {
            checkProviders(entry.getKey(), entry.getValue(), defaultProviders);

            for (Map.Entry<Class<? extends Provider>, Map<String, ProviderFactory>> value : entry.getValue().entrySet()) {
                for (ProviderFactory factory : value.getValue().values()) {
                    factories.computeIfAbsent(entry.getKey(),
                            key -> new HashMap<>())
                            .computeIfAbsent(entry.getKey().getProviderClass(), aClass -> new HashMap<>()).put(factory.getId(),factory.getClass());
                }
            }
        }

        recorder.configSessionFactory(factories, defaultProviders, Environment.isRebuild());
    }

    /**
     * <p>Make the build time configuration available at runtime so that the server can run without having to specify some of
     * the properties again.
     *
     * <p>This build step also adds a static call to {@link org.keycloak.cli.ShowConfigCommand#run} via the recorder
     * so that the configuration can be shown when requested.
     *
     * @param recorder the recorder
     */
    @Record(ExecutionTime.STATIC_INIT)
    @BuildStep
    void setBuildTimeProperties(KeycloakRecorder recorder) {
        Properties properties = new Properties();

        for (String name : getPropertyNames()) {
            if (isNotPersistentProperty(name)) {
                continue;
            }

            Optional<String> value = Configuration.getOptionalValue(name);

            if (value.isPresent()) {
                properties.put(name, value.get());
            }
        }

        File file = KeycloakConfigSourceProvider.getPersistedConfigFile().toFile();

        if (file.exists()) {
            file.delete();
        }

        try (FileOutputStream fos = new FileOutputStream(file)) {
            properties.store(fos, " Auto-generated, DO NOT change this file");
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate persisted.properties file", e);
        }

        recorder.validateAndSetBuildTimeProperties(Environment.isRebuild(), getRawValue("kc.config.args"));

        recorder.showConfig();
    }

    private boolean isNotPersistentProperty(String name) {
        // these properties are ignored from the build time properties as they are runtime-specific
        return !name.startsWith("kc") || "kc.home.dir".equals(name) || "kc.config.args".equals(name);
    }

    /**
     * This will cause quarkus tu include specified modules in the jandex index. For example keycloak-services is needed as it includes
     * most of the JAX-RS resources, which are required to register Resteasy builtin providers. See {@link ResteasyDeployment#isRegisterBuiltin()}.
     * Similar reason is liquibase
     *
     * @param indexDependencyBuildItemBuildProducer
     */
    @BuildStep
    void index(BuildProducer<IndexDependencyBuildItem> indexDependencyBuildItemBuildProducer) {
        indexDependencyBuildItemBuildProducer.produce(new IndexDependencyBuildItem("org.liquibase", "liquibase-core"));
        indexDependencyBuildItemBuildProducer.produce(new IndexDependencyBuildItem("org.keycloak", "keycloak-services"));
    }

    @BuildStep
    void initializeFilter(BuildProducer<FilterBuildItem> filters) {
        filters.produce(new FilterBuildItem(new QuarkusRequestFilter(),FilterBuildItem.AUTHORIZATION - 10));
    }

    /**
     * <p>Initialize metrics and health endpoints.
     *
     * <p>The only reason for manually registering these endpoints is that by default they run as blocking hence
     * running in a different thread than the worker thread started by {@link QuarkusRequestFilter}.
     * See https://github.com/quarkusio/quarkus/issues/12990.
     *
     * <p>By doing this, custom health checks such as {@link org.keycloak.services.health.KeycloakReadyHealthCheck} is
     * executed within an active {@link org.keycloak.models.KeycloakSession}, making possible to use it when calculating the
     * status.
     *
     * @param routes
     */
    @BuildStep
    void initializeMetrics(BuildProducer<RouteBuildItem> routes) {
        Handler<RoutingContext> healthHandler;
        Handler<RoutingContext> metricsHandler;

        if (isMetricsEnabled()) {
            healthHandler = new SmallRyeHealthHandler();
            metricsHandler = new KeycloakMetricsHandler();
        } else {
            healthHandler = new NotFoundHandler();
            metricsHandler = new NotFoundHandler();
        }

        routes.produce(new RouteBuildItem(DEFAULT_HEALTH_ENDPOINT, healthHandler));
        routes.produce(new RouteBuildItem(DEFAULT_HEALTH_ENDPOINT.concat("/live"), healthHandler));
        routes.produce(new RouteBuildItem(DEFAULT_HEALTH_ENDPOINT.concat("/ready"), healthHandler));
        routes.produce(new RouteBuildItem(KeycloakMetricsHandler.DEFAULT_METRICS_ENDPOINT, metricsHandler));
    }

    @BuildStep(onlyIf = IsDevelopment.class)
    void configureDevMode(BuildProducer<HotDeploymentWatchedFileBuildItem> hotFiles) {
        hotFiles.produce(new HotDeploymentWatchedFileBuildItem("META-INF/keycloak.properties"));
    }

    private Map<Spi, Map<Class<? extends Provider>, Map<String, ProviderFactory>>> loadFactories() {
        loadConfig();
        ProviderManager pm = new ProviderManager(KeycloakDeploymentInfo.create().services(), new BuildClassLoader());
        Map<Spi, Map<Class<? extends Provider>, Map<String, ProviderFactory>>> factories = new HashMap<>();

        for (Spi spi : pm.loadSpis()) {
            Map<Class<? extends Provider>, Map<String, ProviderFactory>> providers = new HashMap<>();

            for (ProviderFactory factory : pm.load(spi)) {
                if (Arrays.asList(
                        JBossJtaTransactionManagerLookup.class,
                        DefaultJpaConnectionProviderFactory.class,
                        DefaultLiquibaseConnectionProvider.class,
                        LiquibaseJpaUpdaterProviderFactory.class).contains(factory.getClass())) {
                    continue;
                }

                Config.Scope scope = Config.scope(spi.getName(), factory.getId());

                if (isEnabled(factory, scope)) {
                    if (spi.isInternal() && !isInternal(factory)) {
                        ServicesLogger.LOGGER.spiMayChange(factory.getId(), factory.getClass().getName(), spi.getName());
                    }

                    providers.computeIfAbsent(spi.getProviderClass(), aClass -> new HashMap<>()).put(factory.getId(),
                            factory);
                } else {
                    logger.debugv("SPI {0} provider {1} disabled", spi.getName(), factory.getId());
                }
            }

            factories.put(spi, providers);
        }

        return factories;
    }

    private boolean isEnabled(ProviderFactory factory, Config.Scope scope) {
        if (!scope.getBoolean("enabled", true)) {
            return false;
        }
        if (factory instanceof EnvironmentDependentProviderFactory) {
            return ((EnvironmentDependentProviderFactory) factory).isSupported();
        }
        return true;
    }

    private boolean isInternal(ProviderFactory<?> factory) {
        String packageName = factory.getClass().getPackage().getName();
        return packageName.startsWith("org.keycloak") && !packageName.startsWith("org.keycloak.examples");
    }

    private void checkProviders(Spi spi,
                                Map<Class<? extends Provider>, Map<String, ProviderFactory>> factoriesMap,
                                Map<Class<? extends Provider>, String> defaultProviders) {
        String defaultProvider = Config.getProvider(spi.getName());

        if (defaultProvider != null) {
            Map<String, ProviderFactory> map = factoriesMap.get(spi.getProviderClass());
            if (map == null || map.get(defaultProvider) == null) {
                throw new RuntimeException("Failed to find provider " + defaultProvider + " for " + spi.getName());
            }
        } else {
            Map<String, ProviderFactory> factories = factoriesMap.get(spi.getProviderClass());
            if (factories != null && factories.size() == 1) {
                defaultProvider = factories.values().iterator().next().getId();
            }

            if (factories != null) {
                if (defaultProvider == null) {
                    Optional<ProviderFactory> highestPriority = factories.values().stream()
                            .max(Comparator.comparing(ProviderFactory::order));
                    if (highestPriority.isPresent() && highestPriority.get().order() > 0) {
                        defaultProvider = highestPriority.get().getId();
                    }
                }
            }

            if (defaultProvider == null && (factories == null || factories.containsKey("default"))) {
                defaultProvider = "default";
            }
        }

        if (defaultProvider != null) {
            defaultProviders.put(spi.getProviderClass(), defaultProvider);
        } else {
            logger.debugv("No default provider for {0}", spi.getName());
        }
    }

    protected void loadConfig() {
        ServiceLoader<ConfigProviderFactory> loader = ServiceLoader.load(ConfigProviderFactory.class, KeycloakApplication.class.getClassLoader());

        try {
            ConfigProviderFactory factory = loader.iterator().next();
            logger.debugv("ConfigProvider: {0}", factory.getClass().getName());
            Config.init(factory.create().orElseThrow(() -> new RuntimeException("Failed to load Keycloak configuration")));
        } catch (NoSuchElementException e) {
            throw new RuntimeException("No valid ConfigProvider found");
        }
    }

    private boolean isMetricsEnabled() {
        return Configuration.getOptionalBooleanValue(MicroProfileConfigProvider.NS_KEYCLOAK_PREFIX.concat("metrics.enabled")).orElse(false);
    }
}

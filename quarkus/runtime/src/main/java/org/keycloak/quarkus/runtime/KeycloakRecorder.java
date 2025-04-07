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

import io.agroal.api.AgroalDataSource;
import io.quarkus.agroal.DataSource;
import io.quarkus.arc.Arc;
import io.quarkus.arc.InstanceHandle;
import io.quarkus.hibernate.orm.runtime.integration.HibernateOrmIntegrationRuntimeInitListener;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.annotations.Recorder;
import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;
import liquibase.Scope;
import liquibase.servicelocator.ServiceLocator;
import org.hibernate.cfg.AvailableSettings;
import org.infinispan.commons.util.FileLookupFactory;
import org.infinispan.protostream.SerializationContextInitializer;
import org.jboss.logging.Logger;
import org.keycloak.Config;
import org.keycloak.common.Profile;
import org.keycloak.common.crypto.CryptoIntegration;
import org.keycloak.common.crypto.CryptoProvider;
import org.keycloak.common.crypto.FipsMode;
import org.keycloak.config.TruststoreOptions;
import org.keycloak.marshalling.Marshalling;
import org.keycloak.provider.Provider;
import org.keycloak.provider.ProviderFactory;
import org.keycloak.provider.Spi;
import org.keycloak.quarkus.runtime.configuration.Configuration;
import org.keycloak.quarkus.runtime.configuration.MicroProfileConfigProvider;
import org.keycloak.quarkus.runtime.integration.QuarkusKeycloakSessionFactory;
import org.keycloak.quarkus.runtime.storage.database.liquibase.FastServiceLocator;
import org.keycloak.quarkus.runtime.storage.infinispan.CacheManagerFactory;
import org.keycloak.representations.userprofile.config.UPConfig;
import org.keycloak.theme.ClasspathThemeProviderFactory;
import org.keycloak.truststore.TruststoreBuilder;
import org.keycloak.userprofile.DeclarativeUserProfileProviderFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.annotation.Annotation;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.keycloak.quarkus.runtime.configuration.Configuration.getKcConfigValue;

@Recorder
public class KeycloakRecorder {

    private static final Logger logger = Logger.getLogger(KeycloakRecorder.class);

    public void initConfig() {
        Config.init(new MicroProfileConfigProvider());
    }

    public void configureProfile(Profile.ProfileName profileName, Map<Profile.Feature, Boolean> features) {
        Profile.init(profileName, features);
    }

    // default handler for redirecting to specific path
    public Handler<RoutingContext> getRedirectHandler(String redirectPath) {
        return routingContext -> routingContext.redirect(redirectPath);
    }

    // default handler for the management interface
    public Handler<RoutingContext> getManagementHandler() {
        return routingContext -> routingContext.response().end("Keycloak Management Interface");
    }

    public void configureTruststore() {
        String[] truststores = Configuration.getOptionalKcValue(TruststoreOptions.TRUSTSTORE_PATHS.getKey())
                .map(s -> s.split(",")).orElse(new String[0]);

        String dataDir = Environment.getDataDir();

        File truststoresDir = Optional.ofNullable(Environment.getHomePath()).map(path -> path.resolve("conf").resolve("truststores").toFile()).orElse(null);

        if (truststoresDir != null && truststoresDir.exists() && Optional.ofNullable(truststoresDir.list()).map(a -> a.length).orElse(0) > 0) {
            truststores = Stream.concat(Stream.of(truststoresDir.getAbsolutePath()), Stream.of(truststores)).toArray(String[]::new);
        } else if (truststores.length == 0) {
            return; // nothing to configure, we'll just use the system default
        }

        TruststoreBuilder.setSystemTruststore(truststores, true, dataDir);
    }

    public void configureLiquibase(Map<String, List<String>> services) {
        ServiceLocator locator = Scope.getCurrentScope().getServiceLocator();
        if (locator instanceof FastServiceLocator) {
            ((FastServiceLocator) locator).initServices(services);
        }
    }

    public void configSessionFactory(
            Map<Spi, Map<Class<? extends Provider>, Map<String, Class<? extends ProviderFactory>>>> factories,
            Map<Class<? extends Provider>, String> defaultProviders,
            Map<String, ProviderFactory> preConfiguredProviders,
            List<ClasspathThemeProviderFactory.ThemesRepresentation> themes) {
        QuarkusKeycloakSessionFactory.setInstance(new QuarkusKeycloakSessionFactory(factories, defaultProviders, preConfiguredProviders, themes));
    }

    public RuntimeValue<CacheManagerFactory> createCacheInitializer() {
        try {
            CacheManagerFactory cacheManagerFactory = new CacheManagerFactory(getInfinispanConfigFile());
            return new RuntimeValue<>(cacheManagerFactory);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private String getInfinispanConfigFile() {
        String configFile = getKcConfigValue("spi-connections-infinispan-quarkus-config-file").getValue();

        if (configFile == null) {
            throw new IllegalArgumentException("Option 'configFile' needs to be specified");
        }

        Path configPath = Paths.get(configFile);
        String path;

        if (configPath.toFile().exists()) {
            path = configPath.toFile().getAbsolutePath();
        } else {
            path = configPath.getFileName().toString();
        }

        logger.debugf("Infinispan configuration file: %s", path);

        InputStream url = FileLookupFactory.newInstance().lookupFile(path, KeycloakRecorder.class.getClassLoader());

        if (url == null) {
            throw new IllegalArgumentException("Could not load cluster configuration file at [" + configPath + "]");
        }

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(url))) {
            return reader.lines().collect(Collectors.joining("\n"));
        } catch (Exception cause) {
            throw new RuntimeException("Failed to read clustering configuration from [" + url + "]", cause);
        }
    }

    public void setDefaultUserProfileConfiguration(UPConfig configuration) {
        DeclarativeUserProfileProviderFactory.setDefaultConfig(configuration);
    }

    public HibernateOrmIntegrationRuntimeInitListener createUserDefinedUnitListener(String name) {
        return new HibernateOrmIntegrationRuntimeInitListener() {
            @Override
            public void contributeRuntimeProperties(BiConsumer<String, Object> propertyCollector) {
                try (InstanceHandle<AgroalDataSource> instance = Arc.container().instance(
                        AgroalDataSource.class, new DataSource() {
                            @Override public Class<? extends Annotation> annotationType() {
                                return DataSource.class;
                            }

                            @Override public String value() {
                                return name;
                            }
                        })) {
                    propertyCollector.accept(AvailableSettings.DATASOURCE, instance.get());
                }
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

    public void setCryptoProvider(FipsMode fipsMode) {
        String cryptoProvider = fipsMode.getProviderClassName();

        try {
            CryptoIntegration.setProvider(
                    (CryptoProvider) Thread.currentThread().getContextClassLoader().loadClass(cryptoProvider).getDeclaredConstructor().newInstance());
        } catch (ClassNotFoundException | NoClassDefFoundError cause) {
            if (fipsMode.isFipsEnabled()) {
                throw new RuntimeException("Failed to configure FIPS. Make sure you have added the Bouncy Castle FIPS dependencies to the 'providers' directory.");
            }
            throw new RuntimeException("Unexpected error when configuring the crypto provider: " + cryptoProvider, cause);
        } catch (Exception cause) {
            throw new RuntimeException("Unexpected error when configuring the crypto provider: " + cryptoProvider, cause);
        }
    }

    public void configureProtoStreamSchemas(List<SerializationContextInitializer> schemas) {
        Marshalling.setSchemas(schemas);
    }
}

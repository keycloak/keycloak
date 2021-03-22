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

package org.keycloak.quarkus;

import static org.keycloak.configuration.Configuration.getBuiltTimeProperty;
import static org.keycloak.configuration.Configuration.getConfig;

import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.StreamSupport;

import io.smallrye.config.ConfigValue;
import org.jboss.logging.Logger;
import org.keycloak.QuarkusKeycloakSessionFactory;
import org.keycloak.cli.ShowConfigCommand;
import org.keycloak.common.Profile;
import org.keycloak.configuration.Configuration;
import org.keycloak.configuration.MicroProfileConfigProvider;
import org.keycloak.configuration.PersistedConfigSource;
import org.keycloak.configuration.PropertyMappers;
import org.keycloak.connections.liquibase.FastServiceLocator;
import org.keycloak.connections.liquibase.KeycloakLogger;
import org.keycloak.provider.Provider;
import org.keycloak.provider.ProviderFactory;
import org.keycloak.provider.Spi;

import io.quarkus.runtime.annotations.Recorder;
import liquibase.logging.LogFactory;
import liquibase.servicelocator.ServiceLocator;
import org.keycloak.util.Environment;

@Recorder
public class KeycloakRecorder {

    private static final Logger LOGGER = Logger.getLogger(KeycloakRecorder.class);

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

    /**
     * <p>Validate the build time properties with any property passed during runtime in order to advertise any difference with the
     * server image state.
     * 
     * <p>This method also keep the build time properties available at runtime.
     * 
     * 
     * @param buildTimeProperties the build time properties set when running the last re-augmentation
     * @param rebuild indicates whether or not the server was re-augmented
     * @param configArgs the configuration args if provided when the server was re-augmented
     */
    public void validateAndSetBuildTimeProperties(Boolean rebuild, String configArgs) {
        String configHelpText = configArgs;

        for (String propertyName : getConfig().getPropertyNames()) {
            // we should only validate if there is a server image and if the property is a runtime property
            if (!shouldValidate(propertyName, rebuild)) {
                continue;
            }

            // try to resolve any property set using profiles
            if (propertyName.startsWith("%")) {
                propertyName = propertyName.substring(propertyName.indexOf('.') + 1);
            }

            String buildValue = Environment.getBuiltTimeProperty(propertyName).orElse(null);
            ConfigValue value = getConfig().getConfigValue(propertyName);

            if (value.getValue() != null && !value.getValue().equalsIgnoreCase(buildValue)) {
                if (configHelpText != null) {
                    String cliNameFormat = PropertyMappers.toCLIFormat(propertyName);

                    if (buildValue != null) {
                        String currentProp = "--" + cliNameFormat.substring(3) + "=" + buildValue;
                        String newProp = "--" + cliNameFormat.substring(3) + "=" + value.getValue();

                        if (configHelpText.contains(currentProp)) {
                            LOGGER.warnf("The new value [%s] of the property [%s] in [%s] differs from the value [%s] set into the server image. The new value will override the value set into the server image.",
                                    value.getValue(), propertyName, value.getConfigSourceName(), buildValue);
                            configHelpText = configHelpText.replaceAll(currentProp, newProp);
                        } else if (!configHelpText
                                .contains("--" + cliNameFormat.substring(3))) {
                            LOGGER.warnf("The new value [%s] of the property [%s] in [%s] differs from the value [%s] set into the server image. The new value will override the value set into the server image.",
                                    value.getValue(), propertyName, value.getConfigSourceName(), buildValue);
                            configHelpText += " " + newProp;
                        }
                    } else {
                        String finalPropertyName = propertyName;

                        if (!StreamSupport.stream(getConfig().getPropertyNames().spliterator(), false)
                                .filter(new Predicate<String>() {
                                    @Override
                                    public boolean test(String s) {
                                        ConfigValue configValue = getConfig().getConfigValue(s);

                                        return configValue.getConfigSourceName().equals(PersistedConfigSource.NAME);
                                    }
                                })
                                .anyMatch(new Predicate<String>() {
                                    @Override
                                    public boolean test(String s) {
                                        return PropertyMappers.canonicalFormat(finalPropertyName)
                                                .equalsIgnoreCase(PropertyMappers.canonicalFormat(s));
                                    }
                                })) {
                            String prop = "--" + cliNameFormat.substring(3) + "=" + value.getValue();

                            if (!configHelpText.contains(prop)) {
                                LOGGER.warnf("New property [%s] set with value [%s] in [%s]. This property is not persisted into the server image.",
                                        propertyName, value.getValue(), value.getConfigSourceName(), buildValue);
                                configHelpText += " " + prop;
                            }
                        }
                    }
                }
            }
        }

        if (configArgs != null && !configArgs.equals(configHelpText)) {
            LOGGER.warnf("Please, run the 'config' command if you want to persist the new configuration into the server image:\n\n\t%s config %s\n", Environment.getCommand(), String.join(" ", configHelpText.split(",")));
        }
    }

    private boolean shouldValidate(String name, boolean rebuild) {
        return rebuild && name.contains(MicroProfileConfigProvider.NS_KEYCLOAK_PREFIX) 
                && (!PropertyMappers.isBuildTimeProperty(name)
                && !"kc.version".equals(name) 
                && !"kc.config.args".equals(name) 
                && !"kc.home.dir".equals(name)
                && !"kc.config.file".equals(name)
                && !"kc.profile".equals(name)
                && !"kc.show.config".equals(name)
                && !"kc.show.config.runtime".equals(name)
                && !PropertyMappers.toCLIFormat("kc.config.file").equals(name));
    }

    /**
     * This method should be executed during static init so that the configuration is printed (if demanded) based on the properties
     * set from the previous reaugmentation
     */
    public void showConfig() {
        ShowConfigCommand.run();
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

                String value = getBuiltTimeProperty(feature);

                if (value == null) {
                    value = getBuiltTimeProperty(feature.replaceAll("\\.features\\.", "\\.features-"));
                }
                
                if (value != null) {
                    return value;
                }

                return Configuration.getRawValue(feature);
            }
        });
    }
}

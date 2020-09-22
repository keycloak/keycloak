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

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import io.smallrye.config.ConfigValue;
import org.jboss.logging.Logger;
import org.keycloak.QuarkusKeycloakSessionFactory;
import org.keycloak.cli.ShowConfigCommand;
import org.keycloak.common.Profile;
import org.keycloak.configuration.MicroProfileConfigProvider;
import org.keycloak.configuration.PropertyMappers;
import org.keycloak.connections.liquibase.FastServiceLocator;
import org.keycloak.connections.liquibase.KeycloakLogger;
import org.keycloak.provider.Provider;
import org.keycloak.provider.ProviderFactory;
import org.keycloak.provider.Spi;

import io.quarkus.runtime.annotations.Recorder;
import io.smallrye.config.SmallRyeConfig;
import io.smallrye.config.SmallRyeConfigProviderResolver;
import liquibase.logging.LogFactory;
import liquibase.servicelocator.ServiceLocator;
import org.keycloak.util.Environment;

@Recorder
public class KeycloakRecorder {

    private static final Logger LOGGER = Logger.getLogger(KeycloakRecorder.class);
    
    private static SmallRyeConfig CONFIG = null;
    
    private static Map<String, String> BUILD_TIME_PROPERTIES = Collections.emptyMap();
    
    public static String getBuiltTimeProperty(String name) {
        String value = BUILD_TIME_PROPERTIES.get(name);

        if (value == null) {
            String profile = Environment.getProfile();

            if (profile == null) {
                profile = BUILD_TIME_PROPERTIES.get("kc.profile");
            }

            value = BUILD_TIME_PROPERTIES.get("%" + profile + "." + name);
        }
        
        return value;
    }

    public static SmallRyeConfig getConfig() {
        if (CONFIG == null) {
            CONFIG = (SmallRyeConfig) SmallRyeConfigProviderResolver.instance().getConfig();
        }
        return CONFIG;
    }

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
            Boolean reaugmented) {
        Profile.setInstance(createProfile());
        QuarkusKeycloakSessionFactory.setInstance(new QuarkusKeycloakSessionFactory(factories, defaultProviders, reaugmented));
    }

    public void setBuildTimeProperties(Map<String, String> buildTimeProperties, Boolean rebuild, String configArgs) {
        BUILD_TIME_PROPERTIES = buildTimeProperties;
        String configHelpText = configArgs;

        for (String propertyName : getConfig().getPropertyNames()) {
            if (!propertyName.startsWith(MicroProfileConfigProvider.NS_KEYCLOAK_PREFIX)) {
                continue;
            }

            String buildValue = Environment.getBuiltTimeProperty(propertyName).orElseGet(new Supplier<String>() {
                @Override 
                public String get() {
                    return Environment.getBuiltTimeProperty(PropertyMappers.toCLIFormat(propertyName)).orElse(null);
                }
            });

            ConfigValue value = getConfig().getConfigValue(propertyName);

            if (buildValue != null && isRuntimeValue(value) && !buildValue.equalsIgnoreCase(value.getValue())) {
                if (configHelpText != null) {
                    String currentProp = "--" + PropertyMappers.toCLIFormat(propertyName).substring(3) + "=" + buildValue;
                    String newProp = "--" + PropertyMappers.toCLIFormat(propertyName).substring(3) + "=" + value.getValue();
                    
                    if (configHelpText.contains(currentProp)) {
                        LOGGER.warnf("The new value [%s] of the property [%s] in [%s] differs from the value [%s] set into the server image. The new value will override the value set into the server image.", value.getValue(), propertyName, value.getConfigSourceName(), buildValue);
                        configHelpText = configHelpText.replaceAll(currentProp, newProp);
                    } else if (!configHelpText.contains("--" + PropertyMappers.toCLIFormat(propertyName).substring(3))) {
                        configHelpText += newProp;
                    }
                }
            } else if (configHelpText != null && rebuild && isRuntimeValue(value)) {
                String prop = "--" + PropertyMappers.toCLIFormat(propertyName).substring(3) + "=" + value.getValue();

                if (!configHelpText.contains(prop)) {
                    LOGGER.infof("New property [%s] set with value [%s] in [%s]. This property is not persisted into the server image.",
                            propertyName, value.getValue(), value.getConfigSourceName(), buildValue);
                    configHelpText += " " + prop;
                }
            }
        }

        if (configArgs != null && !configArgs.equals(configHelpText)) {
            LOGGER.infof("Please, run the 'config' command if you want to configure the server image with the new property values:\n\t%s config %s", Environment.getCommand(), String.join(" ", configHelpText.split(",")));
        }
    }

    private boolean isRuntimeValue(ConfigValue value) {
        String name = value.getName();
        return value.getValue() != null && !PropertyMappers.isBuildTimeProperty(name)
                && !"kc.version".equals(name) && !"kc.config.args".equals(
                name) && !"kc.home.dir".equals(name);
    }

    /**
     * This method should be executed during static init so that the configuration is printed (if demanded) based on the properties
     * set from the previous reaugmentation
     */
    public void showConfig() {
        ShowConfigCommand.run(BUILD_TIME_PROPERTIES);
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

                return KeycloakRecorder.getConfig().getRawValue(feature);
            }
        });
    }
}

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

package org.keycloak.quarkus.runtime.cli.command;

import static org.keycloak.quarkus.runtime.Environment.getCurrentOrPersistedProfile;
import static org.keycloak.quarkus.runtime.configuration.Configuration.getConfigValue;
import static org.keycloak.quarkus.runtime.configuration.Configuration.getPropertyNames;
import static org.keycloak.quarkus.runtime.configuration.mappers.PropertyMappers.formatValue;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.keycloak.quarkus.runtime.Environment;
import org.keycloak.quarkus.runtime.configuration.MicroProfileConfigProvider;
import org.keycloak.quarkus.runtime.configuration.PersistedConfigSource;

import io.quarkus.runtime.Quarkus;
import io.smallrye.config.ConfigValue;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

@Command(name = "show-config",
        header = "Print out the current configuration.",
        description = "%nPrint out the current configuration.")
public final class ShowConfig extends AbstractCommand implements Runnable {

    public static final String NAME = "show-config";
    @Parameters(
            paramLabel = "filter",
            defaultValue = "none",
            description = "Show all configuration options. Use 'all' to show all options.")
    String filter;

    @Override
    public void run() {
        System.setProperty("kc.show.config", filter);
        String configArgs = System.getProperty("kc.show.config");

        if (configArgs != null) {
            Map<String, Set<String>> properties = getPropertiesByGroup();
            String profile = getCurrentOrPersistedProfile();

            printRunTimeConfig(properties, profile);

            if (configArgs.equalsIgnoreCase("all")) {
                spec.commandLine().getOut().println("Quarkus Configuration:");
                properties.get(MicroProfileConfigProvider.NS_QUARKUS).stream().sorted()
                        .forEachOrdered(this::printProperty);
            }

            if (!Boolean.getBoolean("kc.show.config.runtime")) {
                Quarkus.asyncExit(0);
            }
        }
    }

    private void printRunTimeConfig(Map<String, Set<String>> properties, String profile) {
        Set<String> uniqueNames = new HashSet<>();

        spec.commandLine().getOut().printf("Current Mode: %s%n", Environment.getKeycloakModeFromProfile(profile));

        spec.commandLine().getOut().println("Runtime Configuration:");

        properties.get(MicroProfileConfigProvider.NS_KEYCLOAK).stream().sorted()
                .filter(uniqueNames::add)
                .forEachOrdered(this::printProperty);
    }

    private static Map<String, Set<String>> getPropertiesByGroup() {
        Map<String, Set<String>> properties = StreamSupport
                .stream(getPropertyNames().spliterator(), false)
                .filter(ShowConfig::filterByGroup)
                .collect(Collectors.groupingBy(ShowConfig::groupProperties, Collectors.toSet()));

        StreamSupport.stream(getPropertyNames().spliterator(), false)
                .filter(new Predicate<String>() {
                    @Override
                    public boolean test(String s) {
                        ConfigValue configValue = getConfigValue(s);

                        if (configValue == null) {
                            return false;
                        }

                        return PersistedConfigSource.NAME.equals(configValue.getConfigSourceName());
                    }
                })
                .filter(ShowConfig::filterByGroup)
                .collect(Collectors.groupingBy(ShowConfig::groupProperties, Collectors.toSet()))
                .forEach(new BiConsumer<String, Set<String>>() {
                    @Override
                    public void accept(String group, Set<String> propertyNames) {
                        properties.computeIfAbsent(group, name -> new HashSet<>()).addAll(propertyNames);
                    }
                });

        return properties;
    }

    private void printProperty(String property) {
        ConfigValue configValue = getConfigValue(property);

        if (configValue.getValue() == null) {
            configValue = getConfigValue(property);
        }


        if (configValue.getValue() == null) {
            return;
        }

        if (configValue.getSourceName() == null) {
            return;
        }

        spec.commandLine().getOut().printf("\t%s =  %s (%s)%n", configValue.getName(), formatValue(configValue.getName(), configValue.getValue()), configValue.getConfigSourceName());
    }

    private static String groupProperties(String property) {
        if (property.startsWith("%")) {
            return "%";
        }

        int endIndex = property.indexOf('.');

        if (endIndex == -1) {
            return "";
        }

        return property.substring(0, endIndex);
    }

    private static boolean filterByGroup(String property) {
        return property.startsWith(MicroProfileConfigProvider.NS_KEYCLOAK)
                || property.startsWith(MicroProfileConfigProvider.NS_QUARKUS)
                || property.startsWith("%");
    }
}

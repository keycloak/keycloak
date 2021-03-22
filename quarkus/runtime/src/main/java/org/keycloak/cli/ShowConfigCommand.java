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

package org.keycloak.cli;

import static java.lang.Boolean.parseBoolean;
import static org.keycloak.configuration.PropertyMappers.formatValue;
import static org.keycloak.util.Environment.getBuiltTimeProperty;
import static org.keycloak.util.Environment.getConfig;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.keycloak.configuration.MicroProfileConfigProvider;
import org.keycloak.util.Environment;

import io.smallrye.config.ConfigValue;

public final class ShowConfigCommand {

    public static void run(Map<String, String> buildTimeProperties) {
        String configArgs = System.getProperty("kc.show.config");

        if (configArgs != null) {
            Map<String, Set<String>> properties = getPropertiesByGroup(buildTimeProperties);
            String profile = getProfile();

            System.out.printf("Current Profile: %s%n", profile == null ? "none" : profile);

            System.out.println("Runtime Configuration:");
            properties.get(MicroProfileConfigProvider.NS_KEYCLOAK).stream().sorted()
                    .forEachOrdered(ShowConfigCommand::printProperty);

            if (configArgs.equalsIgnoreCase("all")) {
                Set<String> profiles = properties.get("%");
                
                if (profiles != null) {
                    profiles.stream()
                            .sorted()
                            .collect(Collectors.groupingBy(s -> s.substring(1, s.indexOf('.'))))
                            .forEach((p, properties1) -> {
                                if (p.equals(profile)) {
                                    System.out.printf("Profile \"%s\" Configuration (%s):%n", p,
                                            p.equals(profile) ? "current" : "");
                                } else {
                                    System.out.printf("Profile \"%s\" Configuration:%n", p);
                                }

                                properties1.stream().sorted().forEachOrdered(ShowConfigCommand::printProperty);
                            });
                }

                System.out.println("Quarkus Configuration:");
                properties.get(MicroProfileConfigProvider.NS_QUARKUS).stream().sorted()
                        .forEachOrdered(ShowConfigCommand::printProperty);
            }

            if (!parseBoolean(System.getProperty("kc.show.config.runtime", Boolean.FALSE.toString()))) {
                System.exit(0);
            }
        }
    }

    private static String getProfile() {
        String profile = Environment.getProfile();

        if (profile == null) {
            return getBuiltTimeProperty("quarkus.profile").orElse(null);
        }

        return profile;
    }

    private static Map<String, Set<String>> getPropertiesByGroup(Map<String, String> buildTimeProperties) {
        Map<String, Set<String>> properties = StreamSupport
                .stream(getConfig().getPropertyNames().spliterator(), false)
                .filter(ShowConfigCommand::filterByGroup)
                .collect(Collectors.groupingBy(ShowConfigCommand::groupProperties, Collectors.toSet()));

        buildTimeProperties.keySet().stream()
                .filter(property -> filterByGroup(property))
                .collect(Collectors.groupingBy(ShowConfigCommand::groupProperties, Collectors.toSet()))
                .forEach(new BiConsumer<String, Set<String>>() {
                    @Override
                    public void accept(String group, Set<String> propertyNames) {
                        properties.computeIfAbsent(group, (name) -> new HashSet<>()).addAll(propertyNames);
                    }
                });

        return properties;
    }

    private static void printProperty(String property) {
        String value = getBuiltTimeProperty(property).orElse(null);

        if (value != null && !"".equals(value.trim())) {
            System.out.printf("\t%s =  %s (persisted)%n", property, formatValue(property, value));
            return;
        }

        ConfigValue configValue = getConfig().getConfigValue(property);

        if (configValue == null) {
            return;
        }

        System.out.printf("\t%s =  %s (%s)%n", property, formatValue(property, configValue.getValue()), configValue.getConfigSourceName());
    }

    private static String groupProperties(String property) {
        if (property.startsWith("%")) {
            return "%";
        }
        return property.substring(0, property.indexOf('.'));
    }

    private static boolean filterByGroup(String property) {
        return property.startsWith(MicroProfileConfigProvider.NS_KEYCLOAK)
                || property.startsWith(MicroProfileConfigProvider.NS_QUARKUS)
                || property.startsWith("%");
    }
}

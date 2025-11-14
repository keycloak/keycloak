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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.StreamSupport;

import org.keycloak.quarkus.runtime.Environment;
import org.keycloak.quarkus.runtime.configuration.KeycloakConfigSourceProvider;
import org.keycloak.quarkus.runtime.configuration.MicroProfileConfigProvider;
import org.keycloak.quarkus.runtime.configuration.mappers.PropertyMapper;
import org.keycloak.quarkus.runtime.configuration.mappers.PropertyMappers;

import io.quarkus.runtime.Quarkus;
import io.smallrye.config.ConfigValue;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

import static org.keycloak.quarkus.runtime.configuration.Configuration.getConfigValue;
import static org.keycloak.quarkus.runtime.configuration.Configuration.getPropertyNames;
import static org.keycloak.quarkus.runtime.configuration.mappers.PropertyMappers.maskValue;

@Command(name = "show-config",
        header = "Print out the current configuration.",
        description = "%nPrint out the current configuration.")
public final class ShowConfig extends AbstractCommand {

    public static final String NAME = "show-config";
    private static final List<String> allowedSystemPropertyKeys = List.of(
            "kc.version");

    @Parameters(
            paramLabel = "filter",
            defaultValue = "none",
            description = "Show all configuration options. Use 'all' to show all options.")
    String filter;

    @Override
    public String getDefaultProfile() {
        return null;
    }

    @Override
    protected void runCommand() {
        String profile = org.keycloak.common.util.Environment.getProfile();

        spec.commandLine().getOut().printf("Current Mode: %s%n", Environment.getKeycloakModeFromProfile(profile));

        spec.commandLine().getOut().println("Current Configuration:");

        Set<String> uniqueNames = new HashSet<>();
        List<ConfigValue> quarkusValues = new ArrayList<ConfigValue>();
        StreamSupport.stream(getPropertyNames().spliterator(), false).forEachOrdered(property -> {
            ConfigValue configValue = getConfigValue(property);

            if (configValue.getValue() == null) {
                return;
            }

            if (configValue.getSourceName() == null) {
                return;
            }

            PropertyMapper<?> mapper = PropertyMappers.getMapper(property);

            if (mapper == null && configValue.getSourceName().equals("SysPropConfigSource") && !allowedSystemPropertyKeys.contains(property)) {
                return; // most system properties are internally used, and not relevant during show-config
            }

            if (mapper != null) {
                String from = mapper.forKey(property).getFrom();

                // only report from when it exists
                if (!property.equals(from)) {
                    ConfigValue value = getConfigValue(from);
                    if (value.getValue() != null) {
                        return;
                    }
                    configValue = value;
                    property = from;
                }
            }

            if (!uniqueNames.add(property)) {
                return;
            }

            if (property.startsWith(MicroProfileConfigProvider.NS_QUARKUS_PREFIX)) {
                if (mapper == null) {
                    quarkusValues.add(configValue);
                    return;
                }
            } else if (!property.startsWith(MicroProfileConfigProvider.NS_KEYCLOAK_PREFIX)) {
                return;
            }

            printProperty(property, mapper, configValue);
        });

        if (filter.equalsIgnoreCase("all")) {
            spec.commandLine().getOut().println("Quarkus Configuration:");
            quarkusValues.forEach(v -> printProperty(v.getName(), null, v));
        }

        Quarkus.asyncExit(0);
    }

    private void printProperty(String property, PropertyMapper<?> mapper, ConfigValue configValue) {
        String sourceName = configValue.getConfigSourceName();
        String value = configValue.getValue();

        value = maskValue(value, sourceName, mapper);

        spec.commandLine().getOut().printf("\t%s =  %s (%s)%n", property, value, KeycloakConfigSourceProvider.getConfigSourceDisplayName(sourceName));
    }

    @Override
    public String getName() {
        return NAME;
    }
}

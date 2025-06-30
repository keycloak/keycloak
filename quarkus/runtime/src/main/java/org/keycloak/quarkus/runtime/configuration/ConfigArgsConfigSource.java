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

package org.keycloak.quarkus.runtime.configuration;

import static org.keycloak.quarkus.runtime.cli.Picocli.ARG_SHORT_PREFIX;
import static org.keycloak.quarkus.runtime.configuration.Configuration.OPTION_PART_SEPARATOR_CHAR;
import static org.keycloak.quarkus.runtime.configuration.MicroProfileConfigProvider.NS_KEYCLOAK_PREFIX;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.regex.Pattern;

import io.smallrye.config.PropertiesConfigSource;

import org.keycloak.quarkus.runtime.cli.command.Main;
import org.keycloak.quarkus.runtime.configuration.mappers.PropertyMapper;
import org.keycloak.quarkus.runtime.configuration.mappers.PropertyMappers;

/**
 * <p>A configuration source for mapping configuration arguments to their corresponding properties so that they can be recognized
 * when building and running the server.
 *
 * <p>The mapping is based on the system property {@code kc.config.args}, where the value is a comma-separated list of
 * the arguments passed during build or runtime. E.g: "--http-enabled=true,--http-port=8180,--database-vendor=postgres".
 *
 * <p>Each argument is going to be mapped to its corresponding configuration property by prefixing the key with the {@link MicroProfileConfigProvider#NS_KEYCLOAK} namespace.
 */
public class ConfigArgsConfigSource extends PropertiesConfigSource {

    public static final Set<String> SHORT_OPTIONS_ACCEPTING_VALUE = Set.of(Main.PROFILE_SHORT_NAME, Main.CONFIG_FILE_SHORT_NAME);

    public static final String CLI_ARGS = "kc.config.args";
    public static final String NAME = "CliConfigSource";
    private static final String ARG_SEPARATOR = ";;";
    private static final Pattern ARG_KEY_VALUE_SPLIT = Pattern.compile("=");

    protected ConfigArgsConfigSource() {
        super(parseArguments(), NAME, 600);
    }

    public static void setCliArgs(String... args) {
        System.setProperty(CLI_ARGS, String.join(ARG_SEPARATOR, args));
    }

    /**
     * Reads the previously set system property for the originally command.
     * Use the System variable, when you trigger other command executions internally, but need a reference to the
     * actually invoked command.
     *
     * @return the invoked command from the CLI, or empty List if not set.
     */
    public static List<String> getAllCliArgs() {
        if(System.getProperty(CLI_ARGS) == null) {
            return Collections.emptyList();
        }

        return List.of(System.getProperty(CLI_ARGS).split(ARG_SEPARATOR));
    }

    private static String getRawConfigArgs() {
        String args = System.getProperty(CLI_ARGS);

        if (args != null) {
            return args;
        }

        // make sure quarkus.args property is properly formatted
        return String.join(ARG_SEPARATOR, System.getProperty("quarkus.args", "").split(" "));
    }

    @Override
    public String getValue(String propertyName) {
        Map<String, String> properties = getProperties();
        String value = properties.get(propertyName);

        if (value != null) {
            return value;
        }

        return properties.get(propertyName.replace(OPTION_PART_SEPARATOR_CHAR, '.'));
    }

    private static Map<String, String> parseArguments() {
        String rawArgs = getRawConfigArgs();

        if (rawArgs == null || "".equals(rawArgs.trim())) {
            return Collections.emptyMap();
        }

        Map<String, String> properties = new HashMap<>();

        parseConfigArgs(getAllCliArgs(), new BiConsumer<String, String>() {
            @Override
            public void accept(String key, String value) {
                key = NS_KEYCLOAK_PREFIX + key.substring(2);

                properties.put(key, value);

                PropertyMapper<?> mapper = PropertyMappers.getMapper(key);

                if (mapper != null) {
                    String to = mapper.getTo();

                    if (to != null) {
                        properties.put(mapper.getTo(), value);
                    }

                    properties.put(mapper.getFrom(), value);
                }
            }
        }, ignored -> {});

        return properties;
    }

    public static void parseConfigArgs(List<String> args, BiConsumer<String, String> valueArgConsumer, Consumer<String> unaryConsumer) {
        for (int i = 0; i < args.size(); i++) {
            String arg = args.get(i);

            if (!arg.startsWith(ARG_SHORT_PREFIX)) {
                unaryConsumer.accept(arg);
                continue;
            }

            String[] keyValue = ARG_KEY_VALUE_SPLIT.split(arg, 2);
            String key = keyValue[0];

            String value;

            if (keyValue.length == 1) {
                if (args.size() <= i + 1) {
                    unaryConsumer.accept(arg);
                    continue;
                }
                PropertyMapper<?> mapper = PropertyMappers.getMapper(key);
                // the weaknesses here:
                // - needs to know all of the short name options that accept a value
                // - does not know all of the picocli parsing rules. picocli will accept -cffile, and short option grouping - that's not accounted for
                if (mapper != null || SHORT_OPTIONS_ACCEPTING_VALUE.contains(key) || arg.startsWith("--spi")) {
                    i++; // consume next as a value to the key
                    value = args.get(i);
                } else {
                    unaryConsumer.accept(arg);
                    continue;
                }
            } else {
                // the argument has a simple value. Eg.: key=pair
                value = keyValue[1];
            }

            valueArgConsumer.accept(key, value);
        }
    }
}

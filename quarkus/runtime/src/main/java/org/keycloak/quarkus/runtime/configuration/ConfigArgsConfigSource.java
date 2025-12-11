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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.keycloak.quarkus.runtime.cli.command.Main;
import org.keycloak.quarkus.runtime.configuration.mappers.PropertyMapper;
import org.keycloak.quarkus.runtime.configuration.mappers.PropertyMappers;

import io.smallrye.config.PropertiesConfigSource;

import static org.keycloak.quarkus.runtime.cli.Picocli.ARG_SHORT_PREFIX;

/**
 * <p>A configuration source for mapping configuration arguments to their corresponding properties so that they can be recognized
 * when building and running the server.
 *
 * <p>Each argument is going to be mapped to its corresponding configuration property by prefixing the key with the {@link MicroProfileConfigProvider#NS_KEYCLOAK} namespace.
 */
public class ConfigArgsConfigSource extends PropertiesConfigSource {

    public static final String SPI_OPTION_PREFIX = "--spi";

    public static final Set<String> SHORT_OPTIONS_ACCEPTING_VALUE = Set.of(Main.PROFILE_SHORT_NAME, Main.CONFIG_FILE_SHORT_NAME);

    private static final String CLI_ARGS = "kc.config.args";
    public static final String NAME = "CliConfigSource";
    private static final Pattern ARG_KEY_VALUE_SPLIT = Pattern.compile("=");
    private static Set<String> duplicatedArgNames;

    protected ConfigArgsConfigSource() {
        super(parseArguments(), NAME, 600);
    }

    public static void setCliArgs(String... args) {
        System.setProperty(CLI_ARGS,
                Stream.of(args).map(arg -> arg.replaceAll(",", ",,")).collect(Collectors.joining(", ")));
    }

    /**
     * Reads the previously set system property for the originally command.
     * Use the System variable, when you trigger other command executions internally, but need a reference to the
     * actually invoked command.
     *
     * @return the invoked command from the CLI, or empty List if not set.
     */
    public static List<String> getAllCliArgs() {
        String args = System.getProperty(CLI_ARGS);
        if(args == null) {
            return Collections.emptyList();
        }

        List<String> result = new ArrayList<String>();
        boolean escaped = false;
        StringBuilder arg = new StringBuilder();
        for (int i = 0; i < args.length(); i++) {
            char c = args.charAt(i);
            if (c == ',') {
                if (escaped) {
                    arg.append(c);
                }
                escaped = !escaped;
            } else if (c == ' ') {
                if (escaped) {
                    result.add(arg.toString());
                    arg.setLength(0);
                    escaped = false;
                } else {
                    arg.append(c);
                }
            } else {
                arg.append(c);
            }
        }
        result.add(arg.toString());

        return result;
    }

    private static Map<String, String> parseArguments() {
        final Map<String, String> properties = new HashMap<>();
        final Set<String> allPropertiesNames = new HashSet<>();
        duplicatedArgNames = new HashSet<>();

        parseConfigArgs(getAllCliArgs(), (key, value) -> {
            if (!allPropertiesNames.add(key)) {
                duplicatedArgNames.add(key);
            }
            PropertyMappers.getKcKeyFromCliKey(key).ifPresent(s -> properties.put(s, value));
        }, ignored -> {});

        return properties;
    }

    public static Set<String> getDuplicatedArgNames() {
        return Collections.unmodifiableSet(duplicatedArgNames);
    }

    public static void clearDuplicatedArgNames() {
        // after handling these duplicates, clear the memory
        duplicatedArgNames = Collections.emptySet();
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
                PropertyMapper<?> mapper = PropertyMappers.getMapperByCliKey(key);
                // the weaknesses here:
                // - runs before propertymapper sanitizing
                // - needs to know all of the short name options that accept a value
                // - Even though We've explicitly disabled PosixClusteredShortOptionsAllowed, it may not know all of the picocli parsing rules.
                if (mapper != null || SHORT_OPTIONS_ACCEPTING_VALUE.contains(key) || arg.startsWith(SPI_OPTION_PREFIX)) {
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

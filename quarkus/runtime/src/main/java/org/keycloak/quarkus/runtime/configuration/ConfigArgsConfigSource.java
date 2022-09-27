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

import static java.util.Arrays.asList;
import static org.keycloak.quarkus.runtime.cli.Picocli.ARG_SHORT_PREFIX;
import static org.keycloak.quarkus.runtime.cli.command.AbstractStartCommand.AUTO_BUILD_OPTION_LONG;
import static org.keycloak.quarkus.runtime.cli.command.AbstractStartCommand.AUTO_BUILD_OPTION_SHORT;
import static org.keycloak.quarkus.runtime.configuration.Configuration.OPTION_PART_SEPARATOR_CHAR;
import static org.keycloak.quarkus.runtime.configuration.MicroProfileConfigProvider.NS_KEYCLOAK_PREFIX;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.regex.Pattern;

import io.smallrye.config.PropertiesConfigSource;

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

    public static final String CLI_ARGS = "kc.config.args";
    public static final String NAME = "CliConfigSource";
    private static final String ARG_SEPARATOR = ";;";
    private static final Pattern ARG_SPLIT = Pattern.compile(";;");
    private static final Pattern ARG_KEY_VALUE_SPLIT = Pattern.compile("=");

    protected ConfigArgsConfigSource() {
        super(parseArgument(), NAME, 600);
    }

    public static void setCliArgs(String[] args) {
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
        return System.getProperty(CLI_ARGS, "");
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

    private static Map<String, String> parseArgument() {
        String rawArgs = getRawConfigArgs();
        
        if (rawArgs == null || "".equals(rawArgs.trim())) {
            return Collections.emptyMap();
        }

        Map<String, String> properties = new HashMap<>();

        parseConfigArgs(new BiConsumer<String, String>() {
            @Override
            public void accept(String key, String value) {
                key = NS_KEYCLOAK_PREFIX + key.substring(2);

                properties.put(key, value);

                PropertyMapper mapper = PropertyMappers.getMapper(key);

                if (mapper != null) {
                    String to = mapper.getTo();

                    if (to != null) {
                        properties.put(mapper.getTo(), value);
                    }

                    properties.put(mapper.getFrom(), value);
                }
            }
        });

        return properties;
    }

    public static void parseConfigArgs(BiConsumer<String, String> cliArgConsumer) {
        // init here because the class might be loaded by CL without init
        List<String> ignoredArgs = asList("--verbose", "-v", "--help", "-h", AUTO_BUILD_OPTION_LONG, AUTO_BUILD_OPTION_SHORT);
        String rawArgs = getRawConfigArgs();

        if (rawArgs == null || "".equals(rawArgs.trim())) {
            return;
        }

        String[] args = ARG_SPLIT.split(rawArgs);

        for (int i = 0; i < args.length; i++) {
            String arg = args[i];

            if (ignoredArgs.contains(arg)) {
                continue;
            }

            if (!arg.startsWith(ARG_SHORT_PREFIX)) {
                continue;
            }

            String[] keyValue = ARG_KEY_VALUE_SPLIT.split(arg, 2);
            String key = keyValue[0];

            if ("".equals(key.trim())) {
                throw new IllegalArgumentException("Invalid argument key");
            }

            String value;

            if (keyValue.length == 1) {
                if (args.length <= i + 1) {
                    continue;
                }
                value = args[i + 1];
            } else if (keyValue.length == 2) {
                // the argument has a simple value. Eg.: key=pair
                value = keyValue[1];
            } else {
                // to support cases like --db-url=jdbc:mariadb://localhost/kc?a=1
                value = arg.substring(key.length() + 1);
            }

            cliArgConsumer.accept(key, value);
        }
    }
}

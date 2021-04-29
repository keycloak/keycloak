/*
 * Copyright 2019 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.configuration;

import static org.keycloak.configuration.MicroProfileConfigProvider.NS_KEYCLOAK_PREFIX;
import static org.keycloak.configuration.MicroProfileConfigProvider.NS_QUARKUS_PREFIX;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import org.jboss.logging.Logger;

import io.smallrye.config.PropertiesConfigSource;
import org.keycloak.util.Environment;

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

    private static final Logger log = Logger.getLogger(ConfigArgsConfigSource.class);

    private static final Pattern ARG_SPLIT = Pattern.compile(",");
    private static final Pattern ARG_KEY_VALUE_SPLIT = Pattern.compile("=");
    private static final String ARG_PREFIX = "--";
    private static final Pattern DOT_SPLIT = Pattern.compile("\\.");

    ConfigArgsConfigSource() {
        // higher priority over default Quarkus config sources
        super(parseArgument(), "CliConfigSource", 500);
    }

    @Override
    public String getValue(String propertyName) {
        String prefix = null;
        
        // we only care about runtime args passed when executing the CLI, no need to check if the property is prefixed with a profile
        if (propertyName.startsWith(NS_KEYCLOAK_PREFIX)) {
            prefix = NS_KEYCLOAK_PREFIX;
        } else if (propertyName.startsWith(NS_QUARKUS_PREFIX)) {
            prefix = NS_QUARKUS_PREFIX;
        }
        
        // we only recognize properties within keycloak and quarkus namespaces
        if (prefix == null) {
            return null;
        }
        
        String[] parts = DOT_SPLIT.split(propertyName.substring(propertyName.indexOf(prefix) + prefix.length()));

        return super.getValue(prefix + String.join("-", parts));
    }

    private static Map<String, String> parseArgument() {
        String args = Environment.getConfigArgs();
        
        if (args == null || "".equals(args.trim())) {
            log.trace("No command-line arguments provided");
            return Collections.emptyMap();
        }
        
        Map<String, String> properties = new HashMap<>();

        for (String arg : ARG_SPLIT.split(args)) {
            if (!arg.startsWith(ARG_PREFIX)) {
                throw new IllegalArgumentException("Invalid argument format [" + arg + "], arguments must start with '--'");
            }

            String[] keyValue = ARG_KEY_VALUE_SPLIT.split(arg);
            String key = keyValue[0];
            
            if ("".equals(key.trim())) {
                throw new IllegalArgumentException("Invalid argument key");
            }
            
            String value;
            
            if (keyValue.length == 1) {
                continue;
            } else if (keyValue.length == 2) {
                // the argument has a simple value. Eg.: key=pair
                value = keyValue[1];
            } else {
                // to support cases like --db-url=jdbc:mariadb://localhost/kc?a=1
                value = arg.substring(key.length() + 1);
            }
            
            key = NS_KEYCLOAK_PREFIX + key.substring(2);
            
            log.tracef("Adding property [%s=%s] from command-line", key, value);
            
            properties.put(key, value);
        }
        
        return properties;
    }
}

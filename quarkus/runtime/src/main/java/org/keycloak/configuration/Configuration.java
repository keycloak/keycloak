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

package org.keycloak.configuration;

import java.util.Optional;
import java.util.function.Function;

import io.smallrye.config.ConfigValue;
import io.smallrye.config.SmallRyeConfig;
import io.smallrye.config.SmallRyeConfigProviderResolver;
import org.keycloak.util.Environment;

/**
 * The entry point for accessing the server configuration
 */
public final class Configuration {

    private static volatile SmallRyeConfig CONFIG;

    public static synchronized SmallRyeConfig getConfig() {
        if (CONFIG == null) {
            CONFIG = (SmallRyeConfig) SmallRyeConfigProviderResolver.instance().getConfig();
        }
        return CONFIG;
    }

    public static String getBuiltTimeProperty(String name) {
        String value = KeycloakConfigSourceProvider.PERSISTED_CONFIG_SOURCE.getValue(name);

        if (value == null) {
            String profile = Environment.getProfile();

            if (profile == null) {
                profile = getConfig().getRawValue("kc.profile");
            }

            value = KeycloakConfigSourceProvider.PERSISTED_CONFIG_SOURCE.getValue("%" + profile + "." + name);
        }

        return value;
    }

    public static String getRawValue(String propertyName) {
        return getConfig().getRawValue(propertyName);
    }

    public static Iterable<String> getPropertyNames() {
        return getConfig().getPropertyNames();
    }

    public static ConfigValue getConfigValue(String propertyName) {
        return getConfig().getConfigValue(propertyName);
    }

    public static Optional<String> getOptionalValue(String name) {
        return getConfig().getOptionalValue(name, String.class);
    }

    public static Optional<Boolean> getOptionalBooleanValue(String name) {
        return getConfig().getOptionalValue(name, String.class).map(new Function<String, Boolean>() {
            @Override
            public Boolean apply(String s) {
                return Boolean.parseBoolean(s);
            }
        });
    }
}

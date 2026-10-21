/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.common.util;

import java.util.Collections;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;

/**
 * <p>An utility class to resolve the value of a key based on the environment variables
 * and system properties available at runtime. In most cases, you do not want to resolve whatever system variable is available at runtime but specify which ones
 * can be used when resolving placeholders.
 *
 * <p>To resolve to an environment variable, the key must have a format like {@code env.<key>} where {@code key} is the name of an environment variable.
 * For system properties, there is no specific format and the value is resolved from a system property that matches the key.
 *
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class SystemEnvProperties extends Properties {

    /**
     * <p>An variation of {@link SystemEnvProperties} that gives unrestricted access to any system variable available at runtime.
     * Most of the time you don't want to use this class but favor creating a {@link SystemEnvProperties} instance that
     * filters which system variables should be available at runtime.
     */
    public static final SystemEnvProperties UNFILTERED = new SystemEnvProperties(Collections.emptySet()) {
        @Override
        protected boolean isAllowed(String key) {
            return true;
        }
    };

    private final Set<String> allowedSystemVariables;

    /**
     * Creates a new instance where system variables where only specific keys can be resolved from system variables.
     *
     * @param allowedSystemVariables the keys of system variables that should be available at runtime
     */
    public SystemEnvProperties(Set<String> allowedSystemVariables) {
        this.allowedSystemVariables = Optional.ofNullable(allowedSystemVariables).orElse(Collections.emptySet());
    }

    @Override
    public String getProperty(String key) {
        if (key.startsWith("env.")) {
            String envKey = key.substring(4);
            return isAllowed(envKey) ? System.getenv().get(envKey) : null;
        } else {
            return isAllowed(key) ? System.getProperty(key) : null;
        }
    }

    @Override
    public String getProperty(String key, String defaultValue) {
        String value = getProperty(key);
        return value != null ? value : defaultValue;
    }

    protected boolean isAllowed(String key) {
        return allowedSystemVariables.contains(key);
    }
}

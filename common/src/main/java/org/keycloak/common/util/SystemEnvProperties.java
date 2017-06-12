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
import java.util.Map;
import java.util.Properties;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class SystemEnvProperties extends Properties {

    private final Map<String, String> overrides;

    public SystemEnvProperties(Map<String, String> overrides) {
        this.overrides = overrides;
    }

    public SystemEnvProperties() {
        this.overrides = Collections.EMPTY_MAP;
    }

    @Override
    public String getProperty(String key) {
        if (overrides.containsKey(key)) {
            return overrides.get(key);
        } else if (key.startsWith("env.")) {
            return System.getenv().get(key.substring(4));
        } else {
            return System.getProperty(key);
        }
    }

    @Override
    public String getProperty(String key, String defaultValue) {
        String value = getProperty(key);
        return value != null ? value : defaultValue;
    }

}

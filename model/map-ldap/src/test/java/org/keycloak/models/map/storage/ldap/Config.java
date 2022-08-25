/*
 * Copyright 2022. Red Hat, Inc. and/or its affiliates
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

package org.keycloak.models.map.storage.ldap;

import java.util.HashMap;
import java.util.Map;

/**
 * Simple configuration holder that allows for unit testing.
 */
public class Config extends org.keycloak.Config.SystemPropertiesScope {

    private final Map<String, String> props;

    private Config(String prefix, Map<String, String> props) {
        super(prefix);
        this.props = props;
    }

    public Config() {
        this("", new HashMap<>());
    }

    public void put(String key, String value) {
        props.put(key, value);
    }

    @Override
    public String get(String key, String defaultValue) {
        String val = props.get(prefix + key);
        if (val != null) {
            return val;
        }
        return super.get(key, defaultValue);
    }

    @Override
    public org.keycloak.Config.Scope scope(String... scope) {
        StringBuilder sb = new StringBuilder();
        sb.append(prefix).append(".");
        for (String s : scope) {
            sb.append(s);
            sb.append(".");
        }
        return new Config(sb.toString(), props);
    }
}

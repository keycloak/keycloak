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

import static io.smallrye.config.common.utils.StringUtil.replaceNonAlphanumericByUnderscores;
import static org.keycloak.quarkus.runtime.configuration.Configuration.getMappedPropertyName;

import java.util.HashMap;
import java.util.Map;

import io.smallrye.config.EnvConfigSource;

public class KcEnvConfigSource extends EnvConfigSource {

    public KcEnvConfigSource() {
        super(buildProperties(), 500);
    }

    private static Map<String, String> buildProperties() {
        Map<String, String> properties = new HashMap<>();
        String kcPrefix = replaceNonAlphanumericByUnderscores(MicroProfileConfigProvider.NS_KEYCLOAK_PREFIX.toUpperCase());

        for (Map.Entry<String, String> entry : System.getenv().entrySet()) {
            String key = entry.getKey();

            if (key.startsWith(kcPrefix)) {
                properties.put(getMappedPropertyName(key), entry.getValue());
            }
        }

        return properties;
    }

    @Override
    public String getName() {
        return "KcEnvVarConfigSource";
    }
}

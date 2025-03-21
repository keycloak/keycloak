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
import static org.keycloak.quarkus.runtime.configuration.MicroProfileConfigProvider.NS_KEYCLOAK_PREFIX;

import java.util.HashMap;
import java.util.Map;

import io.smallrye.config.PropertiesConfigSource;

import org.keycloak.quarkus.runtime.configuration.mappers.PropertyMapper;
import org.keycloak.quarkus.runtime.configuration.mappers.PropertyMappers;
import org.keycloak.quarkus.runtime.configuration.mappers.WildcardPropertyMapper;

// Not extending EnvConfigSource as it's too smart for our own good. It does unnecessary mapping of provided keys
// leading to e.g. duplicate entries (like kc.db-password and kc.db.password), or incorrectly handling getters due to
// how equals() is implemented. We don't need that here as we do our own mapping.
public class KcEnvConfigSource extends PropertiesConfigSource {

    public static final String NAME = "KcEnvVarConfigSource";

    public KcEnvConfigSource() {
        super(buildProperties(), NAME, 500);
    }

    private static Map<String, String> buildProperties() {
        Map<String, String> properties = new HashMap<>();
        String kcPrefix = replaceNonAlphanumericByUnderscores(NS_KEYCLOAK_PREFIX.toUpperCase());

        for (Map.Entry<String, String> entry : System.getenv().entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();

            if (key.startsWith(kcPrefix)) {
                String transformedKey = NS_KEYCLOAK_PREFIX + key.substring(kcPrefix.length()).toLowerCase().replace("_", "-");

                PropertyMapper<?> mapper = PropertyMappers.getMapper(transformedKey);

                if (mapper != null && mapper.hasWildcard()) {
                    // special case - wildcards don't follow the default conversion rule
                    transformedKey = ((WildcardPropertyMapper<?>) mapper).getKcKeyForEnvKey(key, transformedKey)
                            .orElseThrow();
                }

                properties.put(transformedKey, value);
            }
        }

        return properties;
    }
}

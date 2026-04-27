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

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.keycloak.quarkus.runtime.configuration.mappers.PropertyMapper;
import org.keycloak.quarkus.runtime.configuration.mappers.PropertyMappers;
import org.keycloak.quarkus.runtime.configuration.mappers.WildcardPropertyMapper;

import io.smallrye.config.EnvConfigSource;
import io.smallrye.config.PropertiesConfigSource;
import org.eclipse.microprofile.config.spi.ConfigSource;

import static org.keycloak.quarkus.runtime.configuration.MicroProfileConfigProvider.NS_KEYCLOAK_PREFIX;

import static io.smallrye.config.common.utils.StringUtil.replaceNonAlphanumericByUnderscores;

// Not extending EnvConfigSource as it's too smart for our own good. It does unnecessary mapping of provided keys
// leading to e.g. duplicate entries (like kc.db-password and kc.db.password), or incorrectly handling getters due to
// how equals() is implemented. We don't need that here as we do our own mapping.
public class KcEnvConfigSource extends PropertiesConfigSource {

    public static final String NAME = "KcEnvVarConfigSource";
    public static final String KCKEY_PREFIX = "KCKEY_";
    public static final String KCRAW_PREFIX = "KCRAW_";

    static final Map<String, String> ENV_OVERRIDE = new HashMap<String, String>();

    public KcEnvConfigSource(Map<String, String> env) {
        super(buildProperties(env), NAME, 500);
    }

    private static Map<String, String> buildProperties(Map<String, String> env) {
        Map<String, String> properties = new HashMap<>();
        String kcPrefix = replaceNonAlphanumericByUnderscores(NS_KEYCLOAK_PREFIX.toUpperCase());

        for (Map.Entry<String, String> entry : env.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();

            if (!(key.startsWith(kcPrefix) || key.startsWith(KCRAW_PREFIX))) {
                continue;
            }

            boolean isRaw = key.startsWith(KCRAW_PREFIX);
            String baseKey;

            if (isRaw) {
                baseKey = key.substring(KCRAW_PREFIX.length());

                // Fail fast if both KC_ and KCRAW_ are set for the same base key
                if (env.containsKey(kcPrefix + baseKey)) {
                    throw new IllegalArgumentException(
                            "Both " + kcPrefix + baseKey + " and " + KCRAW_PREFIX + baseKey
                                    + " are set. Use only one.");
                }
            } else {
                baseKey = key.substring(kcPrefix.length());
            }

            // Resolve the transformed key
            String transformedKey;
            String actualKey = env.get(KCKEY_PREFIX + baseKey);
            if (actualKey != null) {
                // use the explicit mapping
                transformedKey = NS_KEYCLOAK_PREFIX + actualKey;
            } else {
                // determine the mapping by convention / wildcard handling
                transformedKey = NS_KEYCLOAK_PREFIX + baseKey.toLowerCase().replace("_", "-");

                PropertyMapper<?> mapper = PropertyMappers.getMapper(transformedKey);

                if (mapper != null && mapper.hasWildcard()) {
                    // special case - wildcards don't follow the default conversion rule
                    WildcardPropertyMapper<?> wildcardPropertyMapper = (WildcardPropertyMapper<?>) mapper;

                    transformedKey = wildcardPropertyMapper.getKcKeyForEnvKey(key, transformedKey)
                            .orElseThrow();
                }
            }

            // KCRAW_ values escape $ as $$ to prevent SmallRye Config variable interpolation
            // Then replace \$ with \\ to prevent SmallRye's escapeDollarIfExists from consuming backslashes before dollars
            properties.put(transformedKey, isRaw ? value.replace("$", "$$").replace("\\$", "\\\\") : value);
        }

        return properties;
    }

    public static Collection<ConfigSource> getConfigSources() {
        Map<String, String> env = System.getenv();

        if (ENV_OVERRIDE.isEmpty()) {
            return List.of(new KcEnvConfigSource(env));
        }

        env = new HashMap<String, String>(env);
        env.putAll(ENV_OVERRIDE);

        return List.of(new KcEnvConfigSource(env), new EnvConfigSource(ENV_OVERRIDE, EnvConfigSource.ORDINAL + 1));
    }
}

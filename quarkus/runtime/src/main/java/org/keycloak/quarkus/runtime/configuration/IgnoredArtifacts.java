/*
 * Copyright 2023 Red Hat, Inc. and/or its affiliates
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

import org.keycloak.common.Profile;
import org.keycloak.config.StorageOptions;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static java.util.Collections.emptySet;
import static org.keycloak.quarkus.runtime.Environment.getCurrentOrCreateFeatureProfile;

/**
 * Ignore particular artifacts based on build configuration
 */
public class IgnoredArtifacts {

    public static Set<String> getDefaultIgnoredArtifacts() {
        return new Builder()
                .append(fips())
                .append(storage())
                .build();
    }

    // FIPS
    public static final Set<String> FIPS_ENABLED = Set.of(
            "org.bouncycastle:bcprov-jdk18on",
            "org.bouncycastle:bcpkix-jdk18on",
            "org.bouncycastle:bcutil-jdk18on",
            "org.keycloak:keycloak-crypto-default"
    );

    public static final Set<String> FIPS_DISABLED = Set.of(
            "org.keycloak:keycloak-crypto-fips1402",
            "org.bouncycastle:bc-fips",
            "org.bouncycastle:bctls-fips",
            "org.bouncycastle:bcpkix-fips"
    );

    private static Set<String> fips() {
        final Profile profile = getCurrentOrCreateFeatureProfile();
        boolean isFipsEnabled = profile.getFeatures().get(Profile.Feature.FIPS);

        return isFipsEnabled ? FIPS_ENABLED : FIPS_DISABLED;
    }

    // Map Store
    public static final Set<String> MAP_STORE = Set.of(
            "org.keycloak:keycloak-model-map-jpa",
            "org.keycloak:keycloak-model-map-hot-rod",
            "org.keycloak:keycloak-model-map",
            "org.keycloak:keycloak-model-map-file"
    );

    private static Set<String> storage() {
        Optional<String> storage = Configuration.getOptionalValue(
                MicroProfileConfigProvider.NS_KEYCLOAK_PREFIX + StorageOptions.STORAGE.getKey());

        return storage.isEmpty() ? MAP_STORE : emptySet();
    }

    /**
     * Builder for artifacts aggregation
     */
    private static final class Builder {
        private final Set<String> finalIgnoredArtifacts;

        public Builder() {
            this.finalIgnoredArtifacts = new HashSet<>();
        }

        public Builder append(Set<String> ignoredArtifacts) {
            finalIgnoredArtifacts.addAll(ignoredArtifacts);
            return this;
        }

        public Set<String> build() {
            return finalIgnoredArtifacts;
        }
    }
}

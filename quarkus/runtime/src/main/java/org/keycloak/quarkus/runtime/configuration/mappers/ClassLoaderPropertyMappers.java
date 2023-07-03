package org.keycloak.quarkus.runtime.configuration.mappers;

import static org.keycloak.quarkus.runtime.Environment.getCurrentOrCreateFeatureProfile;
import static org.keycloak.quarkus.runtime.configuration.mappers.PropertyMapper.fromOption;

import io.smallrye.config.ConfigSourceInterceptorContext;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.keycloak.common.Profile;
import org.keycloak.common.Profile.Feature;
import org.keycloak.config.ClassLoaderOptions;
import org.keycloak.config.StorageOptions;
import org.keycloak.quarkus.runtime.Environment;
import org.keycloak.quarkus.runtime.configuration.Configuration;
import org.keycloak.quarkus.runtime.configuration.MicroProfileConfigProvider;

final class ClassLoaderPropertyMappers {

    private ClassLoaderPropertyMappers(){}

    public static PropertyMapper[] getMappers() {
        return new PropertyMapper[] {
                fromOption(ClassLoaderOptions.IGNORE_ARTIFACTS)
                        .to("quarkus.class-loading.removed-artifacts")
                        .transformer(ClassLoaderPropertyMappers::resolveIgnoredArtifacts)
                        .build()
        };
    }

    private static Optional<String> resolveIgnoredArtifacts(Optional<String> value, ConfigSourceInterceptorContext context) {
        if (Environment.isRebuildCheck() || Environment.isRebuild()) {
            Profile profile = getCurrentOrCreateFeatureProfile();
            Set<String> ignoredArtifacts = new HashSet<>();

            if (profile.getFeatures().get(Feature.FIPS)) {
                ignoredArtifacts.addAll(List.of(
                        "org.bouncycastle:bcprov-jdk15on", "org.bouncycastle:bcpkix-jdk15on", "org.bouncycastle:bcutil-jdk15on", "org.keycloak:keycloak-crypto-default"));
            } else {
                ignoredArtifacts.addAll(List.of(
                        "org.keycloak:keycloak-crypto-fips1402", "org.bouncycastle:bc-fips", "org.bouncycastle:bctls-fips", "org.bouncycastle:bcpkix-fips"));
            }

            Optional<String> storage = Configuration.getOptionalValue(
                    MicroProfileConfigProvider.NS_KEYCLOAK_PREFIX + StorageOptions.STORAGE.getKey());

            if (storage.isEmpty()) {
                ignoredArtifacts.add("org.keycloak:keycloak-model-map-jpa");
                ignoredArtifacts.add("org.keycloak:keycloak-model-map-hot-rod");
                ignoredArtifacts.add("org.keycloak:keycloak-model-map");
                ignoredArtifacts.add("org.keycloak:keycloak-model-map-file");
            }

            return Optional.of(String.join(",", ignoredArtifacts));
        }

        return value;
    }
}

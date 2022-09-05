package org.keycloak.quarkus.runtime.configuration.mappers;

import org.keycloak.common.Profile;
import org.keycloak.config.FeatureOptions;
import org.keycloak.config.StorageOptions;
import org.keycloak.quarkus.runtime.configuration.Configuration;
import org.keycloak.quarkus.runtime.configuration.MicroProfileConfigProvider;

import static java.util.Optional.of;
import static org.keycloak.config.StorageOptions.STORAGE;
import static org.keycloak.quarkus.runtime.configuration.MicroProfileConfigProvider.NS_KEYCLOAK_PREFIX;
import static org.keycloak.quarkus.runtime.configuration.mappers.PropertyMapper.fromOption;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import io.smallrye.config.ConfigSourceInterceptorContext;

final class FeaturePropertyMappers {

    private FeaturePropertyMappers() {
    }

    public static PropertyMapper[] getMappers() {
        return new PropertyMapper[] {
                fromOption(FeatureOptions.FEATURES)
                        .paramLabel("feature")
                        .transformer(FeaturePropertyMappers::transformFeatures)
                        .build(),
                fromOption(FeatureOptions.FEATURES_DISABLED)
                        .paramLabel("feature")
                        .build()
        };
    }

    private static Optional<String> transformFeatures(Optional<String> features, ConfigSourceInterceptorContext context) {
        if (Configuration.getOptionalValue(NS_KEYCLOAK_PREFIX.concat(STORAGE.getKey())).isEmpty()) {
            return features;
        }

        Set<String> featureSet = new HashSet<>(List.of(features.orElse("").split(",")));

        featureSet.add(Profile.Feature.MAP_STORAGE.name().replace('_', '-'));

        return of(String.join(",", featureSet));
    }
}

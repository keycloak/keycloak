package org.keycloak.quarkus.runtime.configuration.mappers;

import org.keycloak.common.Profile;
import org.keycloak.common.Profile.Feature;
import org.keycloak.config.FeatureOptions;
import org.keycloak.quarkus.runtime.cli.PropertyException;

import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.keycloak.quarkus.runtime.configuration.mappers.PropertyMapper.fromOption;

public final class FeaturePropertyMappers {

    private static Pattern VERSIONED_PATTERN = Pattern.compile("([^:]+):v(\\d+)");

    private FeaturePropertyMappers() {
    }

    public static PropertyMapper[] getMappers() {
        return new PropertyMapper[] {
                fromOption(FeatureOptions.FEATURES)
                        .paramLabel("feature")
                        .validator((mapper, value) -> validateEnabledFeatures(value.getValue()))
                        .build(),
                fromOption(FeatureOptions.FEATURES_DISABLED)
                        .paramLabel("feature")
                        .build()
        };
    }

    public static void validateEnabledFeatures(String s) {
        Stream.of(s.split(",")).forEach(feature -> {
            if (!Profile.getFeatureVersions(feature).isEmpty()) {
                return;
            }
            if (feature.equals(Profile.Feature.Type.PREVIEW.name().toLowerCase())) {
                return;
            }
            Matcher matcher = VERSIONED_PATTERN.matcher(feature);
            if (!matcher.matches()) {
                if (feature.contains(":")) {
                    throw new PropertyException(String.format(
                            "%s has an invalid format for enabling a feature, expected format is feature:v{version}, e.g. docker:v1",
                            feature));
                }
                throw new PropertyException(String.format("%s is an unrecognized feature, it should be one of %s", feature,
                        FeatureOptions.getFeatureValues(false)));
            }
            String unversionedFeature = matcher.group(1);
            Set<Feature> featureVersions = Profile.getFeatureVersions(unversionedFeature);
            if (featureVersions.isEmpty()) {
                throw new PropertyException(String.format("%s has an unrecognized feature, it should be one of %s",
                        feature, FeatureOptions.getFeatureValues(false)));
            }
            int version = Integer.parseInt(matcher.group(2));
            if (!featureVersions.stream().anyMatch(f -> f.getVersion() == version)) {
                throw new PropertyException(
                        String.format("%s has an unrecognized feature version, it should be one of %s", feature,
                                featureVersions.stream().map(Feature::getVersion).map(String::valueOf).collect(Collectors.toList())));
            }
        });
    }
}

package org.keycloak.quarkus.runtime.configuration.mappers;

import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.keycloak.common.Profile;
import org.keycloak.common.Profile.Feature;
import org.keycloak.config.FeatureOptions;
import org.keycloak.quarkus.runtime.cli.PropertyException;

import static org.keycloak.quarkus.runtime.configuration.mappers.PropertyMapper.fromOption;

public final class FeaturePropertyMappers implements PropertyMapperGrouping {
    private static final Pattern VERSION_SUFFIX_PATTERN = Pattern.compile("^v(\\d+)$");
    private static final Pattern VERSIONED_PATTERN = Pattern.compile("([^:]+):v(\\d+)");

    @Override
    public List<PropertyMapper<?>> getPropertyMappers() {
        return List.of(
                fromOption(FeatureOptions.FEATURES)
                        .paramLabel("feature")
                        .validator(FeaturePropertyMappers::validateEnabledFeature)
                        .build(),
                fromOption(FeatureOptions.FEATURES_DISABLED)
                        .paramLabel("feature")
                        .build(),
                fromOption(FeatureOptions.FEATURE)
                        .paramLabel("enabled|disabled|vX(X is version)")
                        .wildcardKeysValidator(FeaturePropertyMappers::validateSingleFeature)
                        .build()
        );
    }

    public static void validateSingleFeature(String feature, String value) {
        if (!Profile.getAllUnversionedFeatureNames().contains(feature)) {
            throw new PropertyException("'%s' is an unrecognized feature, it should be one of %s".formatted(feature, FeatureOptions.getFeatureValues(false, false)));
        }

        Matcher matcher = VERSION_SUFFIX_PATTERN.matcher(value);
        if (matcher.matches()) {
            int version = Integer.parseInt(matcher.group(1));
            validateFeatureVersions(feature, version);
        } else if (!value.equals("enabled") && !value.equals("disabled")) {
            throw new PropertyException("Wrong value for feature '%s': %s. You can specify either 'enabled', 'disabled', or specific version (lowercase) that will be enabled".formatted(feature, value));
        }
    }

    public static void validateEnabledFeature(String feature) {
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
            throw new PropertyException(String.format("'%s' is an unrecognized feature, it should be one of %s", feature,
                    FeatureOptions.getFeatureValues(false)));
        }
        String unversionedFeature = matcher.group(1);
        int version = Integer.parseInt(matcher.group(2));
        validateFeatureVersions(unversionedFeature, version);
    }

    private static void validateFeatureVersions(String feature, int version) {
        Set<Feature> featureVersions = Profile.getFeatureVersions(feature);
        if (featureVersions.isEmpty() || featureVersions.stream().noneMatch(f -> f.getVersion() == version)) {
            throw new PropertyException(
                    String.format("Feature '%s' has an unrecognized feature version, it should be one of %s", feature,
                            featureVersions.stream().map(Feature::getVersion).map(String::valueOf).collect(Collectors.toList())));
        }
    }
}

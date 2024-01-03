package org.keycloak.config;

import org.keycloak.common.Profile;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class FeatureOptions {

    public static final Option<List> FEATURES = new OptionBuilder("features", List.class, Profile.Feature.class)
            .category(OptionCategory.FEATURE)
            .description("Enables a set of one or more features.")
            .defaultValue(Optional.empty())
            .expectedValues(() -> getFeatureValues(true))
            .buildTime(true)
            .build();

    public static final Option FEATURES_DISABLED = new OptionBuilder("features-disabled", List.class, Profile.Feature.class)
            .category(OptionCategory.FEATURE)
            .description("Disables a set of one or more features.")
            .expectedValues(() -> getFeatureValues(false))
            .buildTime(true)
            .build();

    public static List<String> getFeatureValues(boolean includeVersions) {
        List<String> features = new ArrayList<>();

        if (includeVersions) {
            Profile.getAllUnversionedFeatureNames().forEach(f -> {
                features.add(f + "[:" + Profile.getFeatureVersions(f).stream().sorted().map(v -> "v" + v.getVersion())
                        .collect(Collectors.joining(",")) + "]");
            });
        } else {
            features.addAll(Profile.getAllUnversionedFeatureNames());
        }

        features.add(Profile.Feature.Type.PREVIEW.name().toLowerCase());

        Collections.sort(features);

        return features;
    }
}

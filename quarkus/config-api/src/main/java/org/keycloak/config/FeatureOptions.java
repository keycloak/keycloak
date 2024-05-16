package org.keycloak.config;

import org.keycloak.common.Profile;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class FeatureOptions {

    public static final Option<List<String>> FEATURES = OptionBuilder.listOptionBuilder("features", String.class)
            .category(OptionCategory.FEATURE)
            .description("Enables a set of one or more features.")
            .defaultValue(Optional.empty())
            .expectedValues(getFeatureValues(true))
            .buildTime(true)
            .build();

    public static final Option<List<String>> FEATURES_DISABLED = OptionBuilder.listOptionBuilder("features-disabled", String.class)
            .category(OptionCategory.FEATURE)
            .description("Disables a set of one or more features.")
            .expectedValues(getFeatureValues(false))
            .buildTime(true)
            .build();

    public static List<String> getFeatureValues(boolean toEnable) {
        List<String> features = new ArrayList<>();

        if (toEnable) {
            Profile.getAllUnversionedFeatureNames().forEach(f -> {
                features.add(f + "[:" + Profile.getFeatureVersions(f).stream().sorted().map(v -> "v" + v.getVersion())
                        .collect(Collectors.joining(",")) + "]");
            });
        } else {
            features.addAll(Profile.getDisableableUnversionedFeatureNames());
        }

        features.add(Profile.Feature.Type.PREVIEW.name().toLowerCase());

        Collections.sort(features);

        return features;
    }
}

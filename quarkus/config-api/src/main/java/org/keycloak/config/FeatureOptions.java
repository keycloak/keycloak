package org.keycloak.config;

import org.keycloak.common.Feature;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class FeatureOptions {

    public static final Option<List> FEATURES = new OptionBuilder("features", List.class, Feature.class)
            .category(OptionCategory.FEATURE)
            .description("Enables a set of one or more features.")
            .expectedValues(FeatureOptions::getFeatureValues)
            .defaultValue(Optional.empty())
            .buildTime(true)
            .build();

    public static final Option FEATURES_DISABLED = new OptionBuilder("features-disabled", List.class, Feature.class)
            .category(OptionCategory.FEATURE)
            .description("Disables a set of one or more features.")
            .expectedValues(FeatureOptions::getFeatureValues)
            .buildTime(true)
            .build();

    private static List<String> getFeatureValues() {
        List<String> features = new ArrayList<>();

        for (Feature value : Feature.values()) {
            features.add(value.name().toLowerCase().replace('_', '-'));
        }

        features.add(Feature.Type.PREVIEW.name().toLowerCase());

        return features;
    }
}

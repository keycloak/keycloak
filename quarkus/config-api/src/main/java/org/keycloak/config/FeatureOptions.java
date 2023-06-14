package org.keycloak.config;

import org.keycloak.common.Profile;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class FeatureOptions {

    public static final Option<List> FEATURES = new OptionBuilder("features", List.class, Profile.Feature.class)
            .category(OptionCategory.FEATURE)
            .description("Enables a set of one or more features.")
            .expectedValues(FeatureOptions::getFeatureValues)
            .defaultValue(Optional.empty())
            .buildTime(true)
            .build();

    public static final Option FEATURES_DISABLED = new OptionBuilder("features-disabled", List.class, Profile.Feature.class)
            .category(OptionCategory.FEATURE)
            .description("Disables a set of one or more features.")
            .expectedValues(FeatureOptions::getFeatureValues)
            .buildTime(true)
            .build();

    private static List<String> getFeatureValues() {
        List<String> features = new ArrayList<>();

        for (Profile.Feature value : Profile.Feature.values()) {
            features.add(value.getKey());
        }

        features.add(Profile.Feature.Type.PREVIEW.name().toLowerCase());

        Collections.sort(features);

        return features;
    }
}

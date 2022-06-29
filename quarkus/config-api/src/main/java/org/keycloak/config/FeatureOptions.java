package org.keycloak.config;

import org.keycloak.common.Profile;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class FeatureOptions {

    public static final Option<List> FEATURES = new OptionBuilder("features", List.class, Profile.Feature.class)
            .category(OptionCategory.FEATURE)
            .description("Enables a set of one or more features.")
            .expectedStringValues(getFeatureValues())
            .defaultValue(Optional.empty())
            .buildTime(true)
            .build();

    public static final Option FEATURES_DISABLED = new OptionBuilder("features-disabled", List.class, Profile.Feature.class)
            .category(OptionCategory.FEATURE)
            .description("Disables a set of one or more features.")
            .expectedStringValues(getFeatureValues())
            .buildTime(true)
            .build();

    private static List<String> getFeatureValues() {
        List<String> features = new ArrayList<>();

        for (Profile.Feature value : Profile.Feature.values()) {
            features.add(value.name().toLowerCase().replace('_', '-'));
        }

        features.add(Profile.Type.PREVIEW.name().toLowerCase());

        return features;
    }

    public static final List<Option<?>> ALL_OPTIONS = new ArrayList<>();

    static {
        ALL_OPTIONS.add(FEATURES);
        ALL_OPTIONS.add(FEATURES_DISABLED);
    }
}

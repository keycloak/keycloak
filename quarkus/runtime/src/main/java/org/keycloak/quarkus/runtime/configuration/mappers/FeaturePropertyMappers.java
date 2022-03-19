package org.keycloak.quarkus.runtime.configuration.mappers;

import java.util.ArrayList;
import java.util.List;
import org.keycloak.common.Profile;

final class FeaturePropertyMappers {

    private FeaturePropertyMappers() {
    }

    public static PropertyMapper[] getMappers() {
        return new PropertyMapper[] {
                builder()
                        .from("features")
                        .description("Enables a set of one or more features.")
                        .expectedValues(getFeatureValues())
                        .paramLabel("feature")
                        .build(),
                builder()
                        .from("features-disabled")
                        .expectedValues(getFeatureValues())
                        .paramLabel("feature")
                        .description("Disables a set of one or more features.")
                        .build()
        };
    }

    private static List<String> getFeatureValues() {
        List<String> features = new ArrayList<>();

        for (Profile.Feature value : Profile.Feature.values()) {
            features.add(value.name().toLowerCase().replace('_', '-'));
        }

        features.add(Profile.Type.PREVIEW.name().toLowerCase());

        return features;
    }

    private static PropertyMapper.Builder builder() {
        return PropertyMapper.builder(ConfigCategory.FEATURE).isBuildTimeProperty(true);
    }
}

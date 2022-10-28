package org.keycloak.guides.maven;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class Features {

    private List<Feature> features;

    public Features() {
        this.features = Arrays.stream(org.keycloak.common.Feature.values())
                .filter(f -> !f.getType().equals(org.keycloak.common.Feature.Type.EXPERIMENTAL))
                .map(f -> new Feature(f))
                .sorted(Comparator.comparing(Feature::getName))
                .collect(Collectors.toList());
    }

    public List<Feature> getSupported() {
        return features.stream().filter(f -> f.getType().equals(org.keycloak.common.Feature.Type.DEFAULT)).collect(Collectors.toList());
    }

    public List<Feature> getSupportedDisabledByDefault() {
        return features.stream().filter(f -> f.getType().equals(org.keycloak.common.Feature.Type.DISABLED_BY_DEFAULT)).collect(Collectors.toList());
    }

    public List<Feature> getDeprecated() {
        return features.stream().filter(f -> f.getType().equals(org.keycloak.common.Feature.Type.DEPRECATED)).collect(Collectors.toList());
    }

    public List<Feature> getPreview() {
        return features.stream().filter(f -> f.getType().equals(org.keycloak.common.Feature.Type.PREVIEW)).collect(Collectors.toList());
    }

    public class Feature {

        private org.keycloak.common.Feature profileFeature;

        public Feature(org.keycloak.common.Feature profileFeature) {
            this.profileFeature = profileFeature;
        }

        public String getName() {
            return profileFeature.name().toLowerCase().replaceAll("_", "-");
        }

        public String getDescription() {
            return profileFeature.getLabel();
        }

        private org.keycloak.common.Feature.Type getType() {
            return profileFeature.getType();
        }

    }

}

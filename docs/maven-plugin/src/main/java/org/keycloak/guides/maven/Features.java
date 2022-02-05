package org.keycloak.guides.maven;

import org.keycloak.common.Profile;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class Features {

    private List<Feature> features;

    public Features() {
        this.features = Arrays.stream(Profile.Feature.values())
                .filter(f -> !f.getTypeProject().equals(Profile.Type.EXPERIMENTAL))
                .map(f -> new Feature(f))
                .sorted(Comparator.comparing(Feature::getName))
                .collect(Collectors.toList());
    }

    public List<Feature> getSupported() {
        return features.stream().filter(f -> f.getType().equals(Profile.Type.DEFAULT)).collect(Collectors.toList());
    }

    public List<Feature> getSupportedDisabledByDefault() {
        return features.stream().filter(f -> f.getType().equals(Profile.Type.DISABLED_BY_DEFAULT)).collect(Collectors.toList());
    }

    public List<Feature> getDeprecated() {
        return features.stream().filter(f -> f.getType().equals(Profile.Type.DEPRECATED)).collect(Collectors.toList());
    }

    public List<Feature> getPreview() {
        return features.stream().filter(f -> f.getType().equals(Profile.Type.PREVIEW)).collect(Collectors.toList());
    }

    public class Feature {

        private Profile.Feature profileFeature;

        public Feature(Profile.Feature profileFeature) {
            this.profileFeature = profileFeature;
        }

        public String getName() {
            return profileFeature.name().toLowerCase().replaceAll("_", "-");
        }

        public String getDescription() {
            return profileFeature.getLabel();
        }

        private Profile.Type getType() {
            return profileFeature.getTypeProject();
        }

    }

}

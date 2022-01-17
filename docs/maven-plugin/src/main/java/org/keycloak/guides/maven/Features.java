package org.keycloak.guides.maven;

import org.keycloak.common.Profile;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class Features {

    private List<Feature> features;
    private Options options;

    public Features(Options options) {
        this.features = Arrays.stream(Profile.Feature.values())
                .filter(f -> !f.getTypeProject().equals(Profile.Type.EXPERIMENTAL))
                .map(f -> new Feature(f))
                .collect(Collectors.toList());
        this.options = options;
    }

    public List<Feature> getAll() {
        return features;
    }

    public class Feature {

        private Profile.Feature profileFeature;

        public Feature(Profile.Feature profileFeature) {
            this.profileFeature = profileFeature;
        }

        public String getName() {
            return profileFeature.name().toLowerCase();
        }

        public String getDescription() {
            return profileFeature.getLabel();
        }

        public boolean isEnabledByDefault() {
            return profileFeature.getTypeProject().equals(Profile.Type.DEFAULT);
        }

        public String getSupportLevel() {
            switch (profileFeature.getTypeProject()) {
                case DEFAULT:
                case DISABLED_BY_DEFAULT:
                    return "Supported";
                case DEPRECATED:
                    return "Deprecated";
                case PREVIEW:
                    return "Preview";
                case EXPERIMENTAL:
                    return "Experimental";
                default:
                    return "Unknown";
            }
        }

        public Options.Option getOption() {
            return options.getOption("features." + profileFeature.name().toLowerCase());
        }

    }

}

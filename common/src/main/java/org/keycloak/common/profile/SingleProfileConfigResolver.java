package org.keycloak.common.profile;

import org.keycloak.common.Profile;

import java.util.List;

public class SingleProfileConfigResolver implements ProfileConfigResolver {
    private final List<FeatureState> features;

    public SingleProfileConfigResolver(List<FeatureState> features) {
        this.features = features;
    }

    @Override
    public Profile.ProfileName getProfileName() {
        return null;
    }

    @Override
    public FeatureConfig getFeatureConfig(String feature) {
        for (FeatureState f : features) {
            if (f.feature.equals(feature)) {
                return f.state.equals("disabled") ? FeatureConfig.DISABLED : FeatureConfig.ENABLED;
            }
        }
        return FeatureConfig.UNCONFIGURED;
    }

    public static class FeatureState {
        public final String feature;
        public final String state;

        public FeatureState(String feature, String state) {
            this.feature = feature;
            this.state = state;
        }
    }
}

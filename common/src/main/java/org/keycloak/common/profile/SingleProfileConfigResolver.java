package org.keycloak.common.profile;

import org.keycloak.common.Profile;

import java.util.Map;

public class SingleProfileConfigResolver implements ProfileConfigResolver {
    private final Map<String, Boolean> features;

    public SingleProfileConfigResolver(Map<String, Boolean> features) {
        this.features = features;
    }

    @Override
    public Profile.ProfileName getProfileName() {
        return null;
    }

    @Override
    public FeatureConfig getFeatureConfig(String feature) {
        Boolean state = features.get(feature);
        if (state == null) {
            return FeatureConfig.UNCONFIGURED;
        }

        return state ? FeatureConfig.ENABLED : FeatureConfig.DISABLED;
    }
}

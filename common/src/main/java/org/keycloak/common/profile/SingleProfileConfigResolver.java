package org.keycloak.common.profile;

import org.keycloak.common.Profile;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;

// Features configuration based on the option 'feature-<name>'
public class SingleProfileConfigResolver implements ProfileConfigResolver {
    private final Map<String, Boolean> features;

    public SingleProfileConfigResolver(Map<String, Boolean> features) {
        this.features = Optional.ofNullable(features).orElseGet(Collections::emptyMap);
    }

    @Override
    public Profile.ProfileName getProfileName() {
        Boolean state = features.get(Profile.ProfileName.PREVIEW.name().toLowerCase());
        if (state != null && state) {
            return Profile.ProfileName.PREVIEW;
        }

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

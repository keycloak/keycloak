package org.keycloak.common.profile;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;

import org.keycloak.common.Profile;

// Features configuration based on the option 'feature-<name>'
public class SingleProfileConfigResolver implements ProfileConfigResolver {
    private final Map<String, Boolean> features;

    public SingleProfileConfigResolver(Map<String, Boolean> features) {
        this.features = Optional.ofNullable(features).orElseGet(Collections::emptyMap);
    }

    @Override
    public Profile.ProfileName getProfileName() {
        // not supporting profiles yet - see https://github.com/keycloak/keycloak/issues/44003
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

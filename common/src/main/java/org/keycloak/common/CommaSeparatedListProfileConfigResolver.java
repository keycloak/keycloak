package org.keycloak.common;

import org.keycloak.common.profile.ProfileConfigResolver;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

public class CommaSeparatedListProfileConfigResolver implements ProfileConfigResolver {

    private Set<String> enabledFeatures;
    private Set<String> disabledFeatures;

    public CommaSeparatedListProfileConfigResolver(String enabledFeatures, String disabledFeatures) {
        if (enabledFeatures != null) {
            this.enabledFeatures = Arrays.stream(enabledFeatures.split(",")).collect(Collectors.toSet());
        }
        if (disabledFeatures != null) {
            this.disabledFeatures = Arrays.stream(disabledFeatures.split(",")).collect(Collectors.toSet());
        }
    }

    @Override
    public Profile.ProfileValue getProfile() {
        if (enabledFeatures != null && enabledFeatures.contains(Profile.ProfileValue.PREVIEW.name().toLowerCase())) {
            return Profile.ProfileValue.PREVIEW;
        }
        return null;
    }

    @Override
    public Boolean getFeatureConfig(Feature feature) {
        String key = feature.getKey();
        if (enabledFeatures != null && enabledFeatures.contains(key)) {
            return true;
        } else if (disabledFeatures != null && disabledFeatures.contains(key)) {
            return false;
        }
        return null;
    }
}

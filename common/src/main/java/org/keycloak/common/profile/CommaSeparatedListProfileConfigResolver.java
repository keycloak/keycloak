package org.keycloak.common.profile;

import org.keycloak.common.Profile;

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
    public Profile.ProfileName getProfileName() {
        if (enabledFeatures != null && enabledFeatures.contains(Profile.ProfileName.PREVIEW.name().toLowerCase())) {
            return Profile.ProfileName.PREVIEW;
        }
        return null;
    }

    @Override
    public FeatureConfig getFeatureConfig(Profile.Feature feature) {
        String key = feature.getKey();
        if (enabledFeatures != null && enabledFeatures.contains(key)) {
            return FeatureConfig.ENABLED;
        } else if (disabledFeatures != null && disabledFeatures.contains(key)) {
            return FeatureConfig.DISABLED;
        }
        return FeatureConfig.UNCONFIGURED;
    }
}

package org.keycloak.common.profile;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.keycloak.common.Profile;

public class CommaSeparatedListProfileConfigResolver implements ProfileConfigResolver {

    private Set<String> enabledFeatures;
    private Set<String> disabledFeatures;

    public CommaSeparatedListProfileConfigResolver(String enabledFeatures, String disabledFeatures) {
        if (enabledFeatures != null) {
            this.enabledFeatures = new HashSet<>(Arrays.asList(enabledFeatures.split(",")));
        }
        if (disabledFeatures != null) {
            this.disabledFeatures = new HashSet<>(Arrays.asList(disabledFeatures.split(",")));
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
    public FeatureConfig getFeatureConfig(String feature) {
        if (enabledFeatures != null && enabledFeatures.contains(feature)) {
            if (disabledFeatures != null && disabledFeatures.contains(feature)) {
                throw new ProfileException(feature + " is in both the enabled and disabled feature lists.");
            }
            return FeatureConfig.ENABLED;
        }
        if (disabledFeatures != null && disabledFeatures.contains(feature)) {
            return FeatureConfig.DISABLED;
        }
        return FeatureConfig.UNCONFIGURED;
    }
}

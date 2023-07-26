package org.keycloak.common.profile;

import org.keycloak.common.Profile;

public interface ProfileConfigResolver {

    Profile.ProfileName getProfileName();

    FeatureConfig getFeatureConfig(Profile.Feature feature);

    public enum FeatureConfig {
        ENABLED,
        DISABLED,
        UNCONFIGURED
    }

}

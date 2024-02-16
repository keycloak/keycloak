package org.keycloak.common.profile;

import org.keycloak.common.Profile;

public interface ProfileConfigResolver {

    Profile.ProfileName getProfileName();

    FeatureConfig getFeatureConfig(String feature);

    public enum FeatureConfig {
        ENABLED,
        DISABLED,
        UNCONFIGURED
    }

}

package org.keycloak.common.profile;

import org.keycloak.common.Profile;

import java.util.Properties;

public class PropertiesProfileConfigResolver implements ProfileConfigResolver {

    private Properties properties;

    public PropertiesProfileConfigResolver(Properties properties) {
        this.properties = properties;
    }

    @Override
    public Profile.ProfileName getProfileName() {
        String profile = properties.getProperty("keycloak.profile");
        return profile != null ? Profile.ProfileName.valueOf(profile.toUpperCase()) : null;
    }

    @Override
    public FeatureConfig getFeatureConfig(Profile.Feature feature) {
        String config = properties.getProperty("keycloak.profile.feature." + feature.name().toLowerCase());
        if (config != null) {
            switch (config) {
                case "enabled":
                    return FeatureConfig.ENABLED;
                case "disabled":
                    return FeatureConfig.DISABLED;
                default:
                    throw new ProfileException("Invalid config value '" + config + "' for feature " + feature.getKey());
            }
        }
        return FeatureConfig.UNCONFIGURED;
    }
}

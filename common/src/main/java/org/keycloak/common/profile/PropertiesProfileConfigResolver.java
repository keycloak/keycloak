package org.keycloak.common.profile;

import org.keycloak.common.Feature;
import org.keycloak.common.Profile;
import org.keycloak.common.profile.ProfileConfigResolver;
import org.keycloak.common.profile.ProfileException;

import java.util.Properties;

public class PropertiesProfileConfigResolver implements ProfileConfigResolver {

    private Properties properties;

    public PropertiesProfileConfigResolver(Properties properties) {
        this.properties = properties;
    }

    @Override
    public Profile.ProfileValue getProfile() {
        String profile = properties.getProperty("keycloak.profile");
        return profile != null ? Profile.ProfileValue.valueOf(profile.toUpperCase()) : null;
    }

    @Override
    public Boolean getFeatureConfig(Feature feature) {
        String config = properties.getProperty("keycloak.profile.feature." + feature.name().toLowerCase());
        if (config != null) {
            switch (config) {
                case "enabled":
                    return true;
                case "disabled":
                    return false;
                default:
                    throw new ProfileException("Invalid config value '" + config + "' for feature " + feature.getKey());
            }
        }
        return null;
    }
}

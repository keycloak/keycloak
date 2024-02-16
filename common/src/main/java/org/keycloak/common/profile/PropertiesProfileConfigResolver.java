package org.keycloak.common.profile;

import org.keycloak.common.Profile;
import org.keycloak.common.Profile.Feature;

import java.util.Properties;
import java.util.function.UnaryOperator;

public class PropertiesProfileConfigResolver implements ProfileConfigResolver {

    private UnaryOperator<String> getter;

    public PropertiesProfileConfigResolver(Properties properties) {
        this(properties::getProperty);
    }

    public PropertiesProfileConfigResolver(UnaryOperator<String> getter) {
        this.getter = getter;
    }

    @Override
    public Profile.ProfileName getProfileName() {
        String profile = getter.apply("keycloak.profile");
        return profile != null ? Profile.ProfileName.valueOf(profile.toUpperCase()) : null;
    }

    @Override
    public FeatureConfig getFeatureConfig(String feature) {
        String key = getPropertyKey(feature);
        String config = getter.apply(key);
        if (config != null) {
            switch (config) {
                case "enabled":
                    return FeatureConfig.ENABLED;
                case "disabled":
                    return FeatureConfig.DISABLED;
                default:
                    throw new ProfileException("Invalid config value '" + config + "' for feature key " + key);
            }
        }
        return FeatureConfig.UNCONFIGURED;
    }

    public static String getPropertyKey(Feature feature) {
        return getPropertyKey(feature.getKey());
    }

    public static String getPropertyKey(String feature) {
        return "keycloak.profile.feature." + feature.replaceAll("-", "_");
    }
}

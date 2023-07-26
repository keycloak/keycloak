package org.keycloak.common.profile;

import org.keycloak.common.Profile;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

public class PropertiesFileProfileConfigResolver implements ProfileConfigResolver {

    private Properties properties;

    public PropertiesFileProfileConfigResolver() {
        try {
            String jbossServerConfigDir = System.getProperty("jboss.server.config.dir");
            if (jbossServerConfigDir != null) {
                File file = new File(jbossServerConfigDir, "profile.properties");
                if (file.isFile()) {
                    try (FileInputStream is = new FileInputStream(file)) {
                        properties = new Properties();
                        properties.load(is);
                    }
                }
            }
        } catch (IOException e) {
            throw new ProfileException("Failed to load profile propeties file", e);
        }
    }

    @Override
    public Profile.ProfileName getProfileName() {
        if (properties != null) {
            String profile = properties.getProperty("profile");
            if (profile != null) {
                return Profile.ProfileName.valueOf(profile.toUpperCase());
            }
        }
        return null;
    }

    @Override
    public FeatureConfig getFeatureConfig(Profile.Feature feature) {
        if (properties != null) {
            String config = properties.getProperty("feature." + feature.name().toLowerCase());
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
        }
        return FeatureConfig.UNCONFIGURED;
    }
}

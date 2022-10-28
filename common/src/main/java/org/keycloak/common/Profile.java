/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.keycloak.common;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class Profile {

    private static Profile CURRENT;

    private final ProfileValue profile;

    private Map<Feature, Boolean> features;
    private final PropertyResolver propertyResolver;

    public Profile(PropertyResolver resolver) {
        this.propertyResolver = resolver;
        Config config = new Config();

        profile = ProfileValue.valueOf(config.getProfile().toUpperCase());
        features = Arrays.stream(Feature.values()).collect(Collectors.toMap(f -> f, f -> config.isEnabled(f)));

        checkDependenciesAreEnabled(features);
    }

    private void checkDependenciesAreEnabled(Map<Feature, Boolean> features) {
        for (Feature f : features.keySet()) {
            if (f.getDependencies() != null) {
                for (Feature d : f.getDependencies()) {
                    if (!features.get(d)) {
                        throw new ProfileException("Feature " + f.getKey() + " depends on disabled feature " + d.getKey());
                    }
                }
            }
        }
    }

    private static Profile getInstance() {
        if (CURRENT == null) {
            CURRENT = new Profile(null);
        }
        return CURRENT;
    }

    public static void setInstance(Profile instance) {
        CURRENT = instance;
    }

    public static void init() {
        PropertyResolver resolver = null;

        if (CURRENT != null) {
            resolver = CURRENT.propertyResolver;
        }

        CURRENT = new Profile(resolver);
    }

    public static String getName() {
        return getInstance().profile.name().toLowerCase();
    }

    public static Set<Feature> getDisabledFeatures() {
        return getInstance().features.entrySet().stream().filter(e -> !e.getValue()).map(Map.Entry::getKey).collect(Collectors.toSet());
    }

    public static Set<Feature> getPreviewFeatures() {
        return getFeatures(Feature.Type.PREVIEW);
    }

    public static Set<Feature> getExperimentalFeatures() {
        return getFeatures(Feature.Type.EXPERIMENTAL);
    }

    public static Set<Feature> getDeprecatedFeatures() {
        return getFeatures(Feature.Type.DEPRECATED);
    }

    public static Set<Feature> getFeatures(Feature.Type type) {
        return getInstance().features.keySet().stream().filter(f -> f.getType().equals(type)).collect(Collectors.toSet());
    }

    public static boolean isFeatureEnabled(Feature feature) {
        return getInstance().features.get(feature);
    }

    private enum ProfileValue {
        DEFAULT,
        PREVIEW
    }

    public interface PropertyResolver {
        String resolve(String feature);
    }

    private class Config {

        private Properties properties;

        public Config() {
            properties = new Properties();

            try {
                String jbossServerConfigDir = System.getProperty("jboss.server.config.dir");
                if (jbossServerConfigDir != null) {
                    File file = new File(jbossServerConfigDir, "profile.properties");
                    if (file.isFile()) {
                        try (FileInputStream is = new FileInputStream(file)) {
                            properties.load(is);
                        }
                    }
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        public String getProfile() {
            String profile = getProperty("keycloak.profile");
            if (profile != null) {
                return profile;
            }

            profile = properties.getProperty("profile");
            if (profile != null) {
                if (profile.equals("community")) {
                    profile = "default";
                }

                return profile;
            }

            return ProfileValue.DEFAULT.name();
        }

        public Boolean isEnabled(Feature feature) {
            String config = getProperty("keycloak.profile.feature." + feature.name().toLowerCase());

            if (config == null) {
                config = properties.getProperty("feature." + feature.name().toLowerCase());
            }

            if (config == null) {
                switch (feature.getType()) {
                    case DEFAULT:
                        return true;
                    case PREVIEW:
                        return profile.equals(ProfileValue.PREVIEW) ? true : false;
                    case DEPRECATED:
                    case DISABLED_BY_DEFAULT:
                    case EXPERIMENTAL:
                        return false;
                    default:
                        throw new ProfileException("Unknown feature type " + feature.getType());
                }
            } else {
                switch (config) {
                    case "enabled":
                        return true;
                    case "disabled":
                        return false;
                    default:
                        throw new ProfileException("Invalid config value '" + config + "' for feature " + feature.getKey());
                }
            }
        }

        private String getProperty(String name) {
            String value = System.getProperty(name);

            if (value != null) {
                return value;
            }

            if (propertyResolver != null) {
                return propertyResolver.resolve(name);
            }

            return null;
        }
    }

}
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

import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class Profile {

    private static Profile CURRENT;

    private ProfileValue profile;

    private Map<Feature, Boolean> features;

    private Profile(ProfileConfigResolver resolver) {
        profile = resolver != null ? resolver.getProfile() : null;
        if (profile == null) {
            profile = ProfileValue.DEFAULT;
        }

        features = Arrays.stream(Feature.values()).collect(Collectors.toMap(f -> f, f -> isFeatureEnabled(profile, f, resolver)));

        checkDependenciesAreEnabled(features);
    }

    public static void init(ProfileConfigResolver resolver) {
        CURRENT = new Profile(resolver);
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
        return CURRENT;
    }

    public static void setInstance(Profile instance) {
        CURRENT = instance;
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

    public enum ProfileValue {
        DEFAULT,
        PREVIEW
    }

    private static Boolean isFeatureEnabled(ProfileValue profile, Feature feature, ProfileConfigResolver resolver) {
        Boolean config = resolver != null ? resolver.getFeatureConfig(feature) : null;
        if (config != null) {
            return config;
        } else {
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
        }
    }

}
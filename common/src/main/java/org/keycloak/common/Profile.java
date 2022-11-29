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

import org.jboss.logging.Logger;
import org.keycloak.common.profile.ProfileConfigResolver;
import org.keycloak.common.profile.ProfileException;
import org.keycloak.common.profile.PropertiesFileProfileConfigResolver;
import org.keycloak.common.profile.PropertiesProfileConfigResolver;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class Profile {

    public enum Feature {
        AUTHORIZATION("Authorization Service", Type.DEFAULT),

        ACCOUNT_API("Account Management REST API", Type.DEFAULT),
        ACCOUNT2("New Account Management Console", Type.DEFAULT, Feature.ACCOUNT_API),

        ADMIN_FINE_GRAINED_AUTHZ("Fine-Grained Admin Permissions", Type.PREVIEW),

        ADMIN_API("Admin API", Type.DEFAULT),

        @Deprecated
        ADMIN("Legacy Admin Console", Type.DEPRECATED),

        ADMIN2("New Admin Console", Type.DEFAULT, Feature.ADMIN_API),

        DOCKER("Docker Registry protocol", Type.DISABLED_BY_DEFAULT),

        IMPERSONATION("Ability for admins to impersonate users", Type.DEFAULT),

        OPENSHIFT_INTEGRATION("Extension to enable securing OpenShift", Type.PREVIEW),

        SCRIPTS("Write custom authenticators using JavaScript", Type.PREVIEW),

        TOKEN_EXCHANGE("Token Exchange Service", Type.PREVIEW),

        WEB_AUTHN("W3C Web Authentication (WebAuthn)", Type.DEFAULT),

        CLIENT_POLICIES("Client configuration policies", Type.DEFAULT),

        CIBA("OpenID Connect Client Initiated Backchannel Authentication (CIBA)", Type.DEFAULT),

        MAP_STORAGE("New store", Type.EXPERIMENTAL),

        PAR("OAuth 2.0 Pushed Authorization Requests (PAR)", Type.DEFAULT),

        DECLARATIVE_USER_PROFILE("Configure user profiles using a declarative style", Type.PREVIEW),

        DYNAMIC_SCOPES("Dynamic OAuth 2.0 scopes", Type.EXPERIMENTAL),

        CLIENT_SECRET_ROTATION("Client Secret Rotation", Type.PREVIEW),

        STEP_UP_AUTHENTICATION("Step-up Authentication", Type.DEFAULT),

        RECOVERY_CODES("Recovery codes", Type.PREVIEW),

        UPDATE_EMAIL("Update Email Action", Type.PREVIEW),

        JS_ADAPTER("Host keycloak.js and keycloak-authz.js through the Keycloak sever", Type.DEFAULT);

        private final Type type;
        private String label;

        private Set<Feature> dependencies;
        Feature(String label, Type type) {
            this.label = label;
            this.type = type;
        }

        Feature(String label, Type type, Feature... dependencies) {
            this.label = label;
            this.type = type;
            this.dependencies = Arrays.stream(dependencies).collect(Collectors.toSet());
        }

        public String getKey() {
            return name().toLowerCase().replaceAll("_", "-");
        }

        public String getLabel() {
            return label;
        }

        public Type getType() {
            return type;
        }

        public Set<Feature> getDependencies() {
            return dependencies;
        }

        public enum Type {
            DEFAULT("Default"),
            DISABLED_BY_DEFAULT("Disabled by default"),
            PREVIEW("Preview"),
            EXPERIMENTAL("Experimental"),
            DEPRECATED("Deprecated");

            private String label;

            Type(String label) {
                this.label = label;
            }

            public String getLabel() {
                return label;
            }
        }
    }

    private static final Logger logger = Logger.getLogger(Profile.class);

    private static final List<ProfileConfigResolver> DEFAULT_RESOLVERS = new LinkedList<>();
    static {
        DEFAULT_RESOLVERS.add(new PropertiesProfileConfigResolver(System.getProperties()));
        DEFAULT_RESOLVERS.add(new PropertiesFileProfileConfigResolver());
    };

    private static Profile CURRENT;

    private final ProfileName profileName;

    private final Map<Feature, Boolean> features;

    public static Profile defaults() {
        return configure();
    }

    public static Profile configure(ProfileConfigResolver... resolvers) {
        ProfileName profile = Arrays.stream(resolvers).map(ProfileConfigResolver::getProfileName).filter(Objects::nonNull).findFirst().orElse(ProfileName.DEFAULT);
        Map<Feature, Boolean> features = Arrays.stream(Feature.values()).collect(Collectors.toMap(f -> f, f -> isFeatureEnabled(profile, f, resolvers)));
        verifyConfig(features);

        CURRENT = new Profile(profile, features);
        return CURRENT;
    }

    public static Profile init(ProfileName profileName, Map<Feature, Boolean> features) {
        CURRENT = new Profile(profileName, features);
        return CURRENT;
    }

    private Profile(ProfileName profileName, Map<Feature, Boolean> features) {
        this.profileName = profileName;
        this.features = Collections.unmodifiableMap(features);

        logUnsupportedFeatures();
    }

    public static Profile getInstance() {
        return CURRENT;
    }

    public static boolean isFeatureEnabled(Feature feature) {
        return getInstance().features.get(feature);
    }

    public ProfileName getName() {
        return profileName;
    }

    public Set<Feature> getAllFeatures() {
        return features.keySet();
    }

    public Set<Feature> getDisabledFeatures() {
        return features.entrySet().stream().filter(e -> !e.getValue()).map(Map.Entry::getKey).collect(Collectors.toSet());
    }

    public Set<Feature> getPreviewFeatures() {
        return getFeatures(Feature.Type.PREVIEW);
    }

    public Set<Feature> getExperimentalFeatures() {
        return getFeatures(Feature.Type.EXPERIMENTAL);
    }

    public Set<Feature> getDeprecatedFeatures() {
        return getFeatures(Feature.Type.DEPRECATED);
    }

    public Set<Feature> getFeatures(Feature.Type type) {
        return features.keySet().stream().filter(f -> f.getType().equals(type)).collect(Collectors.toSet());
    }

    public Map<Feature, Boolean> getFeatures() {
        return features;
    }

    public enum ProfileName {
        DEFAULT,
        PREVIEW
    }

    private static Boolean isFeatureEnabled(ProfileName profile, Feature feature, ProfileConfigResolver... resolvers) {
        ProfileConfigResolver.FeatureConfig configuration = Arrays.stream(resolvers).map(r -> r.getFeatureConfig(feature))
                .filter(r -> !r.equals(ProfileConfigResolver.FeatureConfig.UNCONFIGURED))
                .findFirst()
                .orElse(ProfileConfigResolver.FeatureConfig.UNCONFIGURED);
        switch (configuration) {
            case ENABLED:
                return true;
            case DISABLED:
                return false;
            default:
                switch (feature.getType()) {
                    case DEFAULT:
                        return true;
                    case PREVIEW:
                        return profile.equals(ProfileName.PREVIEW) ? true : false;
                    default:
                        return false;
                }
        }
    }

    private static void verifyConfig(Map<Feature, Boolean> features) {
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

    private void logUnsupportedFeatures() {
        logUnsuportedFeatures(Feature.Type.PREVIEW, Logger.Level.INFO);
        logUnsuportedFeatures(Feature.Type.EXPERIMENTAL, Logger.Level.WARN);
        logUnsuportedFeatures(Feature.Type.DEPRECATED, Logger.Level.WARN);
    }

    private void logUnsuportedFeatures(Feature.Type type, Logger.Level level) {
        String enabledFeaturesOfType = features.entrySet().stream()
                .filter(e -> e.getValue() && e.getKey().getType().equals(type))
                .map(e -> e.getKey().getKey()).sorted().collect(Collectors.joining(", "));

        if (!enabledFeaturesOfType.isEmpty()) {
            logger.logv(level, "{0} features enabled: {1}", type.getLabel(), enabledFeaturesOfType);
        }
    }

}

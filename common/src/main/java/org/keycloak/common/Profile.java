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
import org.keycloak.common.util.KerberosJdkProvider;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class Profile {

    public enum Feature {
        AUTHORIZATION("Authorization Service", Type.DEFAULT),

        ACCOUNT_API("Account Management REST API", Type.DEFAULT),
        ACCOUNT2("Account Management Console version 2", Type.DEFAULT, Feature.ACCOUNT_API),
        ACCOUNT3("Account Management Console version 3", Type.PREVIEW, Feature.ACCOUNT_API),

        ADMIN_FINE_GRAINED_AUTHZ("Fine-Grained Admin Permissions", Type.PREVIEW),

        ADMIN_API("Admin API", Type.DEFAULT),

        ADMIN2("New Admin Console", Type.DEFAULT, Feature.ADMIN_API),

        DOCKER("Docker Registry protocol", Type.DISABLED_BY_DEFAULT),

        IMPERSONATION("Ability for admins to impersonate users", Type.DEFAULT),

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

        // Check if kerberos is available in underlying JVM and auto-detect if feature should be enabled or disabled by default based on that
        KERBEROS("Kerberos", KerberosJdkProvider.getProvider().isKerberosAvailable() ? Type.DEFAULT : Type.DISABLED_BY_DEFAULT),

        RECOVERY_CODES("Recovery codes", Type.PREVIEW),

        UPDATE_EMAIL("Update Email Action", Type.PREVIEW),

        JS_ADAPTER("Host keycloak.js and keycloak-authz.js through the Keycloak sever", Type.DEFAULT),

        FIPS("FIPS 140-2 mode", Type.DISABLED_BY_DEFAULT),

        LINKEDIN_OAUTH("LinkedIn Social Identity Provider based on OAuth", Type.DEPRECATED);

        private final Type type;
        private final String label;

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
            PREVIEW_DISABLED_BY_DEFAULT("Preview disabled by default"), // Preview features, which are not automatically enabled even with enabled preview profile (Needs to be enabled explicitly)
            EXPERIMENTAL("Experimental"),
            DEPRECATED("Deprecated");

            private final String label;

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

    /**
     * @return all features of type "preview" or "preview_disabled_by_default"
     */
    public Set<Feature> getPreviewFeatures() {
        return Stream.concat(getFeatures(Feature.Type.PREVIEW).stream(), getFeatures(Feature.Type.PREVIEW_DISABLED_BY_DEFAULT).stream())
                .collect(Collectors.toSet());
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
                        return profile.equals(ProfileName.PREVIEW);
                    default:
                        return false;
                }
        }
    }

    private static void verifyConfig(Map<Feature, Boolean> features) {
        for (Feature f : features.keySet()) {
            if (features.get(f) && f.getDependencies() != null) {
                for (Feature d : f.getDependencies()) {
                    if (!features.get(d)) {
                        throw new ProfileException("Feature " + f.getKey() + " depends on disabled feature " + d.getKey());
                    }
                }
            }
        }
    }

    private void logUnsupportedFeatures() {
        logUnsupportedFeatures(Feature.Type.PREVIEW, getPreviewFeatures(), Logger.Level.INFO);
        logUnsupportedFeatures(Feature.Type.EXPERIMENTAL, getExperimentalFeatures(), Logger.Level.WARN);
        logUnsupportedFeatures(Feature.Type.DEPRECATED, getDeprecatedFeatures(), Logger.Level.WARN);
    }

    private void logUnsupportedFeatures(Feature.Type type, Set<Feature> checkedFeatures, Logger.Level level) {
        Set<Feature.Type> checkedFeatureTypes = checkedFeatures.stream()
                .map(Feature::getType)
                .collect(Collectors.toSet());

        String enabledFeaturesOfType = features.entrySet().stream()
                .filter(e -> e.getValue() && checkedFeatureTypes.contains(e.getKey().getType()))
                .map(e -> e.getKey().getKey()).sorted().collect(Collectors.joining(", "));

        if (!enabledFeaturesOfType.isEmpty()) {
            logger.logv(level, "{0} features enabled: {1}", type.getLabel(), enabledFeaturesOfType);
        }
    }

}

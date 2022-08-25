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

import static org.keycloak.common.Profile.Type.DEPRECATED;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import org.jboss.logging.Logger;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class Profile {

    public static final String PRODUCT_NAME = ProductValue.RHSSO.getName();
    public static final String PROJECT_NAME = ProductValue.KEYCLOAK.getName();
    private static final Logger logger = Logger.getLogger(Profile.class);

    private static Profile CURRENT;
    private final ProductValue product;
    private final ProfileValue profile;
    private final Set<Feature> disabledFeatures = new HashSet<>();
    private final Set<Feature> previewFeatures = new HashSet<>();
    private final Set<Feature> experimentalFeatures = new HashSet<>();
    private final Set<Feature> deprecatedFeatures = new HashSet<>();
    private final PropertyResolver propertyResolver;
    public Profile(PropertyResolver resolver) {
        this.propertyResolver = resolver;
        Config config = new Config();

        product = PRODUCT_NAME.toLowerCase().equals(Version.NAME) ? ProductValue.RHSSO : ProductValue.KEYCLOAK;
        profile = ProfileValue.valueOf(config.getProfile().toUpperCase());

        for (Feature f : Feature.values()) {
            Boolean enabled = config.getConfig(f);
            Type type = product.equals(ProductValue.RHSSO) ? f.getTypeProduct() : f.getTypeProject();

            switch (type) {
                case DEFAULT:
                    if (enabled != null && !enabled) {
                        disabledFeatures.add(f);
                    }
                    break;
                case DEPRECATED:
                    deprecatedFeatures.add(f);
                case DISABLED_BY_DEFAULT:
                    if (enabled == null || !enabled) {
                        disabledFeatures.add(f);
                    } else if (DEPRECATED.equals(type)) {
                        logger.warnf("Deprecated feature enabled: " + f.name().toLowerCase());
                    }
                    break;
                case PREVIEW:
                    previewFeatures.add(f);
                    if ((enabled == null || !enabled) && !profile.equals(ProfileValue.PREVIEW)) {
                        disabledFeatures.add(f);
                    } else {
                        logger.info("Preview feature enabled: " + f.name().toLowerCase());
                    }
                    break;
                case EXPERIMENTAL:
                    experimentalFeatures.add(f);
                    if (enabled == null || !enabled) {
                        disabledFeatures.add(f);
                    } else {
                        logger.warn("Experimental feature enabled: " + f.name().toLowerCase());
                    }
                    break;
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
        return getInstance().disabledFeatures;
    }

    public static Set<Feature> getPreviewFeatures() {
        return getInstance().previewFeatures;
    }

    public static Set<Feature> getExperimentalFeatures() {
        return getInstance().experimentalFeatures;
    }

    public static Set<Feature> getDeprecatedFeatures() {
        return getInstance().deprecatedFeatures;
    }

    public static boolean isFeatureEnabled(Feature feature) {
        return !getInstance().disabledFeatures.contains(feature);
    }

    public static boolean isProduct() {
        return getInstance().profile.equals(ProfileValue.PRODUCT);
    }

    public enum Type {
        DEFAULT,
        DISABLED_BY_DEFAULT,
        PREVIEW,
        EXPERIMENTAL,
        DEPRECATED;
    }

    public enum Feature {
        AUTHORIZATION("Authorization Service", Type.DEFAULT),
        ACCOUNT2("New Account Management Console", Type.DEFAULT),
        ACCOUNT_API("Account Management REST API", Type.DEFAULT),
        ADMIN_FINE_GRAINED_AUTHZ("Fine-Grained Admin Permissions", Type.PREVIEW),
        ADMIN2("New Admin Console", Type.DEFAULT),
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
        UPDATE_EMAIL("Update Email Action", Type.PREVIEW);


        private final Type typeProject;
        private final Type typeProduct;
        private String label;

        Feature(String label, Type type) {
            this(label, type, type);
        }

        Feature(String label, Type typeProject, Type typeProduct) {
            this.label = label;
            this.typeProject = typeProject;
            this.typeProduct = typeProduct;
        }

        public String getLabel() {
            return label;
        }

        public Type getTypeProject() {
            return typeProject;
        }

        public Type getTypeProduct() {
            return typeProduct;
        }

        public boolean hasDifferentProductType() {
            return typeProject != typeProduct;
        }
    }

    private enum ProductValue {
        KEYCLOAK("Keycloak"),
        RHSSO("RH-SSO");

        private final String name;

        ProductValue(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }

    private enum ProfileValue {
        COMMUNITY,
        PRODUCT,
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
                return profile;
            }

            return Version.DEFAULT_PROFILE;
        }

        public Boolean getConfig(Feature feature) {
            String config = getProperty("keycloak.profile.feature." + feature.name().toLowerCase());

            if (config == null) {
                config = properties.getProperty("feature." + feature.name().toLowerCase());
            }

            if (config == null) {
                return null;
            } else if (config.equals("enabled")) {
                return Boolean.TRUE;
            } else if (config.equals("disabled")) {
                return Boolean.FALSE;
            } else {
                throw new RuntimeException("Invalid value for feature " + config);
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

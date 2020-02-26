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

import org.jboss.logging.Logger;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class Profile {

    private static final Logger logger = Logger.getLogger(Profile.class);

    public enum Type {
        DEFAULT,
        DISABLED_BY_DEFAULT,
        PREVIEW,
        EXPERIMENTAL,
        DEPRECATED;
    }
    public enum Feature {
        ACCOUNT2(Type.EXPERIMENTAL),
        ACCOUNT_API(Type.PREVIEW),
        ADMIN_FINE_GRAINED_AUTHZ(Type.PREVIEW),
        DOCKER(Type.DISABLED_BY_DEFAULT),
        IMPERSONATION(Type.DEFAULT),
        OPENSHIFT_INTEGRATION(Type.PREVIEW),
        SCRIPTS(Type.PREVIEW),
        TOKEN_EXCHANGE(Type.PREVIEW),
        UPLOAD_SCRIPTS(DEPRECATED),
        WEB_AUTHN(Type.DEFAULT, Type.PREVIEW);

        private Type typeProject;
        private Type typeProduct;

        Feature(Type type) {
            this(type, type);
        }

        Feature(Type typeProject, Type typeProduct) {
            this.typeProject = typeProject;
            this.typeProduct = typeProduct;
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
        KEYCLOAK,
        RHSSO
    }

    private enum ProfileValue {
        COMMUNITY,
        PRODUCT,
        PREVIEW
    }

    private static Profile CURRENT = new Profile();

    private final ProductValue product;

    private final ProfileValue profile;

    private final Set<Feature> disabledFeatures = new HashSet<>();
    private final Set<Feature> previewFeatures = new HashSet<>();
    private final Set<Feature> experimentalFeatures = new HashSet<>();
    private final Set<Feature> deprecatedFeatures = new HashSet<>();

    private Profile() {
        Config config = new Config();

        product = "rh-sso".equals(Version.NAME) ? ProductValue.RHSSO : ProductValue.KEYCLOAK;
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
                        if (Feature.UPLOAD_SCRIPTS.equals(f)) {
                            previewFeatures.add(Feature.SCRIPTS);
                            disabledFeatures.remove(Feature.SCRIPTS);
                            logger.warnf("Preview feature enabled: " + Feature.SCRIPTS.name().toLowerCase());
                        }
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

    public static void init() {
        CURRENT = new Profile();
    }

    public static String getName() {
        return CURRENT.profile.name().toLowerCase();
    }

    public static Set<Feature> getDisabledFeatures() {
        return CURRENT.disabledFeatures;
    }

    public static Set<Feature> getPreviewFeatures() {
        return CURRENT.previewFeatures;
    }

    public static Set<Feature> getExperimentalFeatures() {
        return CURRENT.experimentalFeatures;
    }

    public static Set<Feature> getDeprecatedFeatures() {
        return CURRENT.deprecatedFeatures;
    }

    public static boolean isFeatureEnabled(Feature feature) {
        return !CURRENT.disabledFeatures.contains(feature);
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
                        properties.load(new FileInputStream(file));
                    }
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        public String getProfile() {
            String profile = System.getProperty("keycloak.profile");
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
            String config = System.getProperty("keycloak.profile.feature." + feature.name().toLowerCase());
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
    }

}

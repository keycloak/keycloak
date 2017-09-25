/*
 * Copyright 2017 Red Hat, Inc. and/or its affiliates
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

package org.keycloak;

import org.keycloak.common.Version;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author <a href="mailto:mstrukel@redhat.com">Marko Strukelj</a>
 */
public enum Feature {

    /**
     *
     *   When you add, remove, or change a feature, you also have to modify methods: fromCaption() and isEnabledByDefault()
     *
     *   The following files will have to be modified as well:
     *     - pom.xml (keycloak-parent - properties in 'community' and 'product' profiles)
     *     - wildfly/server-subsystem/src/main/config/default-server-subsys-config.properties (<features> section)
     *     - wildfly/server-subsystem/src/main/resources/subsystem-templates/keycloak-server.xml (<features> section)
     *     - wildfly/server-subsystem/src/main/resources/cli/default-keycloak-subsys-config.cli
     *     - wildfly/server-subsystem/src/test/java/org/keycloak/subsystem/server/extension/FeaturesTestCase.java (COMMUNITY_FEATURES, and PRODUCT_FEATURES constants)
     *     - distribution/feature-packs/server-feature-pack/src/main/resources/content/bin/migrate-standalone.cli
     *     - distribution/feature-packs/server-feature-pack/src/main/resources/content/bin/migrate-standalone-ha.cli
     *     - distribution/feature-packs/server-feature-pack/src/main/resources/content/bin/migrate-domain-standalone.cli
     *     - distribution/feature-packs/server-feature-pack/src/main/resources/content/bin/migrate-domain-clustered.cli
     *     - distribution/distribution-tests/src/test/java/org/keycloak/test/distribution/DistributionFeaturesTest.java  (COMMUNITY_FEATURES, and PRODUCT_FEATURES constants)
     *
     *   In addition, the following files may need to be modified to support tests:
     *     - testsuite/integration-arquillian/tests/base/src/test/resources/META-INF/keycloak-server.json (feature section)
     *     - testsuite/utils/src/main/resources/META-INF/keycloak-server.json (feature section)
     *
     */

    AUTHORIZATION(true), IMPERSONATION, SCRIPTS(true), DOCKER, ACCOUNT2(true), TOKEN_EXCHANGE;


    private static final Logger log = Logger.getLogger(Feature.class.getName());

    private boolean preview;

    private static boolean foundBadConfig = false;

    Feature() {
        this.preview = false;
    }

    Feature(boolean preview) {
        this.preview = preview;
    }

    public String caption() {
        String s = name().toLowerCase().replace('_', '-');
        return preview ? s + "-preview" : s;
    }

    public static Set<Feature> getDisabledFeatures() {
        // by default all features are disabled
        HashSet<Feature> disabled = new HashSet(Arrays.asList(values()));
        Config.Scope config = Config.scope("feature");
        for (Feature f: values()) {
            Config.Scope featureScope = config.scope(f.caption());
            if (!featureScope.getBoolean("enabled", false)) {
                disabled.add(f);
            } else {
                disabled.remove(f);
            }
        }

        return disabled;
    }

    public static boolean isFeatureEnabled(Feature feature) {
        return Config.scope("feature", feature.caption()).getBoolean("enabled", false);
    }

    public static Feature fromCaption(String name) {
        Feature ret = AUTHORIZATION.caption().equals(name) ? AUTHORIZATION :
                IMPERSONATION.caption().equals(name) ? IMPERSONATION :
                SCRIPTS.caption().equals(name) ? SCRIPTS :
                DOCKER.caption().equals(name) ? DOCKER :
                ACCOUNT2.caption().equals(name) ? ACCOUNT2 :
                TOKEN_EXCHANGE.caption().equals(name) ? TOKEN_EXCHANGE : null;

        if (ret == null) {
            throw new IllegalArgumentException("No such feature: " + name);
        }
        return ret;
    }

    public static List<String> validCaptions() {
        Feature[] features = values();
        List<String> ret = new ArrayList<>(features.length);

        for (Feature f: features) {
            ret.add(f.caption());
        }
        return ret;
    }

    public boolean isEnabledByDefault() {
        // Value is dependent on whether this is a Keycloak release or some other
        // Use defensive programming to catch possible issues when adding new features
        if ("Keycloak".equals(Version.NAME)) {
            switch (this) {
                case AUTHORIZATION: return true;
                case SCRIPTS: return true;
                case IMPERSONATION: return true;
                case DOCKER: return false;
                case ACCOUNT2: return false;
                case TOKEN_EXCHANGE: return true;
                default:
                    throw new RuntimeException("Internal error - feature unaccounted for: " + this);
            }
        } else {
            switch (this) {
                case AUTHORIZATION: return false;
                case SCRIPTS: return false;
                case IMPERSONATION: return true;
                case DOCKER: return false;
                case ACCOUNT2: return false;
                case TOKEN_EXCHANGE: return false;
                default:
                    throw new RuntimeException("Internal error - feature unaccounted for: " + this);
            }
        }
    }

    public static void applyBackwardsCompatibilityOptions() {
        // if feature is disabled in a deprecated way, still take it into account but
        // print a warning
        // check if profile.properties exists

        Properties props = new Properties();

        String jbossServerConfigDir = System.getProperty("jboss.server.config.dir");
        if (jbossServerConfigDir != null) {
            File file = new File(jbossServerConfigDir, "profile.properties");
            if (file.isFile()) {
                foundBadConfig = true;
                log.warning("File 'profile.properties' has been deprecated. Use keycloak-server subsystem to configure features (e.g. <features> in standalone.xml).");
                try {
                    props.load(new FileInputStream(file));

                    for (String k: props.stringPropertyNames()) {
                        if (k.startsWith("feature.")) {
                            String value = props.getProperty(k);

                            try {
                                Feature f = Feature.valueOf(k.substring("feature.".length()).toUpperCase());
                                String newKey = "feature." + f.caption();
                                String newValue = "enabled".equals(value) ? "true" : "false";
                                System.setProperty(newKey, newValue);
                                log.warning("Property '" + k + "' in file 'profile.properties' has been deprecated. Use system property '" + newKey + "=" + newValue + "' instead.");
                            } catch(Exception e) {
                                log.warning("Property '" + k + "' in 'profile.properties' will be ignored.");
                            }
                        } else if ("profile".equals(k)) {
                            log.warning("Property 'profile' in 'profile.properties' will be ignored.");
                        }
                    }
                } catch (Exception e) {
                    throw new RuntimeException("Failed to load: " + file.getAbsolutePath());
                }
            }
        }

        for (String k : System.getProperties().stringPropertyNames()) {
            if (k.startsWith("keycloak.profile.feature.")) {
                foundBadConfig = true;
                String newKey = k.replace("keycloak.profile.feature.", "feature.");
                String value = System.getProperty(k);

                try {
                    Feature f = Feature.valueOf(newKey.substring("feature.".length()).toUpperCase());

                    String properKey = "feature." + f.caption();
                    String properValue = "enabled".equals(value) ? "true" : "false";

                    System.setProperty(properKey, properValue);
                    log.warning("System property '" + k + "' has been deprecated. Use '" + properKey + "=" + properValue + "' instead.");

                } catch (Exception e) {
                    log.warning("Unknown feature: " + k);
                }
            } else if (k.equals("keycloak.profile")) {
                foundBadConfig = true;
                log.warning("System property 'keycloak.profile' has been deprecated and will be ignored. keycloak-server subsystem to configure features (e.g. <features> in standalone.xml).");
            } else if (k.startsWith("feature.")) {
                try {
                    Feature.fromCaption(k.substring("feature.".length()).toLowerCase());
                } catch (Exception e) {
                    foundBadConfig = true;
                    log.warning("Unknown feature: " + k);
                }
            }
        }
    }

    public static void validate() {
        for (Feature f: values()) {
            String val = Config.scope("feature", f.caption()).get("enabled");
            if (val != null && !"true".equals(val) && !"false".equals(val)) {
                foundBadConfig = true;
                log.warning("Feature configuration '" + f.caption() + "' has 'enabled' attribute set to invalid value: '" + val + "'. Possible values: [true, false]");
            }
        }
    }

    public static void printFeatureConfiguration() {
        List<String> disabled = new LinkedList<>();
        for (Feature f : values()) {
            if (!isFeatureEnabled(f)) {
                disabled.add(f.caption());
            }
        }
        log.log(foundBadConfig ? Level.WARNING : Level.INFO, "Features that have been disabled: " + disabled);
    }
}

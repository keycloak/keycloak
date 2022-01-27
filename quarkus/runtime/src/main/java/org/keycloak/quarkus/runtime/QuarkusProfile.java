/*
 * Copyright 2021 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.quarkus.runtime;

import static org.keycloak.quarkus.runtime.configuration.Configuration.getRawPersistedProperty;

import org.keycloak.common.Profile;
import org.keycloak.quarkus.runtime.configuration.Configuration;

public class QuarkusProfile extends Profile {

    public QuarkusProfile() {
        super(new DefaultPropertyResolver());
    }

    private static class DefaultPropertyResolver implements PropertyResolver {

        @Override
        public String resolve(String key) {
            if (isFeaturePresent(key, getCurrentValue("kc.features"))) {
                if (isPreviewProfileKey(key)) {
                    return Profile.Type.PREVIEW.name();
                }

                return "enabled";
            }

            if (isFeaturePresent(key, getCurrentValue("kc.features-disabled"))) {
                if (!isPreviewProfileKey(key)) {
                    return "disabled";
                }
            }

            return null;
        }

        private boolean isFeaturePresent(String key, String features) {
            if (features == null) {
                return false;
            }

            for (String feature : features.split(",")) {
                if (isPreviewProfileKey(key)) {
                    try {
                        Profile.Type profileType = Profile.Type.valueOf(feature);

                        if (Profile.Type.PREVIEW.equals(profileType)) {
                            return true;
                        }
                    } catch (IllegalArgumentException ignore) {
                    }

                    return false;
                }

                if (key.substring(key.lastIndexOf('.') + 1).toUpperCase().equals(feature)) {
                    return true;
                }
            }

            return false;
        }

        private boolean isPreviewProfileKey(String key) {
            return key.equals("keycloak.profile");
        }

        private String getCurrentValue(String name) {
            String enabledFeatures = getRawPersistedProperty(name).orElse(null);

            if (enabledFeatures == null) {
                enabledFeatures = Configuration.getRawValue(name);
            }

            if (enabledFeatures == null) {
                return null;
            }

            return enabledFeatures.toUpperCase().replace('-', '_');
        }
    }
}

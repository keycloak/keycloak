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

package org.keycloak.testsuite;

import java.util.Set;

import org.keycloak.common.Profile;
import org.keycloak.testsuite.arquillian.TestContext;

import org.junit.Assume;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class ProfileAssume {

    private static Set<Profile.Feature> DISABLED_FEATURES;
    private static TestContext TEST_CONTEXT;

    private static void updateProfile() {
        if (DISABLED_FEATURES == null) {
            DISABLED_FEATURES = TEST_CONTEXT.getTestingClient().testing().listDisabledFeatures();
        }
    }

    public static void assumeFeatureEnabled(Profile.Feature feature) {
        updateProfile();
        Assume.assumeTrue("Ignoring test as feature " + feature.getKey() + " is not enabled", isFeatureEnabled(feature));
    }

    public static void assumeFeatureDisabled(Profile.Feature feature) {
        Assume.assumeTrue("Ignoring test as feature " + feature.getKey() + " is enabled", !isFeatureEnabled(feature));
    }

    public static boolean isFeatureEnabled(Profile.Feature feature) {
        updateProfile();
        return !DISABLED_FEATURES.contains(feature);
    }

    public static void updateDisabledFeatures(Set<Profile.Feature> disabledFeatures) {
        DISABLED_FEATURES = disabledFeatures;
    }

    public static void setTestContext(TestContext testContext) {
        TEST_CONTEXT = testContext;
    }
    
    public static Set<Profile.Feature> getDisabledFeatures() {
        return DISABLED_FEATURES;
    }
}

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

import org.junit.Assume;
import org.keycloak.Feature;
import org.keycloak.common.Version;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class ProfileAssume {

    public static void assumeFeatureEnabled(Feature feature) {
        Assume.assumeTrue("Ignoring test as " + feature.caption() + " is not enabled", Feature.isFeatureEnabled(feature));
    }

    public static void assumePreview() {
        Assume.assumeTrue("Ignoring test as community/preview profile is not enabled", Version.NAME.equals("Keycloak"));
    }

    public static void assumePreviewDisabled() {
        Assume.assumeFalse("Ignoring test as community/preview profile is enabled", Version.NAME.equals("Keycloak"));
    }

    public static void assumeCommunity() {
        Assume.assumeTrue("Ignoring test as community profile is not enabled", Version.NAME.equals("Keycloak"));
    }
}

/*
 * Copyright 2023 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.quarkus.runtime.configuration.test;

import org.junit.Test;
import org.keycloak.common.Profile;
import org.keycloak.common.profile.PropertiesProfileConfigResolver;
import org.keycloak.quarkus.runtime.configuration.IgnoredArtifacts;

import java.util.Properties;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class IgnoredArtifactsTest {

    @Test
    public void fipsDisabled() {
        var profile = Profile.defaults();
        assertThat(profile.isFeatureEnabled(Profile.Feature.FIPS), is(false));

        var ignoredArtifacts = IgnoredArtifacts.getDefaultIgnoredArtifacts();
        assertThat(ignoredArtifacts.containsAll(IgnoredArtifacts.FIPS_DISABLED), is(true));
    }

    @Test
    public void fipsEnabled() {
        Properties properties = new Properties();
        properties.setProperty("keycloak.profile.feature.fips", "enabled");
        var profile = Profile.configure(new PropertiesProfileConfigResolver(properties));

        assertThat(profile.isFeatureEnabled(Profile.Feature.FIPS), is(true));

        var ignoredArtifacts = IgnoredArtifacts.getDefaultIgnoredArtifacts();
        assertThat(ignoredArtifacts.containsAll(IgnoredArtifacts.FIPS_ENABLED), is(true));
    }
}

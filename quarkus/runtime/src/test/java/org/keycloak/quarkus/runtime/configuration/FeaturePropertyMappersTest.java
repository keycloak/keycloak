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

package org.keycloak.quarkus.runtime.configuration;

import org.keycloak.common.Profile.Feature;
import org.keycloak.quarkus.runtime.cli.PropertyException;
import org.keycloak.quarkus.runtime.configuration.mappers.FeaturePropertyMappers;

import org.junit.Test;

import static org.junit.Assert.assertThrows;

public class FeaturePropertyMappersTest {

    @Test
    public void testInvalidFeatureFormat() {
        assertThrows(PropertyException.class, () -> FeaturePropertyMappers.validateEnabledFeature("invalid:"));
    }

    @Test
    public void testInvalidFeature() {
        assertThrows(PropertyException.class, () -> FeaturePropertyMappers.validateEnabledFeature("invalid"));
    }

    @Test
    public void testInvalidVersionedFeature() {
        assertThrows(PropertyException.class, () -> FeaturePropertyMappers.validateEnabledFeature("invalid:v1"));
    }

    @Test
    public void testInvalidFeatureVersion() {
        assertThrows(PropertyException.class, () -> FeaturePropertyMappers.validateEnabledFeature(Feature.DOCKER.getUnversionedKey() + ":v0"));
    }

    @Test
    public void testValidFeatures() {
        FeaturePropertyMappers.validateEnabledFeature("preview");
        FeaturePropertyMappers.validateEnabledFeature(Feature.ACCOUNT_V3.getVersionedKey());
    }

}

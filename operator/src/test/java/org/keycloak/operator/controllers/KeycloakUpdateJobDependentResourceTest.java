/*
 * Copyright 2025 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.operator.controllers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import org.junit.jupiter.api.Test;
import org.keycloak.operator.crds.v2alpha1.deployment.Keycloak;
import org.keycloak.operator.crds.v2alpha1.deployment.KeycloakBuilder;

public class KeycloakUpdateJobDependentResourceTest {

    @Test
    void testKeycloakHashConsistency() {
        Keycloak keycloak = new KeycloakBuilder().withNewSpec().withInstances(2).endSpec().build();

        String hash1 = KeycloakUpdateJobDependentResource.keycloakHash(keycloak);
        String hash2 = KeycloakUpdateJobDependentResource.keycloakHash(new KeycloakBuilder(keycloak).editSpec().withInstances(1).endSpec().build());

        assertEquals(hash1, hash2, "Hashes should be equal for identical specs");
    }

    @Test
    void testKeycloakHashDifference() {
        Keycloak keycloak = new KeycloakBuilder().withNewSpec().withInstances(2).endSpec().build();

        String hash1 = KeycloakUpdateJobDependentResource.keycloakHash(keycloak);
        String hash2 = KeycloakUpdateJobDependentResource.keycloakHash(new KeycloakBuilder(keycloak).editSpec().withNewFeatureSpec().withEnabledFeatures("new-feature").endFeatureSpec().withInstances(1).endSpec().build());

        assertNotEquals(hash1, hash2, "Hashes should be different");
    }

}

/*
 * Copyright 2022 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.operator.testsuite.unit;

import io.fabric8.kubernetes.client.utils.Serialization;
import org.junit.jupiter.api.Test;
import org.keycloak.operator.crds.v2alpha1.deployment.Keycloak;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class CRSerializationTest {

    @Test
    public void testDeserialization() {
        Keycloak keycloak = Serialization.unmarshal(this.getClass().getResourceAsStream("/test-serialization-keycloak-cr.yml"), Keycloak.class);

        assertEquals("my-hostname", keycloak.getSpec().getHostname());
        assertEquals("my-image", keycloak.getSpec().getImage());
        assertEquals("my-tls-secret", keycloak.getSpec().getTlsSecret());
        assertTrue(keycloak.getSpec().isDisableDefaultIngress());
    }

}

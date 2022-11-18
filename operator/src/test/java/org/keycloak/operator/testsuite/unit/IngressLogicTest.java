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

import io.fabric8.kubernetes.api.model.networking.v1.Ingress;
import io.fabric8.kubernetes.api.model.networking.v1.IngressBuilder;
import org.junit.jupiter.api.Test;
import org.keycloak.operator.controllers.KeycloakIngress;
import org.keycloak.operator.crds.v2alpha1.deployment.spec.IngressSpec;
import org.keycloak.operator.crds.v2alpha1.deployment.Keycloak;
import org.keycloak.operator.testsuite.utils.K8sUtils;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class IngressLogicTest {

    static class MockKeycloakIngress extends KeycloakIngress {

        private static Keycloak getKeycloak(Boolean defaultIngressEnabled, boolean ingressSpecDefined) {
            var kc = K8sUtils.getDefaultKeycloakDeployment();
            if (ingressSpecDefined) {
                kc.getSpec().setIngressSpec(new IngressSpec());
                if (defaultIngressEnabled != null) kc.getSpec().getIngressSpec().setIngressEnabled(defaultIngressEnabled);
            }
            return kc;
        }

        public static MockKeycloakIngress build(Boolean defaultIngressEnabled, boolean ingressExists, boolean ingressSpecDefined) {
            MockKeycloakIngress.ingressExists = ingressExists;
            return new MockKeycloakIngress(defaultIngressEnabled, ingressSpecDefined);
        }

        public static boolean ingressExists = false;
        private boolean deleted = false;
        public MockKeycloakIngress(Boolean defaultIngressEnabled, boolean ingressSpecDefined) {
            super(null, getKeycloak(defaultIngressEnabled, ingressSpecDefined));
        }

        public boolean reconciled() {
            return getReconciledResource().isPresent();
        }

        public boolean deleted() {
            return deleted;
        }

        @Override
        protected Ingress fetchExistingIngress() {
            if (ingressExists) {
                return new IngressBuilder().withNewMetadata().endMetadata().build();
            } else {
                return null;
            }
        }

        @Override
        protected void deleteExistingIngress() {
            deleted = true;
        }
    }

    @Test
    public void testIngressDisabledExisting() {
        var kc = MockKeycloakIngress.build(false, true, true);
        assertFalse(kc.reconciled());
        assertTrue(kc.deleted());
    }

    @Test
    public void testIngressDisabledNotExisting() {
        var kc = MockKeycloakIngress.build(false, false, true);
        assertFalse(kc.reconciled());
        assertFalse(kc.deleted());
    }

    @Test
    public void testIngressEnabledExisting() {
        var kc = MockKeycloakIngress.build(true, true, true);
        assertTrue(kc.reconciled());
        assertFalse(kc.deleted());
    }

    @Test
    public void testIngressEnabledNotExisting() {
        var kc = MockKeycloakIngress.build(true, false, true);
        assertTrue(kc.reconciled());
        assertFalse(kc.deleted());
    }

    @Test
    public void testIngressEnabledNotSpecified() {
        var kc = MockKeycloakIngress.build(true, false, false);
        assertTrue(kc.reconciled());
        assertFalse(kc.deleted());
    }

    @Test
    public void testIngressSpecDefinedWithoutProperty() {
        var kc = MockKeycloakIngress.build(null, false, true);
        assertTrue(kc.reconciled());
        assertFalse(kc.deleted());
    }
}

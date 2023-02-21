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

import java.util.Collections;
import java.util.Optional;

import io.fabric8.kubernetes.api.model.OwnerReference;
import io.fabric8.kubernetes.api.model.OwnerReferenceBuilder;
import org.junit.jupiter.api.Test;
import org.keycloak.operator.Constants;
import org.keycloak.operator.controllers.KeycloakIngress;
import org.keycloak.operator.crds.v2alpha1.deployment.Keycloak;
import org.keycloak.operator.crds.v2alpha1.deployment.spec.IngressSpec;
import org.keycloak.operator.testsuite.utils.K8sUtils;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.networking.v1.Ingress;
import io.fabric8.kubernetes.api.model.networking.v1.IngressBuilder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class IngressLogicTest {

    static class MockKeycloakIngress extends KeycloakIngress {

        private static Keycloak getKeycloak(Boolean defaultIngressEnabled, boolean ingressSpecDefined, boolean tlsConfigured) {
            var kc = K8sUtils.getDefaultKeycloakDeployment();
            kc.getMetadata().setUid("this-is-a-fake-uid");
            if (ingressSpecDefined) {
                kc.getSpec().setIngressSpec(new IngressSpec());
                if (defaultIngressEnabled != null) kc.getSpec().getIngressSpec().setIngressEnabled(defaultIngressEnabled);
            }
            if (!tlsConfigured) {
                kc.getSpec().getHttpSpec().setTlsSecret(null);
            }
            return kc;
        }

        public static MockKeycloakIngress build(Boolean defaultIngressEnabled, boolean ingressExists, boolean ingressSpecDefined) {
            return build(defaultIngressEnabled, ingressExists, ingressSpecDefined, true);
        }

        public static MockKeycloakIngress build(Boolean defaultIngressEnabled, boolean ingressExists, boolean ingressSpecDefined, boolean tlsConfigured) {
            MockKeycloakIngress.ingressExists = ingressExists;
            return new MockKeycloakIngress(defaultIngressEnabled, ingressSpecDefined, tlsConfigured);
        }

        public static boolean ingressExists = false;
        private boolean deleted = false;
        public MockKeycloakIngress(Boolean defaultIngressEnabled, boolean ingressSpecDefined, boolean tlsConfigured) {
            super(null, getKeycloak(defaultIngressEnabled, ingressSpecDefined, tlsConfigured));
        }

        @Override
        public Optional<HasMetadata> getReconciledResource() {
            return super.getReconciledResource();
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

                OwnerReference sameCROwnerRef = new OwnerReferenceBuilder()
                        .withApiVersion(cr.getApiVersion())
                        .withKind(cr.getKind())
                        .withName(cr.getMetadata().getName())
                        .withUid(cr.getMetadata().getUid())
                        .withBlockOwnerDeletion(true)
                        .withController(true)
                        .build();

                return new IngressBuilder()
                        .withNewMetadata()
                            .withName(getName())
                            .withNamespace(cr.getMetadata().getNamespace())
                            .withOwnerReferences(Collections.singletonList(sameCROwnerRef))
                        .endMetadata()
                        .build();
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

    @Test
    public void testHttpSpecWithTlsSecret() {
        var kc = MockKeycloakIngress.build(null, false, true, true);
        Optional<HasMetadata> reconciled = kc.getReconciledResource();
        assertTrue(reconciled.isPresent());
        assertFalse(kc.deleted());
        assertEquals("HTTPS", reconciled.get().getMetadata().getAnnotations().get("nginx.ingress.kubernetes.io/backend-protocol"));
        assertEquals("passthrough", reconciled.get().getMetadata().getAnnotations().get("route.openshift.io/termination"));
    }

    @Test
    public void testHttpSpecWithoutTlsSecret() {
        var kc = MockKeycloakIngress.build(null, false, true, false);
        Optional<HasMetadata> reconciled = kc.getReconciledResource();
        assertTrue(reconciled.isPresent());
        assertFalse(kc.deleted());
        assertEquals("HTTP", reconciled.get().getMetadata().getAnnotations().get("nginx.ingress.kubernetes.io/backend-protocol"));
        assertEquals("edge", reconciled.get().getMetadata().getAnnotations().get("route.openshift.io/termination"));
    }
}

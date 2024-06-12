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

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.networking.v1.Ingress;

import org.junit.jupiter.api.Test;
import org.keycloak.operator.controllers.KeycloakIngressDependentResource;
import org.keycloak.operator.crds.v2alpha1.deployment.Keycloak;
import org.keycloak.operator.crds.v2alpha1.deployment.spec.IngressSpec;
import org.keycloak.operator.crds.v2alpha1.deployment.spec.IngressSpecBuilder;
import org.keycloak.operator.testsuite.utils.K8sUtils;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class IngressLogicTest {

    private static final String EXISTING_ANNOTATION_KEY = "annotation";

    static class MockKeycloakIngress {

        private static Keycloak getKeycloak(boolean tlsConfigured, IngressSpec ingressSpec, String hostname) {
            var kc = K8sUtils.getDefaultKeycloakDeployment();
            kc.getMetadata().setUid("this-is-a-fake-uid");
            if (ingressSpec != null) {
                kc.getSpec().setIngressSpec(ingressSpec);
            }
            if (!tlsConfigured) {
                kc.getSpec().getHttpSpec().setTlsSecret(null);
            }
            if (hostname != null) {
                kc.getSpec().getHostnameSpec().setHostname(hostname);
            }
            return kc;
        }

        public static MockKeycloakIngress build(Boolean defaultIngressEnabled, boolean ingressExists, boolean ingressSpecDefined) {
            return build(defaultIngressEnabled, ingressExists, ingressSpecDefined, true);
        }

        public static MockKeycloakIngress build(Boolean defaultIngressEnabled, boolean ingressExists, boolean ingressSpecDefined, boolean tlsConfigured) {
            return build(defaultIngressEnabled, ingressExists, ingressSpecDefined, tlsConfigured, null, null);
        }

        public static MockKeycloakIngress build(String hostname) {
            return build(true, false, true, true, null, hostname);
        }

        public static MockKeycloakIngress build(Boolean defaultIngressEnabled, boolean ingressExists, boolean ingressSpecDefined, boolean tlsConfigured, Map<String, String> annotations) {
            return build(defaultIngressEnabled, ingressExists, ingressSpecDefined, tlsConfigured, annotations, null);
        }

        public static MockKeycloakIngress build(Boolean defaultIngressEnabled, boolean ingressExists, boolean ingressSpecDefined, boolean tlsConfigured, Map<String, String> annotations, String hostname) {
            IngressSpec ingressSpec = null;
            if (ingressSpecDefined) {
                ingressSpec = new IngressSpec();
                if (defaultIngressEnabled != null) {
                    ingressSpec.setIngressEnabled(defaultIngressEnabled);
                }
                if (annotations != null) {
                    ingressSpec.setAnnotations(annotations);
                }
            }
            MockKeycloakIngress mock = new MockKeycloakIngress(tlsConfigured, ingressSpec, hostname);
            mock.ingressExists = ingressExists;
            return mock;
        }

        private KeycloakIngressDependentResource keycloakIngressDependentResource = new KeycloakIngressDependentResource();
        private boolean ingressExists = false;
        private boolean deleted = false;
        private Keycloak keycloak;

        public MockKeycloakIngress(boolean tlsConfigured, IngressSpec ingressSpec, String hostname) {
            this.keycloak = getKeycloak(tlsConfigured, ingressSpec, hostname);
        }

        public MockKeycloakIngress(boolean tlsConfigured, IngressSpec ingressSpec) {
            this(tlsConfigured, ingressSpec, null);
        }

        public boolean reconciled() {
            return getReconciledResource().isPresent();
        }

        public boolean deleted() {
            return deleted;
        }

        public Optional<HasMetadata> getReconciledResource() {
            if (!KeycloakIngressDependentResource.isIngressEnabled(keycloak)) {
                if (ingressExists) {
                    deleted = true;
                }
                return Optional.empty();
            }

            return Optional.of(keycloakIngressDependentResource.desired(keycloak, null));
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

    @Test
    public void testCustomAnnotations() {
        var kc = MockKeycloakIngress.build(null, false, true, true, Map.of("custom", "value"));
        Optional<HasMetadata> reconciled = kc.getReconciledResource();
        assertTrue(reconciled.isPresent());
        assertFalse(kc.deleted());
        assertEquals("HTTPS", reconciled.get().getMetadata().getAnnotations().get("nginx.ingress.kubernetes.io/backend-protocol"));
        assertEquals("passthrough", reconciled.get().getMetadata().getAnnotations().get("route.openshift.io/termination"));
        assertEquals("value", reconciled.get().getMetadata().getAnnotations().get("custom"));
        assertFalse(reconciled.get().getMetadata().getAnnotations().containsKey(EXISTING_ANNOTATION_KEY));
    }

    @Test
    public void testRemoveCustomAnnotation() {
        var kc = MockKeycloakIngress.build(null, true, true, true, null);
        Optional<HasMetadata> reconciled = kc.getReconciledResource();
        assertTrue(reconciled.isPresent());
        assertFalse(kc.deleted());
        assertEquals("HTTPS", reconciled.get().getMetadata().getAnnotations().get("nginx.ingress.kubernetes.io/backend-protocol"));
        assertEquals("passthrough", reconciled.get().getMetadata().getAnnotations().get("route.openshift.io/termination"));
        assertFalse(reconciled.get().getMetadata().getAnnotations().containsKey(EXISTING_ANNOTATION_KEY));
    }

    @Test
    public void testUpdateCustomAnnotation() {
        var kc = MockKeycloakIngress.build(null, true, true, true, Map.of(EXISTING_ANNOTATION_KEY, "another-value"));
        Optional<HasMetadata> reconciled = kc.getReconciledResource();
        assertTrue(reconciled.isPresent());
        assertFalse(kc.deleted());
        assertEquals("HTTPS", reconciled.get().getMetadata().getAnnotations().get("nginx.ingress.kubernetes.io/backend-protocol"));
        assertEquals("passthrough", reconciled.get().getMetadata().getAnnotations().get("route.openshift.io/termination"));
        assertEquals("another-value", reconciled.get().getMetadata().getAnnotations().get(EXISTING_ANNOTATION_KEY));
    }

    @Test
    public void testIngressSpecDefinedWithoutClassName() {
        var kc = new MockKeycloakIngress(true, new IngressSpec());
        Optional<HasMetadata> reconciled = kc.getReconciledResource();
        Ingress ingress = reconciled.map(Ingress.class::cast).orElseThrow();
        assertNull(ingress.getSpec().getIngressClassName());
    }

    @Test
    public void testIngressSpecDefinedWithClassName() {
        var kc = new MockKeycloakIngress(true, new IngressSpecBuilder().withIngressClassName("my-class").build());
        Optional<HasMetadata> reconciled = kc.getReconciledResource();
        Ingress ingress = reconciled.map(Ingress.class::cast).orElseThrow();
        assertEquals("my-class", ingress.getSpec().getIngressClassName());
    }

    @Test
    public void testHostnameSanitizing() {
        var kc = MockKeycloakIngress.build("https://my-other.example.com:443/my-path");
        Optional<HasMetadata> reconciled = kc.getReconciledResource();
        Ingress ingress = reconciled.map(Ingress.class::cast).orElseThrow();
        assertEquals("my-other.example.com", ingress.getSpec().getRules().get(0).getHost());
    }
}

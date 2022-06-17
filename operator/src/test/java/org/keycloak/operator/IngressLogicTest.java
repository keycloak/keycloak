package org.keycloak.operator;

import io.fabric8.kubernetes.api.model.networking.v1.Ingress;
import io.fabric8.kubernetes.api.model.networking.v1.IngressBuilder;
import org.junit.jupiter.api.Test;
import org.keycloak.operator.utils.K8sUtils;
import org.keycloak.operator.v2alpha1.KeycloakIngress;
import org.keycloak.operator.v2alpha1.crds.Keycloak;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class IngressLogicTest {

    static class MockKeycloakIngress extends KeycloakIngress {

        private static Keycloak getKeycloak(boolean defaultIngressDisabled) {
            var kc = K8sUtils.getDefaultKeycloakDeployment();
            kc.getSpec().setDisableDefaultIngress(defaultIngressDisabled);
            return kc;
        }

        public static MockKeycloakIngress build(boolean defaultIngressDisabled, boolean ingressExists) {
            MockKeycloakIngress.ingressExists = ingressExists;
            return new MockKeycloakIngress(defaultIngressDisabled);
        }

        public static boolean ingressExists = false;
        private boolean deleted = false;
        public MockKeycloakIngress(boolean defaultIngressDisabled) {
            super(null, getKeycloak(defaultIngressDisabled));
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
    public void testIngressEnabledExisting() {
        var kc = MockKeycloakIngress.build(true, true);
        assertFalse(kc.reconciled());
        assertTrue(kc.deleted());
    }

    @Test
    public void testIngressEnabledNotExisting() {
        var kc = MockKeycloakIngress.build(true, false);
        assertFalse(kc.reconciled());
        assertFalse(kc.deleted());
    }

    @Test
    public void testIngressDisabledExisting() {
        var kc = MockKeycloakIngress.build(false, true);
        assertTrue(kc.reconciled());
        assertFalse(kc.deleted());
    }

    @Test
    public void testIngressDisabledNotExisting() {
        var kc = MockKeycloakIngress.build(false, false);
        assertTrue(kc.reconciled());
        assertFalse(kc.deleted());
    }
}

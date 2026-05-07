package org.keycloak.operator.testsuite.unit;

import java.util.List;
import java.util.Map;

import org.keycloak.operator.Utils;
import org.keycloak.operator.controllers.KeycloakServiceAccountDependentResource;
import org.keycloak.operator.crds.v2beta1.deployment.Keycloak;
import org.keycloak.operator.crds.v2beta1.deployment.spec.ServiceAccountSpec;
import org.keycloak.operator.testsuite.utils.K8sUtils;
import org.keycloak.operator.testsuite.utils.MockController;

import io.fabric8.kubernetes.api.model.LocalObjectReference;
import io.fabric8.kubernetes.api.model.ServiceAccount;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ServiceAccountLogicTest {

    private static class MockKeycloakServiceAccount
            extends MockController<ServiceAccount, KeycloakServiceAccountDependentResource> {

        MockKeycloakServiceAccount(Keycloak keycloak) {
            super(new KeycloakServiceAccountDependentResource(), keycloak);
        }

        @Override
        protected boolean isEnabled() {
            return keycloak.getSpec().getServiceAccountSpec() != null;
        }

        @Override
        protected ServiceAccount desired() {
            return dependentResource.desired(keycloak, null);
        }
    }

    @Test
    public void testDisabledByDefault() {
        var keycloak = K8sUtils.getDefaultKeycloakDeployment();
        var controller = new MockKeycloakServiceAccount(keycloak);
        assertFalse(controller.isEnabled());
        assertFalse(controller.reconciled());
    }

    @Test
    public void testEnabledWhenSpecPresent() {
        var keycloak = K8sUtils.getDefaultKeycloakDeployment();
        keycloak.getSpec().setServiceAccountSpec(new ServiceAccountSpec());
        var controller = new MockKeycloakServiceAccount(keycloak);
        assertTrue(controller.isEnabled());
        assertTrue(controller.reconciled());
    }

    @Test
    public void testDeletedWhenSpecRemoved() {
        var keycloak = K8sUtils.getDefaultKeycloakDeployment();
        keycloak.getSpec().setServiceAccountSpec(new ServiceAccountSpec());
        var controller = new MockKeycloakServiceAccount(keycloak);
        controller.setExists();
        keycloak.getSpec().setServiceAccountSpec(null);
        assertFalse(controller.reconciled());
        assertTrue(controller.deleted());
    }

    @Test
    public void testDesiredSANameAndNamespace() {
        var keycloak = K8sUtils.getDefaultKeycloakDeployment();
        keycloak.getSpec().setServiceAccountSpec(new ServiceAccountSpec());
        var sa = new KeycloakServiceAccountDependentResource().desired(keycloak, null);
        assertEquals(keycloak.getMetadata().getName(), sa.getMetadata().getName());
        assertEquals(keycloak.getMetadata().getNamespace(), sa.getMetadata().getNamespace());
    }

    @Test
    public void testDesiredSALabels() {
        var keycloak = K8sUtils.getDefaultKeycloakDeployment();
        keycloak.getSpec().setServiceAccountSpec(new ServiceAccountSpec());
        var sa = new KeycloakServiceAccountDependentResource().desired(keycloak, null);
        assertEquals(Utils.allInstanceLabels(keycloak), sa.getMetadata().getLabels());
    }

    @Test
    public void testDesiredSAAnnotations() {
        var keycloak = K8sUtils.getDefaultKeycloakDeployment();
        var spec = new ServiceAccountSpec();
        spec.setAnnotations(Map.of("eks.amazonaws.com/role-arn", "arn:aws:iam::123:role/my-role"));
        keycloak.getSpec().setServiceAccountSpec(spec);
        var sa = new KeycloakServiceAccountDependentResource().desired(keycloak, null);
        assertEquals("arn:aws:iam::123:role/my-role",
                sa.getMetadata().getAnnotations().get("eks.amazonaws.com/role-arn"));
    }

    @Test
    public void testDesiredSAImagePullSecrets() {
        var keycloak = K8sUtils.getDefaultKeycloakDeployment();
        var spec = new ServiceAccountSpec();
        spec.setImagePullSecrets(List.of(new LocalObjectReference("my-registry-secret")));
        keycloak.getSpec().setServiceAccountSpec(spec);
        var sa = new KeycloakServiceAccountDependentResource().desired(keycloak, null);
        assertEquals(1, sa.getImagePullSecrets().size());
        assertEquals("my-registry-secret", sa.getImagePullSecrets().get(0).getName());
    }

    @Test
    public void testDesiredSANullAnnotationsAndImagePullSecrets() {
        var keycloak = K8sUtils.getDefaultKeycloakDeployment();
        keycloak.getSpec().setServiceAccountSpec(new ServiceAccountSpec());
        var sa = new KeycloakServiceAccountDependentResource().desired(keycloak, null);
        assertTrue(sa.getMetadata().getAnnotations() == null || sa.getMetadata().getAnnotations().isEmpty());
        assertTrue(sa.getImagePullSecrets() == null || sa.getImagePullSecrets().isEmpty());
    }
}

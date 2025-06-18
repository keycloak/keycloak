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

import io.fabric8.kubernetes.client.dsl.Resource;
import io.fabric8.openshift.api.model.config.v1.Ingress;
import io.fabric8.openshift.api.model.config.v1.IngressBuilder;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.api.reconciler.UpdateControl;

import org.junit.jupiter.api.Test;
import org.keycloak.operator.controllers.KeycloakController;
import org.keycloak.operator.crds.v2alpha1.deployment.Keycloak;
import org.keycloak.operator.crds.v2alpha1.deployment.spec.IngressSpecBuilder;
import org.keycloak.operator.testsuite.utils.K8sUtils;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KeycloakControllerTest {

    @Test
    void testCRDefaults() {
        KeycloakController controller = new KeycloakController();
        Keycloak kc = K8sUtils.getDefaultKeycloakDeployment();
        kc.getSpec().setInstances(null);
        kc.getSpec().getHostnameSpec().setHostname(null);
        kc.getSpec().setIngressSpec(new IngressSpecBuilder().withIngressClassName(KeycloakController.OPENSHIFT_DEFAULT).build());
        kc.getMetadata().setNamespace("ns");
        Context<Keycloak> mockContext = Mockito.mock(Context.class, Mockito.RETURNS_DEEP_STUBS);
        var ingressConfig = new IngressBuilder().withNewSpec().withDomain("openshift.com").endSpec().build();
        var mockResource = Mockito.mock(Resource.class);
        Mockito.when(mockResource.get()).thenReturn(ingressConfig);
        Mockito.when(mockContext.getClient().resources(Ingress.class).withName("cluster")).thenReturn(mockResource);

        // both the instances and hostname should be updated
        UpdateControl<Keycloak> update = controller.reconcile(kc, mockContext);

        assertTrue(update.isPatchResource());
        assertEquals(1, update.getResource().orElseThrow().getSpec().getInstances());
        assertEquals("example-kc-ingress-ns.openshift.com", update.getResource().orElseThrow().getSpec().getHostnameSpec().getHostname());

        // just the instances should be updated if not openshift-default
        kc = K8sUtils.getDefaultKeycloakDeployment();
        kc.getSpec().setIngressSpec(null);
        kc.getSpec().setInstances(null);
        kc.getSpec().getHostnameSpec().setHostname(null);
        update = controller.reconcile(kc, mockContext);
        assertTrue(update.isPatchResource());
        assertEquals(1, update.getResource().orElseThrow().getSpec().getInstances());
        assertNull(update.getResource().orElseThrow().getSpec().getHostnameSpec().getHostname());
    }

}

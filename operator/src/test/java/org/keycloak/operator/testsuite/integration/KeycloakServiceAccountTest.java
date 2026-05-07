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

package org.keycloak.operator.testsuite.integration;

import java.util.List;
import java.util.Map;

import org.keycloak.operator.crds.v2beta1.deployment.Keycloak;
import org.keycloak.operator.crds.v2beta1.deployment.spec.ServiceAccountSpec;
import org.keycloak.operator.testsuite.utils.K8sUtils;

import io.fabric8.kubernetes.api.model.LocalObjectReference;
import io.fabric8.kubernetes.api.model.ServiceAccount;
import io.fabric8.kubernetes.api.model.apps.StatefulSet;
import io.quarkus.test.junit.QuarkusTest;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNull;

@QuarkusTest
public class KeycloakServiceAccountTest extends BaseOperatorTest {

    private ServiceAccount getServiceAccount(Keycloak keycloak) {
        return k8sclient.serviceAccounts()
                .inNamespace(namespaceOf(keycloak))
                .withName(keycloak.getMetadata().getName())
                .get();
    }

    private StatefulSet getStatefulSet(Keycloak keycloak) {
        return k8sclient.apps().statefulSets()
                .inNamespace(namespaceOf(keycloak))
                .withName(keycloak.getMetadata().getName())
                .get();
    }

    @Test
    public void testNoServiceAccountByDefault() {
        var kc = getTestKeycloakDeployment(true);
        K8sUtils.deployKeycloak(k8sclient, kc, true);

        assertNull(getServiceAccount(kc), "No SA expected when serviceAccountSpec absent");

        var ss = getStatefulSet(kc);
        assertThat(ss.getSpec().getTemplate().getSpec().getServiceAccountName())
                .isIn(null, "", "default");
    }

    @Test
    public void testServiceAccountCreatedWithSpec() {
        var kc = getTestKeycloakDeployment(true);
        var spec = new ServiceAccountSpec();
        spec.setAnnotations(Map.of("eks.amazonaws.com/role-arn", "arn:aws:iam::123:role/my-role"));
        kc.getSpec().setServiceAccountSpec(spec);

        K8sUtils.deployKeycloak(k8sclient, kc, true);

        Awaitility.await("SA created").untilAsserted(() -> {
            var sa = getServiceAccount(kc);
            assertThat(sa).isNotNull();
            assertThat(sa.getMetadata().getName()).isEqualTo(kc.getMetadata().getName());
            assertThat(sa.getMetadata().getAnnotations())
                    .containsEntry("eks.amazonaws.com/role-arn", "arn:aws:iam::123:role/my-role");
        });

        var ss = getStatefulSet(kc);
        assertThat(ss.getSpec().getTemplate().getSpec().getServiceAccountName())
                .isEqualTo(kc.getMetadata().getName());
    }

    @Test
    public void testServiceAccountImagePullSecrets() {
        var kc = getTestKeycloakDeployment(true);
        var spec = new ServiceAccountSpec();
        spec.setImagePullSecrets(List.of(new LocalObjectReference("my-registry-secret")));
        kc.getSpec().setServiceAccountSpec(spec);

        K8sUtils.deployKeycloak(k8sclient, kc, true);

        Awaitility.await("SA with imagePullSecrets created").untilAsserted(() -> {
            var sa = getServiceAccount(kc);
            assertThat(sa).isNotNull();
            assertThat(sa.getImagePullSecrets()).hasSize(1);
            assertThat(sa.getImagePullSecrets().get(0).getName()).isEqualTo("my-registry-secret");
        });
    }

    @Test
    public void testServiceAccountDeletedWhenSpecRemoved() {
        var kc = getTestKeycloakDeployment(true);
        kc.getSpec().setServiceAccountSpec(new ServiceAccountSpec());
        K8sUtils.deployKeycloak(k8sclient, kc, true);

        Awaitility.await("SA created").untilAsserted(() ->
                assertThat(getServiceAccount(kc)).isNotNull());

        // remove the spec
        kc.getSpec().setServiceAccountSpec(null);
        K8sUtils.deployKeycloak(k8sclient, kc, true);

        Awaitility.await("SA deleted").untilAsserted(() ->
                assertNull(getServiceAccount(kc), "SA should be deleted when spec removed"));

        var ss = getStatefulSet(kc);
        assertThat(ss.getSpec().getTemplate().getSpec().getServiceAccountName())
                .isIn(null, "", "default");
    }

    @Test
    public void testServiceAccountAnnotationsUpdated() {
        var kc = getTestKeycloakDeployment(true);
        var spec = new ServiceAccountSpec();
        spec.setAnnotations(Map.of("foo", "bar"));
        kc.getSpec().setServiceAccountSpec(spec);
        K8sUtils.deployKeycloak(k8sclient, kc, true);

        Awaitility.await("SA created with initial annotation").untilAsserted(() -> {
            var sa = getServiceAccount(kc);
            assertThat(sa).isNotNull();
            assertThat(sa.getMetadata().getAnnotations()).containsEntry("foo", "bar");
        });

        // update annotation
        kc.getSpec().getServiceAccountSpec().setAnnotations(Map.of("foo", "updated"));
        K8sUtils.deployKeycloak(k8sclient, kc, true);

        Awaitility.await("SA annotation updated").untilAsserted(() -> {
            var sa = getServiceAccount(kc);
            assertThat(sa).isNotNull();
            assertThat(sa.getMetadata().getAnnotations()).containsEntry("foo", "updated");
        });
    }
}

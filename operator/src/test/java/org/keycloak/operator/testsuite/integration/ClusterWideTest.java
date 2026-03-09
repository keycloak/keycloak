/*
 * Copyright 2026 Red Hat, Inc. and/or its affiliates
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

import java.io.FileNotFoundException;
import java.util.Objects;
import java.util.Optional;

import org.keycloak.operator.crds.v2alpha1.deployment.Keycloak;
import org.keycloak.operator.crds.v2alpha1.deployment.KeycloakStatusCondition;
import org.keycloak.operator.testsuite.utils.K8sUtils;

import io.fabric8.kubernetes.api.model.NamespaceBuilder;
import io.fabric8.kubernetes.api.model.Secret;
import io.quarkus.logging.Log;
import io.quarkus.test.junit.QuarkusTest;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.keycloak.operator.testsuite.apiserver.DisabledIfApiServerTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.keycloak.operator.testsuite.utils.CRAssert.assertKeycloakStatusCondition;
import static org.keycloak.operator.testsuite.utils.K8sUtils.getResourceFromFile;

/**
 * Tests that the operator can reconcile Keycloak CRs in a namespace different
 * from the one it is deployed in, verifying cluster-wide operation.
 *
 * @author Marcel Sander <marcel.sander@actidoo.com>
 */
@DisabledIfApiServerTest
@QuarkusTest
public class ClusterWideTest extends BaseOperatorTest {

    @BeforeAll
    public static void before(TestInfo testInfo) throws FileNotFoundException {
        initAll(testInfo, true);
    }

    @Test
    public void testCrossNamespaceReconciliation() {
        String remoteNamespace = getNewRandomNamespaceName();
        try {
            // Create a second namespace
            Log.info("Creating remote namespace: " + remoteNamespace);
            k8sclient.resource(new NamespaceBuilder()
                    .withNewMetadata()
                    .addToLabels("app", "keycloak-test")
                    .withName(remoteNamespace)
                    .endMetadata()
                    .build()).create();

            deployDependencies(remoteNamespace);

            // Create Keycloak CR in the remote namespace
            var kc = getTestKeycloakDeployment(true);
            kc.getMetadata().setNamespace(remoteNamespace);
            var deploymentName = kc.getMetadata().getName();
            Log.info("Deploying Keycloak CR '" + deploymentName + "' in remote namespace: " + remoteNamespace);
            k8sclient.resource(kc).inNamespace(remoteNamespace).create();

            // Verify that the operator reconciles the CR in the remote namespace
            Log.info("Waiting for StatefulSet to be created in remote namespace");
            Awaitility.await()
                    .untilAsserted(() -> assertThat(
                            k8sclient.apps().statefulSets()
                                    .inNamespace(remoteNamespace)
                                    .withName(deploymentName)
                                    .get())
                            .isNotNull());

            // Verify the Keycloak status is updated
            Log.info("Waiting for Keycloak to become ready");
            Awaitility.await()
                    .untilAsserted(() -> {
                        var currentKc = k8sclient.resources(Keycloak.class)
                                .inNamespace(remoteNamespace)
                                .withName(deploymentName)
                                .get();
                        assertThat(currentKc.getStatus()).isNotNull();
                        assertKeycloakStatusCondition(currentKc, KeycloakStatusCondition.READY, true);
                    });

            Log.info("Cross-namespace reconciliation verified successfully");
        } finally {
            // Clean up the remote namespace
            Log.info("Cleaning up remote namespace: " + remoteNamespace);
            k8sclient.namespaces().withName(remoteNamespace).delete();
        }
    }

    @Test
    public void testMultiNamespaceReconciliation() {
        String namespaceA = getNewRandomNamespaceName();
        String namespaceB = getNewRandomNamespaceName();
        try {
            // Create two namespaces
            for (String ns : new String[]{namespaceA, namespaceB}) {
                Log.info("Creating namespace: " + ns);
                k8sclient.resource(new NamespaceBuilder()
                        .withNewMetadata()
                        .addToLabels("app", "keycloak-test")
                        .withName(ns)
                        .endMetadata()
                        .build()).create();
                deployDependencies(ns);
            }

            // Deploy Keycloak CR in both namespaces
            var kcA = getTestKeycloakDeployment(true);
            kcA.getMetadata().setNamespace(namespaceA);
            var deploymentName = kcA.getMetadata().getName();
            Log.info("Deploying Keycloak CR '" + deploymentName + "' in namespace A: " + namespaceA);
            k8sclient.resource(kcA).inNamespace(namespaceA).create();

            var kcB = getTestKeycloakDeployment(true);
            kcB.getMetadata().setNamespace(namespaceB);
            Log.info("Deploying Keycloak CR '" + deploymentName + "' in namespace B: " + namespaceB);
            k8sclient.resource(kcB).inNamespace(namespaceB).create();

            // Verify both become ready
            for (String ns : new String[]{namespaceA, namespaceB}) {
                Log.info("Waiting for Keycloak to become ready in namespace: " + ns);
                Awaitility.await()
                        .untilAsserted(() -> {
                            var currentKc = k8sclient.resources(Keycloak.class)
                                    .inNamespace(ns)
                                    .withName(deploymentName)
                                    .get();
                            assertThat(currentKc.getStatus()).isNotNull();
                            assertKeycloakStatusCondition(currentKc, KeycloakStatusCondition.READY, true);
                        });
            }

            Log.info("Multi-namespace reconciliation verified successfully");
        } finally {
            Log.info("Cleaning up namespaces: " + namespaceA + ", " + namespaceB);
            k8sclient.namespaces().withName(namespaceA).delete();
            k8sclient.namespaces().withName(namespaceB).delete();
        }
    }

    private static void deployDependencies(String targetNamespace) {
        Secret dbSecret = getResourceFromFile("example-db-secret.yaml", Secret.class);
        dbSecret.getMetadata().setNamespace(targetNamespace);
        k8sclient.resource(dbSecret).inNamespace(targetNamespace).create();

        Secret tlsSecret = K8sUtils.getDefaultTlsSecret();
        tlsSecret.getMetadata().setNamespace(targetNamespace);
        k8sclient.resource(tlsSecret).inNamespace(targetNamespace).create();

        K8sUtils.set(k8sclient, Objects.requireNonNull(ClusterWideTest.class.getResourceAsStream("/example-postgres.yaml")),
                resource -> {
                    resource.getMetadata().setNamespace(targetNamespace);
                    return resource;
                });
        Awaitility.await().untilAsserted(() -> assertThat(Optional.ofNullable(k8sclient.apps().statefulSets()
                .inNamespace(targetNamespace)
                .withName(POSTGRESQL_NAME)
                .get())
                .map(s -> Optional.ofNullable(s.getStatus())
                        .map(status -> status.getReadyReplicas())
                        .orElse(0))
                .orElse(0)).isEqualTo(1));
    }
}

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

package org.keycloak.operator.testsuite.integration;

import io.fabric8.kubernetes.api.model.LocalObjectReference;
import io.fabric8.kubernetes.api.model.LocalObjectReferenceBuilder;
import io.fabric8.kubernetes.api.model.NamespaceBuilder;
import io.fabric8.kubernetes.api.model.PodTemplateSpecBuilder;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.apps.StatefulSet;
import io.fabric8.kubernetes.api.model.apps.StatefulSetBuilder;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.fabric8.kubernetes.client.utils.Serialization;
import io.quarkus.logging.Log;
import io.quarkus.test.junit.QuarkusTest;

import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;
import org.keycloak.operator.Utils;
import org.keycloak.operator.crds.v2alpha1.deployment.Keycloak;
import org.keycloak.operator.testsuite.apiserver.DisabledIfApiServerTest;
import org.keycloak.operator.testsuite.utils.CRAssert;
import org.keycloak.operator.testsuite.utils.K8sUtils;

import java.util.Collections;
import java.util.concurrent.TimeUnit;

import static java.util.concurrent.TimeUnit.MINUTES;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.keycloak.operator.crds.v2alpha1.deployment.KeycloakStatusCondition.HAS_ERRORS;
import static org.keycloak.operator.testsuite.utils.K8sUtils.deployKeycloak;
import static org.keycloak.operator.testsuite.utils.K8sUtils.getResourceFromFile;

@QuarkusTest
public class PodTemplateTest extends BaseOperatorTest {

    private Keycloak getEmptyPodTemplateKeycloak() {
        return Serialization.unmarshal(getClass().getResourceAsStream("/empty-podtemplate-keycloak.yml"), Keycloak.class);
    }

    private Resource<Keycloak> getCrSelector() {
        return k8sclient
                .resources(Keycloak.class)
                .inNamespace(namespace)
                .withName("example-podtemplate");
    }

    @DisabledIfApiServerTest
    @Test
    public void testPodTemplateIsMerged() {
        // Act
        K8sUtils.set(k8sclient, getClass().getResourceAsStream("/correct-podtemplate-keycloak.yml"));

        // Assert
        Awaitility
                .await()
                .ignoreExceptions()
                .atMost(3, MINUTES).untilAsserted(() -> {
            Log.info("Getting logs from Keycloak");

            var keycloakPod = k8sclient
                    .pods()
                    .inNamespace(namespace)
                    .withName("example-podtemplate-kc-0").get();

            var logs = k8sclient
                    .pods()
                    .inNamespace(namespace)
                    .withName(keycloakPod.getMetadata().getName())
                    .getLog();

            Log.info("Full logs are:\n" + logs);
            assertThat(logs).contains("Hello World");
            assertThat(keycloakPod.getMetadata().getLabels().get("foo")).isEqualTo("bar");
        });
    }

    @Test
    public void testPodTemplateIncorrectName() {
        // Arrange
        var plainKc = getEmptyPodTemplateKeycloak();
        var podTemplate = new PodTemplateSpecBuilder()
                .withNewMetadata()
                .withName("foo")
                .endMetadata()
                .build();
        plainKc.getSpec().getUnsupported().setPodTemplate(podTemplate);

        // Act
        K8sUtils.set(k8sclient, plainKc);

        // Assert
        Log.info("Getting status of Keycloak");
        Awaitility
                .await()
                .ignoreExceptions()
                .atMost(3, MINUTES).untilAsserted(() -> {
                    CRAssert.assertKeycloakStatusCondition(getCrSelector().get(), HAS_ERRORS, false, "cannot be modified");
                });
    }

    @Test
    public void testPodTemplateIncorrectNamespace() {
        final String wrongNamespace = getNewRandomNamespaceName();
        try {
            // Arrange
            Log.info("Using incorrect namespace: " + wrongNamespace);
            k8sclient.resource(new NamespaceBuilder().withNewMetadata().withName(wrongNamespace).endMetadata().build()).create(); // OpenShift actually checks existence of the NS
            var plainKc = getEmptyPodTemplateKeycloak();
            var podTemplate = new PodTemplateSpecBuilder()
                    .withNewMetadata()
                    .withNamespace(wrongNamespace)
                    .endMetadata()
                    .build();
            plainKc.getSpec().getUnsupported().setPodTemplate(podTemplate);

            // Act
            K8sUtils.set(k8sclient, plainKc);

            // Assert
            Log.info("Getting status of Keycloak");
            Awaitility
                    .await()
                    .ignoreExceptions()
                    .atMost(3, MINUTES).untilAsserted(() -> {
                        CRAssert.assertKeycloakStatusCondition(getCrSelector().get(), HAS_ERRORS, false, "cannot be modified");
                    });
        }
        finally {
            Log.info("Deleting incorrect namespace: " + wrongNamespace);
            k8sclient.namespaces().withName(wrongNamespace).delete();
        }
    }

    @Test
    public void testPodTemplateIncorrectContainerName() {
        // Arrange
        var plainKc = getEmptyPodTemplateKeycloak();
        var podTemplate = new PodTemplateSpecBuilder()
                .withNewSpec()
                .addNewContainer()
                .withName("baz")
                .endContainer()
                .endSpec()
                .build();
        plainKc.getSpec().getUnsupported().setPodTemplate(podTemplate);

        // Act
        K8sUtils.set(k8sclient, plainKc);

        // Assert
        Log.info("Getting status of Keycloak");
        Awaitility
                .await()
                .ignoreExceptions()
                .atMost(3, MINUTES).untilAsserted(() -> {
                    CRAssert.assertKeycloakStatusCondition(getCrSelector().get(), HAS_ERRORS, false, "cannot be modified");
                });
    }

    @Test
    public void testPodTemplateIncorrectDockerImage() {
        // Arrange
        var plainKc = getEmptyPodTemplateKeycloak();
        var podTemplate = new PodTemplateSpecBuilder()
                .withNewSpec()
                .addNewContainer()
                .withImage("foo")
                .endContainer()
                .endSpec()
                .build();
        plainKc.getSpec().getUnsupported().setPodTemplate(podTemplate);

        // Act
        K8sUtils.set(k8sclient, plainKc);

        // Assert
        Log.info("Getting status of Keycloak");
        Awaitility
                .await()
                .ignoreExceptions()
                .atMost(3, MINUTES).untilAsserted(() -> {
                    CRAssert.assertKeycloakStatusCondition(getCrSelector().get(), HAS_ERRORS, false, "cannot be modified");
                });
    }

    @Test
    public void testPodTemplateIncorrectImagePullSecretsConfig() {
        String imagePullSecretName = "docker-regcred-custom-kc-imagepullsecret-01";
        String secretDescriptorFilename = "test-docker-registry-secret.yaml";

        Secret imagePullSecret = getResourceFromFile(secretDescriptorFilename, Secret.class);
        K8sUtils.set(k8sclient, imagePullSecret);
        LocalObjectReference localObjRefAsSecretTmp = new LocalObjectReferenceBuilder().withName(imagePullSecret.getMetadata().getName()).build();

        assertThat(localObjRefAsSecretTmp.getName()).isNotNull();
        assertThat(localObjRefAsSecretTmp.getName()).isEqualTo(imagePullSecretName);

        var podTemplate = new PodTemplateSpecBuilder()
                .withNewSpec()
                    .addAllToImagePullSecrets(Collections.singletonList(localObjRefAsSecretTmp))
                .endSpec()
                .build();

        var plainKc = getEmptyPodTemplateKeycloak();
        plainKc.getSpec().getUnsupported().setPodTemplate(podTemplate);

        // Act
        K8sUtils.set(k8sclient, plainKc);

        // Assert
        Log.info("Getting status of Keycloak");
        Awaitility
                .await()
                .ignoreExceptions()
                .atMost(3, MINUTES).untilAsserted(() -> {
                    CRAssert.assertKeycloakStatusCondition(getCrSelector().get(), HAS_ERRORS, false, "imagePullSecrets");
                    CRAssert.assertKeycloakStatusCondition(getCrSelector().get(), HAS_ERRORS, false, "cannot be modified");
                });
    }

    @Test
    public void testDeploymentUpgrade() {
        var kc = getTestKeycloakDeployment(true);
        kc.getSpec().setInstances(2);
        // all preconditions must be met, otherwise the operator sdk will remove the existing statefulset
        KeycloakDeploymentTest.initCustomBootstrapAdminUser(kc);

        // create a dummy StatefulSet representing the 26.0 state that we'll be forced to delete
        StatefulSet statefulSet = new StatefulSetBuilder().withMetadata(kc.getMetadata()).editMetadata()
                .addToLabels(Utils.allInstanceLabels(kc)).endMetadata().withNewSpec().withNewSelector()
                .withMatchLabels(Utils.allInstanceLabels(kc)).endSelector().withReplicas(0)
                .withNewTemplate().withNewMetadata().withLabels(Utils.allInstanceLabels(kc)).endMetadata()
                .withNewSpec().addNewContainer().withName("pause").withImage("registry.k8s.io/pause:3.1")
                .endContainer().endSpec().endTemplate().endSpec().build();
        var ss = k8sclient.resource(statefulSet).create();

        // start will not be successful because the statefulSet is in the way
        deployKeycloak(k8sclient, kc, false);
        // once the statefulset is owned by the keycloak it will be picked up by the informer
        k8sclient.resource(statefulSet).accept(s -> s.addOwnerReference(k8sclient.resource(kc).get()));

        Awaitility.await().atMost(1, TimeUnit.MINUTES).until(() -> k8sclient.resource(statefulSet).get().getSpec().getReplicas() == 2);

        // we don't expect a recreate - that would indicate the operator sdk saw a precondition failing
        assertEquals(ss.getMetadata().getUid(), k8sclient.resource(statefulSet).get().getMetadata().getUid());
    }

}

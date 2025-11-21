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

import io.fabric8.kubernetes.api.model.ConfigMapBuilder;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.apps.StatefulSet;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.quarkus.test.junit.QuarkusTest;

import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;
import org.keycloak.operator.crds.v2alpha1.deployment.spec.TruststoreBuilder;
import org.keycloak.operator.testsuite.unit.WatchedResourcesTest;
import org.keycloak.operator.testsuite.utils.K8sUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.keycloak.operator.testsuite.utils.K8sUtils.deployKeycloak;
import static org.keycloak.operator.testsuite.utils.K8sUtils.getResourceFromFile;

@QuarkusTest
public class KeycloakTruststoresTests extends BaseOperatorTest {
    @Test
    public void testTruststoreMissing() {
        var kc = getTestKeycloakDeployment(true);
        var deploymentName = kc.getMetadata().getName();
        kc.getSpec().getTruststores().put("xyz", new TruststoreBuilder().withNewSecret().withName("xyz").endSecret().build());
        kc.getSpec().getTruststores().put("abc", new TruststoreBuilder().withNewConfigMap().withName("abc").endConfigMap().build());

        deployKeycloak(k8sclient, kc, false);
        Resource<StatefulSet> stsResource = k8sclient.resources(StatefulSet.class).withName(deploymentName);
        Awaitility.await().ignoreExceptions().untilAsserted(() -> {
            StatefulSet statefulSet = stsResource.get();
            assertEquals("true",
                    statefulSet.getMetadata().getAnnotations().get(WatchedResourcesTest.KEYCLOAK_WATCHING_ANNOTATION));
            assertTrue(statefulSet.getMetadata().getAnnotations().get(WatchedResourcesTest.KEYCLOAK_MISSING_SECRETS_ANNOTATION)
                    .contains("xyz"));
            assertEquals("true",
                    statefulSet.getMetadata().getAnnotations().get(WatchedResourcesTest.KEYCLOAK_WATCHING_CONFIGMAPS_ANNOTATION));
            assertTrue(statefulSet.getMetadata().getAnnotations().get(WatchedResourcesTest.KEYCLOAK_MISSING_CONFIGMAPS_ANNOTATION)
                    .contains("abc"));
        });
    }

    @Test
    public void testTrustroreExists() {
        var kc = getTestKeycloakDeployment(true);
        var deploymentName = kc.getMetadata().getName();

        K8sUtils.set(k8sclient, getResourceFromFile("example-truststore-secret.yaml", Secret.class));
        kc.getSpec().getTruststores().put("example", new TruststoreBuilder().withNewSecret().withName("example-truststore-secret").endSecret().build());
        kc.getSpec().getTruststores().put("abc", new TruststoreBuilder().withNewConfigMap().withName("abc").endConfigMap().build());

        k8sclient.configMaps().resource(new ConfigMapBuilder().withNewMetadata().withName("abc").endMetadata().build()).create();

        deployKeycloak(k8sclient, kc, true);
        Resource<StatefulSet> stsResource = k8sclient.resources(StatefulSet.class).withName(deploymentName);
        StatefulSet statefulSet = stsResource.get();
        assertNull(statefulSet.getMetadata().getAnnotations().get(WatchedResourcesTest.KEYCLOAK_MISSING_SECRETS_ANNOTATION));
        assertTrue(statefulSet.getSpec().getTemplate().getSpec().getContainers().get(0).getVolumeMounts().stream()
                .anyMatch(v -> v.getMountPath()
                        .equals("/opt/keycloak/conf/truststores/secret-example-truststore-secret")));
        assertNull(statefulSet.getMetadata().getAnnotations().get(WatchedResourcesTest.KEYCLOAK_MISSING_CONFIGMAPS_ANNOTATION));
        assertTrue(statefulSet.getSpec().getTemplate().getSpec().getContainers().get(0).getVolumeMounts().stream()
                .anyMatch(v -> v.getMountPath()
                        .equals("/opt/keycloak/conf/truststores/configmap-abc")));
    }

}

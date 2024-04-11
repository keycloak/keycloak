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
import io.fabric8.kubernetes.api.model.apps.StatefulSet;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.quarkus.logging.Log;
import io.quarkus.test.junit.QuarkusTest;

import org.awaitility.Awaitility;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.keycloak.operator.crds.v2alpha1.deployment.Keycloak;
import org.keycloak.operator.crds.v2alpha1.deployment.KeycloakStatusCondition;
import org.keycloak.operator.crds.v2alpha1.deployment.spec.CacheSpecBuilder;
import org.keycloak.operator.testsuite.utils.CRAssert;
import org.keycloak.operator.testsuite.utils.K8sUtils;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.keycloak.operator.testsuite.utils.K8sUtils.deployKeycloak;

@QuarkusTest
public class CacheTest extends BaseOperatorTest {

    private static final String CONFIGMAP_NAME = "my-config";

    @AfterEach
    public void cleanupConfigMap() {
        k8sclient.configMaps().withName(CONFIGMAP_NAME).delete();
    }

    @Test
    public void testCreateCacheConfigMapFileAfterDeployment() {
        var kc = getTestKeycloakDeployment(true);
        var deploymentName = kc.getMetadata().getName();
        kc.getSpec().setCacheSpec(new CacheSpecBuilder().withNewConfigMapFile("file", CONFIGMAP_NAME, false).build());

        deployKeycloak(k8sclient, kc, false);

        // Check Operator has deployed Keycloak and the statefulset exists, this allows
        // for the watched configmap to be picked up
        Log.info("Checking Operator has deployed Keycloak deployment");
        Resource<StatefulSet> stsResource = k8sclient.resources(StatefulSet.class).withName(deploymentName);
        Resource<Keycloak> keycloakResource = k8sclient.resources(Keycloak.class).withName(deploymentName);
        // expect no errors and not ready, which means we'll keep reconciling
        Awaitility.await().ignoreExceptions().atMost(2, TimeUnit.MINUTES).untilAsserted(() -> {
            assertThat(stsResource.get()).isNotNull();
            Keycloak keycloak = keycloakResource.get();
            CRAssert.assertKeycloakStatusCondition(keycloak, KeycloakStatusCondition.HAS_ERRORS, false);
            CRAssert.assertKeycloakStatusCondition(keycloak, KeycloakStatusCondition.READY, false);
        });

        createCacheConfigMap();

        K8sUtils.waitForKeycloakToBeReady(k8sclient, kc);
    }

    @Test
    public void testCacheConfigMapFile() {
        var kc = getTestKeycloakDeployment(true);
        kc.getSpec().setCacheSpec(new CacheSpecBuilder().withNewConfigMapFile("file", CONFIGMAP_NAME, false).build());

        createCacheConfigMap();
        deployKeycloak(k8sclient, kc, true);
    }

    private void createCacheConfigMap() {
        k8sclient.configMaps()
                .resource(new ConfigMapBuilder().withNewMetadata().withName(CONFIGMAP_NAME).endMetadata()
                        .addToData("file",
                                """
                                        <infinispan
                                                xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                                                xsi:schemaLocation="urn:infinispan:config:14.0 http://www.infinispan.org/schemas/infinispan-config-14.0.xsd"
                                                xmlns="urn:infinispan:config:14.0">

                                            <cache-container name="keycloak">
                                                <local-cache name="default">
                                                    <transaction transaction-manager-lookup="org.infinispan.transaction.lookup.JBossStandaloneJTAManagerLookup"/>
                                                </local-cache>
                                            </cache-contianer>
                                        </infinispan>""")
                        .build())
                .create();
    }

}
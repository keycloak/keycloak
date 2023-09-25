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

import io.fabric8.kubernetes.api.model.LocalObjectReferenceBuilder;
import io.quarkus.logging.Log;
import io.quarkus.test.junit.QuarkusTest;

import org.awaitility.Awaitility;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.keycloak.operator.controllers.KeycloakServiceDependentResource;
import org.keycloak.operator.crds.v2alpha1.realmimport.KeycloakRealmImport;
import org.keycloak.operator.testsuite.utils.CRAssert;
import org.keycloak.operator.testsuite.utils.K8sUtils;

import java.util.Arrays;
import java.util.stream.Collectors;

import static java.util.concurrent.TimeUnit.MINUTES;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.keycloak.operator.Constants.KEYCLOAK_HTTPS_PORT;
import static org.keycloak.operator.controllers.KeycloakDistConfigurator.getKeycloakOptionEnvVarName;
import static org.keycloak.operator.crds.v2alpha1.realmimport.KeycloakRealmImportStatusCondition.DONE;
import static org.keycloak.operator.crds.v2alpha1.realmimport.KeycloakRealmImportStatusCondition.HAS_ERRORS;
import static org.keycloak.operator.crds.v2alpha1.realmimport.KeycloakRealmImportStatusCondition.STARTED;
import static org.keycloak.operator.testsuite.utils.K8sUtils.deployKeycloak;
import static org.keycloak.operator.testsuite.utils.K8sUtils.inClusterCurl;

@QuarkusTest
public class RealmImportTest extends BaseOperatorTest {

    @Override
    @BeforeEach
    public void beforeEach(TestInfo testInfo) {
        super.beforeEach(testInfo);
        // Recreating the database and the realm import CR to keep this test isolated
        k8sclient.load(getClass().getResourceAsStream("/example-realm.yaml")).inNamespace(namespace).delete();
        k8sclient.load(getClass().getResourceAsStream("/incorrect-realm.yaml")).inNamespace(namespace).delete();
        deleteDB();
        deployDB();
    }

    private String getJobArgs() {
        return k8sclient
                .batch()
                .v1()
                .jobs()
                .inNamespace(namespace)
                .withName("example-count0-kc")
                .get()
                .getSpec()
                .getTemplate()
                .getSpec()
                .getContainers()
                .get(0)
                .getArgs()
                .stream()
                .collect(Collectors.joining());
    }

    @Test
    public void testWorkingRealmImport() {
        // Arrange
        var kc = getTestKeycloakDeployment(false);
        kc.getSpec().setImage(null); // checks the job args for the base, not custom image
        kc.getSpec().setImagePullSecrets(Arrays.asList(new LocalObjectReferenceBuilder().withName("my-empty-secret").build()));
        deployKeycloak(k8sclient, kc, false);

        // Act
        K8sUtils.set(k8sclient, getClass().getResourceAsStream("/example-realm.yaml"));

        // Assert
        var crSelector = k8sclient
                .resources(KeycloakRealmImport.class)
                .inNamespace(namespace)
                .withName("example-count0-kc");
        Awaitility.await()
                .atMost(5, MINUTES)
                .pollDelay(1, SECONDS)
                .ignoreExceptions()
                .untilAsserted(() -> {
                    KeycloakRealmImport cr = crSelector.get();
                    CRAssert.assertKeycloakRealmImportStatusCondition(cr, DONE, false);
                    CRAssert.assertKeycloakRealmImportStatusCondition(cr, STARTED, true);
                    CRAssert.assertKeycloakRealmImportStatusCondition(cr, HAS_ERRORS, false);
                });

        Awaitility.await()
                .atMost(5, MINUTES)
                .pollDelay(1, SECONDS)
                .ignoreExceptions()
                .untilAsserted(() -> {
                    KeycloakRealmImport cr = crSelector.get();
                    CRAssert.assertKeycloakRealmImportStatusCondition(cr, DONE, true);
                    CRAssert.assertKeycloakRealmImportStatusCondition(cr, STARTED, false);
                    CRAssert.assertKeycloakRealmImportStatusCondition(cr, HAS_ERRORS, false);
                });
        var job = k8sclient.batch().v1().jobs().inNamespace(namespace).withName("example-count0-kc").get();
        assertThat(job.getSpec().getTemplate().getMetadata().getLabels().get("app")).isEqualTo("keycloak-realm-import");
        var envvars = job.getSpec().getTemplate().getSpec().getContainers().get(0).getEnv();
        assertThat(envvars.stream().filter(e -> e.getName().equals(getKeycloakOptionEnvVarName("cache"))).findAny().get().getValue()).isEqualTo("local");
        assertThat(envvars.stream().filter(e -> e.getName().equals(getKeycloakOptionEnvVarName("health-enabled"))).findAny().get().getValue()).isEqualTo("false");
        assertThat(job.getSpec().getTemplate().getSpec().getImagePullSecrets().size()).isEqualTo(1);
        assertThat(job.getSpec().getTemplate().getSpec().getImagePullSecrets().get(0).getName()).isEqualTo("my-empty-secret");

        String url =
                "https://" + KeycloakServiceDependentResource.getServiceName(kc) + "." + namespace + ":" + KEYCLOAK_HTTPS_PORT + "/realms/count0";

        Awaitility.await().atMost(10, MINUTES).ignoreExceptions().untilAsserted(() -> {
            Log.info("Starting curl Pod to test if the realm is available");
            Log.info("Url: '" + url + "'");
            String curlOutput = inClusterCurl(k8sclient, namespace, url);
            Log.info("Output from curl: '" + curlOutput + "'");
            assertThat(curlOutput).isEqualTo("200");
        });

        assertThat(getJobArgs()).contains("build");
    }

    @Test
    @EnabledIfSystemProperty(named = OPERATOR_CUSTOM_IMAGE, matches = ".+")
    public void testWorkingRealmImportWithCustomImage() {
        // Arrange
        var keycloak = getTestKeycloakDeployment(false);
        keycloak.getSpec().setImage(customImage);
        // Removing the Database so that a subsequent build will by default act on h2
        // TODO: uncomment the following line after resolution of: https://github.com/keycloak/keycloak/issues/11767
        // keycloak.getSpec().getAdditionalOptions().removeIf(sc -> sc.getName().equals("db"));
        deployKeycloak(k8sclient, keycloak, false);

        // Act
        K8sUtils.set(k8sclient, getClass().getResourceAsStream("/example-realm.yaml"));

        // Assert
        var crSelector = k8sclient
                .resources(KeycloakRealmImport.class)
                .inNamespace(namespace)
                .withName("example-count0-kc");

        Awaitility.await()
                .atMost(3, MINUTES)
                .pollDelay(5, SECONDS)
                .ignoreExceptions()
                .untilAsserted(() -> {
                    KeycloakRealmImport cr = crSelector.get();
                    CRAssert.assertKeycloakRealmImportStatusCondition(cr, DONE, true);
                    CRAssert.assertKeycloakRealmImportStatusCondition(cr, STARTED, false);
                    CRAssert.assertKeycloakRealmImportStatusCondition(cr, HAS_ERRORS, false);
                });

        assertThat(getJobArgs()).doesNotContain("build");
    }

    @Test
    public void testNotWorkingRealmImport() {
        // Arrange
        deployKeycloak(k8sclient, getTestKeycloakDeployment(false), true); // make sure there are no errors due to missing KC Deployment

        // Act
        K8sUtils.set(k8sclient, getClass().getResourceAsStream("/incorrect-realm.yaml"));

        // Assert
        Awaitility.await()
                .atMost(3, MINUTES)
                .pollDelay(5, SECONDS)
                .ignoreExceptions()
                .untilAsserted(() -> {
                    var crSelector = k8sclient
                            .resources(KeycloakRealmImport.class)
                            .inNamespace(namespace)
                            .withName("example-count0-kc");
                    KeycloakRealmImport cr = crSelector.get();
                    CRAssert.assertKeycloakRealmImportStatusCondition(cr, DONE, false);
                    CRAssert.assertKeycloakRealmImportStatusCondition(cr, STARTED, false);
                    CRAssert.assertKeycloakRealmImportStatusCondition(cr, HAS_ERRORS, true);
                });
    }

}

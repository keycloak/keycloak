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

import com.fasterxml.jackson.databind.JsonNode;
import io.fabric8.kubernetes.client.utils.Serialization;
import io.quarkus.logging.Log;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;
import org.keycloak.operator.Constants;
import org.keycloak.operator.testsuite.utils.CRAssert;
import org.keycloak.operator.controllers.KeycloakService;
import org.keycloak.operator.crds.v2alpha1.deployment.Keycloak;
import org.keycloak.operator.testsuite.utils.K8sUtils;
import org.keycloak.operator.crds.v2alpha1.realmimport.KeycloakRealmImport;
import org.keycloak.operator.crds.v2alpha1.realmimport.KeycloakRealmImportStatusCondition;
import org.keycloak.operator.crds.v2alpha1.deployment.KeycloakStatusCondition;

import java.time.Duration;

import static java.util.concurrent.TimeUnit.MINUTES;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;


@QuarkusTest
public class ClusteringTest extends BaseOperatorTest {

    @Test
    public void testKeycloakScaleAsExpected() {
        // given
        var kc = K8sUtils.getDefaultKeycloakDeployment();
        var crSelector = k8sclient
                .resources(Keycloak.class)
                .inNamespace(kc.getMetadata().getNamespace())
                .withName(kc.getMetadata().getName());
        K8sUtils.deployKeycloak(k8sclient, kc, true);

        var kcPodsSelector = k8sclient.pods().inNamespace(namespace).withLabel("app", "keycloak");

        Keycloak keycloak = crSelector.get();

        // when scale it to 3
        keycloak.getSpec().setInstances(3);
        k8sclient.resources(Keycloak.class).inNamespace(namespace).createOrReplace(keycloak);

        Awaitility.await()
                .atMost(1, MINUTES)
                .pollDelay(1, SECONDS)
                .ignoreExceptions()
                .untilAsserted(() -> CRAssert.assertKeycloakStatusCondition(crSelector.get(), KeycloakStatusCondition.READY, false));

        Awaitility.await()
                .atMost(Duration.ofSeconds(60))
                .ignoreExceptions()
                .untilAsserted(() -> assertThat(kcPodsSelector.list().getItems().size()).isEqualTo(3));

        // when scale it down to 2
        keycloak.getSpec().setInstances(2);
        k8sclient.resources(Keycloak.class).inNamespace(namespace).createOrReplace(keycloak);
        Awaitility.await()
                .atMost(Duration.ofSeconds(180))
                .ignoreExceptions()
                .untilAsserted(() -> assertThat(kcPodsSelector.list().getItems().size()).isEqualTo(2));

        Awaitility.await()
                .atMost(5, MINUTES)
                .pollDelay(1, SECONDS)
                .ignoreExceptions()
                .untilAsserted(() -> CRAssert.assertKeycloakStatusCondition(crSelector.get(), KeycloakStatusCondition.READY, true));

        // get the service
        var service = new KeycloakService(k8sclient, kc);
        String url = "https://" + service.getName() + "." + namespace + ":" + Constants.KEYCLOAK_HTTPS_PORT;

        Awaitility.await().atMost(5, MINUTES).untilAsserted(() -> {
            Log.info("Starting curl Pod to test if the realm is available");
            Log.info("Url: '" + url + "'");
            String curlOutput = K8sUtils.inClusterCurl(k8sclient, namespace, url);
            Log.info("Output from curl: '" + curlOutput + "'");
            assertThat(curlOutput).isEqualTo("200");
        });
    }

    // local debug commands:
    //    export TOKEN=$(curl --data "grant_type=password&client_id=token-test-client&username=test&password=test" http://localhost:8080/realms/token-test/protocol/openid-connect/token | jq -r '.access_token')
    //
    //    curl http://localhost:8080/realms/token-test/protocol/openid-connect/userinfo -H "Authorization: bearer $TOKEN"
    //
    //    example good answer:
    //    {"sub":"b660eec6-a93b-46fd-abb2-e9fbdff67a63","email_verified":false,"preferred_username":"test"}
    //    example error answer:
    //    {"error":"invalid_request","error_description":"Token not provided"}
    @Test
    public void testKeycloakCacheIsConnected() throws Exception {
        // given
        Log.info("Setup");
        var kc = K8sUtils.getDefaultKeycloakDeployment();
        var crSelector = k8sclient
                .resources(Keycloak.class)
                .inNamespace(kc.getMetadata().getNamespace())
                .withName(kc.getMetadata().getName());
        K8sUtils.deployKeycloak(k8sclient, kc, false);
        var targetInstances = 3;
        kc.getSpec().setInstances(targetInstances);
        k8sclient.resources(Keycloak.class).inNamespace(namespace).createOrReplace(kc);
        var realm = k8sclient.resources(KeycloakRealmImport.class).inNamespace(namespace).load(getClass().getResourceAsStream("/token-test-realm.yaml"));
        var realmImportSelector = k8sclient.resources(KeycloakRealmImport.class).inNamespace(namespace).withName("example-token-test-kc");
        realm.createOrReplace();

        Log.info("Waiting for a stable Keycloak Cluster");
        Awaitility.await()
                .atMost(10, MINUTES)
                .pollDelay(5, SECONDS)
                .ignoreExceptions()
                .untilAsserted(() -> {
                    Log.info("Checking realm import has finished.");
                    CRAssert.assertKeycloakRealmImportStatusCondition(realmImportSelector.get(), KeycloakRealmImportStatusCondition.DONE, true);
                    Log.info("Checking Keycloak is stable.");
                    CRAssert.assertKeycloakStatusCondition(crSelector.get(), KeycloakStatusCondition.READY, true);
                });
        // Remove the completed pod for the job
        realmImportSelector.delete();

        Log.info("Testing the Keycloak Cluster");
        Awaitility.await()
                .atMost(20, MINUTES)
                .pollDelay(5, SECONDS)
                .ignoreExceptions()
                .untilAsserted(() -> {
            // Get the list of Keycloak pods
            var pods = k8sclient
                    .pods()
                    .inNamespace(namespace)
                    .withLabels(Constants.DEFAULT_LABELS)
                    .list()
                    .getItems();

            String token = null;
            // Obtaining the token from the first pod
            // Connecting using port-forward and a fixed port to respect the instance issuer used hostname
            for (var pod: pods) {
                Log.info("Testing Pod: " + pod.getMetadata().getName());
                try (var portForward = k8sclient
                        .pods()
                        .inNamespace(namespace)
                        .withName(pod.getMetadata().getName())
                        .portForward(8443, 8443)) {

                    token = (token != null) ? token : RestAssured.given()
                            .relaxedHTTPSValidation()
                            .param("grant_type" , "password")
                            .param("client_id", "token-test-client")
                            .param("username", "test")
                            .param("password", "test")
                            .post("https://localhost:" + portForward.getLocalPort() + "/realms/token-test/protocol/openid-connect/token")
                            .body()
                            .jsonPath()
                            .getString("access_token");

                    Log.info("Using token:" + token);

                    var username = RestAssured.given()
                            .relaxedHTTPSValidation()
                            .header("Authorization",  "Bearer " + token)
                            .get("https://localhost:" + portForward.getLocalPort() + "/realms/token-test/protocol/openid-connect/userinfo")
                            .body()
                            .jsonPath()
                            .getString("preferred_username");

                    Log.info("Username found: " + username);

                    assertThat(username).isEqualTo("test");
                }
            }
        });

        // This is to test passing through the "Service", not 100% deterministic, but a smoke test that things are working as expected
        // Executed here to avoid paying the setup time again
        var service = new KeycloakService(k8sclient, kc);
        Awaitility.await()
                .atMost(20, MINUTES)
                .pollDelay(5, SECONDS)
                .ignoreExceptions()
                .untilAsserted(() -> {
            String token2 = null;
            // Obtaining the token from the first pod
            for (int i = 0; i < (targetInstances + 1); i++) {

                if (token2 == null) {
                    var tokenUrl = "https://" + service.getName() + "." + namespace + ":" + Constants.KEYCLOAK_HTTPS_PORT + "/realms/token-test/protocol/openid-connect/token";
                    Log.info("Checking url: " + tokenUrl);

                    var tokenOutput = K8sUtils.inClusterCurl(k8sclient, namespace, "--insecure", "-s", "--data", "grant_type=password&client_id=token-test-client&username=test&password=test", tokenUrl);
                    Log.info("Curl Output with token: " + tokenOutput);
                    JsonNode tokenAnswer = Serialization.jsonMapper().readTree(tokenOutput);
                    assertThat(tokenAnswer.hasNonNull("access_token")).isTrue();
                    token2 = tokenAnswer.get("access_token").asText();
                }

                String url = "https://" + service.getName() + "." + namespace + ":" + Constants.KEYCLOAK_HTTPS_PORT + "/realms/token-test/protocol/openid-connect/userinfo";
                Log.info("Checking url: " + url);

                var curlOutput = K8sUtils.inClusterCurl(k8sclient, namespace, "--insecure", "-s", "-H", "Authorization: Bearer " + token2, url);
                Log.info("Curl Output on access attempt: " + curlOutput);

                JsonNode answer = Serialization.jsonMapper().readTree(curlOutput);
                assertThat(answer.hasNonNull("preferred_username")).isTrue();
                assertThat(answer.get("preferred_username").asText()).isEqualTo("test");
            }
        });
    }
}

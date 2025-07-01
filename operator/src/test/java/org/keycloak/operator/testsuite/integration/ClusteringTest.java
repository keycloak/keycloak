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

import io.fabric8.kubernetes.api.model.EnvVar;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.apps.StatefulSet;
import io.fabric8.kubernetes.api.model.networking.v1.Ingress;
import io.fabric8.kubernetes.client.readiness.Readiness;
import io.fabric8.kubernetes.client.utils.Serialization;
import io.quarkus.logging.Log;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;

import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;
import org.keycloak.operator.Constants;
import org.keycloak.operator.controllers.KeycloakServiceDependentResource;
import org.keycloak.operator.crds.v2alpha1.deployment.Keycloak;
import org.keycloak.operator.crds.v2alpha1.deployment.KeycloakStatusCondition;
import org.keycloak.operator.crds.v2alpha1.realmimport.KeycloakRealmImport;
import org.keycloak.operator.crds.v2alpha1.realmimport.KeycloakRealmImportStatusCondition;
import org.keycloak.operator.testsuite.apiserver.DisabledIfApiServerTest;
import org.keycloak.operator.testsuite.utils.CRAssert;
import org.keycloak.operator.testsuite.utils.K8sUtils;

import java.time.Duration;
import java.util.Optional;
import java.util.function.Function;

import com.fasterxml.jackson.databind.JsonNode;

import static java.util.concurrent.TimeUnit.MINUTES;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.keycloak.operator.controllers.KeycloakDeploymentDependentResource.KC_TRACING_SERVICE_NAME;

@DisabledIfApiServerTest
@QuarkusTest
public class ClusteringTest extends BaseOperatorTest {

    @Test
    public void testMultipleDeployments() throws InterruptedException {
        // given
        var kc = getTestKeycloakDeployment(true);

        // another instance running off the same database
        // - should eventually give this a separate schema
        var kc1 = getTestKeycloakDeployment(true);
        kc1.getMetadata().setName("another-example");
        kc1.getSpec().getHostnameSpec().setHostname("another-example.com");
        // this is using the wrong tls-secret, but simply removing http spec renders the pod unstartable

        try {
            K8sUtils.deployKeycloak(k8sclient, kc, true);
            K8sUtils.deployKeycloak(k8sclient, kc1, true);
        } catch (Exception e) {
            k8sclient.resources(Keycloak.class).list().getItems().stream().forEach(k -> {
                Log.infof("Keycloak %s status: %s", k.getMetadata().getName(), Serialization.asYaml(k.getStatus()));
            });
            k8sclient.pods().list().getItems().stream().filter(p -> !Readiness.isPodReady(p)).forEach(p -> {
                Log.infof("Pod %s not ready: %s", p.getMetadata().getName(), Serialization.asYaml(p.getStatus()));
            });
            throw e;
        }

        assertThat(k8sclient.resources(Keycloak.class).list().getItems().size()).isEqualTo(2);

        // get the current version for the uid
        kc = k8sclient.resource(kc).get();
        kc1 = k8sclient.resource(kc1).get();

        // the main resources are ready, check for the expected dependents
        checkInstanceCount(1, StatefulSet.class, kc, kc1);
        checkInstanceCount(1, Secret.class, kc, kc1);
        checkInstanceCount(1, Ingress.class, kc, kc1);
        checkInstanceCount(2, Service.class, kc, kc1);

        // Tracing assertions
        var pods = k8sclient
                .pods()
                .inNamespace(namespace)
                .withLabels(Constants.DEFAULT_LABELS)
                .list()
                .getItems();

        assertThat(pods.size()).isEqualTo(2);

        Function<Pod, String> getTracingServiceName = (pod) -> pod.getSpec().getContainers().get(0).getEnv().stream()
                .filter(f -> f.getName().equals(KC_TRACING_SERVICE_NAME)).findAny().map(EnvVar::getValue).orElse(null);

        var kc1Pod = pods.stream().filter(f -> f.getMetadata().getName().startsWith("another-example-")).findAny().orElse(null);
        assertThat(kc1Pod).isNotNull();

        var tracingServiceName1 = getTracingServiceName.apply(kc1Pod);
        assertThat(tracingServiceName1).isNotNull();
        assertThat(tracingServiceName1).isEqualTo("another-example");

        var kcPod = pods.stream().filter(f -> !f.equals(kc1Pod)).findAny().orElse(null);
        assertThat(kcPod).isNotNull();

        var tracingServiceName2 = getTracingServiceName.apply(kcPod);
        assertThat(tracingServiceName2).isNotNull();
        assertThat(tracingServiceName2).isEqualTo("example-kc");

        // ensure they don't see each other's pods
        assertThat(k8sclient.resource(kc).scale().getStatus().getReplicas()).isEqualTo(1);
        assertThat(k8sclient.resource(kc1).scale().getStatus().getReplicas()).isEqualTo(1);

        // could also scale one instance to zero end ensure the services are no longer reachable
    }

    private void checkInstanceCount(int count, Class<? extends HasMetadata> type, HasMetadata... toCheck) {
        var instances = k8sclient.resources(type).list().getItems();

        for (HasMetadata hasMetadata : toCheck) {
            assertThat(instances.stream()
                    .filter(h -> h.getOwnerReferenceFor(hasMetadata).isPresent() && hasMetadata.getMetadata()
                            .getName().equals(h.getMetadata().getLabels().get(Constants.INSTANCE_LABEL)))
                    .count()).isEqualTo(count);
        }
    }

    @Test
    public void testKeycloakScaleAsExpected() {
        // given a starting point of a default keycloak with null/default instances
        var kc = getTestKeycloakDeployment(false);
        kc.getSpec().setInstances(null);
        var crSelector = k8sclient.resource(kc);
        K8sUtils.deployKeycloak(k8sclient, kc, true);

        var kcPodsSelector = k8sclient.pods().inNamespace(namespace).withLabel("app", "keycloak");

        var scale = crSelector.scale();
        assertThat(scale.getSpec().getReplicas()).isEqualTo(1);
        assertThat(scale.getStatus().getReplicas()).isEqualTo(1);
        assertThat(scale.getStatus().getSelector()).isEqualTo("app=keycloak,app.kubernetes.io/managed-by=keycloak-operator,app.kubernetes.io/instance=example-kc");

        // when scale it to 0
        Keycloak scaled = crSelector.scale(0);
        assertThat(scaled.getSpec().getInstances()).isEqualTo(0);

        Awaitility.await()
                .atMost(Duration.ofSeconds(60))
                .ignoreExceptions()
                .untilAsserted(() -> assertThat(Optional.ofNullable(crSelector.scale().getStatus().getReplicas()).orElse(0)).isEqualTo(0));

        Awaitility.await()
                .atMost(1, MINUTES)
                .pollDelay(1, SECONDS)
                .ignoreExceptions()
                .untilAsserted(() -> CRAssert.assertKeycloakStatusCondition(crSelector.get(), KeycloakStatusCondition.READY, true));

        // when scale it to 3
        crSelector.scale(3);
        assertThat(crSelector.scale().getSpec().getReplicas()).isEqualTo(3);

        Awaitility.await()
                .atMost(1, MINUTES)
                .pollDelay(1, SECONDS)
                .ignoreExceptions()
                .untilAsserted(() -> CRAssert.assertKeycloakStatusCondition(crSelector.get(), KeycloakStatusCondition.READY, false));

        Awaitility.await()
                .atMost(Duration.ofSeconds(180))
                .ignoreExceptions()
                .untilAsserted(() -> assertThat(kcPodsSelector.list().getItems().size()).isEqualTo(3));

        // when scale it down to 2
        crSelector.scale(2);
        assertThat(crSelector.scale().getSpec().getReplicas()).isEqualTo(2);

        Awaitility.await()
                .atMost(Duration.ofSeconds(180))
                .ignoreExceptions()
                .untilAsserted(() -> assertThat(kcPodsSelector.list().getItems().size()).isEqualTo(2));

        Awaitility.await()
                .atMost(5, MINUTES)
                .pollDelay(1, SECONDS)
                .ignoreExceptions()
                .untilAsserted(() -> CRAssert.assertKeycloakStatusCondition(crSelector.get(), KeycloakStatusCondition.READY, true));

        Awaitility.await()
                .atMost(Duration.ofSeconds(60))
                .ignoreExceptions()
                .untilAsserted(() -> assertThat(crSelector.scale().getStatus().getReplicas()).isEqualTo(2));

        // get the service
        String url = "https://" + KeycloakServiceDependentResource.getServiceName(kc) + "." + namespace + ":" + Constants.KEYCLOAK_HTTPS_PORT + "/admin/master/console/";

        Awaitility.await().atMost(5, MINUTES).untilAsserted(() -> {
            Log.info("Starting curl Pod to test if the realm is available");
            Log.info("Url: '" + url + "'");
            String curlOutput = K8sUtils.inClusterCurl(k8sclient, namespace, url);
            Log.info("Output from curl: '" + curlOutput + "'");
            assertThat(curlOutput).isEqualTo("200");
        });
    }

    // local debug commands:
    //    export TOKEN=$(curl --data "grant_type=password&client_id=token-test-client&username=test&password=test&scope=openid" http://localhost:8080/realms/token-test/protocol/openid-connect/token | jq -r '.access_token')
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
        var kc = getTestKeycloakDeployment(false);
        var crSelector = k8sclient.resource(kc);
        K8sUtils.deployKeycloak(k8sclient, kc, false);
        var targetInstances = 3;
        crSelector.scale(targetInstances);
        K8sUtils.set(k8sclient, getClass().getResourceAsStream("/token-test-realm.yaml"));
        var realmImportSelector = k8sclient.resources(KeycloakRealmImport.class).inNamespace(namespace).withName("example-token-test-kc");

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
                            .param("scope", "openid")
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
        String serviceName = KeycloakServiceDependentResource.getServiceName(kc);
        Awaitility.await()
                .atMost(20, MINUTES)
                .pollDelay(5, SECONDS)
                .ignoreExceptions()
                .untilAsserted(() -> {
            String token2 = null;
            // Obtaining the token from the first pod
            for (int i = 0; i < (targetInstances + 1); i++) {

                if (token2 == null) {
                    var tokenUrl = "https://" + serviceName + "." + namespace + ":" + Constants.KEYCLOAK_HTTPS_PORT + "/realms/token-test/protocol/openid-connect/token";
                    Log.info("Checking url: " + tokenUrl);

                    var tokenOutput = K8sUtils.inClusterCurl(k8sclient, namespace, "--insecure", "-s", "--data", "grant_type=password&client_id=token-test-client&username=test&password=test&scope=openid", tokenUrl);
                    Log.info("Curl Output with token: " + tokenOutput);
                    JsonNode tokenAnswer = Serialization.jsonMapper().readTree(tokenOutput);
                    assertThat(tokenAnswer.hasNonNull("access_token")).isTrue();
                    token2 = tokenAnswer.get("access_token").asText();
                }

                String url = "https://" + serviceName + "." + namespace + ":" + Constants.KEYCLOAK_HTTPS_PORT + "/realms/token-test/protocol/openid-connect/userinfo";
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

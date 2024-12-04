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

package org.keycloak.operator.testsuite.utils;

import java.io.ByteArrayOutputStream;
import java.net.HttpURLConnection;
import java.time.Duration;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import io.fabric8.kubernetes.api.model.PodBuilder;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.ServicePort;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.kubernetes.client.dsl.ExecWatch;
import io.fabric8.kubernetes.client.utils.Serialization;
import io.quarkus.logging.Log;
import org.assertj.core.api.ObjectAssert;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Assertions;
import org.keycloak.operator.Constants;
import org.keycloak.operator.controllers.KeycloakServiceDependentResource;
import org.keycloak.operator.crds.v2alpha1.deployment.Keycloak;
import org.keycloak.operator.crds.v2alpha1.deployment.KeycloakStatus;
import org.keycloak.operator.crds.v2alpha1.deployment.KeycloakStatusCondition;
import org.keycloak.operator.crds.v2alpha1.realmimport.KeycloakRealmImport;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Vaclav Muzikar <vmuzikar@redhat.com>
 */
public final class CRAssert {

    // ISPN000093 -> cluster view
    // ISPN000094 -> merge view
    private static final Pattern CLUSTER_SIZE_PATTERN = Pattern.compile("ISPN00009[34]: [^]]*] \\((\\d+)\\)");

    public static void assertKeycloakStatusCondition(Keycloak kc, String condition, boolean status) {
        assertKeycloakStatusCondition(kc, condition, status, null);
    }
    public static void assertKeycloakStatusCondition(Keycloak kc, String condition, boolean status, String containedMessage) {
        Log.debugf("Asserting CR: %s, condition: %s, status: %s, message: %s", kc.getMetadata().getName(), condition, status, containedMessage);
        try {
            assertKeycloakStatusCondition(kc.getStatus(), condition, status, containedMessage, null);
        } catch (Exception e) {
            Log.infof("Asserting CR: %s with status:\n%s", kc.getMetadata().getName(), Serialization.asYaml(kc.getStatus()));
            throw e;
        }
    }

    public static void assertKeycloakStatusCondition(KeycloakStatus kcStatus, String condition, Boolean status, String containedMessage) {
        assertKeycloakStatusCondition(kcStatus, condition, status, containedMessage, null);
    }

    public static ObjectAssert<KeycloakStatusCondition> assertKeycloakStatusCondition(KeycloakStatus kcStatus, String condition, Boolean status, String containedMessage, Long observedGeneration) {
        KeycloakStatusCondition statusCondition = kcStatus.findCondition(condition).orElseThrow();
        assertThat(statusCondition.getStatus())
                .withFailMessage(() -> "found status " + statusCondition + " and expected status " + status)
                .isEqualTo(status);
        if (containedMessage != null) {
            assertThat(statusCondition.getMessage())
                    .withFailMessage(() -> "found status " + statusCondition + " and expected it to contain " + containedMessage)
                    .contains(containedMessage);
        }
        if (observedGeneration != null) {
            assertThat(statusCondition.getObservedGeneration())
                    .withFailMessage(() -> "found status " + statusCondition + " and expected it to contain an observed generation of " + observedGeneration)
                    .isEqualTo(observedGeneration);
        }
        if (status != null) {
            assertThat(statusCondition.getLastTransitionTime())
                    .withFailMessage(() -> "found status " + statusCondition + " and expected the last transition time to not be null")
                    .isNotNull();
        }
        return assertThat(statusCondition);
    }

    public static void assertKeycloakStatusDoesNotContainMessage(KeycloakStatus kcStatus, String message) {
        assertThat(kcStatus.getConditions())
                .noneMatch(c -> c.getMessage().contains(message));
    }

    public static void assertKeycloakRealmImportStatusCondition(KeycloakRealmImport kri, String condition, Boolean status) {
        assertThat(kri.getStatus().getConditions())
                .anyMatch(c -> c.getType().equals(condition) && Objects.equals(c.getStatus(), status));
    }

    public static void awaitClusterSize(KubernetesClient client, Keycloak keycloak, int expectedSize) {
        Log.infof("Waiting for cluster size of %s", expectedSize);
        Awaitility
                .await()
                .pollInterval(1, TimeUnit.SECONDS)
                .timeout(Duration.ofMinutes(5))
                .untilAsserted(() -> {
                    client.pods()
                            .inNamespace(namespaceOf(keycloak))
                            .withLabels(org.keycloak.operator.Utils.allInstanceLabels(keycloak))
                            .resources()
                            .forEach(pod -> {
                                var logs = pod.getLog();
                                var matcher = CLUSTER_SIZE_PATTERN.matcher(logs);
                                int size = 0;
                                // We want the last view change.
                                // The other alternative is to reverse the string.
                                while (matcher.find()) {
                                    size = Integer.parseInt(matcher.group(1));
                                }
                                Assertions.assertEquals(expectedSize, size, "Wrong cluster size in pod " + pod);
                            });
                });
    }

    public static void assertKeycloakAccessibleViaService(KubernetesClient client, Keycloak keycloak, boolean https, int port) {
        Awaitility.await()
                .ignoreExceptions()
                .untilAsserted(() -> {
                    String protocol = https ? "https" : "http";
                    var namespace = namespaceOf(keycloak);

                    String serviceName = KeycloakServiceDependentResource.getServiceName(keycloak);
                    assertThat(client.resources(Service.class).withName(serviceName).require().getSpec().getPorts()
                            .stream().map(ServicePort::getName).anyMatch(protocol::equals)).isTrue();

                    String url = protocol + "://" + serviceName + "." + namespace + ":" + port + "/admin/master/console/";
                    Log.info("Checking url: " + url);

                    var curlOutput = K8sUtils.inClusterCurl(client, namespace, url);
                    Log.info("Curl Output: " + curlOutput);

                    assertEquals("200", curlOutput);
                });
    }

    public static void assertManagementInterfaceAccessibleViaService(KubernetesClient client, Keycloak kc, boolean https) {
        assertManagementInterfaceAccessibleViaService(client, kc, https, Constants.KEYCLOAK_MANAGEMENT_PORT);
    }

    public static void assertManagementInterfaceAccessibleViaService(KubernetesClient client, Keycloak keycloak, boolean https, int port) {
        Awaitility.await()
                .ignoreExceptions()
                .untilAsserted(() -> {
                    String serviceName = KeycloakServiceDependentResource.getServiceName(keycloak);
                    var namespace = namespaceOf(keycloak);
                    assertThat(client.resources(Service.class).withName(serviceName).require().getSpec().getPorts()
                            .stream().map(ServicePort::getName).anyMatch(Constants.KEYCLOAK_MANAGEMENT_PORT_NAME::equals)).isTrue();

                    String protocol = https ? "https" : "http";
                    String url = protocol + "://" + serviceName + "." + namespace + ":" + port;
                    Log.info("Checking url: " + url);

                    var curlOutput = K8sUtils.inClusterCurl(client, namespace, url);
                    Log.info("Curl Output: " + curlOutput);

                    assertEquals("200", curlOutput);
                });
    }

    public static void assertJGroupsConnection(KubernetesClient client, String podIp, String namespace, Map<String, String> labels, boolean connects) {
        // Send a bogus command to JGroups port
        // relevant exit codes:
        // 28-Operation timeout.
        // 48-Unknown option specified to libcurl.
        int expectedExitCode = connects ? 48 : 28;
        int exitCode;
        try {
            var builder = new PodBuilder();
            builder.withNewMetadata()
                    .withName("curl-telnet-" + UUID.randomUUID())
                    .withNamespace(namespace)
                    .withLabels(labels)
                    .endMetadata();
            builder.withNewSpec()
                    .addNewContainer()
                    .withImage("curlimages/curl:8.1.2")
                    .withCommand("sh")
                    .withName("curl")
                    .withStdin()
                    .endContainer()
                    .endSpec();

            var curlPod = builder.build();
            try {
                client.resource(curlPod).create();
            } catch (KubernetesClientException e) {
                if (e.getCode() != HttpURLConnection.HTTP_CONFLICT) {
                    throw e;
                }
            }

            var args = new String[]{
                    "curl",
                    "--telnet-option",
                    "'BOGUS=1'",
                    "--connect-timeout",
                    "2",
                    "-s",
                    "telnet://%s:7800".formatted(podIp)
            };

            Log.infof("Run telnet: %s", String.join(" ", args));

            try (ExecWatch watch = client.pods().resource(curlPod).withReadyWaitTimeout(60000)
                    .writingOutput(new ByteArrayOutputStream())
                    .exec(args)) {
                exitCode = watch.exitCode().get(15, TimeUnit.SECONDS);
            }

        } catch (Exception ex) {
            throw KubernetesClientException.launderThrowable(ex);
        }
        assertEquals(expectedExitCode, exitCode);
    }

    private static String namespaceOf(Keycloak keycloak) {
        return keycloak.getMetadata().getNamespace();
    }
}

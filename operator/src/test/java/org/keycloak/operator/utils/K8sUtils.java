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

package org.keycloak.operator.utils;

import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.kubernetes.client.extended.run.RunConfigBuilder;
import io.fabric8.kubernetes.client.utils.KubernetesResourceUtil;
import io.fabric8.kubernetes.client.utils.Serialization;
import io.quarkus.logging.Log;
import org.awaitility.Awaitility;
import org.keycloak.operator.v2alpha1.crds.Keycloak;
import org.keycloak.operator.v2alpha1.crds.KeycloakStatusCondition;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * @author Vaclav Muzikar <vmuzikar@redhat.com>
 */
public final class K8sUtils {
    public static <T> T getResourceFromFile(String fileName) {
        return Serialization.unmarshal(Objects.requireNonNull(K8sUtils.class.getResourceAsStream("/" + fileName)), Collections.emptyMap());
    }

    @SuppressWarnings("unchecked")
    public static <T> T getResourceFromMultiResourceFile(String fileName, int index) {
        return ((List<T>) getResourceFromFile(fileName)).get(index);
    }

    public static Keycloak getDefaultKeycloakDeployment() {
        return getResourceFromMultiResourceFile("example-keycloak.yml", 0);
    }

    public static Secret getDefaultTlsSecret() {
        return getResourceFromMultiResourceFile("example-keycloak.yml", 2);
    }


    public static void deployKeycloak(KubernetesClient client, Keycloak kc, boolean waitUntilReady) {
        client.resources(Keycloak.class).inNamespace(kc.getMetadata().getNamespace()).createOrReplace(kc);
        client.secrets().inNamespace(kc.getMetadata().getNamespace()).createOrReplace(getDefaultTlsSecret());

        if (waitUntilReady) {
            waitForKeycloakToBeReady(client, kc);
        }
    }

    public static void deployDefaultKeycloak(KubernetesClient client) {
        deployKeycloak(client, getDefaultKeycloakDeployment(), true);
    }

    public static void waitForKeycloakToBeReady(KubernetesClient client, Keycloak kc) {
        Log.infof("Waiting for Keycloak \"%s\"", kc.getMetadata().getName());
        Awaitility.await()
                .pollInterval(1, TimeUnit.SECONDS)
                .timeout(5, TimeUnit.MINUTES)
                .ignoreExceptions()
                .untilAsserted(() -> {
                    var currentKc = client
                            .resources(Keycloak.class)
                            .inNamespace(kc.getMetadata().getNamespace())
                            .withName(kc.getMetadata().getName())
                            .get();

                    CRAssert.assertKeycloakStatusCondition(currentKc, KeycloakStatusCondition.READY, true);
                    CRAssert.assertKeycloakStatusCondition(currentKc, KeycloakStatusCondition.HAS_ERRORS, false);
                });
    }

    public static String inClusterCurl(KubernetesClient k8sclient, String namespace, String url) {
        return inClusterCurl(k8sclient, namespace, "--insecure", "-s", "-o", "/dev/null", "-w", "%{http_code}", url);
    }

    public static String inClusterCurl(KubernetesClient k8sclient, String namespace, String... args) {
        var podName = KubernetesResourceUtil.sanitizeName("curl-" + UUID.randomUUID());
        try {
            Pod curlPod = k8sclient.run().inNamespace(namespace)
                    .withRunConfig(new RunConfigBuilder()
                            .withArgs(args)
                            .withName(podName)
                            .withImage("curlimages/curl:7.78.0")
                            .withRestartPolicy("Never")
                            .build())
                    .done();
            Log.info("Waiting for curl Pod to finish running");
            Awaitility.await().atMost(3, TimeUnit.MINUTES)
                    .until(() -> {
                        String phase =
                                k8sclient.pods().inNamespace(namespace).withName(podName).get()
                                        .getStatus().getPhase();
                        return phase.equals("Succeeded") || phase.equals("Failed");
                    });

            String curlOutput =
                    k8sclient.pods().inNamespace(namespace)
                            .withName(curlPod.getMetadata().getName()).getLog();

            return curlOutput;
        } catch (KubernetesClientException ex) {
            throw new AssertionError(ex);
        } finally {
            Log.info("Deleting curl Pod");
            k8sclient.pods().inNamespace(namespace).withName(podName).delete();
            Awaitility.await().atMost(2, TimeUnit.MINUTES)
                    .until(() -> k8sclient.pods().inNamespace(namespace).withName(podName)
                            .get() == null);
        }
    }
}

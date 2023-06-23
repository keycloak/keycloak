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

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.fabric8.kubernetes.client.utils.KubernetesResourceUtil;
import io.fabric8.kubernetes.client.utils.Serialization;
import io.quarkus.logging.Log;
import org.awaitility.Awaitility;
import org.keycloak.operator.crds.v2alpha1.deployment.Keycloak;
import org.keycloak.operator.crds.v2alpha1.deployment.KeycloakStatusCondition;
import org.keycloak.operator.testsuite.integration.BaseOperatorTest;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * @author Vaclav Muzikar <vmuzikar@redhat.com>
 */
public final class K8sUtils {
    public static <T extends HasMetadata> T getResourceFromFile(String fileName, Class<T> type) {
        return Serialization.unmarshal(Objects.requireNonNull(K8sUtils.class.getResourceAsStream("/" + fileName)), type);
    }

    public static Keycloak getDefaultKeycloakDeployment() {
        Keycloak kc = getResourceFromFile("example-keycloak.yaml", Keycloak.class);
        kc.getMetadata().setNamespace(BaseOperatorTest.getCurrentNamespace());
        return kc;
    }

    public static Secret getDefaultTlsSecret() {
        return getResourceFromFile("example-tls-secret.yaml", Secret.class);
    }


    public static void deployKeycloak(KubernetesClient client, Keycloak kc, boolean waitUntilReady) {
        deployKeycloak(client, kc, waitUntilReady, true);
    }

    public static List<HasMetadata> set(KubernetesClient client, InputStream stream) {
        return client.load(stream).items().stream().map(i -> set(client, i)).collect(Collectors.toList());
    }

    public static <T extends HasMetadata> T set(KubernetesClient client, T hasMetadata) {
        Resource<T> resource = client.resource(hasMetadata);
        try {
            return resource.patch();
        } catch (KubernetesClientException e) {
            if (e.getCode() == HttpURLConnection.HTTP_NOT_FOUND) {
                return resource.create();
            }
            throw e;
        }
    }

    public static void deployKeycloak(KubernetesClient client, Keycloak kc, boolean waitUntilReady, boolean deployTlsSecret) {
        if (deployTlsSecret) {
            set(client, getDefaultTlsSecret());
        }

        set(client, kc);

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
                    .withNewRunConfig()
                            .withArgs(args)
                            .withName(podName)
                            .withImage("curlimages/curl:7.78.0")
                            .withRestartPolicy("Never")
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

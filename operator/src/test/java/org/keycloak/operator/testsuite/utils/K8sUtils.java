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
import io.fabric8.kubernetes.api.model.PodBuilder;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.kubernetes.client.dsl.ExecWatch;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.fabric8.kubernetes.client.utils.Serialization;
import io.quarkus.logging.Log;

import org.awaitility.Awaitility;
import org.keycloak.operator.crds.v2alpha1.deployment.Keycloak;
import org.keycloak.operator.crds.v2alpha1.deployment.KeycloakStatusCondition;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author Vaclav Muzikar <vmuzikar@redhat.com>
 */
public final class K8sUtils {
    public static <T extends HasMetadata> T getResourceFromFile(String fileName, Class<T> type) {
        return Serialization.unmarshal(Objects.requireNonNull(K8sUtils.class.getResourceAsStream("/" + fileName)), type);
    }

    public static Keycloak getDefaultKeycloakDeployment() {
        return getResourceFromFile("example-keycloak.yaml", Keycloak.class);
    }

    public static Secret getDefaultTlsSecret() {
        return getResourceFromFile("example-tls-secret.yaml", Secret.class);
    }

    public static void deployKeycloak(KubernetesClient client, Keycloak kc, boolean waitUntilReady) {
        deployKeycloak(client, kc, waitUntilReady, true);
    }

    public static List<HasMetadata> set(KubernetesClient client, InputStream stream) {
        return set(client, stream, Function.identity());
    }

    public static List<HasMetadata> set(KubernetesClient client, InputStream stream, Function<HasMetadata, HasMetadata> modifier) {
        return client.load(stream).items().stream().map(modifier).filter(Objects::nonNull).map(i -> set(client, i)).collect(Collectors.toList());
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

    public static void waitForKeycloakToBeReady(KubernetesClient client, Keycloak kc) {
        Log.infof("Waiting for Keycloak \"%s\"", kc.getMetadata().getName());
        Awaitility.await()
                .pollInterval(1, TimeUnit.SECONDS)
                .timeout(5, TimeUnit.MINUTES)
                .ignoreExceptions()
                .untilAsserted(() -> {
                    var currentKc = client.resource(kc).get();

                    CRAssert.assertKeycloakStatusCondition(currentKc, KeycloakStatusCondition.READY, true);
                    CRAssert.assertKeycloakStatusCondition(currentKc, KeycloakStatusCondition.HAS_ERRORS, false);
                });
    }

    public static String inClusterCurl(KubernetesClient k8sclient, String namespace, String url) {
        return inClusterCurl(k8sclient, namespace, "--insecure", "-s", "-o", "/dev/null", "-w", "%{http_code}", url);
    }

    public static String inClusterCurl(KubernetesClient k8sclient, String namespace, String... args) {
        var podName = "curl-pod";
        try {
            Pod curlPod = new PodBuilder().withNewMetadata().withName(podName).endMetadata().withNewSpec()
                    .addNewContainer()
                    .withImage("curlimages/curl:8.1.2")
                    .withCommand("sh")
                    .withName("curl")
                    .withStdin()
                    .endContainer()
                    .endSpec()
                    .build();

            try {
                k8sclient.resource(curlPod).create();
            } catch (KubernetesClientException e) {
                if (e.getCode() != HttpURLConnection.HTTP_CONFLICT) {
                    throw e;
                }
            }

            ByteArrayOutputStream output = new ByteArrayOutputStream();

            try (ExecWatch watch = k8sclient.pods().resource(curlPod).withReadyWaitTimeout(60000)
                    .writingOutput(output)
                    .exec(Stream.concat(Stream.of("curl"), Stream.of(args)).toArray(String[]::new))) {
                watch.exitCode().get(15, TimeUnit.SECONDS);
            }

            return output.toString(StandardCharsets.UTF_8);
        } catch (Exception ex) {
            throw KubernetesClientException.launderThrowable(ex);
        }
    }
}

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
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.awaitility.Awaitility;
import org.keycloak.operator.Constants;
import org.keycloak.operator.crds.v2alpha1.deployment.Keycloak;
import org.keycloak.operator.crds.v2alpha1.deployment.KeycloakStatusCondition;
import org.keycloak.operator.crds.v2alpha1.deployment.spec.HttpManagementSpecBuilder;
import org.keycloak.operator.crds.v2alpha1.deployment.spec.NetworkPolicySpecBuilder;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.PodBuilder;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.kubernetes.client.dsl.ExecWatch;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.fabric8.kubernetes.client.utils.Serialization;
import io.quarkus.logging.Log;

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
        return inClusterCurl(k8sclient, namespace, Map.of(), url);
    }

    public static String inClusterCurl(KubernetesClient k8sclient, String namespace, Map<String, String> labels, String url) {
        return inClusterCurl(k8sclient, namespace, labels, "--insecure", "-s", "-o", "/dev/null", "-w", "%{http_code}", url).stdout();
    }

    public static String inClusterCurl(KubernetesClient k8sClient, String namespace, String... args) {
        return inClusterCurl(k8sClient, namespace, Map.of(), args).stdout();
    }

    public static CurlResult inClusterCurl(KubernetesClient k8sClient, String namespace, Map<String, String> labels, String... args) {
        return inClusterCurlCommand(k8sClient, namespace, labels, "curl", args);
    }

    public static CurlResult inClusterCurlCommand(KubernetesClient k8sClient, String namespace, Map<String, String> labels, String commandString, String... args) {
        Log.infof("Executing curl labels: %s commandString: %s %s", labels, commandString, String.join(" ", Arrays.stream(args).map(s -> s.contains(" ") ? "'" + s + "'" : s).toList()));

        Log.infof("Starting cURL in namespace '%s' with labels '%s'", namespace, labels);
        var podName = "curl-pod" + (labels.isEmpty()?"":("-" + UUID.randomUUID()));
        try {
            var builder = new PodBuilder();
            builder.withNewMetadata()
                    .withName(podName)
                    .withNamespace(namespace)
                    .withLabels(labels)
                    .endMetadata();
            createCurlContainer(builder);
            var curlPod = builder.build();

            try {
                k8sClient.resource(curlPod).create();
            } catch (KubernetesClientException e) {
                if (!labels.isEmpty() || e.getCode() != HttpURLConnection.HTTP_CONFLICT) {
                    throw e;
                }
            }

            ByteArrayOutputStream output = new ByteArrayOutputStream();
            ByteArrayOutputStream error = new ByteArrayOutputStream();

            String[] command = Stream.concat(Stream.of(commandString), Stream.of(args)).toArray(String[]::new);
            try (ExecWatch watch = k8sClient.pods().resource(curlPod).withReadyWaitTimeout(15000)
                    .writingOutput(output)
                    .writingError(error)
                    .exec(command)) {
                var exitCode = watch.exitCode().get(5, TimeUnit.SECONDS);
                output.close();
                error.close();
                var curlResult = new CurlResult(exitCode, output.toString(StandardCharsets.UTF_8), error.toString(StandardCharsets.UTF_8));
                Log.infof("curl result: %s", curlResult);
                return curlResult;
            } finally {
                if (!labels.isEmpty()) {
                    k8sClient.resource(curlPod).delete();
                }
            }
        } catch (Exception ex) {
            throw KubernetesClientException.launderThrowable(ex);
        }
    }

    private static void createCurlContainer(PodBuilder builder) {
        builder.withNewSpec()
                .addNewContainer()
                .withImage("curlimages/curl:8.1.2")
                .withCommand("sh")
                .withName("curl")
                .withStdin()
                // Mount the projected service account token with audience
                .addNewVolumeMount()
                    .withName("aud-token")
                    .withMountPath("/var/run/secrets/tokens")
                    .withReadOnly(true)
                .endVolumeMount()
                .endContainer()
                // Define the projected volume providing a service account token
                .addNewVolume()
                    .withName("aud-token")
                    .withNewProjected()
                        .addNewSource()
                            .withNewServiceAccountToken()
                                .withAudience("https://example.com:8443/realms/test")
                                .withExpirationSeconds(3600L)
                                .withPath("test-aud-token")
                            .endServiceAccountToken()
                        .endSource()
                    .endProjected()
                .endVolume()
                .endSpec();
    }

    public static void enableNetworkPolicy(Keycloak keycloak) {
        var builder = new NetworkPolicySpecBuilder();
        keycloak.getSpec().setNetworkPolicySpec(builder.build());
    }

    public static void disableNetworkPolicy(Keycloak keycloak) {
        var builder = new NetworkPolicySpecBuilder();
        builder.withNetworkPolicyEnabled(false);
        keycloak.getSpec().setNetworkPolicySpec(builder.build());
    }

    public static int configureManagement(Keycloak keycloak, boolean randomPort) {
        if (!randomPort) {
            return Constants.KEYCLOAK_MANAGEMENT_PORT;
        }
        var port = ThreadLocalRandom.current().nextInt(10_000, 10_100);
        keycloak.getSpec().setHttpManagementSpec(new HttpManagementSpecBuilder().withPort(port).build());
        return port;
    }

    public static int enableHttp(Keycloak keycloak, boolean randomPort) {
        keycloak.getSpec().getHttpSpec().setHttpEnabled(true);
        if (randomPort) {
            var port = ThreadLocalRandom.current().nextInt(10_100, 10_200);
            keycloak.getSpec().getHttpSpec().setHttpPort(port);
            return port;
        }
        return Constants.KEYCLOAK_HTTP_PORT;
    }

    public static int configureHttps(Keycloak keycloak, boolean randomPort) {
        if (!randomPort) {
            return Constants.KEYCLOAK_HTTPS_PORT;
        }
        var port = ThreadLocalRandom.current().nextInt(10_200, 10_300);
        keycloak.getSpec().getHttpSpec().setHttpsPort(port);
        return port;
    }

    public static void disableHttps(Keycloak keycloak) {
        keycloak.getSpec().getHttpSpec().setTlsSecret(null);
    }

    public record CurlResult(int exitCode, String stdout, String stderr) {}
}

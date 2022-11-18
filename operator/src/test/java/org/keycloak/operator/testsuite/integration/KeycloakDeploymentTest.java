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

import io.fabric8.kubernetes.api.model.EnvVarBuilder;
import io.fabric8.kubernetes.api.model.LocalObjectReference;
import io.fabric8.kubernetes.api.model.LocalObjectReferenceBuilder;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.SecretBuilder;
import io.fabric8.kubernetes.api.model.SecretKeySelectorBuilder;
import io.fabric8.kubernetes.api.model.apps.StatefulSetSpecBuilder;
import io.quarkus.logging.Log;
import io.quarkus.test.junit.QuarkusTest;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.keycloak.operator.Constants;
import org.keycloak.operator.controllers.KeycloakAdminSecret;
import org.keycloak.operator.controllers.KeycloakDistConfigurator;
import org.keycloak.operator.controllers.KeycloakService;
import org.keycloak.operator.crds.v2alpha1.deployment.Keycloak;
import org.keycloak.operator.crds.v2alpha1.deployment.KeycloakStatusCondition;
import org.keycloak.operator.crds.v2alpha1.deployment.ValueOrSecret;
import org.keycloak.operator.crds.v2alpha1.deployment.spec.HostnameSpecBuilder;
import org.keycloak.operator.testsuite.utils.K8sUtils;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import static java.util.concurrent.TimeUnit.MINUTES;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.keycloak.operator.testsuite.utils.CRAssert.assertKeycloakStatusCondition;
import static org.keycloak.operator.testsuite.utils.K8sUtils.deployKeycloak;
import static org.keycloak.operator.testsuite.utils.K8sUtils.getDefaultKeycloakDeployment;
import static org.keycloak.operator.testsuite.utils.K8sUtils.getResourceFromFile;
import static org.keycloak.operator.testsuite.utils.K8sUtils.waitForKeycloakToBeReady;

@QuarkusTest
public class KeycloakDeploymentTest extends BaseOperatorTest {
    @Test
    public void testBasicKeycloakDeploymentAndDeletion() {
        try {
            // CR
            Log.info("Creating new Keycloak CR example");
            var kc = getDefaultKeycloakDeployment();
            var deploymentName = kc.getMetadata().getName();
            deployKeycloak(k8sclient, kc, true);

            // Check Operator has deployed Keycloak
            Log.info("Checking Operator has deployed Keycloak deployment");
            assertThat(k8sclient.apps().statefulSets().inNamespace(namespace).withName(deploymentName).get()).isNotNull();

            // Check Keycloak has correct replicas
            Log.info("Checking Keycloak pod has ready replicas == 1");
            assertThat(k8sclient.apps().statefulSets().inNamespace(namespace).withName(deploymentName).get().getStatus().getReadyReplicas()).isEqualTo(1);

            // Delete CR
            Log.info("Deleting Keycloak CR and watching cleanup");
            k8sclient.resources(Keycloak.class).delete(kc);
            Awaitility.await()
                    .untilAsserted(() -> assertThat(k8sclient.apps().statefulSets().inNamespace(namespace).withName(deploymentName).get()).isNull());
        } catch (Exception e) {
            savePodLogs();
            throw e;
        }
    }

    @Test
    public void testCRFields() {
        try {
            var kc = getDefaultKeycloakDeployment();
            var deploymentName = kc.getMetadata().getName();
            deployKeycloak(k8sclient, kc, true);

            final var dbConf = new ValueOrSecret("db-password", "Ay Caramba!");

            kc.getSpec().setImage("quay.io/keycloak/non-existing-keycloak");
            kc.getSpec().getAdditionalOptions().remove(dbConf);
            kc.getSpec().getAdditionalOptions().add(dbConf);
            deployKeycloak(k8sclient, kc, false);

            Awaitility.await()
                    .during(Duration.ofSeconds(15)) // check if the Deployment is stable
                    .untilAsserted(() -> {
                        var c = k8sclient.apps().statefulSets().inNamespace(namespace).withName(deploymentName).get()
                                .getSpec().getTemplate().getSpec().getContainers().get(0);
                        assertThat(c.getImage()).isEqualTo("quay.io/keycloak/non-existing-keycloak");
                        assertThat(c.getEnv().stream()
                                .anyMatch(e -> e.getName().equals(KeycloakDistConfigurator.getKeycloakOptionEnvVarName(dbConf.getName()))
                                        && e.getValue().equals(dbConf.getValue())))
                                .isTrue();
                    });

        } catch (Exception e) {
            savePodLogs();
            throw e;
        }
    }

    @Test
    public void testConfigInCRTakesPrecedence() {
        try {
            var kc = getDefaultKeycloakDeployment();
            var health = new ValueOrSecret("health-enabled", "false");
            var e = new EnvVarBuilder()
                    .withName(KeycloakDistConfigurator.getKeycloakOptionEnvVarName(health.getName()))
                    .withValue(health.getValue())
                    .build();
            kc.getSpec().getAdditionalOptions().add(health);
            deployKeycloak(k8sclient, kc, false);

            assertThat(Constants.DEFAULT_DIST_CONFIG.get(health.getName())).isEqualTo("true"); // just a sanity check default values did not change

            Awaitility.await()
                    .ignoreExceptions()
                    .untilAsserted(() -> {
                        Log.info("Asserting default value was overwritten by CR value");
                        var c = k8sclient.apps().statefulSets().inNamespace(namespace).withName(kc.getMetadata().getName()).get()
                                .getSpec().getTemplate().getSpec().getContainers().get(0);

                        assertThat(c.getEnv()).contains(e);
                    });
        } catch (Exception e) {
            savePodLogs();
            throw e;
        }
    }

    @Test
    public void testDeploymentDurability() {
        try {
            var kc = getDefaultKeycloakDeployment();
            var deploymentName = kc.getMetadata().getName();
            deployKeycloak(k8sclient, kc, true);

            Log.info("Trying to delete deployment");
            assertThat(k8sclient.apps().statefulSets().withName(deploymentName).delete()).isTrue();
            Awaitility.await()
                    .untilAsserted(() -> assertThat(k8sclient.apps().statefulSets().withName(deploymentName).get()).isNotNull());

            waitForKeycloakToBeReady(k8sclient, kc); // wait for reconciler to calm down to avoid race condititon

            Log.info("Trying to modify deployment");

            var deployment = k8sclient.apps().statefulSets().withName(deploymentName).get();
            var labels = Map.of("address", "EvergreenTerrace742");
            var flandersEnvVar = new EnvVarBuilder().withName("NEIGHBOR").withValue("Stupid Flanders!").build();
            var origSpecs = new StatefulSetSpecBuilder(deployment.getSpec()).build(); // deep copy

            deployment.getMetadata().getLabels().putAll(labels);
            deployment.getSpec().getTemplate().getSpec().getContainers().get(0).setEnv(List.of(flandersEnvVar));
            k8sclient.apps().statefulSets().createOrReplace(deployment);

            Awaitility.await()
                    .atMost(5, MINUTES)
                    .pollDelay(1, SECONDS)
                    .ignoreExceptions()
                    .untilAsserted(() -> {
                        var d = k8sclient.apps().statefulSets().withName(deploymentName).get();
                        assertThat(d.getMetadata().getLabels().entrySet().containsAll(labels.entrySet())).isTrue(); // additional labels should not be overwritten
                        assertThat(d.getSpec()).isEqualTo(origSpecs); // specs should be reconciled back to original values
                    });
        } catch (Exception e) {
            savePodLogs();
            throw e;
        }
    }

    @Test
    public void testTlsUsesCorrectSecret() {
        try {
            var kc = getDefaultKeycloakDeployment();
            deployKeycloak(k8sclient, kc, true);

            var service = new KeycloakService(k8sclient, kc);
            Awaitility.await()
                    .ignoreExceptions()
                    .untilAsserted(() -> {
                        String url = "https://" + service.getName() + "." + namespace + ":" + Constants.KEYCLOAK_HTTPS_PORT;
                        Log.info("Checking url: " + url);

                        var curlOutput = K8sUtils.inClusterCurl(k8sclient, namespace, "--insecure", "-s", "-v", url);
                        Log.info("Curl Output: " + curlOutput);

                        assertTrue(curlOutput.contains("issuer: O=mkcert development CA; OU=aperuffo@aperuffo-mac (Andrea Peruffo); CN=mkcert aperuffo@aperuffo-mac (Andrea Peruffo)"));
                    });
        } catch (Exception e) {
            savePodLogs();
            throw e;
        }
    }

    @Test
    public void testTlsDisabled() {
        try {
            var kc = getDefaultKeycloakDeployment();
            kc.getSpec().getHttpSpec().setTlsSecret(null);
            kc.getSpec().getHttpSpec().setHttpEnabled(true);
            deployKeycloak(k8sclient, kc, true);

            assertKeycloakAccessibleViaService(kc, false, Constants.KEYCLOAK_HTTP_PORT);
        } catch (Exception e) {
            savePodLogs();
            throw e;
        }
    }

    @Test
    public void testHostnameStrict() {
        try {
            var kc = getDefaultKeycloakDeployment();
            deployKeycloak(k8sclient, kc, true);

            var service = new KeycloakService(k8sclient, kc);
            Awaitility.await()
                    .ignoreExceptions()
                    .untilAsserted(() -> {
                        String url = "https://" + service.getName() + "." + namespace + ":" + Constants.KEYCLOAK_HTTPS_PORT + "/admin/master/console/";
                        Log.info("Checking url: " + url);

                        var curlOutput = K8sUtils.inClusterCurl(k8sclient, namespace, "-s", "--insecure", "-H", "Host: foo.bar", url);
                        Log.info("Curl Output: " + curlOutput);

                        assertTrue(curlOutput.contains("\"authServerUrl\": \"https://example.com\""));
                    });
        } catch (Exception e) {
            savePodLogs();
            throw e;
        }
    }

    @Test
    public void testHostnameStrictDisabled() {
        try {
            var kc = getDefaultKeycloakDeployment();
            var hostnameSpec = new HostnameSpecBuilder()
                    .withStrict(false)
                    .withStrictBackchannel(false)
                    .build();
            kc.getSpec().setHostnameSpec(hostnameSpec);

            deployKeycloak(k8sclient, kc, true);

            var service = new KeycloakService(k8sclient, kc);
            Awaitility.await()
                    .ignoreExceptions()
                    .untilAsserted(() -> {
                        String url = "https://" + service.getName() + "." + namespace + ":" + Constants.KEYCLOAK_HTTPS_PORT + "/admin/master/console/";
                        Log.info("Checking url: " + url);

                        var curlOutput = K8sUtils.inClusterCurl(k8sclient, namespace, "-s", "--insecure", "-H", "Host: foo.bar", url);
                        Log.info("Curl Output: " + curlOutput);

                        assertTrue(curlOutput.contains("\"authServerUrl\": \"https://foo.bar\""));
                    });
        } catch (Exception e) {
            savePodLogs();
            throw e;
        }
    }

    @Test
    public void testHttpsPort() {
        try {
            final int httpsPort = 8543;
            final int httpPort = 8180;
            var kc = getDefaultKeycloakDeployment();
            kc.getSpec().getHttpSpec().setHttpsPort(httpsPort);
            kc.getSpec().getHttpSpec().setHttpPort(httpPort);

            var hostnameSpec = new HostnameSpecBuilder()
                    .withStrict(false)
                    .withStrictBackchannel(false)
                    .build();
            kc.getSpec().setHostnameSpec(hostnameSpec);

            deployKeycloak(k8sclient, kc, true);

            assertKeycloakAccessibleViaService(kc, true, httpsPort);
        } catch (Exception e) {
            savePodLogs();
            throw e;
        }
    }

    @Test
    public void testHttpPort() {
        try {
            final int httpsPort = 8543;
            final int httpPort = 8180;
            var kc = getDefaultKeycloakDeployment();
            kc.getSpec().getHttpSpec().setHttpsPort(httpsPort);
            kc.getSpec().getHttpSpec().setHttpPort(httpPort);
            kc.getSpec().getHttpSpec().setTlsSecret(null);
            kc.getSpec().getHttpSpec().setHttpEnabled(true);

            var hostnameSpec = new HostnameSpecBuilder()
                    .withStrict(false)
                    .withStrictBackchannel(false)
                    .build();
            kc.getSpec().setHostnameSpec(hostnameSpec);

            deployKeycloak(k8sclient, kc, true);

            assertKeycloakAccessibleViaService(kc, false, httpPort);
        } catch (Exception e) {
            savePodLogs();
            throw e;
        }
    }

    // Reference curl command:
    // curl --insecure --data "grant_type=password&client_id=admin-cli&username=admin&password=adminPassword" https://localhost:8443/realms/master/protocol/openid-connect/token
    @Test
    public void testInitialAdminUser() {
        try {
            var kc = getDefaultKeycloakDeployment();
            var kcAdminSecret = new KeycloakAdminSecret(k8sclient, kc);

            k8sclient
                    .resources(Keycloak.class)
                    .inNamespace(namespace)
                    .delete();
            k8sclient
                    .secrets()
                    .inNamespace(namespace)
                    .withName(kcAdminSecret.getName())
                    .delete();

            // Making sure no other Keycloak pod is still around
            Awaitility.await()
                    .ignoreExceptions()
                    .untilAsserted(() ->
                            assertThat(k8sclient
                                    .pods()
                                    .inNamespace(namespace)
                                    .withLabel("app", "keycloak")
                                    .list()
                                    .getItems()
                                    .size()).isZero());
            // Recreating the database to keep this test isolated
            deleteDB();
            deployDB();
            deployKeycloak(k8sclient, kc, true);
            var decoder = Base64.getDecoder();
            var service = new KeycloakService(k8sclient, kc);

            AtomicReference<String> adminUsername = new AtomicReference<>();
            AtomicReference<String> adminPassword = new AtomicReference<>();
            Awaitility.await()
                    .ignoreExceptions()
                    .untilAsserted(() -> {
                        Log.info("Checking secret, ns: " + namespace + ", name: " + kcAdminSecret.getName());
                        var adminSecret = k8sclient
                                .secrets()
                                .inNamespace(namespace)
                                .withName(kcAdminSecret.getName())
                                .get();

                        adminUsername.set(new String(decoder.decode(adminSecret.getData().get("username").getBytes(StandardCharsets.UTF_8))));
                        adminPassword.set(new String(decoder.decode(adminSecret.getData().get("password").getBytes(StandardCharsets.UTF_8))));

                        String url = "https://" + service.getName() + "." + namespace + ":" + Constants.KEYCLOAK_HTTPS_PORT + "/realms/master/protocol/openid-connect/token";
                        Log.info("Checking url: " + url);

                        var curlOutput = K8sUtils.inClusterCurl(k8sclient, namespace, "--insecure", "-s", "--data", "grant_type=password&client_id=admin-cli&username=" + adminUsername.get() + "&password=" + adminPassword.get(), url);
                        Log.info("Curl Output: " + curlOutput);

                        assertTrue(curlOutput.contains("\"access_token\""));
                        assertTrue(curlOutput.contains("\"token_type\":\"Bearer\""));
                    });

            // Redeploy the same Keycloak without redeploying the Database
            k8sclient.resource(kc).delete();
            deployKeycloak(k8sclient, kc, true);
            Awaitility.await()
                    .ignoreExceptions()
                    .untilAsserted(() -> {
                        Log.info("Checking secret, ns: " + namespace + ", name: " + kcAdminSecret.getName());
                        var adminSecret = k8sclient
                                .secrets()
                                .inNamespace(namespace)
                                .withName(kcAdminSecret.getName())
                                .get();

                        var newPassword = new String(decoder.decode(adminSecret.getData().get("password").getBytes(StandardCharsets.UTF_8)));

                        String url = "https://" + service.getName() + "." + namespace + ":" + Constants.KEYCLOAK_HTTPS_PORT + "/realms/master/protocol/openid-connect/token";
                        Log.info("Checking url: " + url);

                        var curlOutput = K8sUtils.inClusterCurl(k8sclient, namespace, "--insecure", "-s", "--data", "grant_type=password&client_id=admin-cli&username=" + adminUsername.get() + "&password=" + adminPassword.get(), url);
                        Log.info("Curl Output: " + curlOutput);

                        assertTrue(curlOutput.contains("\"access_token\""));
                        assertTrue(curlOutput.contains("\"token_type\":\"Bearer\""));
                        assertNotEquals(adminPassword.get(), newPassword);
                    });
        } catch (Exception e) {
            savePodLogs();
            throw e;
        }
    }

    @Test
    @EnabledIfSystemProperty(named = OPERATOR_CUSTOM_IMAGE, matches = ".+")
    public void testCustomImage() {
        try {
            var kc = getDefaultKeycloakDeployment();
            kc.getSpec().setImage(customImage);
            deployKeycloak(k8sclient, kc, true);

            var pods = k8sclient
                    .pods()
                    .inNamespace(namespace)
                    .withLabels(Constants.DEFAULT_LABELS)
                    .list()
                    .getItems();

            assertThat(pods.get(0).getSpec().getContainers().get(0).getArgs()).containsExactly("start", "--optimized");
        } catch (Exception e) {
            savePodLogs();
            throw e;
        }
    }

    @Test
    @EnabledIfSystemProperty(named = OPERATOR_CUSTOM_IMAGE, matches = ".+")
    public void testCustomImageWithImagePullSecrets() {
        String imagePullSecretName = "docker-regcred-custom-kc-imagepullsecret-01";
        String secretDescriptorFilename = "test-docker-registry-secret.yaml";

        try {
            var kc = getDefaultKeycloakDeployment();
            kc.getSpec().setImage(customImage);

            handleFakeImagePullSecretCreation(kc, secretDescriptorFilename);

            deployKeycloak(k8sclient, kc, true);

            var pods = k8sclient
                    .pods()
                    .inNamespace(namespace)
                    .withLabels(Constants.DEFAULT_LABELS)
                    .list()
                    .getItems();

            assertThat(pods.get(0).getSpec().getContainers().get(0).getArgs()).containsExactly("start", "--optimized");
            assertThat(pods.get(0).getSpec().getImagePullSecrets().size()).isEqualTo(1);
            assertThat(pods.get(0).getSpec().getImagePullSecrets().get(0).getName()).isEqualTo(imagePullSecretName);

        } catch (Exception e) {
            savePodLogs();
            throw e;
        }
    }

    @Test
    public void testHttpRelativePathWithPlainValue() {
        try {
            var kc = getDefaultKeycloakDeployment();
            kc.getSpec().getAdditionalOptions().add(new ValueOrSecret(Constants.KEYCLOAK_HTTP_RELATIVE_PATH_KEY, "/foobar"));
            deployKeycloak(k8sclient, kc, true);

            var pods = k8sclient
                    .pods()
                    .inNamespace(namespace)
                    .withLabels(Constants.DEFAULT_LABELS)
                    .list()
                    .getItems();

            assertTrue(pods.get(0).getSpec().getContainers().get(0).getReadinessProbe().getExec().getCommand().stream().collect(Collectors.joining()).contains("foobar"));
        } catch (Exception e) {
            savePodLogs();
            throw e;
        }
    }

    @Test
    public void testHttpRelativePathWithSecretValue() {
        try {
            var kc = getDefaultKeycloakDeployment();
            var secretName = "my-http-relative-path";
            var keyName = "rel-path";
            var httpRelativePathSecret = new SecretBuilder()
                    .withNewMetadata()
                    .withName(secretName)
                    .withNamespace(namespace)
                    .endMetadata()
                    .addToStringData(keyName, "/barfoo")
                    .build();
            k8sclient.secrets().inNamespace(namespace).createOrReplace(httpRelativePathSecret);

            kc.getSpec().getAdditionalOptions().add(new ValueOrSecret(Constants.KEYCLOAK_HTTP_RELATIVE_PATH_KEY,
                    new SecretKeySelectorBuilder()
                        .withName(secretName)
                        .withKey(keyName)
                        .build()));
            deployKeycloak(k8sclient, kc, true);

            var pods = k8sclient
                    .pods()
                    .inNamespace(namespace)
                    .withLabels(Constants.DEFAULT_LABELS)
                    .list()
                    .getItems();

            assertTrue(pods.get(0).getSpec().getContainers().get(0).getReadinessProbe().getExec().getCommand().stream().collect(Collectors.joining()).contains("barfoo"));
        } catch (Exception e) {
            savePodLogs();
            throw e;
        }
    }

    @Test
    public void testUpgradeRecreatesPods() {
        try {
            var kc = getDefaultKeycloakDeployment();
            kc.getSpec().setInstances(3);
            deployKeycloak(k8sclient, kc, true);

            var stsGetter = k8sclient.apps().statefulSets().inNamespace(namespace).withName(kc.getMetadata().getName());
            final String origImage = stsGetter.get().getSpec().getTemplate().getSpec().getContainers().get(0).getImage();
            final String newImage = "quay.io/keycloak/non-existing-keycloak";

            kc.getSpec().setImage(newImage);
            deployKeycloak(k8sclient, kc, false);

            Awaitility.await()
                    .ignoreExceptions()
                    .pollInterval(Duration.ZERO) // make the test super fast not to miss the moment when Operator changes the STS
                    .untilAsserted(() -> {
                        var sts = stsGetter.get();
                        assertEquals(1, sts.getStatus().getReplicas());
                        assertEquals(origImage, sts.getSpec().getTemplate().getSpec().getContainers().get(0).getImage());

                        var currentKc = k8sclient.resources(Keycloak.class)
                                        .inNamespace(namespace).withName(kc.getMetadata().getName()).get();
                        assertKeycloakStatusCondition(currentKc, KeycloakStatusCondition.READY, false, "Performing Keycloak upgrade");
                    });

            Awaitility.await()
                    .ignoreExceptions()
                    .untilAsserted(() -> {
                        var sts = stsGetter.get();
                        assertEquals(kc.getSpec().getInstances(), sts.getSpec().getReplicas()); // just checking specs as we're using a non-existing image
                        assertEquals(newImage, sts.getSpec().getTemplate().getSpec().getContainers().get(0).getImage());

                        var currentKc = k8sclient.resources(Keycloak.class)
                                .inNamespace(namespace).withName(kc.getMetadata().getName()).get();
                        assertKeycloakStatusCondition(currentKc, KeycloakStatusCondition.READY, false, "Waiting for more replicas");
                    });
        } catch (Exception e) {
            savePodLogs();
            throw e;
        }
    }

    private void handleFakeImagePullSecretCreation(Keycloak keycloakCR,
                                                   String secretDescriptorFilename) {

        Secret imagePullSecret = getResourceFromFile(secretDescriptorFilename, Secret.class);
        k8sclient.secrets().inNamespace(namespace).createOrReplace(imagePullSecret);
        LocalObjectReference localObjRefAsSecretTmp = new LocalObjectReferenceBuilder().withName(imagePullSecret.getMetadata().getName()).build();
        keycloakCR.getSpec().setImagePullSecrets(Collections.singletonList(localObjRefAsSecretTmp));
    }

    private void assertKeycloakAccessibleViaService(Keycloak kc, boolean https, int port) {
        var service = new KeycloakService(k8sclient, kc);
        Awaitility.await()
                .ignoreExceptions()
                .untilAsserted(() -> {
                    String protocol = https ? "https" : "http";
                    String url = protocol + "://" + service.getName() + "." + namespace + ":" + port;
                    Log.info("Checking url: " + url);

                    var curlOutput = K8sUtils.inClusterCurl(k8sclient, namespace, url);
                    Log.info("Curl Output: " + curlOutput);

                    assertEquals("200", curlOutput);
                });
    }
}

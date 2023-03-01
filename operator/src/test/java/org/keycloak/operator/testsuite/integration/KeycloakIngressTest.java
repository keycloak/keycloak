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

import io.fabric8.kubernetes.api.model.networking.v1.Ingress;
import io.fabric8.kubernetes.api.model.networking.v1.IngressBuilder;
import io.fabric8.kubernetes.api.model.networking.v1.ServiceBackendPortBuilder;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.quarkus.logging.Log;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;
import org.keycloak.operator.Constants;
import org.keycloak.operator.crds.v2alpha1.deployment.Keycloak;
import org.keycloak.operator.crds.v2alpha1.deployment.spec.IngressSpec;
import org.keycloak.operator.crds.v2alpha1.deployment.spec.HostnameSpecBuilder;
import org.keycloak.operator.testsuite.utils.K8sUtils;
import org.keycloak.operator.controllers.KeycloakIngress;

import java.util.Map;

import static java.util.concurrent.TimeUnit.MINUTES;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

@QuarkusTest
public class KeycloakIngressTest extends BaseOperatorTest {

    @Test
    public void testIngressOnHTTP() {
        var kc = K8sUtils.getDefaultKeycloakDeployment();
        kc.getSpec().getHttpSpec().setTlsSecret(null);
        kc.getSpec().getHttpSpec().setHttpEnabled(true);
        var hostnameSpec = new HostnameSpecBuilder()
                .withStrict(false)
                .withStrictBackchannel(false)
                .build();
        kc.getSpec().setHostnameSpec(hostnameSpec);
        K8sUtils.deployKeycloak(k8sclient, kc, true);

        Awaitility.await()
                .ignoreExceptions()
                .untilAsserted(() -> {
                    var output = RestAssured.given()
                            .get("http://" + kubernetesIp + ":80/realms/master")
                            .body()
                            .jsonPath()
                            .getString("realm");

                    assertEquals("master", output);
                });

        Awaitility.await()
                .ignoreExceptions()
                .untilAsserted(() -> {
                    var statusCode = RestAssured.given()
                            .get("http://" + kubernetesIp + ":80/admin/master/console")
                            .statusCode();

                    assertEquals(200, statusCode);
                });
    }

    @Test
    public void testIngressOnHTTPS() {
        var kc = K8sUtils.getDefaultKeycloakDeployment();
        var hostnameSpec = new HostnameSpecBuilder()
                .withStrict(false)
                .withStrictBackchannel(false)
                .build();
        kc.getSpec().setHostnameSpec(hostnameSpec);

        K8sUtils.deployKeycloak(k8sclient, kc, true);

        Awaitility.await()
                .ignoreExceptions()
                .untilAsserted(() -> {
                    var output = RestAssured.given()
                            .relaxedHTTPSValidation()
                            .get("https://" + kubernetesIp + ":443/realms/master")
                            .body()
                            .jsonPath()
                            .getString("realm");

                    assertEquals("master", output);
                });

        Awaitility.await()
                .ignoreExceptions()
                .untilAsserted(() -> {
                    var statusCode = RestAssured.given()
                            .relaxedHTTPSValidation()
                            .get("https://" + kubernetesIp + ":443/admin/master/console")
                            .statusCode();

                    assertEquals(200, statusCode);
                });
    }

    @Test
    public void testIngressHostname() {
        var kc = K8sUtils.getDefaultKeycloakDeployment();
        var hostnameSpec = new HostnameSpecBuilder().withHostname("foo.bar").build();
        kc.getSpec().setHostnameSpec(hostnameSpec);

        K8sUtils.deployKeycloak(k8sclient, kc, true);

        var ingress = new KeycloakIngress(k8sclient, kc);
        Awaitility.await()
                .ignoreExceptions()
                .untilAsserted(() -> {
                    var host = k8sclient
                            .network()
                            .v1()
                            .ingresses()
                            .inNamespace(namespace)
                            .withName(ingress.getName())
                            .get()
                            .getSpec()
                            .getRules()
                            .get(0)
                            .getHost();

                    assertEquals("foo.bar", host);
                });
    }

    @Test
    public void testMainIngressDurability() {
        var kc = K8sUtils.getDefaultKeycloakDeployment();
        kc.getSpec().setIngressSpec(new IngressSpec());
        kc.getSpec().getIngressSpec().setIngressEnabled(true);
        K8sUtils.deployKeycloak(k8sclient, kc, true);

        var ingress = new KeycloakIngress(k8sclient, kc);
        var ingressSelector = k8sclient
                .network()
                .v1()
                .ingresses()
                .inNamespace(namespace)
                .withName(ingress.getName());

        Log.info("Trying to delete the ingress");
        assertThat(ingressSelector.delete()).isTrue();
        Awaitility.await()
                .untilAsserted(() -> assertThat(ingressSelector.get()).isNotNull());

        K8sUtils.waitForKeycloakToBeReady(k8sclient, kc); // wait for reconciler to calm down to avoid race condititon

        Log.info("Trying to modify the ingress");

        var currentIngress = ingressSelector.get();
        var labels = Map.of("address", "EvergreenTerrace742");
        currentIngress.getSpec().getDefaultBackend().getService().setPort(new ServiceBackendPortBuilder().withName("foo").build());

        currentIngress.getMetadata().getAnnotations().clear();
        currentIngress.getMetadata().getLabels().putAll(labels);

        ingressSelector.createOrReplace(currentIngress);

        Awaitility.await()
                .ignoreExceptions()
                .untilAsserted(() -> {
                    var i = ingressSelector.get();
                    assertThat(i.getMetadata().getLabels().entrySet().containsAll(labels.entrySet())).isTrue(); // additional labels should not be overwritten
                    assertEquals("HTTPS", i.getMetadata().getAnnotations().get("nginx.ingress.kubernetes.io/backend-protocol"));
                    assertEquals("passthrough", i.getMetadata().getAnnotations().get("route.openshift.io/termination"));
                    assertEquals(Constants.KEYCLOAK_HTTPS_PORT, i.getSpec().getDefaultBackend().getService().getPort().getNumber());
                });

        // Delete the ingress
        kc.getSpec().getIngressSpec().setIngressEnabled(false);
        K8sUtils.deployKeycloak(k8sclient, kc, true);

        Awaitility.await()
                .untilAsserted(() -> {
                    assertThat(k8sclient.network().v1().ingresses().inNamespace(namespace).list().getItems().size()).isEqualTo(0);
                });
    }

    @Test
    public void testCustomIngressDeletion() {

        Keycloak defaultKeycloakDeployment = K8sUtils.getDefaultKeycloakDeployment();
        String kcDeploymentName = defaultKeycloakDeployment.getMetadata().getName();
        Resource<Ingress> customIngressDeployedManuallySelector = null;
        Ingress customIngressCreatedManually;

        try {

            customIngressCreatedManually = createCustomIngress(kcDeploymentName, namespace, 8443);

            customIngressDeployedManuallySelector = k8sclient
                    .network()
                    .v1()
                    .ingresses()
                    .inNamespace(namespace)
                    .withName(customIngressCreatedManually.getMetadata().getName());

            Awaitility.await().atMost(1, MINUTES).untilAsserted(() -> {
                assertThat(k8sclient.network().v1().ingresses().inNamespace(namespace).list().getItems().size()).isEqualTo(1);
            });

            Log.info("Redeploying the Keycloak CR with default Ingress disabled");
            defaultKeycloakDeployment.getSpec().setIngressSpec(new IngressSpec());
            defaultKeycloakDeployment.getSpec().getIngressSpec().setIngressEnabled(false);

            K8sUtils.deployKeycloak(k8sclient, defaultKeycloakDeployment, true);

            Awaitility.await().untilAsserted(() -> {
                Log.info("Make sure the Custom Ingress still remains");
                assertThat(k8sclient.network().v1().ingresses().inNamespace(namespace).list().getItems().size()).isEqualTo(1);
            });

        } catch (Exception e) {
            savePodLogs();
            throw e;
        } finally {
            Log.info("Destroying the Custom Ingress created manually to avoid errors in others Tests methods");
            if (customIngressDeployedManuallySelector != null && customIngressDeployedManuallySelector.isReady()) {
                assertThat(customIngressDeployedManuallySelector.delete()).isTrue();
                Awaitility.await().untilAsserted(() -> {
                    assertThat(k8sclient.network().v1().ingresses().inNamespace(namespace).list().getItems().size()).isEqualTo(0);
                });
            }
        }
    }

    private Ingress createCustomIngress(String baseResourceName, String targetNamespace, int portNumber) {

        Ingress customIngressCreated;

        customIngressCreated = new IngressBuilder()
                .withNewMetadata()
                    .withName(baseResourceName + Constants.KEYCLOAK_INGRESS_SUFFIX)
                    .withNamespace(targetNamespace)
                    .addToAnnotations("nginx.ingress.kubernetes.io/backend-protocol", "HTTPS")
                    .addToAnnotations("route.openshift.io/termination", "passthrough")
                .endMetadata()
                .withNewSpec()
                    .withNewDefaultBackend()
                        .withNewService()
                            .withName(baseResourceName + Constants.KEYCLOAK_SERVICE_SUFFIX)
                            .withNewPort()
                                .withNumber(portNumber)
                            .endPort()
                        .endService()
                    .endDefaultBackend()
                .endSpec()
                .build();

        customIngressCreated = k8sclient.network().v1().ingresses().inNamespace(targetNamespace).create(customIngressCreated);

        return customIngressCreated;
    }

}

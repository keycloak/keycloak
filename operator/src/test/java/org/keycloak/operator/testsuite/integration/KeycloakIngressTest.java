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
import org.keycloak.operator.controllers.KeycloakController;
import org.keycloak.operator.controllers.KeycloakIngressDependentResource;
import org.keycloak.operator.crds.v2alpha1.deployment.Keycloak;
import org.keycloak.operator.crds.v2alpha1.deployment.spec.HostnameSpecBuilder;
import org.keycloak.operator.crds.v2alpha1.deployment.spec.IngressSpec;
import org.keycloak.operator.crds.v2alpha1.deployment.spec.IngressSpecBuilder;
import org.keycloak.operator.testsuite.apiserver.DisabledIfApiServerTest;
import org.keycloak.operator.testsuite.utils.K8sUtils;

import java.util.Map;

import static java.util.concurrent.TimeUnit.MINUTES;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.keycloak.operator.testsuite.utils.K8sUtils.disableHttps;
import static org.keycloak.operator.testsuite.utils.K8sUtils.enableHttp;

@QuarkusTest
public class KeycloakIngressTest extends BaseOperatorTest {

    @DisabledIfApiServerTest
    @Test
    public void testIngressOnHTTP() {
        var kc = getTestKeycloakDeployment(false);
        disableHttps(kc);
        enableHttp(kc, false);
        var hostnameSpecBuilder = new HostnameSpecBuilder()
                .withStrict(false)
                .withStrictBackchannel(false);
        if (isOpenShift) {
            kc.getSpec().setIngressSpec(new IngressSpecBuilder().withIngressClassName(KeycloakController.OPENSHIFT_DEFAULT).build());
        }
        kc.getSpec().setHostnameSpec(hostnameSpecBuilder.build());

        K8sUtils.deployKeycloak(k8sclient, kc, true);

        String baseUrl;

        if (isOpenShift) {
            String testHostname = k8sclient.resource(kc).get().getSpec().getHostnameSpec().getHostname();
            // on OpenShift, when Keycloak is configured for HTTP only, we use edge TLS termination, i.e. Route still uses TLS
            baseUrl = "https://" + testHostname + ":443";
        } else {
            baseUrl = "http://" + kubernetesIp + ":80";
        }

        testIngressURLs(baseUrl);
    }

    @DisabledIfApiServerTest
    @Test
    public void testIngressOnHTTPSAndProxySettings() {
        var kc = getTestKeycloakDeployment(false);
        var hostnameSpecBuilder = new HostnameSpecBuilder()
                .withStrict(false)
                .withStrictBackchannel(false);
        if (isOpenShift) {
            kc.getSpec().setIngressSpec(new IngressSpecBuilder().withIngressClassName(KeycloakController.OPENSHIFT_DEFAULT).build());
        }
        kc.getSpec().setHostnameSpec(hostnameSpecBuilder.build());

        K8sUtils.deployKeycloak(k8sclient, kc, true);

        String testHostname;
        if (isOpenShift) {
            testHostname = k8sclient.resource(kc).get().getSpec().getHostnameSpec().getHostname();
        } else {
            testHostname = kubernetesIp;
        }

        testIngressURLs("https://" + testHostname + ":443");

        // just check we really have proxy set correctly
        var envVars = k8sclient.apps().statefulSets().withName(kc.getMetadata().getName()).get().getSpec()
                .getTemplate().getSpec().getContainers().get(0).getEnv();
        assertThat(envVars)
                .noneMatch(e -> "KC_PROXY".equals(e.getName()))
                .anyMatch(e -> "KC_PROXY_HEADERS".equals(e.getName()) && "xforwarded".equals(e.getValue()));
    }

    @DisabledIfApiServerTest
    @Test
    public void testIngressTLSTermination() {
        var kc = getTestKeycloakDeployment(false);
        var hostnameSpecBuilder = new HostnameSpecBuilder()
                .withStrict(false)
                .withStrictBackchannel(false);
        IngressSpec ingressSpec = new IngressSpec();
        ingressSpec.setIngressEnabled(true);
        kc.getSpec().setIngressSpec(ingressSpec);
        if (isOpenShift) {
            ingressSpec.setIngressClassName(KeycloakController.OPENSHIFT_DEFAULT);
        }
        kc.getSpec().setHostnameSpec(hostnameSpecBuilder.build());
        String secret = kc.getSpec().getHttpSpec().getTlsSecret();
        kc.getSpec().getHttpSpec().setHttpEnabled(true);
        kc.getSpec().getHttpSpec().setTlsSecret(null);
        ingressSpec.setTlsSecret(secret);

        K8sUtils.deployKeycloak(k8sclient, kc, true);

        String testHostname;
        if (isOpenShift) {
            testHostname = k8sclient.resource(kc).get().getSpec().getHostnameSpec().getHostname();
        } else {
            testHostname = kubernetesIp;
        }

        testIngressURLs("https://" + testHostname + ":443");
    }

    private void testIngressURLs(String baseUrl) {
        Awaitility.await()
                .ignoreExceptions()
                .untilAsserted(() -> {
                    var url = baseUrl + "/realms/master";
                    Log.info("Testing URL: " + url);

                    var output = RestAssured.given()
                            .relaxedHTTPSValidation()
                            .get(url)
                            .body()
                            .jsonPath()
                            .getString("token-service");

                    // the Keycloak URL must be without port, otherwise proxy resolution doesn't work correctly
                    assertEquals(url.replaceAll(":\\d+", "") + "/protocol/openid-connect", output);
                });

        Awaitility.await()
                .ignoreExceptions()
                .untilAsserted(() -> {
                    var url = baseUrl + "/admin/master/console";
                    Log.info("Testing URL: " + url);

                    var statusCode = RestAssured.given()
                            .relaxedHTTPSValidation()
                            .get(url)
                            .statusCode();

                    assertEquals(200, statusCode);
                });
    }

    @Test
    public void testIngressHostname() {
        var kc = getTestKeycloakDeployment(true);
        var hostnameSpec = new HostnameSpecBuilder().withHostname("foo.bar").build();
        kc.getSpec().setHostnameSpec(hostnameSpec);

        K8sUtils.deployKeycloak(k8sclient, kc, true);

        Awaitility.await()
                .ignoreExceptions()
                .untilAsserted(() -> {
                    var host = k8sclient
                            .network()
                            .v1()
                            .ingresses()
                            .inNamespace(namespace)
                            .withName(KeycloakIngressDependentResource.getName(kc))
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
        var kc = getTestKeycloakDeployment(true);
        kc.getSpec().setIngressSpec(new IngressSpec());
        kc.getSpec().getIngressSpec().setIngressEnabled(true);
        kc.getSpec().getIngressSpec().setAnnotations(Map.of("haproxy.router.openshift.io/disable_cookies", "true"));
        K8sUtils.deployKeycloak(k8sclient, kc, true);

        var ingressSelector = k8sclient
                .network()
                .v1()
                .ingresses()
                .inNamespace(namespace)
                .withName(KeycloakIngressDependentResource.getName(kc));

        Log.info("Trying to delete the ingress");
        assertThat(ingressSelector.delete()).isNotNull();
        Awaitility.await()
                .untilAsserted(() -> assertThat(ingressSelector.get()).isNotNull());

        K8sUtils.waitForKeycloakToBeReady(k8sclient, kc); // wait for reconciler to calm down to avoid race condititon

        Log.info("Trying to modify the ingress");

        var labels = Map.of("address", "EvergreenTerrace742");
		ingressSelector.accept(currentIngress -> {
			currentIngress.getMetadata().setResourceVersion(null);
			currentIngress.getSpec().getDefaultBackend().getService().setPort(new ServiceBackendPortBuilder().withNumber(6500).build());

	        currentIngress.getMetadata().getAnnotations().clear();
	        currentIngress.getMetadata().getLabels().putAll(labels);
		});

        Awaitility.await()
                .timeout(1, MINUTES)
                .ignoreExceptions()
                .untilAsserted(() -> {
                    var i = ingressSelector.get();
                    assertThat(i.getMetadata().getLabels().entrySet().containsAll(labels.entrySet())).isTrue(); // additional labels should not be overwritten
                    assertEquals("HTTPS", i.getMetadata().getAnnotations().get("nginx.ingress.kubernetes.io/backend-protocol"));
                    assertEquals("passthrough", i.getMetadata().getAnnotations().get("route.openshift.io/termination"));
                    assertEquals("true", i.getMetadata().getAnnotations().get("haproxy.router.openshift.io/disable_cookies"));
                    assertEquals(Constants.KEYCLOAK_HTTPS_PORT_NAME, i.getSpec().getDefaultBackend().getService().getPort().getName());
                });

        // Delete the ingress
        kc.getSpec().getIngressSpec().setIngressEnabled(false);
        K8sUtils.deployKeycloak(k8sclient, kc, true);

        Awaitility.await()
                .untilAsserted(() -> assertThat(k8sclient.network().v1().ingresses().inNamespace(namespace).list().getItems()).isEmpty());
    }

    @Test
    public void testCustomIngressDeletion() {
        Keycloak defaultKeycloakDeployment = getTestKeycloakDeployment(true);
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

            Awaitility.await()
                    .atMost(1, MINUTES)
                    .untilAsserted(() -> assertThat(k8sclient.network().v1().ingresses().inNamespace(namespace).list().getItems().size()).isEqualTo(1));

            Log.info("Deploying the Keycloak CR with default Ingress disabled");
            defaultKeycloakDeployment.getSpec().setIngressSpec(new IngressSpec());
            defaultKeycloakDeployment.getSpec().getIngressSpec().setIngressEnabled(false);

            K8sUtils.deployKeycloak(k8sclient, defaultKeycloakDeployment, true);

            Awaitility.await().untilAsserted(() -> {
                Log.info("Make sure the Custom Ingress still remains");
                assertThat(k8sclient.network().v1().ingresses().inNamespace(namespace).list().getItems().size()).isEqualTo(1);
            });

        } finally {
            Log.info("Destroying the Custom Ingress created manually to avoid errors in others Tests methods");
            if (customIngressDeployedManuallySelector != null && customIngressDeployedManuallySelector.isReady()) {
                assertThat(customIngressDeployedManuallySelector.delete()).isNotNull();
                Awaitility.await()
                        .untilAsserted(() -> assertThat(k8sclient.network().v1().ingresses().inNamespace(namespace).list().getItems()).isEmpty());
            }
        }
    }

    @Test
    public void testCustomIngressClassName() {
        var kc = getTestKeycloakDeployment(true);
        kc.getSpec().setIngressSpec(new IngressSpecBuilder().withIngressClassName("nginx").build());
        K8sUtils.deployKeycloak(k8sclient, kc, true);

        var ingressSelector = k8sclient
                .network()
                .v1()
                .ingresses()
                .inNamespace(namespace)
                .withName(KeycloakIngressDependentResource.getName(kc));

        Awaitility.await()
                .ignoreExceptions()
                .untilAsserted(() -> {
                    var i = ingressSelector.get();
                    assertEquals("nginx", i.getSpec().getIngressClassName());
                });

        // update to a different classname
        kc.getSpec().setIngressSpec(new IngressSpecBuilder().withIngressClassName("nginx-latest").build());
        K8sUtils.deployKeycloak(k8sclient, kc, true);

        Awaitility.await()
                .ignoreExceptions()
                .untilAsserted(() -> {
                    var i = ingressSelector.get();
                    assertEquals("nginx-latest", i.getSpec().getIngressClassName());
                });
    }

    @Test
    public void testCustomIngressAnnotations() {
        var kc = getTestKeycloakDeployment(true);
        kc.getSpec().setIngressSpec(new IngressSpec());
        kc.getSpec().getIngressSpec().setIngressEnabled(true);

        // set 'a'
        kc.getSpec().getIngressSpec().setAnnotations(Map.of("a", "b"));
        K8sUtils.deployKeycloak(k8sclient, kc, true);

        var ingressSelector = k8sclient
                .network()
                .v1()
                .ingresses()
                .inNamespace(namespace)
                .withName(KeycloakIngressDependentResource.getName(kc));

        Awaitility.await()
                .ignoreExceptions()
                .untilAsserted(() -> {
                    var i = ingressSelector.get();
                    assertEquals("HTTPS", i.getMetadata().getAnnotations().get("nginx.ingress.kubernetes.io/backend-protocol"));
                    assertEquals("passthrough", i.getMetadata().getAnnotations().get("route.openshift.io/termination"));
                    assertEquals("b", i.getMetadata().getAnnotations().get("a"));
                });

        // update 'a'
        kc.getSpec().getIngressSpec().setAnnotations(Map.of("a", "bb"));
        K8sUtils.deployKeycloak(k8sclient, kc, true);

        Awaitility.await()
                .ignoreExceptions()
                .untilAsserted(() -> {
                    var i = ingressSelector.get();
                    assertEquals("HTTPS", i.getMetadata().getAnnotations().get("nginx.ingress.kubernetes.io/backend-protocol"));
                    assertEquals("passthrough", i.getMetadata().getAnnotations().get("route.openshift.io/termination"));
                    assertEquals("bb", i.getMetadata().getAnnotations().get("a"));
                });

        // remove 'a' and add 'c'
        kc.getSpec().getIngressSpec().setAnnotations(Map.of("c", "d"));
        K8sUtils.deployKeycloak(k8sclient, kc, true);

        Awaitility.await()
                .ignoreExceptions()
                .untilAsserted(() -> {
                    var i = ingressSelector.get();
                    assertEquals("HTTPS", i.getMetadata().getAnnotations().get("nginx.ingress.kubernetes.io/backend-protocol"));
                    assertEquals("passthrough", i.getMetadata().getAnnotations().get("route.openshift.io/termination"));
                    assertFalse(i.getMetadata().getAnnotations().containsKey("a"));
                    assertEquals("d", i.getMetadata().getAnnotations().get("c"));
                });

        // remove all
        kc.getSpec().getIngressSpec().setAnnotations(null);
        K8sUtils.deployKeycloak(k8sclient, kc, true);

        Awaitility.await()
                .ignoreExceptions()
                .untilAsserted(() -> {
                    var i = ingressSelector.get();
                    assertEquals("HTTPS", i.getMetadata().getAnnotations().get("nginx.ingress.kubernetes.io/backend-protocol"));
                    assertEquals("passthrough", i.getMetadata().getAnnotations().get("route.openshift.io/termination"));
                    assertFalse(i.getMetadata().getAnnotations().containsKey("a"));
                    assertFalse(i.getMetadata().getAnnotations().containsKey("c"));
                });
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

        customIngressCreated = k8sclient.resource(customIngressCreated).create();

        return customIngressCreated;
    }

}

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

import java.util.Map;
import java.util.concurrent.TimeUnit;

import jakarta.inject.Inject;

import org.keycloak.operator.Config;
import org.keycloak.operator.Constants;
import org.keycloak.operator.Utils;
import org.keycloak.operator.controllers.KeycloakClientBaseController;
import org.keycloak.operator.crds.v2alpha1.client.KeycloakOIDCClient;
import org.keycloak.operator.crds.v2alpha1.client.KeycloakOIDCClientBuilder;
import org.keycloak.operator.crds.v2alpha1.deployment.Keycloak;
import org.keycloak.operator.crds.v2alpha1.deployment.spec.BootstrapAdminSpec;
import org.keycloak.operator.crds.v2alpha1.deployment.spec.FeatureSpecBuilder;
import org.keycloak.operator.testsuite.apiserver.DisabledIfApiServerTest;
import org.keycloak.operator.testsuite.utils.K8sUtils;

import io.fabric8.kubernetes.api.model.SecretBuilder;
import io.fabric8.kubernetes.api.model.ServiceBuilder;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.keycloak.operator.testsuite.utils.K8sUtils.deployKeycloak;

import static org.junit.jupiter.api.Assertions.assertNull;

@DisabledIfApiServerTest
@Tag(BaseOperatorTest.SLOW)
@QuarkusTest
public class KeycloakClientTest extends BaseOperatorTest {

    @Inject
    Config config;

    static String initCustomBootstrapAdminServiceAccount(Keycloak kc) {
        String secretName = kc.getMetadata().getName() + "-admin";
        // fluents don't seem to work here because of the inner classes
        kc.getSpec().setBootstrapAdminSpec(new BootstrapAdminSpec());
        kc.getSpec().getBootstrapAdminSpec().setService(new BootstrapAdminSpec.Service());
        kc.getSpec().getBootstrapAdminSpec().getService().setSecret(secretName);
        k8sclient.resource(new SecretBuilder().withNewMetadata().withName(secretName).endMetadata()
                .addToStringData(Constants.CLIENT_ID_KEY, "admin-service")
                .addToStringData(Constants.CLIENT_SECRET_KEY, "secret").build()).serverSideApply();
        return secretName;
    }

    @Test
    public void testBasicClientCreationAndDeletion() throws InterruptedException {
        boolean https = true;
        var kc = getTestKeycloakDeployment(false);
        kc.getSpec().getHostnameSpec().setHostname("example.com");
        // TODO will need validation that this is enabled
        kc.getSpec().setFeatureSpec(new FeatureSpecBuilder().withEnabledFeatures("client-admin-api:v2").build());
        if (!https) {
            kc.getSpec().getHttpSpec().setTlsSecret(null);
            kc.getSpec().getHttpSpec().setHttpEnabled(true);
        }

        // TODO: for the sake of testing, this uses the built-in bootstrap admin
        // we don't expect users to do this
        initCustomBootstrapAdminServiceAccount(kc);
        var deploymentName = kc.getMetadata().getName();
        deployKeycloak(k8sclient, kc, true);

        if (operatorDeployment == OperatorDeployment.local) {
            Map<String, String> labels = Utils.allInstanceLabels(kc);
            labels.put("app.kubernetes.io/component", "server");

            var nodeport = new ServiceBuilder().withNewMetadata().withName("nodeport-service").endMetadata().withNewSpec()
                    .withType("NodePort").addToSelector(labels).addNewPort().withPort(https?Constants.KEYCLOAK_HTTPS_PORT:Constants.KEYCLOAK_HTTP_PORT)
                    .endPort().endSpec().build();
            nodeport = k8sclient.resource(nodeport).create();
            int port = nodeport.getSpec().getPorts().get(0).getNodePort();

            System.setProperty(KeycloakClientBaseController.KEYCLOAK_TEST_ADDRESS, kubernetesIp + ":" + port);
            //System.setProperty(KeycloakClientBaseController.KEYCLOAK_TEST_ADDRESS, "localhost:8080");
        }

        KeycloakOIDCClient client = new KeycloakOIDCClientBuilder().withNewMetadata().withName("test-client")
                .endMetadata().withNewSpec().withRealm("master").withKeycloakCRName(deploymentName).withNewClient()
                .withEnabled(true).endClient().endSpec().build();

        K8sUtils.set(k8sclient, client);

        Thread.sleep(10000);

        k8sclient.resource(client).withTimeout(10, TimeUnit.SECONDS).delete();

        assertNull(k8sclient.resource(client).get());
    }

}

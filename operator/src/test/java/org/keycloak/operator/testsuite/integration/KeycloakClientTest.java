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
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import jakarta.enterprise.inject.spi.CDI;
import jakarta.inject.Inject;

import org.keycloak.operator.Config;
import org.keycloak.operator.Constants;
import org.keycloak.operator.Utils;
import org.keycloak.operator.controllers.KeycloakClientBaseController;
import org.keycloak.operator.controllers.KeycloakOIDCClientController;
import org.keycloak.operator.controllers.KeycloakSAMLClientController;
import org.keycloak.operator.crds.v2alpha1.client.KeycloakClientStatusCondition;
import org.keycloak.operator.crds.v2alpha1.client.KeycloakOIDCClient;
import org.keycloak.operator.crds.v2alpha1.client.KeycloakOIDCClientBuilder;
import org.keycloak.operator.crds.v2alpha1.client.KeycloakOIDCClientRepresentation.AuthWithSecretRef;
import org.keycloak.operator.crds.v2alpha1.client.KeycloakSAMLClient;
import org.keycloak.operator.crds.v2alpha1.client.KeycloakSAMLClientBuilder;
import org.keycloak.operator.crds.v2alpha1.deployment.Keycloak;
import org.keycloak.operator.crds.v2alpha1.deployment.ValueOrSecret;
import org.keycloak.operator.crds.v2alpha1.deployment.spec.AdminSpec;
import org.keycloak.operator.crds.v2alpha1.deployment.spec.BootstrapAdminSpec;
import org.keycloak.operator.crds.v2alpha1.deployment.spec.FeatureSpecBuilder;
import org.keycloak.operator.crds.v2alpha1.deployment.spec.TruststoreBuilder;
import org.keycloak.operator.testsuite.apiserver.DisabledIfApiServerTest;
import org.keycloak.operator.testsuite.utils.K8sUtils;

import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.SecretBuilder;
import io.fabric8.kubernetes.api.model.SecretKeySelector;
import io.fabric8.kubernetes.api.model.ServiceBuilder;
import io.quarkus.test.junit.QuarkusTest;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.keycloak.operator.testsuite.utils.K8sUtils.deployKeycloak;

import static org.junit.jupiter.api.Assertions.assertNull;

@DisabledIfApiServerTest
@Tag(BaseOperatorTest.SLOW)
@QuarkusTest
public class KeycloakClientTest extends BaseOperatorTest {

    private static final String CLIENT_SECRET = "client-secret";
    private static final String CLIENT_TRUSTSTORE_SECRET = "example-mtls-truststore-secret";
    private static final String CLIENT_TLS_SECRET = "example-mtls-secret";

    private static final String NODEPORT_SERVICE = "nodeport-service";

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

    @AfterEach
    public void afterEach() {
        k8sclient.services().withName(NODEPORT_SERVICE).delete();
        k8sclient.secrets().withName(CLIENT_SECRET).delete();
        k8sclient.secrets().withName(CLIENT_TRUSTSTORE_SECRET).delete();
        k8sclient.secrets().withName(CLIENT_TLS_SECRET).delete();
    }

    @Test
    public void testBasicSamlClientCreationAndDeletionHttp() throws InterruptedException {
        var kc = getTestKeycloakDeployment(false);
        deployKeycloakWithAdminApiV2(false, kc);
        String addressOverride = createNodePort(false, kc);
        var deploymentName = kc.getMetadata().getName();

        String clientName = "saml-client";
        KeycloakSAMLClient client = new KeycloakSAMLClientBuilder().withNewMetadata().withName(clientName)
                .endMetadata().withNewSpec().withRealm("master").withKeycloakCRName(deploymentName).withNewClient()
                .withEnabled(true).endClient().endSpec().build();

        K8sUtils.set(k8sclient, client);

        Awaitility.await().ignoreExceptions()
                .until(() -> k8sclient.resource(client).get().getStatus().getConditions().stream()
                        .noneMatch(c -> Boolean.TRUE.equals(c.getStatus())
                                && KeycloakClientStatusCondition.HAS_ERRORS.equals(c.getType())));

        // TODO: a success or ready status?

        try (var adminClient = KeycloakClientBaseController.getAdminClient(k8sclient, kc, addressOverride)) {
            Awaitility.await().until(() -> adminClient.realm("master").clients().findAll().stream().anyMatch(cr -> cr.getClientId().equals(clientName)));

            k8sclient.resource(client).withTimeout(10, TimeUnit.SECONDS).delete();

            Awaitility.await().until(() -> adminClient.realm("master").clients().findAll().stream().noneMatch(cr -> cr.getClientId().equals(clientName)));
        }

        assertNull(k8sclient.resource(client).get());
    }

    @Test
    public void testBasicOIDCClientCreationAndDeletionHttp() throws InterruptedException {
        helpTestBasicOIDCClientCreationAndDeletion(false);
    }

    @Test
    public void testBasicOIDCClientCreationAndDeletionHttps() throws InterruptedException {
        helpTestBasicOIDCClientCreationAndDeletion(true);
    }

    public void helpTestBasicOIDCClientCreationAndDeletion(boolean https) throws InterruptedException {
        var kc = getTestKeycloakDeployment(false);
        deployKeycloakWithAdminApiV2(https, kc);
        String addressOverride = createNodePort(https, kc);
        var deploymentName = kc.getMetadata().getName();

        AuthWithSecretRef auth = new AuthWithSecretRef();
        auth.setMethod("client-jwt");
        auth.setSecretRef(new SecretKeySelector("secret", CLIENT_SECRET, null));
        String clientName = "test-client";
        KeycloakOIDCClient client = new KeycloakOIDCClientBuilder().withNewMetadata().withName(clientName)
                .endMetadata().withNewSpec().withRealm("master").withKeycloakCRName(deploymentName).withNewClient()
                .withAuth(auth)
                .withEnabled(true).endClient().endSpec().build();

        K8sUtils.set(k8sclient, client);

        Awaitility.await().ignoreExceptions()
                .until(() -> Optional.ofNullable(k8sclient.resource(client).get().getStatus()).filter(s -> s.getConditions().stream()
                        .anyMatch(c -> Boolean.TRUE.equals(c.getStatus())
                                && KeycloakClientStatusCondition.HAS_ERRORS.equals(c.getType())
                                && c.getMessage().contains(CLIENT_SECRET))).isPresent());

        K8sUtils.set(k8sclient, new SecretBuilder().withNewMetadata().withName(CLIENT_SECRET).endMetadata()
                .addToStringData("secret", "1234567890").build());

        Awaitility.await()
                .until(() -> k8sclient.resource(client).get().getStatus().getConditions().stream()
                        .noneMatch(c -> Boolean.TRUE.equals(c.getStatus())
                                && KeycloakClientStatusCondition.HAS_ERRORS.equals(c.getType())));

        // TODO: a success or ready status?

        try (var adminClient = KeycloakClientBaseController.getAdminClient(k8sclient, kc, addressOverride)) {
            Awaitility.await().until(() -> adminClient.realm("master").clients().findAll().stream().anyMatch(cr -> cr.getClientId().equals(clientName)));

            k8sclient.resource(client).withTimeout(10, TimeUnit.SECONDS).delete();

            Awaitility.await().until(() -> adminClient.realm("master").clients().findAll().stream().noneMatch(cr -> cr.getClientId().equals(clientName)));
        }

        assertNull(k8sclient.resource(client).get());
    }

    private String createNodePort(boolean https, Keycloak kc) {
        Map<String, String> labels = Utils.allInstanceLabels(kc);
        labels.put("app.kubernetes.io/component", "server");

        var nodeport = new ServiceBuilder().withNewMetadata().withName(NODEPORT_SERVICE).endMetadata().withNewSpec()
                .withType("NodePort").addToSelector(labels).addNewPort().withPort(https?Constants.KEYCLOAK_HTTPS_PORT:Constants.KEYCLOAK_HTTP_PORT)
                .endPort().endSpec().build();
        nodeport = k8sclient.resource(nodeport).serverSideApply();
        int port = nodeport.getSpec().getPorts().get(0).getNodePort();

        String addressOverride = kubernetesIp + ":" + port;
        if (operatorDeployment == OperatorDeployment.local) {
            CDI.current().select(KeycloakOIDCClientController.class).get().setAddressOverride(addressOverride);
            CDI.current().select(KeycloakSAMLClientController.class).get().setAddressOverride(addressOverride);
        }
        return addressOverride;
    }

    private void deployKeycloakWithAdminApiV2(boolean https, Keycloak kc) {
        kc.getSpec().setStartOptimized(false);
        // TODO will need validation that this is enabled
        kc.getSpec().setFeatureSpec(new FeatureSpecBuilder().withEnabledFeatures("client-admin-api:v2").build());
        if (!https) {
            kc.getSpec().getHttpSpec().setTlsSecret(null);
            kc.getSpec().getHttpSpec().setHttpEnabled(true);
        } else {
            AdminSpec adminSpec = new AdminSpec();
            K8sUtils.set(k8sclient, K8sUtils.getResourceFromFile("/example-mtls-secret.yaml", Secret.class));
            adminSpec.setTlsSecret(CLIENT_TLS_SECRET);
            kc.getSpec().setAdminSpec(adminSpec);
            K8sUtils.set(k8sclient, getClass().getResourceAsStream("/example-mtls-truststore-secret.yaml"));
            kc.getSpec().getTruststores().put("example", new TruststoreBuilder().withNewSecret().withName(CLIENT_TRUSTSTORE_SECRET).endSecret().build());
            kc.getSpec().getAdditionalOptions().add(new ValueOrSecret("https-client-auth", "required"));
            kc.getSpec().getAdditionalOptions().add(new ValueOrSecret("https-management-client-auth", "none"));
        }

        // TODO: for the sake of testing, this uses the built-in bootstrap admin
        // we don't expect users to do this
        initCustomBootstrapAdminServiceAccount(kc);
        deployKeycloak(k8sclient, kc, true);
    }

}

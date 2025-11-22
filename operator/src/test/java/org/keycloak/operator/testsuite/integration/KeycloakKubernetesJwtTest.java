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

import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.EnvVar;
import io.fabric8.kubernetes.api.model.PodBuilder;
import io.fabric8.kubernetes.api.model.PodSpec;
import io.fabric8.kubernetes.api.model.PodTemplateSpec;
import io.fabric8.kubernetes.client.LocalPortForward;
import io.quarkus.logging.Log;
import io.quarkus.test.junit.QuarkusTest;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import org.keycloak.jose.jws.JWSInput;
import org.keycloak.operator.controllers.KeycloakServiceDependentResource;
import org.keycloak.operator.crds.v2alpha1.deployment.Keycloak;
import org.keycloak.operator.crds.v2alpha1.deployment.spec.FeatureSpec;
import org.keycloak.operator.crds.v2alpha1.deployment.spec.UnsupportedSpec;
import org.keycloak.operator.testsuite.apiserver.DisabledIfApiServerTest;
import org.keycloak.operator.testsuite.utils.TrustAllSSLContext;
import org.keycloak.representations.JsonWebToken;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.IdentityProviderRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static java.util.concurrent.TimeUnit.MINUTES;
import static org.assertj.core.api.Assertions.assertThat;
import static org.keycloak.operator.Constants.KEYCLOAK_HTTPS_PORT;
import static org.keycloak.operator.testsuite.utils.K8sUtils.deployKeycloak;
import static org.keycloak.operator.testsuite.utils.K8sUtils.inClusterCurl;
import static org.keycloak.operator.testsuite.utils.K8sUtils.inClusterCurlCommand;

@DisabledIfApiServerTest
@Tag(BaseOperatorTest.SLOW)
@QuarkusTest
public class KeycloakKubernetesJwtTest extends BaseOperatorTest {

    @DisabledIfApiServerTest
    @Test
    public void testSignedJWTs() throws IOException {
        // Arrange
        var kc = getTestKeycloakDeployment(false);

        if (kc.getSpec().getFeatureSpec() ==null) {
            kc.getSpec().setFeatureSpec(new FeatureSpec());
        }
        kc.getSpec().getFeatureSpec().setEnabledFeatures(List.of("kubernetes-service-accounts", "client-auth-federated"));
        kc.getSpec().setStartOptimized(false);

        PodTemplateSpec podTemplate = new PodTemplateSpec();
        kc.getSpec().setUnsupported(new UnsupportedSpec(podTemplate));
        PodSpec podSpec = new PodSpec();
        podTemplate.setSpec(podSpec);
        Container container = new Container();
        podSpec.setContainers(List.of(container));
        container.setEnv(List.of(
                new EnvVar("JAVA_OPTS_APPEND", "-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:8787", null),
                new EnvVar("KC_BOOTSTRAP_ADMIN_USERNAME", "admin", null),
                new EnvVar("KC_BOOTSTRAP_ADMIN_PASSWORD", "admin", null)
        ));

        deployKeycloak(k8sclient, kc, false);

        var builder = new PodBuilder();
        builder.withNewMetadata()
                .withName("example-kc-0")
                .withNamespace(namespace)
                .endMetadata();
        var kcPod = builder.build();

        k8sclient.pods().resource(kcPod).waitUntilReady(5, MINUTES);

        try (LocalPortForward pf = k8sclient.pods().resource(kcPod)
                .portForward(8443)) {
            var keycloak = org.keycloak.admin.client.Keycloak.getInstance(
                    "https://127.0.0.1:" + pf.getLocalPort(),
                    "master",
                    "admin",
                    "admin",
                    "admin-cli",
                    TrustAllSSLContext.getContext());
            RealmRepresentation realm = new RealmRepresentation();
            realm.setRealm("test");
            realm.setEnabled(true);
            keycloak.realms().create(realm);

            ClientRepresentation client = new ClientRepresentation();
            client.setClientId("kubernetes-client");
            client.setEnabled(true);
            client.setClientAuthenticatorType("federated-jwt");
            client.setServiceAccountsEnabled(true);
            client.setAttributes(Map.of(
                    "jwt.credential.issuer", "kubernetes",
                    "jwt.credential.sub", "system:serviceaccount:" + namespace + ":default"
            ));
            keycloak.realm("test").clients().create(client).close();

            IdentityProviderRepresentation idp = new IdentityProviderRepresentation();
            idp.setAlias("kubernetes");
            idp.setProviderId("kubernetes");
            idp.getConfig().put("issuer", "https://kubernetes.default.svc.cluster.local");
            keycloak.realm("test").identityProviders().create(idp).close();

        }

        // Assert
        assertSignedJWT(kc);

    }

    private void assertSignedJWT(Keycloak kc) {
        Awaitility.await().atMost(10, MINUTES).ignoreExceptions().untilAsserted(() -> {
            Log.info("Starting curl Pod to test if Kubernetes Signed JWT is available");

            var curlOutput = inClusterCurlCommand(k8sclient, namespace, Map.of(), "cat", "/var/run/secrets/tokens/test-aud-token");
            assertThat(curlOutput.exitCode()).isZero();
            var token = curlOutput.stdout();

            String url =
                    "https://" + KeycloakServiceDependentResource.getServiceName(kc) + "." + namespace + ":" + KEYCLOAK_HTTPS_PORT + "/realms/test/protocol/openid-connect/token";
            // To not quote arguments as this is only necessary on a shell CLI, but this is executed directly and it then confuses cURL
            String[] args = {
                    "-v", "-k",
                    url,
                    "-H", "Content-Type: application/x-www-form-urlencoded",
                    "--data-urlencode", "grant_type=client_credentials",
                    "--data-urlencode", "client_assertion_type=urn:ietf:params:oauth:client-assertion-type:jwt-bearer",
                    "--data-urlencode", "client_assertion=" + token
            };
            Log.info("Url: '" + url + "'");
            String clientCredentialsOutput = inClusterCurl(k8sclient, namespace, args);
            assertThat(clientCredentialsOutput).contains("access_token");
        });
    }


}

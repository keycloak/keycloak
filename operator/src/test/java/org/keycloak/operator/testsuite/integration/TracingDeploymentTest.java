/*
 * Copyright 2024 Red Hat, Inc. and/or its affiliates
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

import io.fabric8.kubernetes.api.model.EnvVar;
import io.fabric8.kubernetes.api.model.SecretBuilder;
import io.fabric8.kubernetes.api.model.SecretKeySelectorBuilder;
import io.quarkus.test.junit.QuarkusTest;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;
import org.keycloak.operator.Constants;
import org.keycloak.operator.crds.v2alpha1.deployment.ValueOrSecret;
import org.keycloak.operator.crds.v2alpha1.deployment.spec.TracingSpec;
import org.keycloak.operator.crds.v2alpha1.deployment.spec.TracingSpecBuilder;
import org.keycloak.operator.testsuite.apiserver.DisabledIfApiServerTest;
import org.keycloak.operator.testsuite.utils.K8sUtils;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.keycloak.operator.controllers.KeycloakDeploymentDependentResource.KC_TRACING_RESOURCE_ATTRIBUTES;
import static org.keycloak.operator.controllers.KeycloakDeploymentDependentResource.KC_TRACING_SERVICE_NAME;
import static org.keycloak.operator.testsuite.utils.K8sUtils.deployKeycloak;

@DisabledIfApiServerTest
@QuarkusTest
public class TracingDeploymentTest extends BaseOperatorTest {

    @Test
    public void podNamePropagation() {
        var kc = getTestKeycloakDeployment(false);
        kc.getSpec().setStartOptimized(false);

        var tracingSpec = new TracingSpec();
        tracingSpec.setEnabled(true);
        kc.getSpec().setTracingSpec(tracingSpec);

        kc.getSpec().getAdditionalOptions().add(new ValueOrSecret("log-level", "io.opentelemetry:fine"));
        deployKeycloak(k8sclient, kc, true);

        Awaitility.await()
                .ignoreExceptions()
                .untilAsserted(() ->
                        assertThat(k8sclient
                                .pods()
                                .inNamespace(namespace)
                                .withLabel("app", "keycloak")
                                .list()
                                .getItems()
                                .size()).isNotZero());

        var pods = k8sclient
                .pods()
                .inNamespace(namespace)
                .withLabels(Constants.DEFAULT_LABELS)
                .list()
                .getItems();

        var envVars = pods.get(0).getSpec().getContainers().get(0).getEnv();
        assertThat(envVars).isNotNull();
        assertThat(envVars).isNotEmpty();

        var serviceNameEnv = envVars.stream().filter(f -> f.getName().equals(KC_TRACING_SERVICE_NAME)).findAny().orElse(null);
        assertThat(serviceNameEnv).isNotNull();
        assertThat(serviceNameEnv.getValue()).isEqualTo(kc.getMetadata().getName());

        var resourceAttributesEnv = envVars.stream().filter(f -> f.getName().equals(KC_TRACING_RESOURCE_ATTRIBUTES)).findAny().orElse(null);
        assertThat(resourceAttributesEnv).isNotNull();

        var expectedAttributes = Map.of(
                "k8s.namespace.name", kc.getMetadata().getNamespace()
        ).entrySet().stream().map(e -> e.getKey() + "=" + e.getValue()).collect(Collectors.joining(","));

        assertThat(resourceAttributesEnv.getValue()).isEqualTo(expectedAttributes);
    }

    @Test
    public void tracingSpec() {
        var kc = getTestKeycloakDeployment(false);
        kc.getSpec().setStartOptimized(false);

        var tracingSpec = new TracingSpecBuilder()
                .withEnabled()
                .withEndpoint("http://0.0.0.0:4317")
                .withServiceName("my-best-keycloak")
                .withProtocol("http/protobuf")
                .withSamplerType("parentbased_traceidratio")
                .withSamplerRatio(0.01)
                .withCompression("gzip")
                .withResourceAttributes(Map.of(
                        "something.a", "keycloak-rocks",
                        "something.b", "keycloak-rocks2"))
                .build();

        var additionalOptions = List.of(
                new ValueOrSecret("tracing-header-Some-Header", "some-value-for-header")
        );

        kc.getSpec().setTracingSpec(tracingSpec);
        kc.getSpec().setAdditionalOptions(additionalOptions);

        deployKeycloak(k8sclient, kc, true);

        var pods = k8sclient
                .pods()
                .inNamespace(namespace)
                .withLabels(Constants.DEFAULT_LABELS)
                .list()
                .getItems();

        assertThat(pods).isNotNull();
        assertThat(pods).isNotEmpty();

        var map = pods.get(0).getSpec().getContainers().get(0).getEnv().stream()
                .filter(Objects::nonNull).filter(f -> f.getName().startsWith("KC_TRACING_"))
                .collect(Collectors.toMap(EnvVar::getName, EnvVar::getValue));

        assertThat(map).isNotNull();
        assertThat(map).isNotEmpty();

        // assertions

        var enabled = map.get("KC_TRACING_ENABLED");
        assertThat(enabled).isNotNull();
        assertThat(enabled).isEqualTo("true");

        var endpoint = map.get("KC_TRACING_ENDPOINT");
        assertThat(endpoint).isNotNull();
        assertThat(endpoint).isEqualTo("http://0.0.0.0:4317");

        var serviceName = map.get("KC_TRACING_SERVICE_NAME");
        assertThat(serviceName).isNotNull();
        assertThat(serviceName).isEqualTo("my-best-keycloak");

        var protocol = map.get("KC_TRACING_PROTOCOL");
        assertThat(protocol).isNotNull();
        assertThat(protocol).isEqualTo("http/protobuf");

        var samplerType = map.get("KC_TRACING_SAMPLER_TYPE");
        assertThat(samplerType).isNotNull();
        assertThat(samplerType).isEqualTo("parentbased_traceidratio");

        var samplerRatio = map.get("KC_TRACING_SAMPLER_RATIO");
        assertThat(samplerRatio).isNotNull();
        assertThat(samplerRatio).isEqualTo("0.01");

        var compression = map.get("KC_TRACING_COMPRESSION");
        assertThat(compression).isNotNull();
        assertThat(compression).isEqualTo("gzip");

        var resourceAttributes = map.get("KC_TRACING_RESOURCE_ATTRIBUTES");
        assertThat(resourceAttributes).isNotNull();
        assertThat(resourceAttributes).contains("something.a=keycloak-rocks");
        assertThat(resourceAttributes).contains("something.b=keycloak-rocks2");
        assertThat(resourceAttributes).contains(String.format("k8s.namespace.name=%s", namespace));

        var headerSomeHeader=map.get("KC_TRACING_HEADER_SOME_HEADER");
        assertThat(headerSomeHeader).isNotNull();
        assertThat(headerSomeHeader).isEqualTo("some-value-for-header");
    }

    @Test
    public void testTracingHeaders() {
        var kc = getTestKeycloakDeployment(false);
        kc.getSpec().setImage(null); // doesn't seem to become ready with the custom image
        var secretName = "tracing-secret";
        var keyName = "token";
        var tokenTracingSecret = new SecretBuilder()
                .withNewMetadata()
                .withName(secretName)
                .withNamespace(namespace)
                .endMetadata()
                .addToStringData(keyName, "Bearer asdfasdfasdfasdf2345")
                .build();
        K8sUtils.set(k8sclient, tokenTracingSecret);

        kc.getSpec().getAdditionalOptions().add(new ValueOrSecret("tracing-header-Authorization",
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

        assertTrue(pods.get(0).getSpec().getContainers().get(0).getEnv().stream().anyMatch(
                e -> e.getName().equals("KC_TRACING_HEADER_AUTHORIZATION") && e.getValueFrom() != null
                        && e.getValueFrom().getSecretKeyRef().getName().equals(secretName) && e.getValueFrom().getSecretKeyRef().getKey().equals(keyName)));
    }
}

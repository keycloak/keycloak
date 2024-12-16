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

import io.quarkus.test.junit.QuarkusTest;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;
import org.keycloak.operator.Constants;
import org.keycloak.operator.crds.v2alpha1.deployment.ValueOrSecret;
import org.keycloak.operator.crds.v2alpha1.deployment.spec.TracingSpec;

import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.keycloak.operator.controllers.KeycloakDeploymentDependentResource.KC_TRACING_RESOURCE_ATTRIBUTES;
import static org.keycloak.operator.controllers.KeycloakDeploymentDependentResource.KC_TRACING_SERVICE_NAME;
import static org.keycloak.operator.testsuite.utils.K8sUtils.deployKeycloak;

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
}

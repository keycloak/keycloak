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

package org.keycloak.operator.testsuite.unit;

import io.fabric8.kubernetes.api.model.EnvVar;
import io.quarkus.test.junit.QuarkusTest;

import org.junit.jupiter.api.Test;
import org.keycloak.common.util.CollectionUtil;
import org.keycloak.operator.Constants;
import org.keycloak.operator.controllers.KeycloakDistConfigurator;
import org.keycloak.operator.crds.v2alpha1.deployment.Keycloak;
import org.keycloak.operator.crds.v2alpha1.deployment.KeycloakStatus;
import org.keycloak.operator.crds.v2alpha1.deployment.KeycloakStatusAggregator;
import org.keycloak.operator.crds.v2alpha1.deployment.KeycloakStatusCondition;
import org.keycloak.operator.crds.v2alpha1.deployment.ValueOrSecret;
import org.keycloak.operator.testsuite.utils.K8sUtils;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.keycloak.common.util.ObjectUtil.isBlank;
import static org.keycloak.operator.controllers.KeycloakDistConfigurator.getKeycloakOptionEnvVarName;
import static org.keycloak.operator.testsuite.utils.CRAssert.assertKeycloakStatusCondition;
import static org.keycloak.operator.testsuite.utils.CRAssert.assertKeycloakStatusDoesNotContainMessage;

@QuarkusTest
public class KeycloakDistConfiguratorTest {

    final KeycloakDistConfigurator distConfig = new KeycloakDistConfigurator();

    @Test
    public void enabledFeatures() {
        testFirstClassCitizen(Map.of("features", "docker,authorization"));
    }

    @Test
    public void disabledFeatures() {
        testFirstClassCitizen(Map.of("features-disabled", "admin,step-up-authentication"));
    }

    @Test
    public void transactions() {
        testFirstClassCitizen(Map.of("transaction-xa-enabled", "false"));
    }

    @Test
    public void cache() {
        testFirstClassCitizen(Map.of("cache-config-file", "cache/file.xml"));
    }

    @Test
    public void http() {
        final Map<String, String> expectedValues = Map.of(
                "http-enabled", "true",
                "http-port", "123",
                "https-port", "456",
                "https-certificate-file", Constants.CERTIFICATES_FOLDER + "/tls.crt",
                "https-certificate-key-file", Constants.CERTIFICATES_FOLDER + "/tls.key"
        );

        testFirstClassCitizen(expectedValues);
    }

    @Test
    public void featuresEmptyLists() {
        final Keycloak keycloak = K8sUtils.getResourceFromFile("test-serialization-keycloak-cr-with-empty-list.yml", Keycloak.class);
        var envVars = distConfig.configureDistOptions(keycloak);
        assertEnvVarNotPresent(envVars, "KC_FEATURES");
        assertEnvVarNotPresent(envVars, "KC_FEATURES_DISABLED");
    }

    @Test
    public void db() {
        final Map<String, String> expectedValues = new HashMap<>(Map.of(
                "db", "vendor",
                "db-username", "usernameSecret",
                "db-password", "passwordSecret",
                "db-url-database", "database",
                "db-schema", "schema",
                "db-url-host", "host",
                "db-url-port", "123",
                "db-pool-initial-size", "1",
                "db-pool-min-size", "2",
                "db-pool-max-size", "3"
        ));
        expectedValues.put("db-url", "url");

        testFirstClassCitizen(expectedValues);
    }

    @Test
    public void hostname() {
        final Map<String, String> expectedValues = Map.of(
                "hostname", "my-hostname",
                "hostname-admin-url", "https://www.my-admin-hostname.org:8448/something",
                "hostname-strict", "true",
                "hostname-strict-backchannel", "true",
                "hostname-backchannel-dynamic", "true",
                "hostname-admin", "my-admin-hostname"
        );

        testFirstClassCitizen(expectedValues);
    }

    @Test
    public void missingHostname() {
        final Keycloak keycloak = K8sUtils.getResourceFromFile("test-serialization-keycloak-cr-with-empty-list.yml", Keycloak.class);

        var envVars = distConfig.configureDistOptions(keycloak);

        assertEnvVarNotPresent(envVars, "KC_HOSTNAME");
        assertEnvVarNotPresent(envVars, "KC_HOSTNAME_ADMIN");
        assertEnvVarNotPresent(envVars, "KC_HOSTNAME_ADMIN_URL");
        assertEnvVarNotPresent(envVars, "KC_HOSTNAME_STRICT");
        assertEnvVarNotPresent(envVars, "KC_HOSTNAME_STRICT_BACKCHANNEL");
        assertEnvVarNotPresent(envVars, "KC_HOSTNAME_BACKCHANNEL_DYNAMIC");
    }

    @Test
    public void proxy() {
        final Map<String, String> expectedValues = Map.of(
                "proxy-headers", "forwarded"
        );

        testFirstClassCitizen(expectedValues);
    }

    @Test
    public void management() {
        final Map<String, String> expectedValues = new HashMap<>(Map.of(
                "http-management-port", "9003"
        ));

        testFirstClassCitizen(expectedValues);
    }
    
    @Test
    public void bootstrapAdmin() {
        final Map<String, String> expectedValues = Map.of(
                "bootstrap-admin-username", "something",
                "bootstrap-admin-password", "something",
                "bootstrap-admin-client-id", "else",
                "bootstrap-admin-client-secret", "else"
        );

        testFirstClassCitizen(expectedValues);
    }

    @Test
    public void tracing() {
        final Map<String, String> expectedValues = Map.of(
                "tracing-enabled", "true",
                "tracing-endpoint", "http://my-tracing:4317",
                "tracing-service-name", "my-best-keycloak",
                "tracing-protocol", "http/protobuf",
                "tracing-sampler-type", "parentbased_traceidratio",
                "tracing-sampler-ratio", "0.01",
                "tracing-compression", "gzip",
                "tracing-resource-attributes", "service.namespace=keycloak-namespace,service.name=custom-service-name"
        );

        testFirstClassCitizen(expectedValues);
    }

    /* UTILS */

    private void testFirstClassCitizen(Map<String, String> expectedValues) {
        testFirstClassCitizen("/test-serialization-keycloak-cr.yml", expectedValues);
    }

    private void testFirstClassCitizen(String crName, Map<String, String> expectedValues) {
        final Keycloak keycloak = K8sUtils.getResourceFromFile(crName, Keycloak.class);

        final List<ValueOrSecret> serverConfig = expectedValues.keySet()
                .stream()
                .map(f -> new ValueOrSecret(f, "foo"))
                .collect(Collectors.toUnmodifiableList());

        keycloak.getSpec().setAdditionalOptions(serverConfig);

        final var expectedFields = expectedValues.keySet();

        assertWarningStatusFirstClassFields(keycloak, distConfig, true, expectedFields);

        var envVars = distConfig.configureDistOptions(keycloak);
        expectedValues.forEach((k, v) -> assertContainerEnvVar(envVars, getKeycloakOptionEnvVarName(k), v));
    }

    /**
     * assertContainerEnvVar(container.getEnv(), "KC_FEATURES", "admin,authorization");
     * assertContainerEnvVar(container.getEnv(), "KC_HOSTNAME", "someHostname");
     */
    private void assertContainerEnvVar(List<EnvVar> envVars, String varName, String expectedValue) {
        assertThat(envVars).isNotNull();
        assertEnvVarPresent(envVars, varName);

        var matching = envVars.stream().filter(f -> varName.equals(f.getName()))
                .map(envVar -> {
                    if (envVar.getValue() != null) {
                        return envVar.getValue();
                    }

                    if (envVar.getValueFrom() != null && envVar.getValueFrom().getSecretKeyRef() != null) {
                        return envVar.getValueFrom().getSecretKeyRef().getName();
                    }

                    return null;
                }).collect(Collectors.toList());
        assertThat(matching.size()).isLessThan(2);
        final String foundValue = matching.stream().findFirst().orElse(null);

        assertThat(foundValue).isNotNull();
        assertThat(foundValue).isEqualTo(expectedValue);
    }

    private void assertEnvVarPresent(List<EnvVar> envVars, String varName) {
        assertThat(containsEnvironmentVariable(envVars, varName)).isTrue();
    }

    private void assertEnvVarNotPresent(List<EnvVar> envVars, String varName) {
        assertThat(containsEnvironmentVariable(envVars, varName)).isFalse();
    }

    private void assertWarningStatusFirstClassFields(Keycloak keycloak, KeycloakDistConfigurator distConfig, boolean expectWarning, Collection<String> firstClassFields) {
        final String message = "warning: You need to specify these fields as the first-class citizen of the CR: ";
        final KeycloakStatusAggregator statusBuilder = new KeycloakStatusAggregator(1L);
        distConfig.validateOptions(keycloak, statusBuilder);
        final KeycloakStatus status = statusBuilder.build();

        if (expectWarning) {
            assertKeycloakStatusCondition(status, KeycloakStatusCondition.HAS_ERRORS, false, message);

            var fullMessage = getFullMessageFromStatus(status, message);
            assertThat(fullMessage).isPresent();

            var foundFields = fullMessage.get().substring(message.length());
            assertThat(isBlank(foundFields)).isFalse();
            assertThat(foundFields.split(",")).containsAll(firstClassFields);
        } else {
            assertKeycloakStatusDoesNotContainMessage(status, message);
        }
    }

    private Optional<String> getFullMessageFromStatus(KeycloakStatus status, String containedMessage) {
        if (isBlank(containedMessage)) return Optional.empty();

        return status.getConditions()
                .stream()
                .filter(f -> f.getMessage().contains(containedMessage))
                .findAny()
                .map(KeycloakStatusCondition::getMessage);
    }

    private boolean containsEnvironmentVariable(List<EnvVar> envVars, String varName) {
        if (CollectionUtil.isEmpty(envVars) || isBlank(varName)) return false;
        return envVars.stream().anyMatch(f -> varName.equals(f.getName()));
    }
}

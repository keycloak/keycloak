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

import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.EnvVar;
import io.fabric8.kubernetes.api.model.apps.StatefulSet;
import io.fabric8.kubernetes.api.model.apps.StatefulSetBuilder;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;
import org.keycloak.common.util.CollectionUtil;
import org.keycloak.common.util.ObjectUtil;
import org.keycloak.operator.Constants;
import org.keycloak.operator.controllers.KeycloakDistConfigurator;
import org.keycloak.operator.crds.v2alpha1.deployment.Keycloak;
import org.keycloak.operator.crds.v2alpha1.deployment.KeycloakStatus;
import org.keycloak.operator.crds.v2alpha1.deployment.KeycloakStatusBuilder;
import org.keycloak.operator.crds.v2alpha1.deployment.KeycloakStatusCondition;
import org.keycloak.operator.crds.v2alpha1.deployment.ValueOrSecret;
import org.keycloak.operator.testsuite.utils.K8sUtils;

import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.keycloak.operator.testsuite.utils.CRAssert.assertKeycloakStatusCondition;
import static org.keycloak.operator.testsuite.utils.CRAssert.assertKeycloakStatusDoesNotContainMessage;

@QuarkusTest
public class KeycloakDistConfiguratorTest {

    @Test
    public void enabledFeatures() {
        testFirstClassCitizen("KC_FEATURES", "features",
                KeycloakDistConfigurator::configureFeatures, "docker", "authorization");
    }

    @Test
    public void disabledFeatures() {
        testFirstClassCitizen("KC_FEATURES_DISABLED", "features-disabled",
                KeycloakDistConfigurator::configureFeatures, "admin", "step-up-authentication");
    }

    @Test
    public void transactions() {
        testFirstClassCitizen("KC_TRANSACTION_XA_ENABLED", "transaction-xa-enabled",
                KeycloakDistConfigurator::configureTransactions, "false");
    }

    @Test
    public void httpEnabled() {
        testFirstClassCitizen("KC_HTTP_ENABLED", "http-enabled",
                KeycloakDistConfigurator::configureHttp, "true");
    }

    @Test
    public void httpPort() {
        testFirstClassCitizen("KC_HTTP_PORT", "http-port",
                KeycloakDistConfigurator::configureHttp, "123");
    }

    @Test
    public void httpsPort() {
        testFirstClassCitizen("KC_HTTPS_PORT", "https-port",
                KeycloakDistConfigurator::configureHttp, "456");
    }

    @Test
    public void tlsSecret() {
        testFirstClassCitizen("KC_HTTPS_CERTIFICATE_FILE", "https-certificate-file",
                KeycloakDistConfigurator::configureHttp, Constants.CERTIFICATES_FOLDER + "/tls.crt");
        testFirstClassCitizen("KC_HTTPS_CERTIFICATE_KEY_FILE", "https-certificate-key-file",
                KeycloakDistConfigurator::configureHttp, Constants.CERTIFICATES_FOLDER + "/tls.key");
    }

    @Test
    public void testEmptyLists() {
        final Keycloak keycloak = K8sUtils.getResourceFromFile("test-serialization-keycloak-cr-with-empty-list.yml", Keycloak.class);
        final StatefulSet deployment = getBasicKcDeployment();
        final KeycloakDistConfigurator distConfig = new KeycloakDistConfigurator(keycloak, deployment, null);

        final List<EnvVar> envVars = deployment.getSpec().getTemplate().getSpec().getContainers().get(0).getEnv();
        distConfig.configureFeatures();
        assertEnvVarNotPresent(envVars, "KC_FEATURES");
        assertEnvVarNotPresent(envVars, "KC_FEATURES_DISABLED");
    }

    @Test
    public void testDatabaseSettings() {
        testFirstClassCitizen("KC_DB", "db",
                KeycloakDistConfigurator::configureDatabase, "vendor");
        testFirstClassCitizen("KC_DB_USERNAME", "db-username",
                KeycloakDistConfigurator::configureDatabase, "usernameSecret");
        testFirstClassCitizen("KC_DB_PASSWORD", "db-password",
                KeycloakDistConfigurator::configureDatabase, "passwordSecret");
        testFirstClassCitizen("KC_DB_SCHEMA", "db-schema",
                KeycloakDistConfigurator::configureDatabase, "schema");
        testFirstClassCitizen("KC_DB_URL_HOST", "db-url-host",
                KeycloakDistConfigurator::configureDatabase, "host");
        testFirstClassCitizen("KC_DB_URL_PORT", "db-url-port",
                KeycloakDistConfigurator::configureDatabase, "123");
        testFirstClassCitizen("KC_DB_POOL_INITIAL_SIZE", "db-pool-initial-size",
                KeycloakDistConfigurator::configureDatabase, "1");
        testFirstClassCitizen("KC_DB_POOL_MIN_SIZE", "db-pool-min-size",
                KeycloakDistConfigurator::configureDatabase, "2");
        testFirstClassCitizen("KC_DB_POOL_MAX_SIZE", "db-pool-max-size",
                KeycloakDistConfigurator::configureDatabase, "3");
    }

    /* UTILS */
    private void testFirstClassCitizen(String envVarName, String optionName, Consumer<KeycloakDistConfigurator> config, String... expectedValues) {
        testFirstClassCitizen("/test-serialization-keycloak-cr.yml", envVarName, optionName, config, expectedValues);
    }

    private void testFirstClassCitizen(String crName, String envVarName, String optionName, Consumer<KeycloakDistConfigurator> config, String... expectedValues) {
        final Keycloak keycloak = K8sUtils.getResourceFromFile(crName, Keycloak.class);
        final StatefulSet deployment = getBasicKcDeployment();
        final KeycloakDistConfigurator distConfig = new KeycloakDistConfigurator(keycloak, deployment, null);

        final Container container = deployment.getSpec().getTemplate().getSpec().getContainers().get(0);
        assertThat(container).isNotNull();

        assertEnvVarNotPresent(container.getEnv(), envVarName);
        assertWarningStatus(distConfig, optionName, false);

        config.accept(distConfig);

        assertContainerEnvVar(container.getEnv(), envVarName, expectedValues);

        keycloak.getSpec().setServerConfiguration(List.of(new ValueOrSecret(optionName, "foo")));
        assertWarningStatus(distConfig, optionName, true);
    }

    /**
     * assertContainerEnvVar(container.getEnv(), "KC_FEATURES", "admin,authorization");
     * assertContainerEnvVar(container.getEnv(), "KC_HOSTNAME", "someHostname");
     */
    private void assertContainerEnvVar(List<EnvVar> envVars, String varName, String... expectedValue) {
        assertThat(envVars).isNotNull();
        assertEnvVarPresent(envVars, varName);

        final List<String> foundValues = getValuesFromEnvVar(envVars, varName);
        assertThat(CollectionUtil.isNotEmpty(foundValues)).isTrue();
        for (String val : expectedValue) {
            assertThat(foundValues.contains(val)).isTrue();
        }
    }

    private void assertEnvVarPresent(List<EnvVar> envVars, String varName) {
        assertThat(containsEnvironmentVariable(envVars, varName)).isTrue();

    }

    private void assertEnvVarNotPresent(List<EnvVar> envVars, String varName) {
        assertThat(containsEnvironmentVariable(envVars, varName)).isFalse();
    }

    private void assertWarningStatus(KeycloakDistConfigurator distConfig, String optionName, boolean expectWarning) {
        final String message = "warning: You need to specify these fields as the first-class citizen of the CR: " + optionName;
        final KeycloakStatusBuilder statusBuilder = new KeycloakStatusBuilder();
        distConfig.validateOptions(statusBuilder);
        final KeycloakStatus status = statusBuilder.build();

        if (expectWarning) {
            assertKeycloakStatusCondition(status, KeycloakStatusCondition.HAS_ERRORS, false, message);
        } else {
            assertKeycloakStatusDoesNotContainMessage(status, message);
        }
    }

    private StatefulSet getBasicKcDeployment() {
        return new StatefulSetBuilder()
                .withNewSpec()
                .withNewTemplate()
                .withNewSpec()
                .addNewContainer()
                .withName("keycloak")
                .withArgs("start")
                .endContainer()
                .endSpec()
                .endTemplate()
                .endSpec()
                .build();
    }

    private boolean containsEnvironmentVariable(List<EnvVar> envVars, String varName) {
        if (CollectionUtil.isEmpty(envVars) || ObjectUtil.isBlank(varName)) return false;
        return envVars.stream().anyMatch(f -> varName.equals(f.getName()));
    }

    /**
     * Returns values of environment variable separated by comma (f.e KC_FEATURES=admin2,ciba)
     */
    private List<String> getValuesFromEnvVar(List<EnvVar> envVars, String varName) {
        if (CollectionUtil.isEmpty(envVars) || ObjectUtil.isBlank(varName)) return Collections.emptyList();

        return envVars.stream().filter(f -> varName.equals(f.getName()))
                .findFirst()
                .map(new Function<EnvVar, String>() {
                    @Override
                    public String apply(EnvVar envVar) {
                        if (envVar.getValue() != null) {
                            return envVar.getValue();
                        }

                        if (envVar.getValueFrom() != null && envVar.getValueFrom().getSecretKeyRef() != null) {
                            return envVar.getValueFrom().getSecretKeyRef().getName();
                        }

                        return null;
                    }
                })
                .map(f -> f.split(","))
                .map(List::of)
                .orElseGet(Collections::emptyList);
    }
}

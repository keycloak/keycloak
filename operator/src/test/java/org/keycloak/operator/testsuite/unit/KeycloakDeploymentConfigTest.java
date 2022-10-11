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
import org.keycloak.operator.controllers.KeycloakDeploymentConfig;
import org.keycloak.operator.crds.v2alpha1.deployment.Keycloak;
import org.keycloak.operator.testsuite.utils.K8sUtils;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@QuarkusTest
public class KeycloakDeploymentConfigTest {

    @Test
    public void enabledFeatures() {
        testFeatures(true, "docker", "authorization");
    }

    @Test
    public void disabledFeatures() {
        testFeatures(false, "admin", "step-up-authentication");
    }

    private void testFeatures(boolean enabledFeatures, String... features) {
        final String featureEnvVar = enabledFeatures ? "KC_FEATURES" : "KC_FEATURES_DISABLED";

        final Keycloak keycloak = K8sUtils.getResourceFromFile("/test-serialization-keycloak-cr.yml", Keycloak.class);
        final StatefulSet deployment = getBasicKcDeployment();
        final KeycloakDeploymentConfig deploymentConfig = new KeycloakDeploymentConfig(keycloak, deployment, null);

        final Container container = deployment.getSpec().getTemplate().getSpec().getContainers().get(0);
        assertThat(container).isNotNull();

        assertEnvVarNotPresent(container.getEnv(), featureEnvVar);

        deploymentConfig.configureFeatures();

        assertContainerEnvVar(container.getEnv(), featureEnvVar, features);
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
                .map(EnvVar::getValue)
                .map(f -> f.split(","))
                .map(List::of)
                .orElseGet(Collections::emptyList);
    }
}

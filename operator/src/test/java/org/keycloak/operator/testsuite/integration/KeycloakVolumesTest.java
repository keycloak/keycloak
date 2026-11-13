/*
 * Copyright 2026 Red Hat, Inc. and/or its affiliates
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

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

import org.keycloak.operator.crds.v2beta1.deployment.Keycloak;
import org.keycloak.operator.testsuite.apiserver.DisabledIfApiServerTest;
import org.keycloak.operator.testsuite.unit.WatchedResourcesTest;
import org.keycloak.operator.testsuite.utils.CRAssert;

import io.fabric8.kubernetes.api.model.ConfigMapBuilder;
import io.fabric8.kubernetes.api.model.VolumeBuilder;
import io.fabric8.kubernetes.api.model.VolumeMountBuilder;
import io.fabric8.kubernetes.api.model.apps.StatefulSet;
import io.fabric8.kubernetes.client.dsl.ExecWatch;
import io.quarkus.test.junit.QuarkusTest;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;

import static org.keycloak.operator.crds.v2beta1.deployment.KeycloakStatusCondition.HAS_ERRORS;
import static org.keycloak.operator.testsuite.utils.K8sUtils.deployKeycloak;

import static org.assertj.core.api.Assertions.assertThat;

@QuarkusTest
public class KeycloakVolumesTest extends BaseOperatorTest {

    private static final String THEME_DIR = "/opt/keycloak/themes/custom-theme";

    private static void addThemeVolume(Keycloak kc) {
        kc.getSpec().getVolumes().add(new VolumeBuilder()
                .withName("custom-theme")
                .withNewConfigMap()
                    .withName("custom-theme")
                    .addNewItem().withKey("login-theme.properties").withPath("login/theme.properties").endItem()
                .endConfigMap()
                .build());
        kc.getSpec().getVolumeMounts().add(new VolumeMountBuilder()
                .withName("custom-theme")
                .withMountPath(THEME_DIR)
                .withReadOnly()
                .build());
    }

    @DisabledIfApiServerTest
    @Test
    public void testConfigMapVolumeIsMounted() throws Exception {
        k8sclient.configMaps().resource(new ConfigMapBuilder()
                .withNewMetadata().withName("custom-theme").endMetadata()
                .addToData("login-theme.properties", "parent=keycloak.v2\n")
                .build()).create();

        var kc = getTestKeycloakDeployment(false);
        addThemeVolume(kc);
        deployKeycloak(k8sclient, kc, true);

        var output = new ByteArrayOutputStream();
        try (ExecWatch watch = k8sclient.pods().withName(kc.getMetadata().getName() + "-0")
                .inContainer("keycloak")
                .writingOutput(output)
                .exec("cat", THEME_DIR + "/login/theme.properties")) {
            assertThat(watch.exitCode().get(30, TimeUnit.SECONDS)).isZero();
        }
        assertThat(output.toString(StandardCharsets.UTF_8)).contains("parent=keycloak.v2");
    }

    @Test
    public void testMissingConfigMapIsReported() {
        var kc = getTestKeycloakDeployment(true);
        addThemeVolume(kc);
        deployKeycloak(k8sclient, kc, false);

        var stsResource = k8sclient.resources(StatefulSet.class).withName(kc.getMetadata().getName());
        Awaitility.await().ignoreExceptions().untilAsserted(() -> {
            var statefulSet = stsResource.get();
            assertThat(statefulSet.getMetadata().getAnnotations())
                    .containsEntry(WatchedResourcesTest.KEYCLOAK_WATCHING_CONFIGMAPS_ANNOTATION, "true")
                    .containsEntry(WatchedResourcesTest.KEYCLOAK_MISSING_CONFIGMAPS_ANNOTATION, "custom-theme");
            assertThat(statefulSet.getSpec().getTemplate().getSpec().getContainers().get(0).getVolumeMounts())
                    .anyMatch(vm -> vm.getMountPath().equals(THEME_DIR));
        });
    }

    @Test
    public void testConflictingVolumeNameIsReported() {
        var kc = getTestKeycloakDeployment(true);
        kc.getSpec().getVolumes().add(new VolumeBuilder()
                .withName("keycloak-tls-certificates")
                .withNewEmptyDir().endEmptyDir()
                .build());
        deployKeycloak(k8sclient, kc, false);

        Awaitility.await().ignoreExceptions().untilAsserted(() -> CRAssert.assertKeycloakStatusCondition(
                k8sclient.resources(Keycloak.class).withName(kc.getMetadata().getName()).get(), HAS_ERRORS, true, "Volume keycloak-tls-certificates is already defined"));
    }
}

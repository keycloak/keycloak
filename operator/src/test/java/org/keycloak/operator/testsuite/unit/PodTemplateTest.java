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
import io.fabric8.kubernetes.api.model.IntOrString;
import io.fabric8.kubernetes.api.model.PodTemplateSpec;
import io.fabric8.kubernetes.api.model.PodTemplateSpecBuilder;
import io.fabric8.kubernetes.api.model.ProbeBuilder;
import io.fabric8.kubernetes.api.model.apps.StatefulSet;
import io.fabric8.kubernetes.api.model.apps.StatefulSetBuilder;
import io.quarkus.test.junit.QuarkusTest;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.keycloak.operator.Config;
import org.keycloak.operator.controllers.KeycloakDeployment;
import org.keycloak.operator.controllers.OperatorManagedResource;
import org.keycloak.operator.crds.v2alpha1.deployment.KeycloakBuilder;
import org.keycloak.operator.crds.v2alpha1.deployment.KeycloakSpecBuilder;
import org.keycloak.operator.crds.v2alpha1.deployment.ValueOrSecret;
import org.keycloak.operator.crds.v2alpha1.deployment.spec.HostnameSpecBuilder;
import org.keycloak.operator.crds.v2alpha1.deployment.spec.HttpSpecBuilder;
import org.keycloak.operator.crds.v2alpha1.deployment.spec.UnsupportedSpec;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
public class PodTemplateTest {
    private StatefulSet getDeployment(PodTemplateSpec podTemplate, StatefulSet existingDeployment, Consumer<KeycloakSpecBuilder> additionalSpec) {
        var config = new Config() {
            @Override
            public Keycloak keycloak() {
                return new Keycloak() {
                    @Override
                    public String image() {
                        return "dummy-image";
                    }

                    @Override
                    public String imagePullPolicy() {
                        return "Never";
                    }
                    @Override
                    public Map<String, String> podLabels() {
                        return Collections.emptyMap();
                    }
                };
            }
        };
        var kc = new KeycloakBuilder().withNewMetadata().withName("instance").endMetadata().build();
        existingDeployment = new StatefulSetBuilder(existingDeployment).editOrNewSpec().editOrNewSelector()
                .addToMatchLabels(OperatorManagedResource.updateWithInstanceLabels(null, kc.getMetadata().getName()))
                .endSelector().endSpec().build();

        var httpSpec = new HttpSpecBuilder().withTlsSecret("example-tls-secret").build();
        var hostnameSpec = new HostnameSpecBuilder().withHostname("example.com").build();

        var keycloakSpecBuilder = new KeycloakSpecBuilder()
                .withUnsupported(new UnsupportedSpec(podTemplate))
                .withHttpSpec(httpSpec)
                .withHostnameSpec(hostnameSpec);

        if (additionalSpec != null) {
            additionalSpec.accept(keycloakSpecBuilder);
        }

        kc.setSpec(keycloakSpecBuilder.build());

        var deployment = new KeycloakDeployment(null, config, kc, existingDeployment, "dummy-admin");

        return deployment.getReconciledResource().get();
    }

    private StatefulSet getDeployment(PodTemplateSpec podTemplate, StatefulSet existingDeployment) {
        return getDeployment(podTemplate, existingDeployment, null);
    }

    private StatefulSet getDeployment(PodTemplateSpec podTemplate) {
        return getDeployment(podTemplate, new StatefulSet());
    }

    @Test
    public void testEmpty() {
        // Arrange
        PodTemplateSpec additionalPodTemplate = null;

        // Act
        var podTemplate = getDeployment(additionalPodTemplate).getSpec().getTemplate();

        // Assert
        assertEquals("keycloak", podTemplate.getSpec().getContainers().get(0).getName());
    }

    @Test
    public void testMetadataIsMerged() {
        // Arrange
        var additionalPodTemplate = new PodTemplateSpecBuilder()
                .withNewMetadata()
                .addToLabels("one", "1")
                .addToAnnotations("two", "2")
                .endMetadata()
                .build();

        // Act
        var podTemplate = getDeployment(additionalPodTemplate).getSpec().getTemplate();

        // Assert
        assertTrue(podTemplate.getMetadata().getLabels().containsKey("one"));
        assertTrue(podTemplate.getMetadata().getLabels().containsValue("1"));
        assertTrue(podTemplate.getMetadata().getAnnotations().containsKey("two"));
        assertTrue(podTemplate.getMetadata().getAnnotations().containsValue("2"));
    }

    @Test
    public void testVolumesAreMerged() {
        // Arrange
        var volumeName = "foo-volume";
        var additionalPodTemplate = new PodTemplateSpecBuilder()
                .withNewSpec()
                .addNewVolume()
                .withName("foo-volume")
                .withNewEmptyDir()
                .endEmptyDir()
                .endVolume()
                .endSpec()
                .build();

        // Act
        var podTemplate = getDeployment(additionalPodTemplate).getSpec().getTemplate();

        // Assert
        assertEquals(volumeName, podTemplate.getSpec().getVolumes().get(1).getName());
    }

    @Test
    public void testVolumeMountsAreMerged() {
        // Arrange
        var volumeMountName = "foo";
        var volumeMountPath = "/mnt/path";
        var additionalPodTemplate = new PodTemplateSpecBuilder()
                .withNewSpec()
                .addNewContainer()
                .addNewVolumeMount()
                .withName(volumeMountName)
                .withMountPath(volumeMountPath)
                .endVolumeMount()
                .endContainer()
                .endSpec()
                .build();

        // Act
        var podTemplate = getDeployment(additionalPodTemplate).getSpec().getTemplate();

        // Assert
        assertEquals(volumeMountName, podTemplate.getSpec().getContainers().get(0).getVolumeMounts().get(1).getName());
        assertEquals(volumeMountPath, podTemplate.getSpec().getContainers().get(0).getVolumeMounts().get(1).getMountPath());
    }

    @Test
    public void testCommandsAndArgsAreMerged() {
        // Arrange
        var command = "foo";
        var arg = "bar";
        var additionalPodTemplate = new PodTemplateSpecBuilder()
                .withNewSpec()
                .addNewContainer()
                .withCommand(command)
                .withArgs(arg)
                .endContainer()
                .endSpec()
                .build();

        // Act
        var podTemplate = getDeployment(additionalPodTemplate).getSpec().getTemplate();

        // Assert
        assertEquals(1, podTemplate.getSpec().getContainers().get(0).getCommand().size());
        assertEquals(command, podTemplate.getSpec().getContainers().get(0).getCommand().get(0));
        assertEquals(1, podTemplate.getSpec().getContainers().get(0).getArgs().size());
        assertEquals(arg, podTemplate.getSpec().getContainers().get(0).getArgs().get(0));
    }

    @Test
    public void testProbesAreMerged() {
        // Arrange
        var ready = new ProbeBuilder()
                .withNewExec()
                .withCommand("foo")
                .endExec()
                .withFailureThreshold(1)
                .withInitialDelaySeconds(2)
                .withTimeoutSeconds(3)
                .build();
        var live = new ProbeBuilder()
                .withNewHttpGet()
                .withPort(new IntOrString(1000))
                .withScheme("UDP")
                .withPath("/foo")
                .endHttpGet()
                .withFailureThreshold(4)
                .withInitialDelaySeconds(5)
                .withTimeoutSeconds(6)
                .build();
        var additionalPodTemplate = new PodTemplateSpecBuilder()
                .withNewSpec()
                .addNewContainer()
                .withReadinessProbe(ready)
                .withLivenessProbe(live)
                .endContainer()
                .endSpec()
                .build();

        // Act
        var podTemplate = getDeployment(additionalPodTemplate).getSpec().getTemplate();

        // Assert
        var readyProbe = podTemplate.getSpec().getContainers().get(0).getReadinessProbe();
        var liveProbe = podTemplate.getSpec().getContainers().get(0).getLivenessProbe();
        assertEquals("foo", ready.getExec().getCommand().get(0));
        assertEquals(1, readyProbe.getFailureThreshold());
        assertEquals(2, readyProbe.getInitialDelaySeconds());
        assertEquals(3, readyProbe.getTimeoutSeconds());
        assertEquals(1000, liveProbe.getHttpGet().getPort().getIntVal());
        assertEquals("UDP", liveProbe.getHttpGet().getScheme());
        assertEquals("/foo", liveProbe.getHttpGet().getPath());
        assertEquals(4, liveProbe.getFailureThreshold());
        assertEquals(5, liveProbe.getInitialDelaySeconds());
        assertEquals(6, liveProbe.getTimeoutSeconds());
    }

    @Test
    public void testEnvVarsAreMerged() {
        // Arrange
        var env = "KC_SOMETHING";
        var value = "some-value";
        var additionalPodTemplate = new PodTemplateSpecBuilder()
                .withNewSpec()
                .addNewContainer()
                .addNewEnv()
                .withName(env)
                .withValue(value)
                .endEnv()
                .endContainer()
                .endSpec()
                .build();

        // Act
        var podTemplate = getDeployment(additionalPodTemplate).getSpec().getTemplate();

        // Assert
        var envVar = podTemplate.getSpec().getContainers().get(0).getEnv().stream().filter(e -> e.getName().equals(env)).findFirst().get();
        assertEquals(env, envVar.getName());
        assertEquals(value, envVar.getValue());
    }

    @Test
    public void testEnvVarConflict() {
        // Arrange
        var additionalPodTemplate = new PodTemplateSpecBuilder()
                .withNewSpec()
                .addNewContainer()
                .addNewEnv()
                .withName("KC_CACHE_STACK")
                .withValue("template_stack")
                .endEnv()
                .addNewEnv()
                .withName("KC_DB_URL_HOST")
                .withValue("template_host")
                .endEnv()
                .endContainer()
                .endSpec()
                .build();

        // Act
        var podTemplate = getDeployment(additionalPodTemplate, null,
                s -> s.addNewAdditionalOption("cache.stack", "additional_stack")
                        .addNewAdditionalOption("http.port", "additional_port").withNewDatabaseSpec().withHost("spec-host")
                        .endDatabaseSpec())
                .getSpec().getTemplate();

        // Assert
        var envVar = podTemplate.getSpec().getContainers().get(0).getEnv();
        var envVarMap = envVar.stream().collect(Collectors.toMap(EnvVar::getName, Function.identity(), (e1, e2) -> {
            Assertions.fail("duplicate env" + e1.getName());
            return e1;
        }));
        // template spec takes the most priority for envs - only fields called out in the KeycloakDeployment warning are overriden by the rest of the spec
        assertThat(envVarMap.get("KC_CACHE_STACK").getValue()).isEqualTo("template_stack");
        assertThat(envVarMap.get("KC_DB_URL_HOST").getValue()).isEqualTo("template_host");
        // the main spec takes priority over the additional options
        assertThat(envVarMap.get("KC_HTTP_PORT").getValue()).isEqualTo("8080");
    }

    @Test
    public void testAnnotationsAreNotMerged() {
        // Arrange
        var existingDeployment = new StatefulSetBuilder()
                .withNewSpec()
                .withNewTemplate()
                .withNewMetadata()
                .addToAnnotations("one", "1")
                .addToAnnotations("two", "3")
                .endMetadata()
                .endTemplate()
                .endSpec()
                .build();
        var additionalPodTemplate = new PodTemplateSpecBuilder()
                .withNewMetadata()
                .addToAnnotations("two", "2")
                .endMetadata()
                .build();

        // Act
        var podTemplate = getDeployment(additionalPodTemplate, existingDeployment).getSpec().getTemplate();

        // Assert
        assertThat(podTemplate.getMetadata().getAnnotations()).containsEntry("two", "2");
    }

    @Test
    public void testRelativePathHealthProbes() {
        final Function<String, Container> setUpRelativePath = (path) -> getDeployment(null, new StatefulSet(),
                spec -> spec.withAdditionalOptions(new ValueOrSecret("http-relative-path", path)))
                .getSpec()
                .getTemplate()
                .getSpec()
                .getContainers()
                .get(0);

        var first = setUpRelativePath.apply("/");
        assertEquals("/health/ready", first.getReadinessProbe().getHttpGet().getPath());
        assertEquals("/health/live", first.getLivenessProbe().getHttpGet().getPath());

        var second = setUpRelativePath.apply("some");
        assertEquals("some/health/ready", second.getReadinessProbe().getHttpGet().getPath());
        assertEquals("some/health/live", second.getLivenessProbe().getHttpGet().getPath());

        var third = setUpRelativePath.apply("");
        assertEquals("/health/ready", third.getReadinessProbe().getHttpGet().getPath());
        assertEquals("/health/live", third.getLivenessProbe().getHttpGet().getPath());

        var fourth = setUpRelativePath.apply("/some/");
        assertEquals("/some/health/ready", fourth.getReadinessProbe().getHttpGet().getPath());
        assertEquals("/some/health/live", fourth.getLivenessProbe().getHttpGet().getPath());
    }

    @Test
    public void testDefaultArgs() {
        // Arrange
        PodTemplateSpec additionalPodTemplate = null;

        // Act
        var podTemplate = getDeployment(additionalPodTemplate).getSpec().getTemplate();

        // Assert
        assertThat(podTemplate.getSpec().getContainers().get(0).getArgs()).doesNotContain("--optimized");
    }
}

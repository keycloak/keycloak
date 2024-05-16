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
import io.fabric8.kubernetes.api.model.Volume;
import io.fabric8.kubernetes.api.model.VolumeMount;
import io.fabric8.kubernetes.api.model.apps.StatefulSet;
import io.fabric8.kubernetes.api.model.apps.StatefulSetBuilder;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.keycloak.operator.Constants;
import org.keycloak.operator.Utils;
import org.keycloak.operator.controllers.KeycloakDeploymentDependentResource;
import org.keycloak.operator.controllers.WatchedResources;
import org.keycloak.operator.crds.v2alpha1.deployment.Keycloak;
import org.keycloak.operator.crds.v2alpha1.deployment.KeycloakBuilder;
import org.keycloak.operator.crds.v2alpha1.deployment.KeycloakSpecBuilder;
import org.keycloak.operator.crds.v2alpha1.deployment.ValueOrSecret;
import org.keycloak.operator.crds.v2alpha1.deployment.spec.HostnameSpecBuilder;
import org.keycloak.operator.crds.v2alpha1.deployment.spec.HttpSpecBuilder;
import org.keycloak.operator.crds.v2alpha1.deployment.spec.UnsupportedSpec;
import org.mockito.Mockito;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
public class PodTemplateTest {

    @InjectMock
    WatchedResources watchedResources;

    @Inject
    KeycloakDeploymentDependentResource deployment;

    private StatefulSet getDeployment(PodTemplateSpec podTemplate, StatefulSet existingDeployment, Consumer<KeycloakSpecBuilder> additionalSpec) {
        var kc = new KeycloakBuilder().withNewMetadata().withName("instance").endMetadata().build();
        existingDeployment = new StatefulSetBuilder(existingDeployment).editOrNewSpec().editOrNewSelector()
                .withMatchLabels(Utils.allInstanceLabels(kc))
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

        Context<Keycloak> context = Mockito.mock(Context.class);
        Mockito.when(context.getSecondaryResource(StatefulSet.class)).thenReturn(Optional.ofNullable(existingDeployment));

        return deployment.desired(kc, context);
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
        assertEquals(2, podTemplate.getSpec().getContainers().get(0).getArgs().size());
        assertEquals(arg, podTemplate.getSpec().getContainers().get(0).getArgs().get(1));
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
                spec -> spec.withAdditionalOptions(new ValueOrSecret("http-management-relative-path", path)))
                .getSpec()
                .getTemplate()
                .getSpec()
                .getContainers()
                .get(0);

        var first = setUpRelativePath.apply("/");
        assertEquals("/health/ready", first.getReadinessProbe().getHttpGet().getPath());
        assertEquals("/health/live", first.getLivenessProbe().getHttpGet().getPath());
        assertEquals("/health/started", first.getStartupProbe().getHttpGet().getPath());

        var second = setUpRelativePath.apply("some");
        assertEquals("some/health/ready", second.getReadinessProbe().getHttpGet().getPath());
        assertEquals("some/health/live", second.getLivenessProbe().getHttpGet().getPath());
        assertEquals("some/health/started", second.getStartupProbe().getHttpGet().getPath());

        var third = setUpRelativePath.apply("");
        assertEquals("/health/ready", third.getReadinessProbe().getHttpGet().getPath());
        assertEquals("/health/live", third.getLivenessProbe().getHttpGet().getPath());
        assertEquals("/health/started", third.getStartupProbe().getHttpGet().getPath());

        var fourth = setUpRelativePath.apply("/some/");
        assertEquals("/some/health/ready", fourth.getReadinessProbe().getHttpGet().getPath());
        assertEquals("/some/health/live", fourth.getLivenessProbe().getHttpGet().getPath());
        assertEquals("/some/health/started", fourth.getStartupProbe().getHttpGet().getPath());
    }

    @Test
    public void testDefaults() {
        // Arrange
        PodTemplateSpec additionalPodTemplate = null;

        // Act
        var podTemplate = getDeployment(additionalPodTemplate).getSpec().getTemplate();
        var container = podTemplate.getSpec().getContainers().get(0);

        // Assert
        assertNotNull(container);
        assertThat(container.getArgs()).doesNotContain(KeycloakDeploymentDependentResource.OPTIMIZED_ARG);
        assertThat(container.getEnv().stream()).anyMatch(envVar -> envVar.getName().equals(KeycloakDeploymentDependentResource.KC_TRUSTSTORE_PATHS));

        var readiness = container.getReadinessProbe().getHttpGet();
        assertNotNull(readiness);
        assertThat(readiness.getPath()).isEqualTo("/health/ready");
        assertThat(readiness.getPort().getIntVal()).isEqualTo(Constants.KEYCLOAK_MANAGEMENT_PORT);

        var liveness = container.getLivenessProbe().getHttpGet();
        assertNotNull(liveness);
        assertThat(liveness.getPath()).isEqualTo("/health/live");
        assertThat(liveness.getPort().getIntVal()).isEqualTo(Constants.KEYCLOAK_MANAGEMENT_PORT);

        var startup = container.getStartupProbe().getHttpGet();
        assertNotNull(startup);
        assertThat(startup.getPath()).isEqualTo("/health/started");
        assertThat(startup.getPort().getIntVal()).isEqualTo(Constants.KEYCLOAK_MANAGEMENT_PORT);
    }

    @Test
    public void testImageNotOptimized() {
        // Arrange
        PodTemplateSpec additionalPodTemplate = null;

        // Act
        var podTemplate = getDeployment(additionalPodTemplate, null,
                s -> s.withImage("some-image").withStartOptimized(false))
                .getSpec().getTemplate();

        // Assert
        assertThat(podTemplate.getSpec().getContainers().get(0).getArgs()).doesNotContain(KeycloakDeploymentDependentResource.OPTIMIZED_ARG);
    }

    @Test
    public void testAdditionalOptionTruststorePath() {
        // Arrange
        PodTemplateSpec additionalPodTemplate = null;

        // Act
        var podTemplate = getDeployment(additionalPodTemplate, null,
                s -> s.addToAdditionalOptions(new ValueOrSecret("truststore-paths", "/something")))
                .getSpec().getTemplate();

        // Assert
        assertThat(podTemplate.getSpec().getContainers().get(0).getEnv().stream())
                .anyMatch(envVar -> envVar.getName().equals(KeycloakDeploymentDependentResource.KC_TRUSTSTORE_PATHS)
                        && envVar.getValue().equals("/something"));
    }

    @Test
    public void testImageForceOptimized() {
        // Arrange
        PodTemplateSpec additionalPodTemplate = null;

        // Act
        var podTemplate = getDeployment(additionalPodTemplate, null,
                s -> s.withStartOptimized(true))
                .getSpec().getTemplate();

        // Assert
        assertThat(podTemplate.getSpec().getContainers().get(0).getArgs()).contains(KeycloakDeploymentDependentResource.OPTIMIZED_ARG);
    }

    @Test
    public void testCacheConfigFileMount() {
        // Arrange
        PodTemplateSpec additionalPodTemplate = null;

        // Act
        var podTemplate = getDeployment(additionalPodTemplate, null,
                s -> s.withNewCacheSpec().withNewConfigMapFile("file.xml", "cm", null).endCacheSpec())
                .getSpec().getTemplate();

        // Assert
        VolumeMount volumeMount = podTemplate.getSpec().getContainers().get(0).getVolumeMounts().stream()
                .filter(vm -> vm.getName().equals(KeycloakDeploymentDependentResource.CACHE_CONFIG_FILE_MOUNT_NAME))
                .findFirst().orElseThrow();
        assertThat(volumeMount.getMountPath()).isEqualTo(Constants.CACHE_CONFIG_FOLDER);

        Volume volume = podTemplate.getSpec().getVolumes().stream()
                .filter(v -> v.getName().equals(KeycloakDeploymentDependentResource.CACHE_CONFIG_FILE_MOUNT_NAME))
                .findFirst().orElseThrow();
        assertThat(volume.getConfigMap().getName()).isEqualTo("cm");
    }

    @Test
    public void testServiceCaCrt() {
        this.deployment.setUseServiceCaCrt(true);
        try {
            // Arrange
            PodTemplateSpec additionalPodTemplate = null;

            // Act
            var podTemplate = getDeployment(additionalPodTemplate, null, null).getSpec().getTemplate();

            // Assert
            var paths = podTemplate.getSpec().getContainers().get(0).getEnv().stream().filter(envVar -> envVar.getName().equals(KeycloakDeploymentDependentResource.KC_TRUSTSTORE_PATHS)).findFirst().orElseThrow();
            assertThat(paths.getValue()).isEqualTo("/var/run/secrets/kubernetes.io/serviceaccount/ca.crt,/var/run/secrets/kubernetes.io/serviceaccount/service-ca.crt");
        } finally {
            this.deployment.setUseServiceCaCrt(false);
        }
    }

}

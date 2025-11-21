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

import io.fabric8.kubernetes.api.model.Affinity;
import io.fabric8.kubernetes.api.model.AffinityBuilder;
import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.EnvVar;
import io.fabric8.kubernetes.api.model.IntOrString;
import io.fabric8.kubernetes.api.model.LocalObjectReference;
import io.fabric8.kubernetes.api.model.PodTemplateSpec;
import io.fabric8.kubernetes.api.model.PodTemplateSpecBuilder;
import io.fabric8.kubernetes.api.model.ProbeBuilder;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.SecretKeySelector;
import io.fabric8.kubernetes.api.model.Toleration;
import io.fabric8.kubernetes.api.model.TopologySpreadConstraint;
import io.fabric8.kubernetes.api.model.TopologySpreadConstraintBuilder;
import io.fabric8.kubernetes.api.model.Volume;
import io.fabric8.kubernetes.api.model.VolumeMount;
import io.fabric8.kubernetes.api.model.apps.StatefulSet;
import io.fabric8.kubernetes.api.model.apps.StatefulSetBuilder;
import io.fabric8.kubernetes.api.model.apps.StatefulSetSpec;
import io.fabric8.kubernetes.api.model.batch.v1.Job;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.utils.Serialization;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.api.reconciler.dependent.managed.ManagedWorkflowAndDependentResourceContext;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.keycloak.operator.Config;
import org.keycloak.operator.Constants;
import org.keycloak.operator.ContextUtils;
import org.keycloak.operator.Utils;
import org.keycloak.operator.controllers.KeycloakDeploymentDependentResource;
import org.keycloak.operator.controllers.KeycloakDistConfigurator;
import org.keycloak.operator.controllers.KeycloakRealmImportJobDependentResource;
import org.keycloak.operator.controllers.KeycloakUpdateJobDependentResource;
import org.keycloak.operator.controllers.WatchedResources;
import org.keycloak.operator.controllers.WatchedResources.Watched;
import org.keycloak.operator.crds.v2alpha1.CRDUtils;
import org.keycloak.operator.crds.v2alpha1.deployment.Keycloak;
import org.keycloak.operator.crds.v2alpha1.deployment.KeycloakBuilder;
import org.keycloak.operator.crds.v2alpha1.deployment.KeycloakSpecBuilder;
import org.keycloak.operator.crds.v2alpha1.deployment.ValueOrSecret;
import org.keycloak.operator.crds.v2alpha1.deployment.spec.HostnameSpecBuilder;
import org.keycloak.operator.crds.v2alpha1.deployment.spec.HttpSpecBuilder;
import org.keycloak.operator.crds.v2alpha1.deployment.spec.UnsupportedSpec;
import org.keycloak.operator.crds.v2alpha1.realmimport.KeycloakRealmImportBuilder;
import org.keycloak.operator.crds.v2alpha1.realmimport.KeycloakRealmImportSpecBuilder;
import org.keycloak.representations.idm.RealmRepresentation;
import org.mockito.Mockito;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.keycloak.operator.ContextUtils.DIST_CONFIGURATOR_KEY;
import static org.keycloak.operator.ContextUtils.NEW_DEPLOYMENT_KEY;
import static org.keycloak.operator.ContextUtils.OLD_DEPLOYMENT_KEY;
import static org.keycloak.operator.ContextUtils.OPERATOR_CONFIG_KEY;
import static org.keycloak.operator.ContextUtils.WATCHED_RESOURCES_KEY;

@QuarkusTest
public class PodTemplateTest {

    @InjectMock
    WatchedResources watchedResources;

    @Inject
    Config operatorConfig;

    @Inject
    KeycloakDistConfigurator distConfigurator;

    KeycloakDeploymentDependentResource deployment;

    @Inject
    KeycloakUpdateJobDependentResource jobResource;

    @Inject
    KeycloakRealmImportJobDependentResource importJobResource;

    @BeforeEach
    protected void setup() {
        this.deployment = new KeycloakDeploymentDependentResource();
    }

    private StatefulSet getDeployment(PodTemplateSpec podTemplate, StatefulSet existingDeployment, Consumer<KeycloakSpecBuilder> additionalSpec) {
        var kc = createKeycloak(podTemplate, additionalSpec);

        existingDeployment = new StatefulSetBuilder(existingDeployment).editOrNewMetadata().endMetadata().editOrNewSpec().editOrNewSelector()
                .withMatchLabels(Utils.allInstanceLabels(kc))
                .endSelector().endSpec().build();

        //noinspection unchecked
        Context context = mockContext(null);
        return deployment.initialDesired(kc, context);
    }

    private Keycloak createKeycloak(PodTemplateSpec podTemplate, Consumer<KeycloakSpecBuilder> additionalSpec) {
        var kc = new KeycloakBuilder().withNewMetadata().withName("instance").withNamespace("keycloak-ns").endMetadata().build();
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
        return kc;
    }

    private Context<Keycloak> mockContext(StatefulSet existingDeployment) {
        Context<Keycloak> context = Mockito.mock(Context.class);
        ManagedWorkflowAndDependentResourceContext managedWorkflowAndDependentResourceContext = Mockito.mock(ManagedWorkflowAndDependentResourceContext.class);
        Mockito.when(context.managedWorkflowAndDependentResourceContext()).thenReturn(managedWorkflowAndDependentResourceContext);
        Mockito.when(managedWorkflowAndDependentResourceContext.get(OLD_DEPLOYMENT_KEY, StatefulSet.class)).thenReturn(Optional.ofNullable(existingDeployment));
        Mockito.when(managedWorkflowAndDependentResourceContext.getMandatory(OPERATOR_CONFIG_KEY, Config.class)).thenReturn(operatorConfig);
        Mockito.when(managedWorkflowAndDependentResourceContext.getMandatory(WATCHED_RESOURCES_KEY, WatchedResources.class)).thenReturn(watchedResources);
        Mockito.when(managedWorkflowAndDependentResourceContext.getMandatory(DIST_CONFIGURATOR_KEY, KeycloakDistConfigurator.class)).thenReturn(distConfigurator);
        Mockito.when(context.getClient()).thenReturn(Mockito.mock(KubernetesClient.class));
        return context;
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
    public void testHttpManagment() {
        var result = getDeployment(null, new StatefulSet(),
                spec -> spec.withAdditionalOptions(new ValueOrSecret(KeycloakDeploymentDependentResource.HTTP_MANAGEMENT_SCHEME, "http")))
                .getSpec()
                .getTemplate()
                .getSpec()
                .getContainers()
                .get(0);

        assertEquals("HTTP", result.getReadinessProbe().getHttpGet().getScheme());
        assertEquals(9000, result.getReadinessProbe().getHttpGet().getPort().getIntVal());
    }

    @Test
    public void testHealthOnMain() {
        var result = getDeployment(null, new StatefulSet(),
                spec -> spec.withAdditionalOptions(new ValueOrSecret(CRDUtils.HTTP_MANAGEMENT_HEALTH_ENABLED, "false")))
                .getSpec()
                .getTemplate()
                .getSpec()
                .getContainers()
                .get(0);

        assertEquals("HTTPS", result.getReadinessProbe().getHttpGet().getScheme());
        assertEquals(8443, result.getReadinessProbe().getHttpGet().getPort().getIntVal());
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
        StatefulSetSpec spec = getDeployment(additionalPodTemplate).getSpec();
        var podTemplate = spec.getTemplate();
        var container = podTemplate.getSpec().getContainers().get(0);

        // Assert
        assertThat(spec.getServiceName()).isEqualTo("instance-discovery");
        assertNotNull(container);
        assertThat(container.getArgs()).doesNotContain(KeycloakDeploymentDependentResource.OPTIMIZED_ARG);
        assertThat(container.getArgs()).contains("-Djgroups.bind.address=$(POD_IP)");

        var envVars = container.getEnv();
        assertThat(envVars.stream()).anyMatch(envVar -> envVar.getName().equals(KeycloakDeploymentDependentResource.KC_TRUSTSTORE_PATHS));
        assertThat(envVars.stream()).anyMatch(envVar -> envVar.getName().equals(KeycloakDeploymentDependentResource.KC_TRACING_SERVICE_NAME));
        assertThat(envVars.stream()).anyMatch(envVar -> envVar.getName().equals(KeycloakDeploymentDependentResource.KC_TRACING_RESOURCE_ATTRIBUTES));
        assertThat(envVars.stream()).anyMatch(envVar -> envVar.getName().equals(KeycloakDeploymentDependentResource.POD_IP));

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

        var topologySpreadConstraints = podTemplate.getSpec().getTopologySpreadConstraints();
        assertNotNull(topologySpreadConstraints);
        assertThat(topologySpreadConstraints).hasSize(2);
        assertThat(Serialization.asYaml(topologySpreadConstraints)).isEqualTo("""
                ---
                - labelSelector:
                    matchLabels:
                      app: "keycloak"
                      app.kubernetes.io/managed-by: "keycloak-operator"
                      app.kubernetes.io/instance: "instance"
                      app.kubernetes.io/component: "server"
                  maxSkew: 1
                  topologyKey: "topology.kubernetes.io/zone"
                  whenUnsatisfiable: "ScheduleAnyway"
                - labelSelector:
                    matchLabels:
                      app: "keycloak"
                      app.kubernetes.io/managed-by: "keycloak-operator"
                      app.kubernetes.io/instance: "instance"
                      app.kubernetes.io/component: "server"
                  maxSkew: 1
                  topologyKey: "kubernetes.io/hostname"
                  whenUnsatisfiable: "ScheduleAnyway"
                """);
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
    public void testAdditionalOptionEnvKey() {
        // Arrange
        PodTemplateSpec additionalPodTemplate = null;

        var additionalOptions = List.of(
                new ValueOrSecret("log-level-org.package.some_class", "debug"),
                new ValueOrSecret("tracing-header-Authorization", "Bearer aldskfjqweoiruzxcv"),
                new ValueOrSecret("tracing-header-My-BEST_$header", "api-asdflkqjwer-key")
        );

        // Act
        var podTemplate = getDeployment(additionalPodTemplate, null,
                s -> s.addAllToAdditionalOptions(additionalOptions))
                .getSpec().getTemplate();

        // Assert
        var assertEnvVarKeys = Map.of(
                "KCKEY_LOG_LEVEL_ORG_PACKAGE_SOME_CLASS", "log-level-org.package.some_class",
                "KC_LOG_LEVEL_ORG_PACKAGE_SOME_CLASS", "debug",
                "KCKEY_TRACING_HEADER_AUTHORIZATION", "tracing-header-Authorization",
                "KC_TRACING_HEADER_AUTHORIZATION", "Bearer aldskfjqweoiruzxcv",
                "KCKEY_TRACING_HEADER_MY_BEST__HEADER", "tracing-header-My-BEST_$header",
                "KC_TRACING_HEADER_MY_BEST__HEADER", "api-asdflkqjwer-key"
        );

        var envVars = podTemplate.getSpec().getContainers().get(0).getEnv().stream()
                .filter(e -> e.getValue() != null)
                .collect(Collectors.toMap(EnvVar::getName, EnvVar::getValue));
        assertThat(envVars).containsAllEntriesOf(assertEnvVarKeys);
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

        Mockito.verify(this.watchedResources).annotateDeployment(Mockito.eq(Watched.of("cm")), Mockito.eq(ConfigMap.class), Mockito.any(), Mockito.any());
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

    @Test
    public void testPriorityClass() {
        // Arrange
        PodTemplateSpec additionalPodTemplate = null;

        // Act
        var podTemplate = getDeployment(additionalPodTemplate, null,
                s -> s.withNewSchedulingSpec().withPriorityClassName("important").endSchedulingSpec())
                .getSpec().getTemplate();

        // Assert
        assertThat(podTemplate.getSpec().getPriorityClassName()).isEqualTo("important");

        podTemplate = getDeployment(new PodTemplateSpecBuilder().withNewSpec().withPriorityClassName("existing").endSpec().build(), null,
                s -> s.withNewSchedulingSpec().withPriorityClassName("important").endSchedulingSpec())
                .getSpec().getTemplate();

        assertThat(podTemplate.getSpec().getPriorityClassName()).isEqualTo("existing");
    }

    @Test
    public void testTolerations() {
        // Arrange
        PodTemplateSpec additionalPodTemplate = null;

        Toleration toleration = new Toleration("NoSchedule", "key", "=", null, "value");

        // Act
        var podTemplate = getDeployment(additionalPodTemplate, null,
                s -> s.withNewSchedulingSpec().addToTolerations(toleration).endSchedulingSpec())
                .getSpec().getTemplate();

        // Assert
        assertThat(podTemplate.getSpec().getTolerations()).isEqualTo(List.of(toleration));

        podTemplate = getDeployment(new PodTemplateSpecBuilder().withNewSpec().withTolerations(new Toleration()).endSpec().build(), null,
                s -> s.withNewSchedulingSpec().addToTolerations(toleration).endSchedulingSpec())
                .getSpec().getTemplate();

        // Assert
        assertThat(podTemplate.getSpec().getTolerations()).isNotEqualTo(List.of(toleration));

    }

    @Test
    public void testTopologySpreadConstraints() {
        // Arrange
        PodTemplateSpec additionalPodTemplate = null;

        TopologySpreadConstraint tsc = new TopologySpreadConstraintBuilder().withTopologyKey("key").build();

        // Act
        var podTemplate = getDeployment(additionalPodTemplate, null,
                s -> s.withNewSchedulingSpec().addToTopologySpreadConstraints(tsc).endSchedulingSpec())
                .getSpec().getTemplate();

        // Assert
        assertThat(podTemplate.getSpec().getTopologySpreadConstraints()).isEqualTo(List.of(tsc));

        podTemplate = getDeployment(new PodTemplateSpecBuilder().withNewSpec().withTopologySpreadConstraints(new TopologySpreadConstraint()).endSpec().build(), null,
                s -> s.withNewSchedulingSpec().addToTopologySpreadConstraints(tsc).endSchedulingSpec())
                .getSpec().getTemplate();

        // Assert
        assertThat(podTemplate.getSpec().getTopologySpreadConstraints()).isNotEqualTo(List.of(tsc));
    }

    @Test
    public void testAffinity() {
        // Arrange
        PodTemplateSpec additionalPodTemplate = null;

        var affinity = new AffinityBuilder().withNewPodAffinity()
                .addNewPreferredDuringSchedulingIgnoredDuringExecution().withNewPodAffinityTerm().withNamespaces("x")
                .endPodAffinityTerm().endPreferredDuringSchedulingIgnoredDuringExecution().endPodAffinity().build();

        // Act
        var podTemplate = getDeployment(additionalPodTemplate, null,
                s -> s.withNewSchedulingSpec().withAffinity(affinity).endSchedulingSpec())
                .getSpec().getTemplate();

        // Assert
        assertThat(podTemplate.getSpec().getAffinity()).isEqualTo(affinity);

        podTemplate = getDeployment(new PodTemplateSpecBuilder().withNewSpec().withAffinity(new Affinity()).endSpec().build(), null,
                s -> s.withNewSchedulingSpec().withAffinity(affinity).endSchedulingSpec())
                .getSpec().getTemplate();

        // Assert
        assertThat(podTemplate.getSpec().getAffinity()).isNotEqualTo(affinity);
    }

    @Test
    public void testProbe(){
        PodTemplateSpec additionalPodTemplate = null;
        var readinessProbe = new ProbeBuilder().withFailureThreshold(1).withPeriodSeconds(2).build();
        var livenessProbe = new ProbeBuilder().withFailureThreshold(3).withPeriodSeconds(4).build();
        var startupProbe = new ProbeBuilder().withFailureThreshold(5).withPeriodSeconds(6).build();
        var readinessPodTemplate = getDeployment(additionalPodTemplate, null,
                s-> s.withNewReadinessProbeSpec()
                        .withProbeFailureThreshold(1)
                        .withProbePeriodSeconds(2)
                        .endReadinessProbeSpec()).getSpec().getTemplate();
        assertThat(readinessPodTemplate.getSpec().getContainers().get(0).getReadinessProbe().getPeriodSeconds()).isEqualTo(readinessProbe.getPeriodSeconds());
        assertThat(readinessPodTemplate.getSpec().getContainers().get(0).getReadinessProbe().getFailureThreshold()).isEqualTo(readinessProbe.getFailureThreshold());

        var livenessPodTemplate = getDeployment(additionalPodTemplate, null,
                s-> s.withNewLivenessProbeSpec()
                        .withProbeFailureThreshold(3)
                        .withProbePeriodSeconds(4)
                        .endLivenessProbeSpec()).getSpec().getTemplate();
        assertThat(livenessPodTemplate.getSpec().getContainers().get(0).getLivenessProbe().getPeriodSeconds()).isEqualTo(livenessProbe.getPeriodSeconds());
        assertThat(livenessPodTemplate.getSpec().getContainers().get(0).getLivenessProbe().getFailureThreshold()).isEqualTo(livenessProbe.getFailureThreshold());

        var startupPodTemplate = getDeployment(additionalPodTemplate, null,
                s-> s.withNewStartupProbeSpec()
                        .withProbeFailureThreshold(5)
                        .withProbePeriodSeconds(6)
                        .endStartupProbeSpec()).getSpec().getTemplate();
        assertThat(startupPodTemplate.getSpec().getContainers().get(0).getStartupProbe().getPeriodSeconds()).isEqualTo(startupProbe.getPeriodSeconds());
        assertThat(startupPodTemplate.getSpec().getContainers().get(0).getStartupProbe().getFailureThreshold()).isEqualTo(startupProbe.getFailureThreshold());
    }

    private Job getUpdateJob(Consumer<KeycloakSpecBuilder> newSpec, Consumer<KeycloakSpecBuilder> oldSpec, Consumer<StatefulSetBuilder> existingModifier) {
        // create an existing from the old spec and modifier
        StatefulSetBuilder existingBuilder = getDeployment(null, null, oldSpec).toBuilder();
        existingModifier.accept(existingBuilder);
        StatefulSet existingStatefulSet = existingBuilder.build();

        // determine the desired statefulset state
        StatefulSetBuilder desired = getDeployment(null, existingStatefulSet, newSpec).toBuilder();

        // setup the mock context
        Context<Keycloak> context = mockContext(null);
        var managedWorkflowAndDependentResourceContext = context.managedWorkflowAndDependentResourceContext();
        Mockito.when(managedWorkflowAndDependentResourceContext.get(OLD_DEPLOYMENT_KEY, StatefulSet.class)).thenReturn(Optional.of(existingStatefulSet));
        Mockito.when(managedWorkflowAndDependentResourceContext.getMandatory(NEW_DEPLOYMENT_KEY, StatefulSet.class)).thenReturn(desired.build());

        return jobResource.desired(createKeycloak(null, newSpec), context);
    }

    private Job getImportJob(Consumer<KeycloakSpecBuilder> keycloakSpec, Consumer<KeycloakRealmImportSpecBuilder> realmImportSpec, Consumer<StatefulSetBuilder> existingModifier) {
        StatefulSetBuilder existingBuilder = getDeployment(null, null, keycloakSpec).toBuilder();
        existingModifier.accept(existingBuilder);
        StatefulSet existingStatefulSet = existingBuilder.build();

        Context context = mockContext(existingStatefulSet);
        var kc = createKeycloak(null, keycloakSpec);
        Mockito.when(context.managedWorkflowAndDependentResourceContext().getMandatory(ContextUtils.KEYCLOAK, Keycloak.class)).thenReturn(kc);

        var builder = new KeycloakRealmImportBuilder();
        RealmRepresentation rep = new RealmRepresentation();
        rep.setRealm("realm");
        var realmImport = builder.withNewSpec().withKeycloakCRName(existingStatefulSet.getMetadata().getName())
                .withRealm(rep).endSpec().build();
        KeycloakRealmImportSpecBuilder specBuilder = new KeycloakRealmImportSpecBuilder(realmImport.getSpec());
        realmImportSpec.accept(specBuilder);
        realmImport.setSpec(specBuilder.build());

        return importJobResource.desired(realmImport, context);
    }

    @Test
    public void testUpdateJobSchedulingDefault() {
        Consumer<KeycloakSpecBuilder> addJobScheduling = builder -> {};

        Job job = getUpdateJob(addJobScheduling, addJobScheduling, builder -> {});

        // nothing should be set
        assertNull(job.getSpec().getTemplate().getSpec().getTopologySpreadConstraints());
        assertNull(job.getSpec().getTemplate().getSpec().getAffinity());
    }

    @Test
    public void testUpdateJobSchedulingInherited() {
        Consumer<KeycloakSpecBuilder> addJobScheduling = builder -> {
            builder.editOrNewSchedulingSpec().withPriorityClassName("priority").endSchedulingSpec();
        };

        Job job = getUpdateJob(addJobScheduling, addJobScheduling, builder -> {});

        assertEquals("priority", job.getSpec().getTemplate().getSpec().getPriorityClassName());
    }

    @Test
    public void testUpdateJobSchedulingOverride() {
        Consumer<KeycloakSpecBuilder> addJobScheduling = builder -> {
            builder.editOrNewSchedulingSpec().withPriorityClassName("priority").endSchedulingSpec();
            builder.editOrNewUpdateSpec().editOrNewSchedulingSpec().endSchedulingSpec().endUpdateSpec();
        };

        Job job = getUpdateJob(addJobScheduling, addJobScheduling, builder -> {});
        assertNull(job.getSpec().getTemplate().getSpec().getPriorityClassName());
    }

    @Test
    public void testRealmImportJobSchedulingOverride() {
        Job job = getImportJob(
                builder -> builder.editOrNewSchedulingSpec().addNewToleration("NoSchedule", "key", "value", 10L, "in")
                        .endSchedulingSpec().withNewImportSpec().withNewSchedulingSpec()
                        .addNewToleration("NoSchedule", "key1", "value1", 10L, "in").endSchedulingSpec()
                        .endImportSpec(),
                builder -> {},
                builder -> {});
        assertEquals("---\n"
                + "effect: \"NoSchedule\"\n"
                + "key: \"key1\"\n"
                + "operator: \"value1\"\n"
                + "tolerationSeconds: 10\n"
                + "value: \"in\"\n", Serialization.asYaml(job.getSpec().getTemplate().getSpec().getTolerations().get(0)));
    }

    @Test
    public void testUpdateJobSecretHandling() {
        Job job = getUpdateJob(builder -> {}, builder -> {}, builder -> {});
        assertEquals(List.of(), job.getSpec().getTemplate().getSpec().getImagePullSecrets());

        LocalObjectReference secret = new LocalObjectReference("secret");
        Consumer<StatefulSetBuilder> addSecret = builder -> builder.editSpec().editTemplate().editSpec().addToImagePullSecrets(secret).endSpec().endTemplate().endSpec();
        job = getUpdateJob(builder -> {}, builder -> {}, addSecret);
        assertEquals(List.of(new LocalObjectReference("secret")), job.getSpec().getTemplate().getSpec().getImagePullSecrets());

        job = getUpdateJob(builder -> builder.addToImagePullSecrets(new LocalObjectReference("new-secret")), builder -> {}, addSecret);
        assertEquals(List.of(new LocalObjectReference("new-secret"), new LocalObjectReference("secret")), job.getSpec().getTemplate().getSpec().getImagePullSecrets());
    }

    @Test
    public void testFieldRemovalForInitContainer() {
        Job job = getUpdateJob(builder -> {
        }, builder -> builder.withNewUnsupported().withNewPodTemplate().withNewSpec().addNewContainer()
                .withRestartPolicy("OnFailure")
                .withNewLifecycle().withNewPostStart().withNewExec().withCommand("hello").endExec().endPostStart()
                .endLifecycle().endContainer().endSpec().endPodTemplate().endUnsupported(), builder -> {
                });
        assertNull(job.getSpec().getTemplate().getSpec().getInitContainers().get(0).getLifecycle());
        assertNull(job.getSpec().getTemplate().getSpec().getInitContainers().get(0).getRestartPolicy());
    }

    @Test
    public void testEnvVars() {
        var statefulSet = getDeployment(null, null, builder -> builder.addNewEnv("JAVA_OPTS", "my opts")
                .addToEnv(new ValueOrSecret("SECRET", new SecretKeySelector("key", "my-secret", null))));

        var env = statefulSet.getSpec().getTemplate().getSpec().getContainers().get(0).getEnv().stream()
                .collect(Collectors.toMap(EnvVar::getName, Function.identity()));

        // make sure the raw value is present
        var envVar = env.get("JAVA_OPTS");
        assertEquals("my opts", envVar.getValue());

        // make sure the secret is there, and is watched
        envVar = env.get("SECRET");
        assertEquals("key", envVar.getValueFrom().getSecretKeyRef().getKey());

        Mockito.verify(this.watchedResources).annotateDeployment(Mockito.eq(Watched.of("example-tls-secret", "instance-initial-admin", "my-secret")), Mockito.eq(Secret.class), Mockito.any(), Mockito.any());
    }

}

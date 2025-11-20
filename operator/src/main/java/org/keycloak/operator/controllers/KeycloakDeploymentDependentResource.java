/*
 * Copyright 2021 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.operator.controllers;

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.EnvVar;
import io.fabric8.kubernetes.api.model.EnvVarBuilder;
import io.fabric8.kubernetes.api.model.EnvVarSource;
import io.fabric8.kubernetes.api.model.EnvVarSourceBuilder;
import io.fabric8.kubernetes.api.model.PodSpecFluent;
import io.fabric8.kubernetes.api.model.PodTemplateSpec;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.SecretKeySelector;
import io.fabric8.kubernetes.api.model.VolumeBuilder;
import io.fabric8.kubernetes.api.model.VolumeMountBuilder;
import io.fabric8.kubernetes.api.model.apps.StatefulSet;
import io.fabric8.kubernetes.api.model.apps.StatefulSetBuilder;
import io.javaoperatorsdk.operator.api.config.informer.Informer;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.api.reconciler.dependent.DependentResource;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.CRUDKubernetesDependentResource;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.KubernetesDependent;
import io.javaoperatorsdk.operator.processing.dependent.workflow.Condition;
import io.quarkus.logging.Log;
import org.keycloak.operator.Config;
import org.keycloak.operator.Constants;
import org.keycloak.operator.ContextUtils;
import org.keycloak.operator.Utils;
import org.keycloak.operator.crds.v2alpha1.CRDUtils;
import org.keycloak.operator.crds.v2alpha1.deployment.Keycloak;
import org.keycloak.operator.crds.v2alpha1.deployment.KeycloakSpec;
import org.keycloak.operator.crds.v2alpha1.deployment.ValueOrSecret;
import org.keycloak.operator.crds.v2alpha1.deployment.spec.CacheSpec;
import org.keycloak.operator.crds.v2alpha1.deployment.spec.HttpManagementSpec;
import org.keycloak.operator.crds.v2alpha1.deployment.spec.HttpSpec;
import org.keycloak.operator.crds.v2alpha1.deployment.spec.ProbeSpec;
import org.keycloak.operator.crds.v2alpha1.deployment.spec.SchedulingSpec;
import org.keycloak.operator.crds.v2alpha1.deployment.spec.Truststore;
import org.keycloak.operator.crds.v2alpha1.deployment.spec.TruststoreSource;
import org.keycloak.operator.crds.v2alpha1.deployment.spec.UnsupportedSpec;
import org.keycloak.operator.crds.v2alpha1.deployment.spec.UpdateSpec;
import org.keycloak.operator.update.impl.RecreateOnImageChangeUpdateLogic;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.keycloak.operator.Utils.addResources;
import static org.keycloak.operator.controllers.KeycloakDistConfigurator.getKeycloakOptionEnvVarName;
import static org.keycloak.operator.crds.v2alpha1.CRDUtils.LEGACY_MANAGEMENT_ENABLED;
import static org.keycloak.operator.crds.v2alpha1.CRDUtils.isTlsConfigured;
import static org.keycloak.operator.crds.v2alpha1.deployment.spec.TracingSpec.convertTracingAttributesToString;

@KubernetesDependent(
        informer = @Informer(labelSelector = Constants.DEFAULT_LABELS_AS_STRING)
)
public class KeycloakDeploymentDependentResource extends CRUDKubernetesDependentResource<StatefulSet, Keycloak> {

    public static final String HTTP_MANAGEMENT_SCHEME = "http-management-scheme";

    public static final String POD_IP = "POD_IP";
    public static final String HOST_IP_SPI_OPTION = "KC_SPI_CACHE_EMBEDDED_DEFAULT_MACHINE_NAME";

    private static final List<String> COPY_ENV = Arrays.asList("HTTP_PROXY", "HTTPS_PROXY", "NO_PROXY");

    private static final String SERVICE_ACCOUNT_DIR = "/var/run/secrets/kubernetes.io/serviceaccount/";
    private static final String SERVICE_CA_CRT = SERVICE_ACCOUNT_DIR + "service-ca.crt";

    public static final String CACHE_CONFIG_FILE_MOUNT_NAME = "cache-config-file-configmap";

    public static final String KC_TRUSTSTORE_PATHS = "KC_TRUSTSTORE_PATHS";

    // Tracing
    public static final String KC_TRACING_SERVICE_NAME = "KC_TRACING_SERVICE_NAME";
    public static final String KC_TRACING_RESOURCE_ATTRIBUTES = "KC_TRACING_RESOURCE_ATTRIBUTES";

    public static final String OPTIMIZED_ARG = "--optimized";

    private boolean useServiceCaCrt;

    // Do not create the deployment before the initial admin secret is created to prevent the deployment from restarting.
    // Not using native dependsOn as the initial admin secret may not be created by the operator and might be provided by the user,
    // in which case we want to create the deployment immediately.
    public static class ReconcilePrecondition implements Condition<StatefulSet, Keycloak> {
        @Override
        public boolean isMet(DependentResource<StatefulSet, Keycloak> dependentResource, Keycloak primary, Context<Keycloak> context) {
            return KeycloakAdminSecretDependentResource.hasCustomAdminSecret(primary)
                    || context.getSecondaryResourcesAsStream(Secret.class)
                    .anyMatch(s -> s.getMetadata().getName().equals(KeycloakAdminSecretDependentResource.getName(primary)));
        }
    }

    public KeycloakDeploymentDependentResource() {
        super(StatefulSet.class);
        useServiceCaCrt = Files.exists(Path.of(SERVICE_CA_CRT));
    }

    public void setUseServiceCaCrt(boolean useServiceCaCrt) {
        this.useServiceCaCrt = useServiceCaCrt;
    }

    public StatefulSet initialDesired(Keycloak primary, Context<Keycloak> context) {
        Config operatorConfig = ContextUtils.getOperatorConfig(context);
        WatchedResources watchedResources = ContextUtils.getWatchedResources(context);

        StatefulSet baseDeployment = createBaseDeployment(primary, context, operatorConfig);
        WatchedResources.Watched allSecrets = new WatchedResources.Watched();
        WatchedResources.Watched allConfigMaps = new WatchedResources.Watched();
        if (isTlsConfigured(primary)) {
            configureTLS(primary, baseDeployment, allSecrets);
        }
        Container kcContainer = baseDeployment.getSpec().getTemplate().getSpec().getContainers().get(0);
        addTruststores(primary, baseDeployment, kcContainer, allSecrets, allConfigMaps);
        addEnvVars(baseDeployment, primary, allSecrets, context);
        addResources(primary.getSpec().getResourceRequirements(), operatorConfig, kcContainer);
        Optional.ofNullable(primary.getSpec().getCacheSpec())
                .ifPresent(c -> configureCache(baseDeployment, kcContainer, c, allConfigMaps));

        watchedResources.annotateDeployment(allSecrets, Secret.class, baseDeployment, context);
        watchedResources.annotateDeployment(allConfigMaps, ConfigMap.class, baseDeployment, context);

        // default to the new revision - will be overriden to the old one if needed
        UpdateSpec.getRevision(primary).ifPresent(rev -> addUpdateRevisionAnnotation(rev, baseDeployment));
        addUpdateHashAnnotation(KeycloakUpdateJobDependentResource.keycloakHash(primary), baseDeployment);

        var existingDeployment = ContextUtils.getCurrentStatefulSet(context).orElse(null);

        String serviceName = KeycloakDiscoveryServiceDependentResource.getName(primary);
        if (existingDeployment != null) {
            // copy the existing annotations to keep the status consistent
            CRDUtils.findUpdateReason(existingDeployment).ifPresent(r -> baseDeployment.getMetadata().getAnnotations()
                    .put(Constants.KEYCLOAK_UPDATE_REASON_ANNOTATION, r));
            CRDUtils.fetchIsRecreateUpdate(existingDeployment).ifPresent(b -> baseDeployment.getMetadata()
                    .getAnnotations().put(Constants.KEYCLOAK_RECREATE_UPDATE_ANNOTATION, b.toString()));
            serviceName = existingDeployment.getSpec().getServiceName();
        }

        baseDeployment.getSpec().setServiceName(serviceName);
        return baseDeployment;
    }

    @Override
    public StatefulSet desired(Keycloak primary, Context<Keycloak> context) {
        StatefulSet baseDeployment = ContextUtils.getDesiredStatefulSet(context);
        var existingDeployment = ContextUtils.getCurrentStatefulSet(context).orElse(null);

        var updateType = ContextUtils.getUpdateType(context);

        if (existingDeployment == null || updateType.isEmpty()) {
            return baseDeployment;
        }

        // version 22 changed the match labels, account for older versions
        if (!existingDeployment.isMarkedForDeletion() && !hasExpectedMatchLabels(existingDeployment, primary)) {
            context.getClient().resource(existingDeployment).lockResourceVersion().delete();
            Log.info("Existing Deployment found with old label selector, it will be recreated");
        }

        baseDeployment.getMetadata().getAnnotations().put(Constants.KEYCLOAK_UPDATE_REASON_ANNOTATION, ContextUtils.getUpdateReason(context));

        return switch (updateType.get()) {
            case ROLLING -> handleRollingUpdate(baseDeployment);
            case RECREATE -> handleRecreateUpdate(existingDeployment, baseDeployment, CRDUtils.firstContainerOf(baseDeployment).orElseThrow());
        };
    }

    private void configureCache(StatefulSet deployment, Container kcContainer, CacheSpec spec, WatchedResources.Watched allConfigMaps) {
        Optional.ofNullable(spec.getConfigMapFile()).ifPresent(configFile -> {
            if (configFile.getName() == null || configFile.getKey() == null) {
                throw new IllegalStateException("Cache file ConfigMap requires both a name and a key");
            }

            var volume = new VolumeBuilder()
                    .withName(CACHE_CONFIG_FILE_MOUNT_NAME)
                    .withNewConfigMap()
                    .withName(configFile.getName())
                    .withOptional(configFile.getOptional())
                    .endConfigMap()
                    .build();

            var volumeMount = new VolumeMountBuilder()
                    .withName(volume.getName())
                    .withMountPath(Constants.CACHE_CONFIG_FOLDER)
                    .build();

            deployment.getSpec().getTemplate().getSpec().getVolumes().add(0, volume);
            kcContainer.getVolumeMounts().add(0, volumeMount);
            allConfigMaps.add(configFile.getName(), configFile.getOptional());
        });
    }

    private void addTruststores(Keycloak keycloakCR, StatefulSet deployment, Container kcContainer, WatchedResources.Watched allSecrets, WatchedResources.Watched allConfigMaps) {
        for (Truststore truststore : keycloakCR.getSpec().getTruststores().values()) {
            // for now we'll assume only secrets, later we can support configmaps
            TruststoreSource source = truststore.getSecret();
            if (source != null) {
                String secretName = source.getName();
                var volume = new VolumeBuilder()
                        .withName("truststore-secret-" + secretName)
                        .withNewSecret()
                        .withSecretName(secretName)
                        .withOptional(source.getOptional())
                        .endSecret()
                        .build();

                var volumeMount = new VolumeMountBuilder()
                        .withName(volume.getName())
                        .withMountPath(Constants.TRUSTSTORES_FOLDER + "/secret-" + secretName)
                        .build();

                deployment.getSpec().getTemplate().getSpec().getVolumes().add(0, volume);
                kcContainer.getVolumeMounts().add(0, volumeMount);
                allSecrets.add(secretName, source.getOptional());
            } else {
                source = truststore.getConfigMap();
                if (source != null) {
                    String name = source.getName();
                    var volume = new VolumeBuilder()
                            .withName("truststore-configmap-" + name)
                            .withNewConfigMap()
                            .withName(name)
                            .withOptional(source.getOptional())
                            .endConfigMap()
                            .build();

                    var volumeMount = new VolumeMountBuilder()
                            .withName(volume.getName())
                            .withMountPath(Constants.TRUSTSTORES_FOLDER + "/configmap-" + name)
                            .build();

                    deployment.getSpec().getTemplate().getSpec().getVolumes().add(0, volume);
                    kcContainer.getVolumeMounts().add(0, volumeMount);
                    allConfigMaps.add(name, source.getOptional());
                }
            }
        }
    }

    void configureTLS(Keycloak keycloakCR, StatefulSet deployment, WatchedResources.Watched allSecrets) {
        var kcContainer = deployment.getSpec().getTemplate().getSpec().getContainers().get(0);

        var volume = new VolumeBuilder()
                .withName("keycloak-tls-certificates")
                .withNewSecret()
                .withSecretName(keycloakCR.getSpec().getHttpSpec().getTlsSecret())
                .withOptional(false)
                .endSecret()
                .build();

        var volumeMount = new VolumeMountBuilder()
                .withName(volume.getName())
                .withMountPath(Constants.CERTIFICATES_FOLDER)
                .build();

        deployment.getSpec().getTemplate().getSpec().getVolumes().add(0, volume);
        kcContainer.getVolumeMounts().add(0, volumeMount);
        allSecrets.add(keycloakCR.getSpec().getHttpSpec().getTlsSecret(), null);
    }

    private boolean hasExpectedMatchLabels(StatefulSet statefulSet, Keycloak keycloak) {
        return Optional.ofNullable(statefulSet).map(s -> Utils.allInstanceLabels(keycloak).equals(s.getSpec().getSelector().getMatchLabels())).orElse(true);
    }

    static Optional<PodTemplateSpec> getPodTemplateSpec(Keycloak keycloakCR) {
        return Optional.ofNullable(keycloakCR.getSpec()).map(KeycloakSpec::getUnsupported).map(UnsupportedSpec::getPodTemplate);
    }

    private StatefulSet createBaseDeployment(Keycloak keycloakCR, Context<Keycloak> context, Config operatorConfig) {
        Map<String, String> labels = Utils.allInstanceLabels(keycloakCR);
        labels.put("app.kubernetes.io/component", "server");
        Map<String, String> schedulingLabels = new LinkedHashMap<>(labels);
        if (operatorConfig.keycloak().podLabels() != null) {
            labels.putAll(operatorConfig.keycloak().podLabels());
        }

        /* Create a builder for the statefulset, note that the pod template spec is used as the basis
         * over that some values are forced, others will let the template override, others merge
         */

        StatefulSetBuilder baseDeploymentBuilder = new StatefulSetBuilder()
                .withNewMetadata()
                    .withName(getName(keycloakCR))
                    .withNamespace(keycloakCR.getMetadata().getNamespace())
                    .withLabels(Utils.allInstanceLabels(keycloakCR))
                    .addToAnnotations(Constants.KEYCLOAK_MIGRATING_ANNOTATION, Boolean.FALSE.toString())
                .endMetadata()
                .withNewSpec()
                    .withNewSelector()
                        .withMatchLabels(Utils.allInstanceLabels(keycloakCR))
                    .endSelector()
                    .withNewTemplateLike(getPodTemplateSpec(keycloakCR).orElseGet(PodTemplateSpec::new))
                        .editOrNewMetadata().addToLabels(labels).endMetadata()
                        .editOrNewSpec().withImagePullSecrets(keycloakCR.getSpec().getImagePullSecrets()).endSpec()
                    .endTemplate()
                    .withReplicas(keycloakCR.getSpec().getInstances())
                .endSpec();

        var specBuilder = baseDeploymentBuilder.editSpec().editTemplate().editOrNewSpec();

        if (!specBuilder.hasRestartPolicy()) {
            specBuilder.withRestartPolicy("Always");
        }
        if (!specBuilder.hasTerminationGracePeriodSeconds()) {
            specBuilder.withTerminationGracePeriodSeconds(30L);
        }
        if (!specBuilder.hasDnsPolicy()) {
            specBuilder.withDnsPolicy("ClusterFirst");
        }
        boolean automount = !Boolean.FALSE.equals(keycloakCR.getSpec().getAutomountServiceAccountToken());
        specBuilder.withAutomountServiceAccountToken(automount);
        handleScheduling(keycloakCR, schedulingLabels, specBuilder);

        // there isn't currently an editOrNewFirstContainer, so we need to do this manually
        var containerBuilder = specBuilder.buildContainers().isEmpty() ? specBuilder.addNewContainer() : specBuilder.editFirstContainer();

        containerBuilder.withName("keycloak");

        var customImage = Optional.ofNullable(keycloakCR.getSpec().getImage());
        containerBuilder.withImage(customImage.orElse(operatorConfig.keycloak().image()));

        if (!containerBuilder.hasImagePullPolicy()) {
            containerBuilder.withImagePullPolicy(operatorConfig.keycloak().imagePullPolicy());
        }
        if (Optional.ofNullable(containerBuilder.getArgs()).orElse(List.of()).isEmpty()) {
            containerBuilder.withArgs("--verbose", "start");
        }
        if (Boolean.TRUE.equals(keycloakCR.getSpec().getStartOptimized())
                || keycloakCR.getSpec().getStartOptimized() == null
                        && (customImage.isPresent() || operatorConfig.keycloak().startOptimized())) {
            containerBuilder.addToArgs(OPTIMIZED_ARG);
        }
        // Set bind address as this is required for JGroups to form a cluster in IPv6 environments
        containerBuilder.addToArgs(0, "-Djgroups.bind.address=$(%s)".formatted(POD_IP));

        ManagementEndpoint endpoint = managementEndpoint(keycloakCR, context, true);

        // probes
        var readinessOptionalSpec = Optional.ofNullable(keycloakCR.getSpec().getReadinessProbeSpec());
        var livenessOptionalSpec = Optional.ofNullable(keycloakCR.getSpec().getLivenessProbeSpec());
        var startupOptionalSpec = Optional.ofNullable(keycloakCR.getSpec().getStartupProbeSpec());

        if (!containerBuilder.hasReadinessProbe()) {
            containerBuilder.withNewReadinessProbe()
                .withPeriodSeconds(readinessOptionalSpec.map(ProbeSpec::getProbePeriodSeconds).orElse(10))
                .withFailureThreshold(readinessOptionalSpec.map(ProbeSpec::getProbeFailureThreshold).orElse(3))
                .withNewHttpGet()
                .withScheme(endpoint.protocol)
                .withNewPort(endpoint.port)
                .withPath(endpoint.relativePath + "health/ready")
                .endHttpGet()
                .endReadinessProbe();
        }
        if (!containerBuilder.hasLivenessProbe()) {
            containerBuilder.withNewLivenessProbe()
                .withPeriodSeconds(livenessOptionalSpec.map(ProbeSpec::getProbePeriodSeconds).orElse(10))
                .withFailureThreshold(livenessOptionalSpec.map(ProbeSpec::getProbeFailureThreshold).orElse(3))
                .withNewHttpGet()
                .withScheme(endpoint.protocol)
                .withNewPort(endpoint.port)
                .withPath(endpoint.relativePath + "health/live")
                .endHttpGet()
                .endLivenessProbe();
        }
        if (!containerBuilder.hasStartupProbe()) {
            containerBuilder.withNewStartupProbe()
                .withPeriodSeconds(startupOptionalSpec.map(ProbeSpec::getProbePeriodSeconds).orElse(1))
                .withFailureThreshold(startupOptionalSpec.map(ProbeSpec::getProbeFailureThreshold).orElse(600))
                .withNewHttpGet()
                .withScheme(endpoint.protocol)
                .withNewPort(endpoint.port)
                .withPath(endpoint.relativePath + "health/started")
                .endHttpGet()
                .endStartupProbe();
        }

        // add in ports - there's no merging being done here
        return containerBuilder
            .addNewPort()
                .withName(Constants.KEYCLOAK_HTTPS_PORT_NAME)
                .withContainerPort(Constants.KEYCLOAK_HTTPS_PORT)
                .withProtocol(Constants.KEYCLOAK_SERVICE_PROTOCOL)
            .endPort()
            .addNewPort()
                .withName(Constants.KEYCLOAK_HTTP_PORT_NAME)
                .withContainerPort(Constants.KEYCLOAK_HTTP_PORT)
                .withProtocol(Constants.KEYCLOAK_SERVICE_PROTOCOL)
            .endPort()
            .addNewPort()
                .withName(Constants.KEYCLOAK_MANAGEMENT_PORT_NAME)
                .withContainerPort(Constants.KEYCLOAK_MANAGEMENT_PORT)
                .withProtocol(Constants.KEYCLOAK_SERVICE_PROTOCOL)
            .endPort()
            .endContainer().endSpec().endTemplate().endSpec().build();
    }

    private void handleScheduling(Keycloak keycloakCR, Map<String, String> labels, PodSpecFluent<?> specBuilder) {
        SchedulingSpec schedulingSpec = keycloakCR.getSpec().getSchedulingSpec();
        if (schedulingSpec != null) {
            if (!specBuilder.hasPriorityClassName()) {
                specBuilder.withPriorityClassName(schedulingSpec.getPriorityClassName());
            }
            if (!specBuilder.hasAffinity()) {
                specBuilder.withAffinity(schedulingSpec.getAffinity());
            }
            if (!specBuilder.hasTolerations()) {
                specBuilder.withTolerations(schedulingSpec.getTolerations());
            }
            if (!specBuilder.hasTopologySpreadConstraints()) {
                specBuilder.withTopologySpreadConstraints(schedulingSpec.getTopologySpreadConstraints());
            }
        }

        if (!specBuilder.hasTopologySpreadConstraints()) {
            specBuilder.addNewTopologySpreadConstraint()
                    .withMaxSkew(1)
                    .withTopologyKey("topology.kubernetes.io/zone")
                    .withWhenUnsatisfiable("ScheduleAnyway")
                    .withNewLabelSelector()
                    .withMatchLabels(labels)
                    .endLabelSelector()
                    .endTopologySpreadConstraint()
                    .addNewTopologySpreadConstraint()
                    .withMaxSkew(1)
                    .withTopologyKey("kubernetes.io/hostname")
                    .withWhenUnsatisfiable("ScheduleAnyway")
                    .withNewLabelSelector()
                    .withMatchLabels(labels)
                    .endLabelSelector()
                    .endTopologySpreadConstraint();
        }
    }

    private void addEnvVars(StatefulSet baseDeployment, Keycloak keycloakCR, WatchedResources.Watched allSecrets, Context<Keycloak> context) {
        var distConfigurator = ContextUtils.getDistConfigurator(context);
        var firstClasssEnvVars = distConfigurator.configureDistOptions(keycloakCR);

        var additionalEnvVars = getDefaultAndAdditionalEnvVars(keycloakCR);

        var unsupportedEnv = Optional.ofNullable(baseDeployment.getSpec().getTemplate().getSpec().getContainers().get(0).getEnv()).orElse(List.of());

        var env = keycloakCR.getSpec().getEnv().stream().map(this::toEnvVar);

        // accumulate the env vars in priority order - unsupported, first class, additional, env
        LinkedHashMap<String, EnvVar> varMap = Stream.concat(Stream.concat(unsupportedEnv.stream(), firstClasssEnvVars.stream()), Stream.concat(additionalEnvVars.stream(), env))
                .collect(Collectors.toMap(EnvVar::getName, Function.identity(), (e1, e2) -> e1, LinkedHashMap::new));


        if (!Boolean.FALSE.equals(keycloakCR.getSpec().getAutomountServiceAccountToken())) {
            String truststores = SERVICE_ACCOUNT_DIR + "ca.crt";

            if (useServiceCaCrt) {
                truststores += "," + SERVICE_CA_CRT;
            }

            // include the kube CA if the user is not controlling KC_TRUSTSTORE_PATHS via the unsupported or the additional
            varMap.putIfAbsent(KC_TRUSTSTORE_PATHS, new EnvVarBuilder().withName(KC_TRUSTSTORE_PATHS).withValue(truststores).build());
        }

        setTracingEnvVars(keycloakCR, varMap);

        var envVars = new ArrayList<>(varMap.values());
        baseDeployment.getSpec().getTemplate().getSpec().getContainers().get(0).setEnv(envVars);

        // watch the secrets used by secret key - we don't currently expect configmaps or watch the initial-admin
        TreeSet<String> serverConfigSecretsNames = envVars.stream().map(EnvVar::getValueFrom).filter(Objects::nonNull)
                .map(EnvVarSource::getSecretKeyRef).filter(Objects::nonNull).peek(s -> allSecrets.add(s.getName(), s.getOptional())).map(SecretKeySelector::getName).collect(Collectors.toCollection(TreeSet::new));

        Log.debugf("Found config secrets names: %s", serverConfigSecretsNames);
    }

    private static void setTracingEnvVars(Keycloak keycloakCR, Map<String, EnvVar> varMap) {
        varMap.putIfAbsent(KC_TRACING_SERVICE_NAME,
                new EnvVarBuilder().withName(KC_TRACING_SERVICE_NAME)
                        .withValue(keycloakCR.getMetadata().getName())
                        .build()
        );

        // Possible OTel k8s attributes convention can be found here: https://opentelemetry.io/docs/specs/semconv/attributes-registry/k8s/#kubernetes-attributes
        var tracingAttributes = Map.of("k8s.namespace.name", keycloakCR.getMetadata().getNamespace());

        if (varMap.containsKey(KC_TRACING_RESOURCE_ATTRIBUTES)) {
            // append 'tracingAttributes' to the existing attributes defined in the 'KC_TRACING_RESOURCE_ATTRIBUTES' env var
            var existingAttributes = convertTracingAttributesToMap(varMap);
            tracingAttributes.forEach(existingAttributes::putIfAbsent);
            varMap.get(KC_TRACING_RESOURCE_ATTRIBUTES).setValue(convertTracingAttributesToString(existingAttributes));
        } else {
            varMap.put(KC_TRACING_RESOURCE_ATTRIBUTES,
                    new EnvVarBuilder().withName(KC_TRACING_RESOURCE_ATTRIBUTES)
                            .withValue(convertTracingAttributesToString(tracingAttributes))
                            .build()
            );
        }
    }

    private static Map<String, String> convertTracingAttributesToMap(Map<String, EnvVar> envVars) {
        return Arrays.stream(Optional.ofNullable(envVars.get(KC_TRACING_RESOURCE_ATTRIBUTES).getValue()).orElse("").split(","))
                .filter(entry -> entry.contains("="))
                .map(entry -> entry.split("=", 2))
                .collect(Collectors.toMap(entry -> entry[0], entry -> entry[1]));
    }

    private EnvVar toEnvVar(ValueOrSecret v) {
        var envBuilder = new EnvVarBuilder().withName(v.getName());
        var secret = v.getSecret();
        if (secret != null) {
            envBuilder.withValueFrom(
                    new EnvVarSourceBuilder().withSecretKeyRef(secret).build());
        } else {
            envBuilder.withValue(v.getValue());
        }
        return envBuilder.build();
    }

    private List<EnvVar> getDefaultAndAdditionalEnvVars(Keycloak keycloakCR) {
        // default config values
        List<ValueOrSecret> serverConfigsList = new ArrayList<>(Constants.DEFAULT_DIST_CONFIG_LIST);
        Set<String> defaultKeys = serverConfigsList.stream().map(ValueOrSecret::getName).collect(Collectors.toSet());

        // merge with the CR; the values in CR take precedence
        if (keycloakCR.getSpec().getAdditionalOptions() != null) {
            Set<String> inCr = keycloakCR.getSpec().getAdditionalOptions().stream().map(ValueOrSecret::getName).collect(Collectors.toSet());
            serverConfigsList.removeIf(v -> inCr.contains(v.getName()));
            serverConfigsList.addAll(keycloakCR.getSpec().getAdditionalOptions());
        }

        // set env vars
        List<EnvVar> envVars = serverConfigsList.stream()
                .flatMap(v -> {
                    var envBuilder = new EnvVarBuilder().withName(getKeycloakOptionEnvVarName(v.getName()));
                    var secret = v.getSecret();
                    if (secret != null) {
                        envBuilder.withValueFrom(
                                new EnvVarSourceBuilder().withSecretKeyRef(secret).build());
                    } else {
                        envBuilder.withValue(v.getValue());
                    }
                    EnvVar mainVar = envBuilder.build();
                    if (!defaultKeys.contains(v.getName())) {
                        EnvVar keyVar = new EnvVarBuilder()
                                .withName("KCKEY_" + mainVar.getName().substring(KeycloakDistConfigurator.KC_PREFIX.length()))
                                .withValue(v.getName()).build();
                        return Stream.of(mainVar, keyVar);
                    }
                    return Stream.of(mainVar);
                })
                .collect(Collectors.toCollection(ArrayList::new));

        for (String env : COPY_ENV) {
            String value = System.getenv(env);
            if (value != null) {
                envVars.add(new EnvVarBuilder().withName(env).withValue(value).build());
            }
        }

        envVars.add(new EnvVarBuilder().withName(POD_IP).withNewValueFrom().withNewFieldRef()
                .withFieldPath("status.podIP").withApiVersion("v1").endFieldRef().endValueFrom().build());

        // Both status.hostIP or spec.nodeName would be fine here.
        // In theory, status.hostIP is a smaller value and, as this value is tagged in all JGroups messages, it should have a lower overhead.
        // Using spec.nodeName to avoid exposing the IP addresses in the logs.
        envVars.add(new EnvVarBuilder().withName(HOST_IP_SPI_OPTION).withNewValueFrom().withNewFieldRef()
                .withFieldPath("spec.nodeName").withApiVersion("v1").endFieldRef().endValueFrom().build());

        return envVars;
    }

    public static String getName(Keycloak keycloak) {
        return keycloak.getMetadata().getName();
    }

    static Optional<String> readConfigurationValue(String key, Keycloak keycloakCR, Context<Keycloak> context) {
        return Optional.ofNullable(keycloakCR.getSpec()).map(KeycloakSpec::getAdditionalOptions)
                .flatMap(l -> l.stream().filter(sc -> sc.getName().equals(key)).findFirst().map(serverConfigValue -> {
            if (serverConfigValue.getValue() != null) {
                return serverConfigValue.getValue();
            }
            var secretSelector = serverConfigValue.getSecret();
            if (secretSelector == null) {
                throw new IllegalStateException("Secret " + serverConfigValue.getName() + " not defined");
            }
            var secret = context.getClient().secrets().inNamespace(keycloakCR.getMetadata().getNamespace()).withName(secretSelector.getName()).get();
            if (secret == null) {
                throw new IllegalStateException("Secret " + secretSelector.getName() + " not found in cluster");
            }
            if (secret.getData().containsKey(secretSelector.getKey())) {
                return new String(Base64.getDecoder().decode(secret.getData().get(secretSelector.getKey())), StandardCharsets.UTF_8);
            }
            throw new IllegalStateException("Secret " + secretSelector.getName() + " doesn't contain the expected key " + secretSelector.getKey());
        }));
    }

    private static StatefulSet handleRollingUpdate(StatefulSet desired) {
        // return the desired stateful set since Kubernetes does a rolling in-place update by default.
        Log.debug("Performing a rolling update");
        desired.getMetadata().getAnnotations().put(Constants.KEYCLOAK_RECREATE_UPDATE_ANNOTATION, Boolean.FALSE.toString());
        return desired;
    }

    private static StatefulSet handleRecreateUpdate(StatefulSet actual, StatefulSet desired, Container kcContainer) {
        desired.getMetadata().getAnnotations().put(Constants.KEYCLOAK_RECREATE_UPDATE_ANNOTATION, Boolean.TRUE.toString());

        if (Optional.ofNullable(actual.getStatus().getReplicas()).orElse(0) == 0) {
            Log.debug("Performing a recreate update - scaling up the stateful set");

            // desired state correct as is
        } else {
            Log.debug("Performing a recreate update - scaling down the stateful set");

            // keep the old revision, image, and hash, then mark as migrating, and scale down
            addOrRemoveAnnotation(CRDUtils.getRevision(actual).orElse(null), Constants.KEYCLOAK_UPDATE_REVISION_ANNOTATION, desired);
            addOrRemoveAnnotation(CRDUtils.getUpdateHash(actual).orElse(null), Constants.KEYCLOAK_UPDATE_HASH_ANNOTATION, desired);
            desired.getMetadata().getAnnotations().put(Constants.KEYCLOAK_MIGRATING_ANNOTATION, Boolean.TRUE.toString());
            desired.getSpec().setReplicas(0);
            var currentImage = RecreateOnImageChangeUpdateLogic.extractImage(actual);
            kcContainer.setImage(currentImage);
        }
        return desired;
    }

    private static void addUpdateRevisionAnnotation(String revision, StatefulSet toUpdate) {
        toUpdate.getMetadata().getAnnotations().put(Constants.KEYCLOAK_UPDATE_REVISION_ANNOTATION, revision);
    }

    private static void addUpdateHashAnnotation(String hash, StatefulSet toUpdate) {
        toUpdate.getMetadata().getAnnotations().put(Constants.KEYCLOAK_UPDATE_HASH_ANNOTATION, hash);
    }

    private static void addOrRemoveAnnotation(String value, String annotation, StatefulSet toUpdate) {
        toUpdate.getMetadata().getAnnotations().compute(annotation, (k, v) -> value);
    }

    record ManagementEndpoint(String relativePath, String protocol, int port, String portName) {}

    static ManagementEndpoint managementEndpoint(Keycloak keycloakCR, Context<Keycloak> context, boolean health) {
        boolean tls = isTlsConfigured(keycloakCR);
        String protocol = tls ? "HTTPS" : "HTTP";
        int port;
        String portName;

        var legacy = readConfigurationValue(LEGACY_MANAGEMENT_ENABLED, keycloakCR, context).map(Boolean::valueOf).orElse(false);

        var healthManagementEnabled = readConfigurationValue(CRDUtils.HTTP_MANAGEMENT_HEALTH_ENABLED, keycloakCR, context).map(Boolean::valueOf).orElse(true);

        if (!legacy && (!health || healthManagementEnabled)) {
            port = HttpManagementSpec.managementPort(keycloakCR);
            portName = Constants.KEYCLOAK_MANAGEMENT_PORT_NAME;
            if (readConfigurationValue(HTTP_MANAGEMENT_SCHEME, keycloakCR, context).filter("http"::equals).isPresent()) {
                protocol = "HTTP";
            }
        } else {
            port = tls ? HttpSpec.httpsPort(keycloakCR) : HttpSpec.httpPort(keycloakCR);
            portName = tls ? Constants.KEYCLOAK_HTTPS_PORT_NAME : Constants.KEYCLOAK_HTTP_PORT_NAME;
        }

        var relativePath = readConfigurationValue(Constants.KEYCLOAK_HTTP_MANAGEMENT_RELATIVE_PATH_KEY, keycloakCR, context)
              .or(() -> readConfigurationValue(Constants.KEYCLOAK_HTTP_RELATIVE_PATH_KEY, keycloakCR, context))
              .map(path -> !path.endsWith("/") ? path + "/" : path)
              .orElse("/");

        return new ManagementEndpoint(relativePath, protocol, port, portName);
    }
}

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
package org.keycloak.operator.v2alpha1;

import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.ContainerBuilder;
import io.fabric8.kubernetes.api.model.EnvVar;
import io.fabric8.kubernetes.api.model.EnvVarBuilder;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.VolumeBuilder;
import io.fabric8.kubernetes.api.model.VolumeMountBuilder;
import io.fabric8.kubernetes.api.model.PodTemplateSpec;
import io.fabric8.kubernetes.api.model.ResourceRequirements;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.apps.DeploymentBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.utils.Serialization;
import io.quarkus.logging.Log;
import org.keycloak.operator.Config;
import org.keycloak.operator.Constants;
import org.keycloak.operator.OperatorManagedResource;
import org.keycloak.operator.StatusUpdater;
import org.keycloak.operator.v2alpha1.crds.Keycloak;
import org.keycloak.operator.v2alpha1.crds.KeycloakStatusBuilder;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class KeycloakDeployment extends OperatorManagedResource implements StatusUpdater<KeycloakStatusBuilder> {

//    public static final Pattern CONFIG_SECRET_PATTERN = Pattern.compile("^\\$\\{secret:([^:]+):(.+)}$");

    private final Config config;
    private final Keycloak keycloakCR;
    private final Deployment existingDeployment;
    private final Deployment baseDeployment;

    public KeycloakDeployment(KubernetesClient client, Config config, Keycloak keycloakCR, Deployment existingDeployment) {
        super(client, keycloakCR);
        this.config = config;
        this.keycloakCR = keycloakCR;

        if (existingDeployment != null) {
            Log.info("Existing Deployment provided by controller");
            this.existingDeployment = existingDeployment;
        }
        else {
            Log.info("Trying to fetch existing Deployment from the API");
            this.existingDeployment = fetchExistingDeployment();
        }

        baseDeployment = createBaseDeployment();
    }

    @Override
    public Optional<HasMetadata> getReconciledResource() {
        Deployment baseDeployment = new DeploymentBuilder(this.baseDeployment).build(); // clone not to change the base template
        Deployment reconciledDeployment;
        if (existingDeployment == null) {
            Log.info("No existing Deployment found, using the default");
            reconciledDeployment = baseDeployment;
        }
        else {
            Log.info("Existing Deployment found, updating specs");
            reconciledDeployment = existingDeployment;
            // don't override metadata, just specs
            reconciledDeployment.setSpec(baseDeployment.getSpec());
        }

        return Optional.of(reconciledDeployment);
    }

    private Deployment fetchExistingDeployment() {
        return client
                .apps()
                .deployments()
                .inNamespace(getNamespace())
                .withName(getName())
                .get();
    }

    private void addInitContainer(Deployment baseDeployment, List<String> extensions) {
        var skipExtensions = Optional
                .ofNullable(extensions)
                .map(e -> e.isEmpty())
                .orElse(true);

        if (skipExtensions) {
            return;
        }

        // Add emptyDir Volume
        var volumes = baseDeployment.getSpec().getTemplate().getSpec().getVolumes();

        var extensionVolume = new VolumeBuilder()
                .withName(Constants.EXTENSIONS_VOLUME_NAME)
                .withNewEmptyDir()
                .endEmptyDir()
                .build();

        volumes.add(extensionVolume);
        baseDeployment.getSpec().getTemplate().getSpec().setVolumes(volumes);

        // Add the main deployment Volume Mount
        var container = baseDeployment.getSpec().getTemplate().getSpec().getContainers().get(0);
        var containerVolumeMounts = container.getVolumeMounts();

        var extensionVM = new VolumeMountBuilder()
                .withName(Constants.EXTENSIONS_VOLUME_NAME)
                .withMountPath(Constants.KEYCLOAK_PROVIDERS_FOLDER)
                .withReadOnly(true)
                .build();
        containerVolumeMounts.add(extensionVM);

        container.setVolumeMounts(containerVolumeMounts);

        // Add the Extensions downloader init container
        var extensionsValue = extensions.stream().collect(Collectors.joining(","));
        var initContainer = new ContainerBuilder()
                .withName(Constants.INIT_CONTAINER_NAME)
                .withImage(config.keycloak().initContainerImage())
                .withImagePullPolicy(config.keycloak().initContainerImagePullPolicy())
                .addNewVolumeMount()
                .withName(Constants.EXTENSIONS_VOLUME_NAME)
                .withMountPath(Constants.INIT_CONTAINER_EXTENSIONS_FOLDER)
                .endVolumeMount()
                .addNewEnv()
                .withName(Constants.INIT_CONTAINER_EXTENSIONS_ENV_VAR)
                .withValue(extensionsValue)
                .endEnv()
                .build();

        baseDeployment.getSpec().getTemplate().getSpec().setInitContainers(Collections.singletonList(initContainer));
    }

    public void validatePodTemplate(KeycloakStatusBuilder status) {
        if (keycloakCR.getSpec() == null ||
                keycloakCR.getSpec().getUnsupported() == null ||
                keycloakCR.getSpec().getUnsupported().getPodTemplate() == null) {
            return;
        }
        var overlayTemplate = this.keycloakCR.getSpec().getUnsupported().getPodTemplate();

        if (overlayTemplate.getMetadata() != null &&
            overlayTemplate.getMetadata().getName() != null) {
            status.addWarningMessage("The name of the podTemplate cannot be modified");
        }

        if (overlayTemplate.getMetadata() != null &&
            overlayTemplate.getMetadata().getNamespace() != null) {
            status.addWarningMessage("The namespace of the podTemplate cannot be modified");
        }

        if (overlayTemplate.getSpec() != null &&
            overlayTemplate.getSpec().getContainers() != null &&
            overlayTemplate.getSpec().getContainers().get(0) != null &&
            overlayTemplate.getSpec().getContainers().get(0).getName() != null) {
            status.addWarningMessage("The name of the keycloak container cannot be modified");
        }

        if (overlayTemplate.getSpec() != null &&
            overlayTemplate.getSpec().getContainers() != null &&
            overlayTemplate.getSpec().getContainers().get(0) != null &&
            overlayTemplate.getSpec().getContainers().get(0).getImage() != null) {
            status.addWarningMessage("The image of the keycloak container cannot be modified using podTemplate");
        }
    }

    private <T, V> void mergeMaps(Map<T, V> map1, Map<T, V> map2, Consumer<Map<T, V>> consumer) {
        var map = new HashMap<T, V>();
        Optional.ofNullable(map1).ifPresent(e -> map.putAll(e));
        Optional.ofNullable(map2).ifPresent(e -> map.putAll(e));
        consumer.accept(map);
    }

    private <T> void mergeLists(List<T> list1, List<T> list2, Consumer<List<T>> consumer) {
        var list = new ArrayList<T>();
        Optional.ofNullable(list1).ifPresent(e -> list.addAll(e));
        Optional.ofNullable(list2).ifPresent(e -> list.addAll(e));
        consumer.accept(list);
    }

    private <T> void mergeField(T value, Consumer<T> consumer) {
        if (value != null && (!(value instanceof List) || ((List<?>) value).size() > 0)) {
            consumer.accept(value);
        }
    }

    private void mergePodTemplate(PodTemplateSpec baseTemplate) {
        if (keycloakCR.getSpec() == null ||
            keycloakCR.getSpec().getUnsupported() == null ||
            keycloakCR.getSpec().getUnsupported().getPodTemplate() == null) {
            return;
        }

        var overlayTemplate = keycloakCR.getSpec().getUnsupported().getPodTemplate();

        mergeMaps(
                Optional.ofNullable(baseTemplate.getMetadata()).map(m -> m.getLabels()).orElse(null),
                Optional.ofNullable(overlayTemplate.getMetadata()).map(m -> m.getLabels()).orElse(null),
                labels -> baseTemplate.getMetadata().setLabels(labels));

        mergeMaps(
                Optional.ofNullable(baseTemplate.getMetadata()).map(m -> m.getAnnotations()).orElse(null),
                Optional.ofNullable(overlayTemplate.getMetadata()).map(m -> m.getAnnotations()).orElse(null),
                annotations -> baseTemplate.getMetadata().setAnnotations(annotations));

        var baseSpec = baseTemplate.getSpec();
        var overlaySpec = overlayTemplate.getSpec();

        var containers = new ArrayList<Container>();
        var overlayContainers =
                (overlaySpec == null || overlaySpec.getContainers() == null) ?
                        new ArrayList<Container>() : overlaySpec.getContainers();
        if (overlayContainers.size() >= 1) {
            var keycloakBaseContainer = baseSpec.getContainers().get(0);
            var keycloakOverlayContainer = overlayContainers.get(0);
            mergeField(keycloakOverlayContainer.getCommand(), v -> keycloakBaseContainer.setCommand(v));
            mergeField(keycloakOverlayContainer.getReadinessProbe(), v -> keycloakBaseContainer.setReadinessProbe(v));
            mergeField(keycloakOverlayContainer.getLivenessProbe(), v -> keycloakBaseContainer.setLivenessProbe(v));
            mergeField(keycloakOverlayContainer.getStartupProbe(), v -> keycloakBaseContainer.setStartupProbe(v));
            mergeField(keycloakOverlayContainer.getArgs(), v -> keycloakBaseContainer.setArgs(v));
            mergeField(keycloakOverlayContainer.getImagePullPolicy(), v -> keycloakBaseContainer.setImagePullPolicy(v));
            mergeField(keycloakOverlayContainer.getLifecycle(), v -> keycloakBaseContainer.setLifecycle(v));
            mergeField(keycloakOverlayContainer.getSecurityContext(), v -> keycloakBaseContainer.setSecurityContext(v));
            mergeField(keycloakOverlayContainer.getWorkingDir(), v -> keycloakBaseContainer.setWorkingDir(v));

            var resources = new ResourceRequirements();
            mergeMaps(
                    Optional.ofNullable(keycloakBaseContainer.getResources()).map(r -> r.getRequests()).orElse(null),
                    Optional.ofNullable(keycloakOverlayContainer.getResources()).map(r -> r.getRequests()).orElse(null),
                    requests -> resources.setRequests(requests));
            mergeMaps(
                    Optional.ofNullable(keycloakBaseContainer.getResources()).map(l -> l.getLimits()).orElse(null),
                    Optional.ofNullable(keycloakOverlayContainer.getResources()).map(l -> l.getLimits()).orElse(null),
                    limits -> resources.setLimits(limits));
            keycloakBaseContainer.setResources(resources);

            mergeLists(
                    keycloakBaseContainer.getPorts(),
                    keycloakOverlayContainer.getPorts(),
                    p -> keycloakBaseContainer.setPorts(p));
            mergeLists(
                    keycloakBaseContainer.getEnvFrom(),
                    keycloakOverlayContainer.getEnvFrom(),
                    e -> keycloakBaseContainer.setEnvFrom(e));
            mergeLists(
                    keycloakBaseContainer.getEnv(),
                    keycloakOverlayContainer.getEnv(),
                    e -> keycloakBaseContainer.setEnv(e));
            mergeLists(
                    keycloakBaseContainer.getVolumeMounts(),
                    keycloakOverlayContainer.getVolumeMounts(),
                    vm -> keycloakBaseContainer.setVolumeMounts(vm));
            mergeLists(
                    keycloakBaseContainer.getVolumeDevices(),
                    keycloakOverlayContainer.getVolumeDevices(),
                    vd -> keycloakBaseContainer.setVolumeDevices(vd));

            containers.add(keycloakBaseContainer);

            // Skip keycloak container and add the rest
            for (int i = 1; i < overlayContainers.size(); i++) {
                containers.add(overlayContainers.get(i));
            }

            baseSpec.setContainers(containers);
        }

        if (overlaySpec != null) {
            mergeField(overlaySpec.getActiveDeadlineSeconds(), ads -> baseSpec.setActiveDeadlineSeconds(ads));
            mergeField(overlaySpec.getAffinity(), a -> baseSpec.setAffinity(a));
            mergeField(overlaySpec.getAutomountServiceAccountToken(), a -> baseSpec.setAutomountServiceAccountToken(a));
            mergeField(overlaySpec.getDnsConfig(), dc -> baseSpec.setDnsConfig(dc));
            mergeField(overlaySpec.getDnsPolicy(), dp -> baseSpec.setDnsPolicy(dp));
            mergeField(overlaySpec.getEnableServiceLinks(), esl -> baseSpec.setEnableServiceLinks(esl));
            mergeField(overlaySpec.getHostIPC(), h -> baseSpec.setHostIPC(h));
            mergeField(overlaySpec.getHostname(), h -> baseSpec.setHostname(h));
            mergeField(overlaySpec.getHostNetwork(), h -> baseSpec.setHostNetwork(h));
            mergeField(overlaySpec.getHostPID(), h -> baseSpec.setHostPID(h));
            mergeField(overlaySpec.getNodeName(), n -> baseSpec.setNodeName(n));
            mergeField(overlaySpec.getNodeSelector(), ns -> baseSpec.setNodeSelector(ns));
            mergeField(overlaySpec.getPreemptionPolicy(), pp -> baseSpec.setPreemptionPolicy(pp));
            mergeField(overlaySpec.getPriority(), p -> baseSpec.setPriority(p));
            mergeField(overlaySpec.getPriorityClassName(), pcn -> baseSpec.setPriorityClassName(pcn));
            mergeField(overlaySpec.getRestartPolicy(), rp -> baseSpec.setRestartPolicy(rp));
            mergeField(overlaySpec.getRuntimeClassName(), rcn -> baseSpec.setRuntimeClassName(rcn));
            mergeField(overlaySpec.getSchedulerName(), sn -> baseSpec.setSchedulerName(sn));
            mergeField(overlaySpec.getSecurityContext(), sc -> baseSpec.setSecurityContext(sc));
            mergeField(overlaySpec.getServiceAccount(), sa -> baseSpec.setServiceAccount(sa));
            mergeField(overlaySpec.getServiceAccountName(), san -> baseSpec.setServiceAccountName(san));
            mergeField(overlaySpec.getSetHostnameAsFQDN(), h -> baseSpec.setSetHostnameAsFQDN(h));
            mergeField(overlaySpec.getShareProcessNamespace(), spn -> baseSpec.setShareProcessNamespace(spn));
            mergeField(overlaySpec.getSubdomain(), s -> baseSpec.setSubdomain(s));
            mergeField(overlaySpec.getTerminationGracePeriodSeconds(), t -> baseSpec.setTerminationGracePeriodSeconds(t));

            mergeLists(
                    baseSpec.getImagePullSecrets(),
                    overlaySpec.getImagePullSecrets(),
                    ips -> baseSpec.setImagePullSecrets(ips));
            mergeLists(
                    baseSpec.getHostAliases(),
                    overlaySpec.getHostAliases(),
                    ha -> baseSpec.setHostAliases(ha));
            mergeLists(
                    baseSpec.getEphemeralContainers(),
                    overlaySpec.getEphemeralContainers(),
                    ec -> baseSpec.setEphemeralContainers(ec));
            mergeLists(
                    baseSpec.getInitContainers(),
                    overlaySpec.getInitContainers(),
                    ic -> baseSpec.setInitContainers(ic));
            mergeLists(
                    baseSpec.getReadinessGates(),
                    overlaySpec.getReadinessGates(),
                    rg -> baseSpec.setReadinessGates(rg));
            mergeLists(
                    baseSpec.getTolerations(),
                    overlaySpec.getTolerations(),
                    t -> baseSpec.setTolerations(t));
            mergeLists(
                    baseSpec.getTopologySpreadConstraints(),
                    overlaySpec.getTopologySpreadConstraints(),
                    tpc -> baseSpec.setTopologySpreadConstraints(tpc));

            mergeLists(
                    baseSpec.getVolumes(),
                    overlaySpec.getVolumes(),
                    v -> baseSpec.setVolumes(v));

            mergeMaps(
                    baseSpec.getOverhead(),
                    overlaySpec.getOverhead(),
                    o -> baseSpec.setOverhead(o));
        }
    }

    private Deployment createBaseDeployment() {
        var is = this.getClass().getResourceAsStream("/base-keycloak-deployment.yaml");
        Deployment baseDeployment = Serialization.unmarshal(is, Deployment.class);

        baseDeployment.getMetadata().setName(getName());
        baseDeployment.getMetadata().setNamespace(getNamespace());
        baseDeployment.getSpec().getSelector().setMatchLabels(Constants.DEFAULT_LABELS);
        baseDeployment.getSpec().setReplicas(keycloakCR.getSpec().getInstances());
        baseDeployment.getSpec().getTemplate().getMetadata().setLabels(Constants.DEFAULT_LABELS);

        Container container = baseDeployment.getSpec().getTemplate().getSpec().getContainers().get(0);
        container.setImage(Optional.ofNullable(keycloakCR.getSpec().getImage()).orElse(config.keycloak().image()));
        container.setImagePullPolicy(config.keycloak().imagePullPolicy());

        container.setEnv(getEnvVars());

        addInitContainer(baseDeployment, keycloakCR.getSpec().getExtensions());
        mergePodTemplate(baseDeployment.getSpec().getTemplate());

//        Set<String> configSecretsNames = new HashSet<>();
//        List<EnvVar> configEnvVars = serverConfig.entrySet().stream()
//                .map(e -> {
//                    EnvVarBuilder builder = new EnvVarBuilder().withName(e.getKey());
//                    Matcher matcher = CONFIG_SECRET_PATTERN.matcher(e.getValue());
//                    // check if given config var is actually a secret reference
//                    if (matcher.matches()) {
//                        builder.withValueFrom(
//                                new EnvVarSourceBuilder()
//                                        .withNewSecretKeyRef(matcher.group(2), matcher.group(1), false)
//                                        .build());
//                        configSecretsNames.add(matcher.group(1)); // for watching it later
//                    } else {
//                        builder.withValue(e.getValue());
//                    }
//                    builder.withValue(e.getValue());
//                    return builder.build();
//                })
//                .collect(Collectors.toList());
//        container.setEnv(configEnvVars);
//        this.configSecretsNames = Collections.unmodifiableSet(configSecretsNames);
//        Log.infof("Found config secrets names: %s", configSecretsNames);

        return baseDeployment;
    }

    private List<EnvVar> getEnvVars() {
        var serverConfig = new HashMap<>(Constants.DEFAULT_DIST_CONFIG);
        serverConfig.put("jgroups.dns.query", getName() + Constants.KEYCLOAK_DISCOVERY_SERVICE_SUFFIX +"." + getNamespace());
        if (keycloakCR.getSpec().getServerConfiguration() != null) {
            serverConfig.putAll(keycloakCR.getSpec().getServerConfiguration());
        }
        return serverConfig.entrySet().stream()
                .map(e -> new EnvVarBuilder()
                        .withName(e.getKey())
                        .withValue(e.getValue())
                        .build())
                .collect(Collectors.toList());
    }

    public void updateStatus(KeycloakStatusBuilder status) {
        validatePodTemplate(status);
        if (existingDeployment == null) {
            status.addNotReadyMessage("No existing Deployment found, waiting for creating a new one");
            return;
        }

        var replicaFailure = existingDeployment.getStatus().getConditions().stream()
                .filter(d -> d.getType().equals("ReplicaFailure")).findFirst();
        if (replicaFailure.isPresent()) {
            status.addNotReadyMessage("Deployment failures");
            status.addErrorMessage("Deployment failure: " + replicaFailure.get());
            return;
        }

        if (existingDeployment.getStatus() == null
                || existingDeployment.getStatus().getReadyReplicas() == null
                || existingDeployment.getStatus().getReadyReplicas() < keycloakCR.getSpec().getInstances()) {
            status.addNotReadyMessage("Waiting for more replicas");
        }
    }

//    public Set<String> getConfigSecretsNames() {
//        return configSecretsNames;
//    }

    @Override
    public String getName() {
        return keycloakCR.getMetadata().getName();
    }

    public void rollingRestart() {
        client.apps().deployments()
                .inNamespace(getNamespace())
                .withName(getName())
                .rolling().restart();
    }
}

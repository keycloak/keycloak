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

import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.EnvVar;
import io.fabric8.kubernetes.api.model.EnvVarBuilder;
import io.fabric8.kubernetes.api.model.EnvVarSourceBuilder;
import io.fabric8.kubernetes.api.model.ExecActionBuilder;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.PodTemplateSpec;
import io.fabric8.kubernetes.api.model.ResourceRequirements;
import io.fabric8.kubernetes.api.model.VolumeBuilder;
import io.fabric8.kubernetes.api.model.VolumeMountBuilder;
import io.fabric8.kubernetes.api.model.apps.StatefulSet;
import io.fabric8.kubernetes.api.model.apps.StatefulSetBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.quarkus.logging.Log;
import org.keycloak.operator.Config;
import org.keycloak.operator.Constants;
import org.keycloak.operator.crds.v2alpha1.deployment.Keycloak;
import org.keycloak.operator.crds.v2alpha1.deployment.KeycloakStatusBuilder;
import org.keycloak.operator.crds.v2alpha1.deployment.ValueOrSecret;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static io.smallrye.config.common.utils.StringUtil.replaceNonAlphanumericByUnderscores;

public class KeycloakDeployment extends OperatorManagedResource implements StatusUpdater<KeycloakStatusBuilder> {

    private final Config config;
    private final Keycloak keycloakCR;
    private final StatefulSet existingDeployment;
    private final StatefulSet baseDeployment;
    private final String adminSecretName;

    private Set<String> serverConfigSecretsNames;

    private boolean migrationInProgress;

    public KeycloakDeployment(KubernetesClient client, Config config, Keycloak keycloakCR, StatefulSet existingDeployment, String adminSecretName) {
        super(client, keycloakCR);
        this.config = config;
        this.keycloakCR = keycloakCR;
        this.adminSecretName = adminSecretName;

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
        StatefulSet baseDeployment = new StatefulSetBuilder(this.baseDeployment).build(); // clone not to change the base template
        StatefulSet reconciledDeployment;
        if (existingDeployment == null) {
            Log.info("No existing Deployment found, using the default");
            reconciledDeployment = baseDeployment;
        }
        else {
            Log.info("Existing Deployment found, updating specs");
            reconciledDeployment = new StatefulSetBuilder(existingDeployment).build();

            // don't overwrite metadata, just specs
            reconciledDeployment.setSpec(baseDeployment.getSpec());

            // don't fully overwrite annotations in pod templates to support rolling restarts (K8s sets some extra annotation to track restart)
            // instead, merge it
            if (existingDeployment.getSpec() != null && existingDeployment.getSpec().getTemplate() != null) {
                mergeMaps(
                        Optional.ofNullable(existingDeployment.getSpec().getTemplate().getMetadata()).map(m -> m.getAnnotations()).orElse(null),
                        Optional.ofNullable(reconciledDeployment.getSpec().getTemplate().getMetadata()).map(m -> m.getAnnotations()).orElse(null),
                        annotations -> reconciledDeployment.getSpec().getTemplate().getMetadata().setAnnotations(annotations));
            }

            migrateDeployment(existingDeployment, reconciledDeployment);
        }

        return Optional.of(reconciledDeployment);
    }

    private StatefulSet fetchExistingDeployment() {
        return client
                .apps()
                .statefulSets()
                .inNamespace(getNamespace())
                .withName(getName())
                .get();
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
            overlayTemplate.getSpec().getContainers().size() > 0 &&
            overlayTemplate.getSpec().getContainers().get(0) != null &&
            overlayTemplate.getSpec().getContainers().get(0).getName() != null) {
            status.addWarningMessage("The name of the keycloak container cannot be modified");
        }

        if (overlayTemplate.getSpec() != null &&
            overlayTemplate.getSpec().getContainers() != null &&
            overlayTemplate.getSpec().getContainers().size() > 0 &&
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

    private void configureHostname(StatefulSet deployment) {
        var kcContainer = deployment.getSpec().getTemplate().getSpec().getContainers().get(0);
        var hostname = this.keycloakCR.getSpec().getHostname();
        var envVars =  kcContainer.getEnv();
        if (this.keycloakCR.getSpec().isHostnameDisabled()) {
            var disableStrictHostname = List.of(
                new EnvVarBuilder()
                        .withName("KC_HOSTNAME_STRICT")
                        .withValue("false")
                        .build(),
                new EnvVarBuilder()
                        .withName("KC_HOSTNAME_STRICT_BACKCHANNEL")
                        .withValue("false")
                        .build());

            envVars.addAll(disableStrictHostname);
        } else {
            var enabledStrictHostname = List.of(
                new EnvVarBuilder()
                        .withName("KC_HOSTNAME")
                        .withValue(hostname)
                        .build());

            envVars.addAll(enabledStrictHostname);
        }
    }

    private void configureTLS(StatefulSet deployment) {
        var kcContainer = deployment.getSpec().getTemplate().getSpec().getContainers().get(0);
        var tlsSecret = this.keycloakCR.getSpec().getTlsSecret();
        var envVars =  kcContainer.getEnv();

        if (this.keycloakCR.getSpec().isHttp()) {
            var disableTls = List.of(
                    new EnvVarBuilder()
                            .withName("KC_HTTP_ENABLED")
                            .withValue("true")
                            .build(),
                    new EnvVarBuilder()
                            .withName("KC_HOSTNAME_STRICT_HTTPS")
                            .withValue("false")
                            .build(),
                    new EnvVarBuilder()
                            .withName("KC_PROXY")
                            .withValue("edge")
                            .build());

            envVars.addAll(disableTls);
        } else {
            var enabledTls = List.of(
                    new EnvVarBuilder()
                            .withName("KC_HTTPS_CERTIFICATE_FILE")
                            .withValue(Constants.CERTIFICATES_FOLDER + "/tls.crt")
                            .build(),
                    new EnvVarBuilder()
                            .withName("KC_HTTPS_CERTIFICATE_KEY_FILE")
                            .withValue(Constants.CERTIFICATES_FOLDER + "/tls.key")
                            .build(),
                    new EnvVarBuilder()
                            .withName("KC_PROXY")
                            .withValue("passthrough")
                            .build());

            envVars.addAll(enabledTls);

            var volume = new VolumeBuilder()
                    .withName("keycloak-tls-certificates")
                    .withNewSecret()
                    .withSecretName(tlsSecret)
                    .withOptional(false)
                    .endSecret()
                    .build();

            var volumeMount = new VolumeMountBuilder()
                    .withName(volume.getName())
                    .withMountPath(Constants.CERTIFICATES_FOLDER)
                    .build();

            deployment.getSpec().getTemplate().getSpec().getVolumes().add(volume);
            kcContainer.getVolumeMounts().add(volumeMount);
        }

        var userRelativePath = readConfigurationValue(Constants.KEYCLOAK_HTTP_RELATIVE_PATH_KEY);
        var kcRelativePath = (userRelativePath == null) ? "" : userRelativePath;
        var protocol = (this.keycloakCR.getSpec().isHttp()) ? "http" : "https";
        var kcPort = (this.keycloakCR.getSpec().isHttp()) ? Constants.KEYCLOAK_HTTP_PORT : Constants.KEYCLOAK_HTTPS_PORT;

        var baseProbe = new ArrayList<>(List.of("curl", "--head", "--fail", "--silent"));

        if (!this.keycloakCR.getSpec().isHttp()) {
            baseProbe.add("--insecure");
        }

        var readyProbe = new ArrayList<>(baseProbe);
        readyProbe.add(protocol + "://127.0.0.1:" + kcPort + kcRelativePath + "/health/ready");
        var liveProbe = new ArrayList<>(baseProbe);
        liveProbe.add(protocol + "://127.0.0.1:" + kcPort + kcRelativePath + "/health/live");

        kcContainer
                .getReadinessProbe()
                .setExec(new ExecActionBuilder().withCommand(readyProbe).build());
        kcContainer
                .getLivenessProbe()
                .setExec(new ExecActionBuilder().withCommand(liveProbe).build());
    }

    public String readConfigurationValue(String key) {
        if (this.keycloakCR != null &&
                this.keycloakCR.getSpec() != null &&
                this.keycloakCR.getSpec().getServerConfiguration() != null
        ) {
            var serverConfigValue = this.keycloakCR
                    .getSpec()
                    .getServerConfiguration()
                    .stream()
                    .filter(sc -> sc.getName().equals(key))
                    .findFirst();
            if (serverConfigValue.isPresent()) {
                if (serverConfigValue.get().getValue() != null) {
                    return serverConfigValue.get().getValue();
                } else {
                    var secretSelector = serverConfigValue.get().getSecret();
                    if (secretSelector == null) {
                        throw new IllegalStateException("Secret " + serverConfigValue.get().getName() + " not defined");
                    }
                    var secret = client.secrets().inNamespace(getNamespace()).withName(secretSelector.getName()).get();
                    if (secret == null) {
                        throw new IllegalStateException("Secret " + secretSelector.getName() + " not found in cluster");
                    }
                    if (secret.getData().containsKey(secretSelector.getKey())) {
                        return new String(Base64.getDecoder().decode(secret.getData().get(secretSelector.getKey())), StandardCharsets.UTF_8);
                    } else {
                        throw new IllegalStateException("Secret " + secretSelector.getName() + " doesn't contain the expected key " + secretSelector.getKey());
                    }
                }
            } else {
                return null;
            }
        } else {
            return null;
        }
    }

    private StatefulSet createBaseDeployment() {
        StatefulSet baseDeployment = new StatefulSetBuilder()
                .withNewMetadata()
                .endMetadata()
                .withNewSpec()
                    .withNewSelector()
                        .addToMatchLabels("app", "")
                    .endSelector()
                    .withNewTemplate()
                        .withNewMetadata()
                            .addToLabels("app", "")
                        .endMetadata()
                        .withNewSpec()
                            .withRestartPolicy("Always")
                            .withTerminationGracePeriodSeconds(30L)
                            .withDnsPolicy("ClusterFirst")
                            .addNewContainer()
                                .withName("keycloak")
                                .withArgs("start")
                                .addNewPort()
                                    .withContainerPort(8443)
                                    .withProtocol("TCP")
                                .endPort()
                                .addNewPort()
                                    .withContainerPort(8080)
                                    .withProtocol("TCP")
                                .endPort()
                                .withNewReadinessProbe()
                                    .withInitialDelaySeconds(20)
                                    .withPeriodSeconds(2)
                                    .withFailureThreshold(250)
                                .endReadinessProbe()
                                .withNewLivenessProbe()
                                    .withInitialDelaySeconds(20)
                                    .withPeriodSeconds(2)
                                    .withFailureThreshold(150)
                                .endLivenessProbe()
                            .endContainer()
                        .endSpec()
                    .endTemplate()
                .endSpec()
                .build();

        baseDeployment.getMetadata().setName(getName());
        baseDeployment.getMetadata().setNamespace(getNamespace());
        baseDeployment.getSpec().getSelector().setMatchLabels(Constants.DEFAULT_LABELS);
        baseDeployment.getSpec().setReplicas(keycloakCR.getSpec().getInstances());
        baseDeployment.getSpec().getTemplate().getMetadata().setLabels(Constants.DEFAULT_LABELS);

        Container container = baseDeployment.getSpec().getTemplate().getSpec().getContainers().get(0);
        var customImage = Optional.ofNullable(keycloakCR.getSpec().getImage());
        container.setImage(customImage.orElse(config.keycloak().image()));

        if (customImage.isPresent()) {
            container.getArgs().add("--optimized");
        }

        container.setImagePullPolicy(config.keycloak().imagePullPolicy());

        container.setEnv(getEnvVars());

        configureHostname(baseDeployment);
        configureTLS(baseDeployment);
        mergePodTemplate(baseDeployment.getSpec().getTemplate());

        return baseDeployment;
    }

    private List<EnvVar> getEnvVars() {
        // default config values
        List<ValueOrSecret> serverConfig = Constants.DEFAULT_DIST_CONFIG.entrySet().stream()
                .map(e -> new ValueOrSecret(e.getKey(), e.getValue()))
                .collect(Collectors.toList());

        // merge with the CR; the values in CR take precedence
        if (keycloakCR.getSpec().getServerConfiguration() != null) {
            serverConfig.removeAll(keycloakCR.getSpec().getServerConfiguration());
            serverConfig.addAll(keycloakCR.getSpec().getServerConfiguration());
        }

        // set env vars
        serverConfigSecretsNames = new HashSet<>();
        List<EnvVar> envVars = serverConfig.stream()
                .map(v -> {
                    var envBuilder = new EnvVarBuilder().withName(getEnvVarName(v.getName()));
                    var secret = v.getSecret();
                    if (secret != null) {
                        envBuilder.withValueFrom(
                                new EnvVarSourceBuilder().withSecretKeyRef(secret).build());
                        serverConfigSecretsNames.add(secret.getName()); // for watching it later
                    } else {
                        envBuilder.withValue(v.getValue());
                    }
                    return envBuilder.build();
                })
                .collect(Collectors.toList());
        Log.infof("Found config secrets names: %s", serverConfigSecretsNames);

        envVars.add(
                new EnvVarBuilder()
                        .withName("KEYCLOAK_ADMIN")
                        .withNewValueFrom()
                        .withNewSecretKeyRef()
                        .withName(adminSecretName)
                        .withKey("username")
                        .withOptional(false)
                        .endSecretKeyRef()
                        .endValueFrom()
                        .build());
        envVars.add(
                new EnvVarBuilder()
                        .withName("KEYCLOAK_ADMIN_PASSWORD")
                        .withNewValueFrom()
                        .withNewSecretKeyRef()
                        .withName(adminSecretName)
                        .withKey("password")
                        .withOptional(false)
                        .endSecretKeyRef()
                        .endValueFrom()
                        .build());

        envVars.add(
                new EnvVarBuilder()
                        .withName("jgroups.dns.query")
                        .withValue(getName() + Constants.KEYCLOAK_DISCOVERY_SERVICE_SUFFIX +"." + getNamespace())
                        .build());

        return envVars;
    }

    public void updateStatus(KeycloakStatusBuilder status) {
        validatePodTemplate(status);
        if (existingDeployment == null) {
            status.addNotReadyMessage("No existing StatefulSet found, waiting for creating a new one");
            return;
        }

        if (existingDeployment.getStatus() == null
                || existingDeployment.getStatus().getReadyReplicas() == null
                || existingDeployment.getStatus().getReadyReplicas() < keycloakCR.getSpec().getInstances()) {
            status.addNotReadyMessage("Waiting for more replicas");
        }

        if (migrationInProgress) {
            status.addNotReadyMessage("Performing Keycloak upgrade, scaling down the deployment");
        } else if (existingDeployment.getStatus() != null
                && existingDeployment.getStatus().getCurrentRevision() != null
                && existingDeployment.getStatus().getUpdateRevision() != null
                && !existingDeployment.getStatus().getCurrentRevision().equals(existingDeployment.getStatus().getUpdateRevision())) {
            status.addRollingUpdateMessage("Rolling out deployment update");
        }
    }

    public Set<String> getConfigSecretsNames() {
        Set<String> ret = new HashSet<>(serverConfigSecretsNames);
        if (!keycloakCR.getSpec().isHttp()) {
            ret.add(keycloakCR.getSpec().getTlsSecret());
        }
        return ret;
    }

    @Override
    public String getName() {
        return keycloakCR.getMetadata().getName();
    }

    public void rollingRestart() {
        client.apps().statefulSets()
                .inNamespace(getNamespace())
                .withName(getName())
                .rolling().restart();
    }

    public void migrateDeployment(StatefulSet previousDeployment, StatefulSet reconciledDeployment) {
        if (previousDeployment == null
                || previousDeployment.getSpec() == null
                || previousDeployment.getSpec().getTemplate() == null
                || previousDeployment.getSpec().getTemplate().getSpec() == null
                || previousDeployment.getSpec().getTemplate().getSpec().getContainers() == null
                || previousDeployment.getSpec().getTemplate().getSpec().getContainers().get(0) == null)
        {
            return;
        }

        var previousContainer = previousDeployment.getSpec().getTemplate().getSpec().getContainers().get(0);
        var reconciledContainer = reconciledDeployment.getSpec().getTemplate().getSpec().getContainers().get(0);

        if (!previousContainer.getImage().equals(reconciledContainer.getImage())
                && previousDeployment.getStatus().getReplicas() > 1) {
            // TODO Check if migration is really needed (e.g. based on actual KC version); https://github.com/keycloak/keycloak/issues/10441
            Log.info("Detected changed Keycloak image, assuming Keycloak upgrade. Scaling down the deployment to one instance to perform a safe database migration");
            Log.infof("original image: %s; new image: %s");

            reconciledContainer.setImage(previousContainer.getImage());
            reconciledDeployment.getSpec().setReplicas(1);

            migrationInProgress = true;
        }
    }

    public static String getEnvVarName(String kcConfigName) {
        // TODO make this use impl from Quarkus dist (Configuration.toEnvVarFormat)
        return "KC_" + replaceNonAlphanumericByUnderscores(kcConfigName).toUpperCase();
    }
}

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

import io.fabric8.kubernetes.api.model.ContainerState;
import io.fabric8.kubernetes.api.model.ContainerStateWaiting;
import io.fabric8.kubernetes.api.model.EnvVar;
import io.fabric8.kubernetes.api.model.EnvVarBuilder;
import io.fabric8.kubernetes.api.model.EnvVarSourceBuilder;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.PodSpec;
import io.fabric8.kubernetes.api.model.PodSpecFluent.ContainersNested;
import io.fabric8.kubernetes.api.model.PodStatus;
import io.fabric8.kubernetes.api.model.PodTemplateSpec;
import io.fabric8.kubernetes.api.model.PodTemplateSpecFluent.SpecNested;
import io.fabric8.kubernetes.api.model.apps.StatefulSet;
import io.fabric8.kubernetes.api.model.apps.StatefulSetBuilder;
import io.fabric8.kubernetes.api.model.apps.StatefulSetSpecFluent.TemplateNested;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.readiness.Readiness;
import io.fabric8.kubernetes.client.utils.Serialization;
import io.quarkus.logging.Log;

import org.keycloak.common.util.CollectionUtil;
import org.keycloak.operator.Config;
import org.keycloak.operator.Constants;
import org.keycloak.operator.Utils;
import org.keycloak.operator.crds.v2alpha1.deployment.Keycloak;
import org.keycloak.operator.crds.v2alpha1.deployment.KeycloakSpec;
import org.keycloak.operator.crds.v2alpha1.deployment.KeycloakStatusAggregator;
import org.keycloak.operator.crds.v2alpha1.deployment.ValueOrSecret;
import org.keycloak.operator.crds.v2alpha1.deployment.spec.UnsupportedSpec;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.keycloak.operator.crds.v2alpha1.CRDUtils.isTlsConfigured;

public class KeycloakDeployment extends OperatorManagedResource implements StatusUpdater<KeycloakStatusAggregator> {

    private final Config operatorConfig;
    private final KeycloakDistConfigurator distConfigurator;

    private final Keycloak keycloakCR;
    private final StatefulSet existingDeployment;
    private final StatefulSet baseDeployment;
    private final String adminSecretName;

    private Set<String> serverConfigSecretsNames;

    private boolean migrationInProgress;

    public KeycloakDeployment(KubernetesClient client, Config config, Keycloak keycloakCR, StatefulSet existingDeployment, String adminSecretName) {
        super(client, keycloakCR);
        this.operatorConfig = config;
        this.keycloakCR = keycloakCR;
        this.adminSecretName = adminSecretName;
        this.existingDeployment = existingDeployment;
        this.baseDeployment = createBaseDeployment();
        this.distConfigurator = new KeycloakDistConfigurator(keycloakCR, baseDeployment, client);
        this.distConfigurator.configureDistOptions();
        // after the distConfiguration, we can add the remaining default / additionalConfig
        addRemainingEnvVars();
    }

    @Override
    public Optional<HasMetadata> getReconciledResource() {
        if (existingDeployment == null) {
            Log.info("No existing Deployment found, using the default");
        }
        else {
            Log.info("Existing Deployment found, handling migration");

            if (!existingDeployment.isMarkedForDeletion() && !hasExpectedMatchLabels(existingDeployment)) {
                client.resource(existingDeployment).lockResourceVersion().delete();
                Log.info("Existing Deployment found with old label selector, it will be recreated");
            }

            migrateDeployment(existingDeployment, baseDeployment);
        }
        return Optional.of(baseDeployment);
    }

    private boolean hasExpectedMatchLabels(StatefulSet statefulSet) {
        return Optional.ofNullable(statefulSet).map(s -> getInstanceLabels().equals(s.getSpec().getSelector().getMatchLabels())).orElse(true);
    }

    public void validatePodTemplate(KeycloakStatusAggregator status) {
        var spec = getPodTemplateSpec();
        if (spec.isEmpty()) {
            return;
        }
        var overlayTemplate = spec.orElseThrow();

        if (overlayTemplate.getMetadata() != null) {
            if (overlayTemplate.getMetadata().getName() != null) {
                status.addWarningMessage("The name of the podTemplate cannot be modified");
            }
            if (overlayTemplate.getMetadata().getNamespace() != null) {
                status.addWarningMessage("The namespace of the podTemplate cannot be modified");
            }
        }

        Optional.ofNullable(overlayTemplate.getSpec()).map(PodSpec::getContainers).flatMap(l -> l.stream().findFirst())
                .ifPresent(container -> {
                    if (container.getName() != null) {
                        status.addWarningMessage("The name of the keycloak container cannot be modified");
                    }
                    if (container.getImage() != null) {
                        status.addWarningMessage(
                                "The image of the keycloak container cannot be modified using podTemplate");
                    }
                });

        if (overlayTemplate.getSpec() != null &&
            CollectionUtil.isNotEmpty(overlayTemplate.getSpec().getImagePullSecrets())) {
            status.addWarningMessage("The imagePullSecrets of the keycloak container cannot be modified using podTemplate");
        }
    }

    private Optional<PodTemplateSpec> getPodTemplateSpec() {
        return Optional.ofNullable(keycloakCR.getSpec()).map(KeycloakSpec::getUnsupported).map(UnsupportedSpec::getPodTemplate);
    }

    private StatefulSet createBaseDeployment() {
        Map<String, String> labels = getInstanceLabels();
        if (operatorConfig.keycloak().podLabels() != null) {
            labels.putAll(operatorConfig.keycloak().podLabels());
        }

        /* Create a builder for the statefulset, note that the pod template spec is used as the basis
         * over that some values are forced, others will let the template override, others merge
         */

        StatefulSetBuilder baseDeploymentBuilder = new StatefulSetBuilder()
                .withNewMetadata()
                    .withName(getName())
                    .withNamespace(getNamespace())
                .endMetadata()
                .withNewSpec()
                    .withNewSelector()
                        .withMatchLabels(getInstanceLabels())
                    .endSelector()
                    .withNewTemplateLike(getPodTemplateSpec().orElseGet(PodTemplateSpec::new))
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

        // there isn't currently an editOrNewFirstContainer, so we need to do this manually
        ContainersNested<SpecNested<TemplateNested<io.fabric8.kubernetes.api.model.apps.StatefulSetFluent.SpecNested<StatefulSetBuilder>>>> containerBuilder = null;
        if (specBuilder.buildContainers().isEmpty()) {
            containerBuilder = specBuilder.addNewContainer();
        } else {
            containerBuilder = specBuilder.editFirstContainer();
        }

        containerBuilder.withName("keycloak");

        var customImage = Optional.ofNullable(keycloakCR.getSpec().getImage());
        containerBuilder.withImage(customImage.orElse(operatorConfig.keycloak().image()));

        if (!containerBuilder.hasImagePullPolicy()) {
            containerBuilder.withImagePullPolicy(operatorConfig.keycloak().imagePullPolicy());
        }
        if (Optional.ofNullable(containerBuilder.getArgs()).orElse(List.of()).isEmpty()) {
            containerBuilder.withArgs("start");
        }
        if (customImage.isPresent()) {
            containerBuilder.addToArgs("--optimized");
        }

        // probes
        var tlsConfigured = isTlsConfigured(keycloakCR);
        var protocol = !tlsConfigured ? "HTTP" : "HTTPS";
        var kcPort = KeycloakService.getServicePort(keycloakCR);

        // Relative path ends with '/'
        var kcRelativePath = Optional.ofNullable(readConfigurationValue(Constants.KEYCLOAK_HTTP_RELATIVE_PATH_KEY))
                .map(path -> !path.endsWith("/") ? path + "/" : path)
                .orElse("/");

        if (!containerBuilder.hasReadinessProbe()) {
            containerBuilder.withNewReadinessProbe()
                .withInitialDelaySeconds(20)
                .withPeriodSeconds(2)
                .withFailureThreshold(250)
                .withNewHttpGet()
                .withScheme(protocol)
                .withNewPort(kcPort)
                .withPath(kcRelativePath + "health/ready")
                .endHttpGet()
                .endReadinessProbe();
        }
        if (!containerBuilder.hasLivenessProbe()) {
            containerBuilder.withNewLivenessProbe()
                .withInitialDelaySeconds(20)
                .withPeriodSeconds(2)
                .withFailureThreshold(150)
                .withNewHttpGet()
                .withScheme(protocol)
                .withNewPort(kcPort)
                .withPath(kcRelativePath + "health/live")
                .endHttpGet()
                .endLivenessProbe();
        }

        // add in ports - there's no merging being done here
        StatefulSet baseDeployment = containerBuilder
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
            .endContainer().endSpec().endTemplate().endSpec().build();

        return baseDeployment;
    }

    private void addRemainingEnvVars() {
        // add in the remaining envVars, but only if they are not already set
        var envVars = getEnvVars();
        var env = baseDeployment.getSpec().getTemplate().getSpec().getContainers().get(0).getEnv();
        if (env != null && !env.isEmpty()) {
            // this is also a final rationalization of whatever was added first by any previous manipulation wins
            envVars = new ArrayList<>(Stream.concat(env.stream(), envVars.stream())
                    .collect(Collectors.toMap(EnvVar::getName, Function.identity(), (e1, e2) -> e1, LinkedHashMap::new))
                    .values());
        }
        baseDeployment.getSpec().getTemplate().getSpec().getContainers().get(0).setEnv(envVars);
    }

    private List<EnvVar> getEnvVars() {
        // default config values
        List<ValueOrSecret> serverConfigsList = new ArrayList<>(Constants.DEFAULT_DIST_CONFIG_LIST);

        // merge with the CR; the values in CR take precedence
        if (keycloakCR.getSpec().getAdditionalOptions() != null) {
            Set<String> inCr = keycloakCR.getSpec().getAdditionalOptions().stream().map(v -> v.getName()).collect(Collectors.toSet());
            serverConfigsList.removeIf(v -> inCr.contains(v.getName()));
            serverConfigsList.addAll(keycloakCR.getSpec().getAdditionalOptions());
        }

        // set env vars
        serverConfigSecretsNames = new HashSet<>();
        List<EnvVar> envVars = serverConfigsList.stream()
                .map(v -> {
                    var envBuilder = new EnvVarBuilder().withName(KeycloakDistConfigurator.getKeycloakOptionEnvVarName(v.getName()));
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

    @Override
    public void updateStatus(KeycloakStatusAggregator status) {
        status.apply(b -> b.withSelector(Utils.toSelectorString(getInstanceLabels())));
        validatePodTemplate(status);
        if (existingDeployment == null) {
            status.addNotReadyMessage("No existing StatefulSet found, waiting for creating a new one");
            return;
        }

        if (existingDeployment.getStatus() == null) {
            status.addNotReadyMessage("Waiting for deployment status");
        } else {
            status.apply(b -> b.withInstances(existingDeployment.getStatus().getReadyReplicas()));
            if (Optional.ofNullable(existingDeployment.getStatus().getReadyReplicas()).orElse(0) < keycloakCR.getSpec().getInstances()) {
                checkForPodErrors(status);
                status.addNotReadyMessage("Waiting for more replicas");
            }
        }

        if (migrationInProgress) {
            status.addNotReadyMessage("Performing Keycloak upgrade, scaling down the deployment");
        } else if (existingDeployment.getStatus() != null
                && existingDeployment.getStatus().getCurrentRevision() != null
                && existingDeployment.getStatus().getUpdateRevision() != null
                && !existingDeployment.getStatus().getCurrentRevision().equals(existingDeployment.getStatus().getUpdateRevision())) {
            status.addRollingUpdateMessage("Rolling out deployment update");
        }

        distConfigurator.validateOptions(status);
    }

    private void checkForPodErrors(KeycloakStatusAggregator status) {
        client.pods().inNamespace(existingDeployment.getMetadata().getNamespace())
                .withLabel("controller-revision-hash", existingDeployment.getStatus().getUpdateRevision())
                .withLabels(getInstanceLabels())
                .list().getItems().stream()
                .filter(p -> !Readiness.isPodReady(p)
                        && Optional.ofNullable(p.getStatus()).map(PodStatus::getContainerStatuses).isPresent())
                .sorted((p1, p2) -> p1.getMetadata().getName().compareTo(p2.getMetadata().getName()))
                .forEachOrdered(p -> {
                    Optional.of(p.getStatus()).map(s -> s.getContainerStatuses()).stream().flatMap(List::stream)
                            .filter(cs -> !Boolean.TRUE.equals(cs.getReady()))
                            .sorted((cs1, cs2) -> cs1.getName().compareTo(cs2.getName())).forEachOrdered(cs -> {
                                if (Optional.ofNullable(cs.getState()).map(ContainerState::getWaiting)
                                        .map(ContainerStateWaiting::getReason).map(String::toLowerCase)
                                        .filter(s -> s.contains("err") || s.equals("crashloopbackoff")).isPresent()) {
                                    Log.infof("Found unhealthy container on pod %s/%s: %s",
                                            p.getMetadata().getNamespace(), p.getMetadata().getName(),
                                            Serialization.asYaml(cs));
                                    status.addErrorMessage(
                                            String.format("Waiting for %s/%s due to %s: %s", p.getMetadata().getNamespace(),
                                                    p.getMetadata().getName(), cs.getState().getWaiting().getReason(),
                                                    cs.getState().getWaiting().getMessage()));
                                }
                            });
                });
    }

    public Set<String> getConfigSecretsNames() {
        Set<String> ret = new HashSet<>(serverConfigSecretsNames);
        ret.addAll(distConfigurator.getSecretNames());
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
                && previousDeployment.getStatus().getReplicas() > 0) {
            // TODO Check if migration is really needed (e.g. based on actual KC version); https://github.com/keycloak/keycloak/issues/10441
            Log.info("Detected changed Keycloak image, assuming Keycloak upgrade. Scaling down the deployment to one instance to perform a safe database migration");
            Log.infof("original image: %s; new image: %s", previousContainer.getImage(), reconciledContainer.getImage());

            reconciledContainer.setImage(previousContainer.getImage());
            reconciledDeployment.getSpec().setReplicas(0);

            migrationInProgress = true;
        }
    }

    protected String readConfigurationValue(String key) {
        if (keycloakCR != null &&
                keycloakCR.getSpec() != null &&
                keycloakCR.getSpec().getAdditionalOptions() != null
        ) {

            var serverConfigValue = keycloakCR
                    .getSpec()
                    .getAdditionalOptions()
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
                    var secret = client.secrets().inNamespace(keycloakCR.getMetadata().getNamespace()).withName(secretSelector.getName()).get();
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
}

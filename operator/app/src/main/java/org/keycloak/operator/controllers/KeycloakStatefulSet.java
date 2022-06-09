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
import io.fabric8.kubernetes.api.model.apps.Deployment;
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

public class KeycloakStatefulSet extends AbstractKeycloak<StatefulSet> {

    public KeycloakStatefulSet(KubernetesClient client, Config config, Keycloak keycloakCR, StatefulSet existing, String adminSecretName) {
        super(client, config, keycloakCR, existing, adminSecretName);
    }

    @Override
    public Optional<HasMetadata> getReconciledResource() {
        StatefulSet baseStatefulSet = new StatefulSetBuilder(this.base).build(); // clone not to change the base template
        StatefulSet reconciledStatefulSet;
        if (existing == null) {
            Log.info("No existing StatefulSet found, using the default");
            reconciledStatefulSet = baseStatefulSet;
        } else {
            Log.info("Existing StatefulSet found, updating specs");
            reconciledStatefulSet = new StatefulSetBuilder(existing).build();

            // don't overwrite metadata, just specs
            reconciledStatefulSet.setSpec(baseStatefulSet.getSpec());

            // don't overwrite annotations in pod templates to support rolling restarts
            if (existing.getSpec() != null && existing.getSpec().getTemplate() != null) {
                mergeMaps(
                        Optional.ofNullable(reconciledStatefulSet.getSpec().getTemplate().getMetadata()).map(m -> m.getAnnotations()).orElse(null),
                        Optional.ofNullable(existing.getSpec().getTemplate().getMetadata()).map(m -> m.getAnnotations()).orElse(null),
                        annotations -> reconciledStatefulSet.getSpec().getTemplate().getMetadata().setAnnotations(annotations));
            }
        }

        return Optional.of(reconciledStatefulSet);
    }

    @Override
    protected StatefulSet fetchExisting() {
        return client
                .apps()
                .statefulSets()
                .inNamespace(getNamespace())
                .withName(getName())
                .get();
    }

    @Override
    protected StatefulSet createBase() {
        StatefulSet baseStatefulSet = new StatefulSetBuilder()
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

        baseStatefulSet.getMetadata().setName(getName());
        baseStatefulSet.getMetadata().setNamespace(getNamespace());
        baseStatefulSet.getSpec().getSelector().setMatchLabels(Constants.DEFAULT_LABELS);
        baseStatefulSet.getSpec().setReplicas(keycloakCR.getSpec().getInstances());
        baseStatefulSet.getSpec().getTemplate().getMetadata().setLabels(Constants.DEFAULT_LABELS);

        Container container = baseStatefulSet.getSpec().getTemplate().getSpec().getContainers().get(0);
        var customImage = Optional.ofNullable(keycloakCR.getSpec().getImage());
        container.setImage(customImage.orElse(config.keycloak().image()));
        if (customImage.isEmpty()) {
            container.getArgs().add("--auto-build");
        }

        container.setImagePullPolicy(config.keycloak().imagePullPolicy());

        container.setEnv(getEnvVars());

        configureHostname(container);
        configureTLS(baseStatefulSet.getSpec().getTemplate());
        mergePodTemplate(baseStatefulSet.getSpec().getTemplate());

        return baseStatefulSet;
    }

    public void updateStatus(KeycloakStatusBuilder status) {
        validatePodTemplate(status);
        if (existing == null) {
            status.addNotReadyMessage("No existing StatefulSet found, waiting for creating a new one");
            return;
        }

        var replicaFailure = existing.getStatus().getConditions().stream()
                .filter(d -> d.getType().equals("ReplicaFailure")).findFirst();
        if (replicaFailure.isPresent()) {
            status.addNotReadyMessage("StatefulSet failures");
            status.addErrorMessage("StatefulSet failure: " + replicaFailure.get());
            return;
        }

        if (existing.getStatus() == null
                || existing.getStatus().getReadyReplicas() == null
                || existing.getStatus().getReadyReplicas() < keycloakCR.getSpec().getInstances()) {
            status.addNotReadyMessage("Waiting for more replicas");
        }

        var progressing = existing.getStatus().getConditions().stream()
                .filter(c -> c.getType().equals("Progressing")).findFirst();
        progressing.ifPresent(p -> {
            String reason = p.getReason();
            // https://kubernetes.io/docs/concepts/workloads/controllers/deployment/#progressing-deployment
            if (p.getStatus().equals("True") &&
                    (reason.equals("NewReplicaSetCreated") || reason.equals("FoundNewReplicaSet") || reason.equals("ReplicaSetUpdated"))) {
                status.addRollingUpdateMessage("Rolling out statefulset update");
            }
        });
    }

    @Override
    public void rollingRestart() {
        client.apps().statefulSets()
                .inNamespace(getNamespace())
                .withName(getName())
                .rolling().restart();
    }

}

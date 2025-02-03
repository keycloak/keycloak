/*
 * Copyright 2025 Red Hat, Inc. and/or its affiliates
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

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import io.fabric8.kubernetes.api.model.ContainerFluent;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.fabric8.kubernetes.api.model.PodSpec;
import io.fabric8.kubernetes.api.model.PodSpecBuilder;
import io.fabric8.kubernetes.api.model.Volume;
import io.fabric8.kubernetes.api.model.batch.v1.Job;
import io.fabric8.kubernetes.api.model.batch.v1.JobBuilder;
import io.fabric8.kubernetes.api.model.batch.v1.JobSpecFluent;
import io.javaoperatorsdk.operator.api.config.informer.InformerConfiguration;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.CRUDKubernetesDependentResource;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.KubernetesDependentResourceConfigBuilder;
import jakarta.enterprise.context.ApplicationScoped;
import org.keycloak.operator.Constants;
import org.keycloak.operator.ContextUtils;
import org.keycloak.operator.Utils;
import org.keycloak.operator.crds.v2alpha1.CRDUtils;
import org.keycloak.operator.crds.v2alpha1.deployment.Keycloak;

@ApplicationScoped
public class KeycloakUpdateJobDependentResource extends CRUDKubernetesDependentResource<Job, Keycloak> {

    // shared volume configuration
    private static final String WORK_DIR_VOLUME_NAME = "workdir";
    private static final String WORK_DIR_VOLUME_MOUNT_PATH = "/mnt/workdir";
    private static final String UPDATES_FILE_PATH = WORK_DIR_VOLUME_MOUNT_PATH + "/updates.json";

    // Annotations
    public static final String KEYCLOAK_CR_HASH_ANNOTATION = "operator.keycloak.org/keycloak-hash";

    // container configuration
    private static final String INIT_CONTAINER_NAME = "actual";
    private static final String CONTAINER_NAME = "desired";
    private static final List<String> INIT_CONTAINER_ARGS = List.of("update-compatibility", "metadata", "--file", UPDATES_FILE_PATH);
    private static final List<String> CONTAINER_ARGS = List.of("update-compatibility", "check", "--file", UPDATES_FILE_PATH);

    // Job and Pod defaults
    // Pod is restarted if it fails with an exit code != 0, and we don't want that.
    private static final int JOB_RETRIES = 0;
    // Job time to live
    private static final int JOB_TIME_TO_LIVE_SECONDS = (int) TimeUnit.MINUTES.toSeconds(30);

    // container args to replace
    private static final Set<String> START_ARGS = Set.of("start", "start-dev");

    public KeycloakUpdateJobDependentResource() {
        super(Job.class);
        this.configureWith(new KubernetesDependentResourceConfigBuilder<Job>()
                .withKubernetesDependentInformerConfig(InformerConfiguration.builder(resourceType())
                        .withLabelSelector(Constants.DEFAULT_LABELS_AS_STRING)
                        .build())
                .build());
    }

    @Override
    protected Job desired(Keycloak primary, Context<Keycloak> context) {
        var builder = new JobBuilder();
        builder.withMetadata(createMetadata(jobName(primary), primary));
        var specBuilder = builder.withNewSpec();
        addPodSpecTemplate(specBuilder, primary, context);
        // we don't need retries; we use exit code != 1 to signal the upgrade decision.
        specBuilder.withBackoffLimit(JOB_RETRIES);
        // Remove the job after 30 minutes.
        specBuilder.withTtlSecondsAfterFinished(JOB_TIME_TO_LIVE_SECONDS);
        specBuilder.endSpec();
        return builder.build();
    }

    public static boolean isJobFromCurrentKeycloakCr(Job job, Keycloak keycloak) {
        var annotations = job.getMetadata().getAnnotations();
        var hash = annotations.get(KEYCLOAK_CR_HASH_ANNOTATION);
        return Objects.equals(hash, keycloakHash(keycloak));
    }

    public static String jobName(Keycloak keycloak) {
        return keycloak.getMetadata().getName() + "-update-job";
    }

    private static String podName(Keycloak keycloak) {
        return keycloak.getMetadata().getName() + "-update-pod";
    }

    private static ObjectMeta createMetadata(String name, Keycloak keycloak) {
        var builder = new ObjectMetaBuilder();
        builder.withName(name)
                .withNamespace(keycloak.getMetadata().getNamespace())
                .withLabels(Utils.allInstanceLabels(keycloak))
                .withAnnotations(Map.of(KEYCLOAK_CR_HASH_ANNOTATION, keycloakHash(keycloak)));
        return builder.build();
    }

    private void addPodSpecTemplate(JobSpecFluent<?> builder, Keycloak keycloak, Context<Keycloak> context) {
        var podTemplate = builder.withNewTemplate();
        podTemplate.withMetadata(createMetadata(podName(keycloak), keycloak));
        podTemplate.withSpec(createPodSpec(context));
        podTemplate.endTemplate();
    }

    private PodSpec createPodSpec(Context<Keycloak> context) {
        var allVolumes = getAllVolumes(context);
        Collection<String> requiredVolumes = new HashSet<>();
        var builder = new PodSpecBuilder();
        builder.withRestartPolicy("Never");
        addInitContainer(builder, context, allVolumes.keySet(), requiredVolumes);
        addContainer(builder, context, allVolumes.keySet(), requiredVolumes);
        builder.addNewVolume()
                .withName(WORK_DIR_VOLUME_NAME)
                .withNewEmptyDir()
                .endEmptyDir()
                .endVolume();
        // add volumes to the pod
        requiredVolumes.stream()
                .map(allVolumes::get)
                .forEach(volume -> builder.addNewVolumeLike(volume).endVolume());
        // For test KeycloakDeploymentTest#testDeploymentDurability
        // it uses a pause image, which never ends.
        // After this seconds, the job is terminated allowing the test to complete.
        builder.withActiveDeadlineSeconds(ContextUtils.getOperatorConfig(context).keycloak().updatePodDeadlineSeconds());
        return builder.build();
    }

    private static void addInitContainer(PodSpecBuilder builder, Context<Keycloak> context, Collection<String> availableVolumes, Collection<String> requiredVolumes) {
        var existing = CRDUtils.firstContainerOf(ContextUtils.getCurrentStatefulSet(context).orElseThrow()).orElseThrow();
        var containerBuilder = builder.addNewInitContainerLike(existing);
        configureContainer(containerBuilder, INIT_CONTAINER_NAME, INIT_CONTAINER_ARGS, availableVolumes, requiredVolumes);
        containerBuilder.endInitContainer();
    }

    private static void addContainer(PodSpecBuilder builder, Context<Keycloak> context, Collection<String> availableVolumes, Collection<String> requiredVolumes) {
        var existing = CRDUtils.firstContainerOf(ContextUtils.getDesiredStatefulSet(context)).orElseThrow();
        var containerBuilder = builder.addNewContainerLike(existing);
        configureContainer(containerBuilder, CONTAINER_NAME, CONTAINER_ARGS, availableVolumes, requiredVolumes);
        containerBuilder.endContainer();
    }

    private static void configureContainer(ContainerFluent<?> containerBuilder, String name, List<String> args, Collection<String> availableVolumes, Collection<String> requiredVolumes) {
        containerBuilder.withName(name);
        containerBuilder.withArgs(replaceStartWithUpdateCommand(containerBuilder.getArgs(), args));

        // remove volume devices
        containerBuilder.withVolumeDevices();

        // add existing volume mounts
        var volumeMounts = containerBuilder.buildVolumeMounts();
        if (volumeMounts != null) {
            var newVolumeMounts = volumeMounts.stream()
                    .filter(volumeMount -> availableVolumes.contains(volumeMount.getName()))
                    .filter(volumeMount -> !volumeMount.getName().startsWith("kube-api"))
                    .peek(volumeMount -> requiredVolumes.add(volumeMount.getName()))
                    .toList();
            containerBuilder.withVolumeMounts(newVolumeMounts);
        }

        // remove restart policy and probes
        containerBuilder.withRestartPolicy(null);
        containerBuilder.withReadinessProbe(null);
        containerBuilder.withLivenessProbe(null);
        containerBuilder.withStartupProbe(null);

        // add the shared volume
        containerBuilder.addNewVolumeMount()
                .withName(WORK_DIR_VOLUME_NAME)
                .withMountPath(WORK_DIR_VOLUME_MOUNT_PATH)
                .endVolumeMount();
    }

    private Map<String, Volume> getAllVolumes(Context<Keycloak> context) {
        Map<String, Volume> allVolumes = new HashMap<>();
        Consumer<Volume> volumeConsumer = volume -> allVolumes.put(volume.getName(), volume);
        CRDUtils.volumesFromStatefulSet(ContextUtils.getCurrentStatefulSet(context).orElseThrow()).forEach(volumeConsumer);
        CRDUtils.volumesFromStatefulSet(ContextUtils.getDesiredStatefulSet(context)).forEach(volumeConsumer);
        return allVolumes;
    }


    private static List<String> replaceStartWithUpdateCommand(List<String> currentArgs, List<String> updateArgs) {
        return currentArgs.stream().
                <String>mapMulti((arg, downstream) -> {
            if (START_ARGS.contains(arg)) {
                updateArgs.forEach(downstream);
                return;
            }
            downstream.accept(arg);
        }).toList();
    }

    private static String keycloakHash(Keycloak keycloak) {
        return Utils.hash(List.of(keycloak.getSpec()));
    }

}

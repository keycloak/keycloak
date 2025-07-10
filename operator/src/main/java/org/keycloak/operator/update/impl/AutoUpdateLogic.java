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

package org.keycloak.operator.update.impl;

import java.util.Collection;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

import io.fabric8.kubernetes.api.model.ContainerState;
import io.fabric8.kubernetes.api.model.ContainerStateTerminated;
import io.fabric8.kubernetes.api.model.ContainerStatus;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodStatus;
import io.fabric8.kubernetes.api.model.batch.v1.Job;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.api.reconciler.UpdateControl;
import io.quarkus.logging.Log;
import org.keycloak.operator.controllers.KeycloakUpdateJobDependentResource;
import org.keycloak.operator.crds.v2alpha1.CRDUtils;
import org.keycloak.operator.crds.v2alpha1.deployment.Keycloak;

public class AutoUpdateLogic extends BaseUpdateLogic {

    private final KeycloakUpdateJobDependentResource updateJobResource;

    public AutoUpdateLogic(Context<Keycloak> context, Keycloak keycloak, KeycloakUpdateJobDependentResource updateJobResource) {
        super(context, keycloak);
        this.updateJobResource = updateJobResource;
    }

    @Override
    Optional<UpdateControl<Keycloak>> onUpdate() {
        var existingJob = context.getSecondaryResource(Job.class);
        if (existingJob.isEmpty()) {
            updateJobResource.reconcile(keycloak, context);
            Log.debug("Creating Update Job");
            return Optional.of(UpdateControl.noUpdate());
        }

        // Keycloak CR may be updated while the job is running; we need to delete and start over.
        if (!KeycloakUpdateJobDependentResource.isJobFromCurrentKeycloakCr(existingJob.get(), keycloak)) {
            context.getClient().resource(existingJob.get()).lockResourceVersion().delete();
            return Optional.of(UpdateControl.noUpdate());
        }

        if (isJobRunning(existingJob.get())) {
            Log.debug("Update Job is running. Waiting until terminated.");
            return Optional.of(UpdateControl.noUpdate());
        }

        var pod = findPodForJob(context.getClient(), existingJob.get());
        if (pod.isEmpty()) {
            // TODO some cases the pod is removed. Do we start over or use recreate update?
            Log.warn("Pod for Update Job not found.");
            decideRecreateUpdate("The Pod running update-compatibility command not found.");
            return Optional.empty();
        }

        checkUpdateType(pod.get());
        return Optional.empty();
    }

    private boolean isJobRunning(Job job) {
        var status = job.getStatus();
        Log.debugf("Update Job Status:%n%s", CRDUtils.toJsonNode(status, context).toPrettyString());
        var completed = Optional.ofNullable(status).stream()
                .mapMultiToInt((jobStatus, downstream) -> {
                    if (jobStatus.getSucceeded() != null) {
                        downstream.accept(jobStatus.getSucceeded());
                    }
                    if (jobStatus.getFailed() != null) {
                        downstream.accept(jobStatus.getFailed());
                    }
                }).sum();
        // we only have a single pod, so completed will be zero if running or 1 if finished.
        return completed == 0;
    }

    private void checkUpdateType(Pod pod) {
        // check init container.
        var initContainerExitCode = initContainer(pod)
                .map(AutoUpdateLogic::exitCode);
        if (initContainerExitCode.isEmpty()) {
            Log.warn("InitContainer not found for Update Job.");
            decideRecreateUpdate("InitContainer running update-compatibility command not found. Did it crash? Check update job for details.");
            return;
        }
        if (initContainerExitCode.get() != 0) {
            if (initContainerExitCode.get() == 4) {
                Log.warn("Feature 'rolling-update' not enabled.");
                decideRecreateUpdate("Feature 'rolling-update' not enabled.");
                return;
            }
            Log.warn("InitContainer unexpectedly failed for Update Job.");
            decideRecreateUpdate("Unexpected update-compatibility command exit code (%s). Check update job for details.".formatted(initContainerExitCode.get()));
            return;
        }

        // check container.
        var containerExitCode = container(pod)
                .map(AutoUpdateLogic::exitCode);
        if (containerExitCode.isEmpty()) {
            Log.warn("Container not found for Update Job.");
            decideRecreateUpdate("Container running update-compatibility command not found. Did it crash?");
            return;
        }
        switch (containerExitCode.get()) {
            case 0: {
                decideRollingUpdate("Compatible changes detected.");
                return;
            }
            case 1: {
                Log.warn("Container has an unexpected error for Update Job");
                decideRecreateUpdate("Unexpected update-compatibility command error. Check update job for details.");
                return;
            }
            case 2: {
                Log.warn("Container has an invalid arguments for Update Job.");
                decideRecreateUpdate("Invalid arguments in update-compatibility command. Check update job for details.");
                return;
            }
            case 3: {
                Log.warn("Rolling Update not possible.");
                decideRecreateUpdate("Incompatible changes detected. Check update job for details.");
                return;
            }
            case 4: {
                Log.warn("Feature 'rolling-update' not enabled.");
                decideRecreateUpdate("Feature 'rolling-update' not enabled.");
                return;
            }
            default: {
                Log.warnf("Unexpected Update Job exit code: %s", containerExitCode.get());
                decideRecreateUpdate("Unexpected update-compatibility command exit code (%s). Check update job for details.".formatted(containerExitCode.get()));
            }
        }
    }

    public static Optional<Pod> findPodForJob(KubernetesClient client, Job job) {
        return client.pods()
                .inNamespace(job.getMetadata().getNamespace())
                .withLabelSelector(Objects.requireNonNull(job.getSpec().getSelector()))
                .list()
                .getItems()
                .stream()
                .findFirst();
    }

    private static Optional<ContainerStatus> initContainer(Pod pod) {
        return java.util.Optional.ofNullable(pod.getStatus())
                .map(PodStatus::getInitContainerStatuses)
                .map(Collection::stream)
                .flatMap(Stream::findFirst);
    }

    public static Optional<ContainerStatus> container(Pod pod) {
        return java.util.Optional.ofNullable(pod.getStatus())
                .map(PodStatus::getContainerStatuses)
                .map(Collection::stream)
                .flatMap(Stream::findFirst);
    }

    public static int exitCode(ContainerStatus containerStatus) {
        return Optional.ofNullable(containerStatus)
                .map(ContainerStatus::getState)
                .map(ContainerState::getTerminated)
                .map(ContainerStateTerminated::getExitCode)
                .orElse(1);
    }

}

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

import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

import org.keycloak.operator.Constants;
import org.keycloak.operator.ContextUtils;
import org.keycloak.operator.controllers.KeycloakUpdateJobDependentResource;
import org.keycloak.operator.crds.v2beta1.CRDUtils;
import org.keycloak.operator.crds.v2beta1.deployment.Keycloak;

import io.fabric8.kubernetes.api.model.ContainerState;
import io.fabric8.kubernetes.api.model.ContainerStateTerminated;
import io.fabric8.kubernetes.api.model.ContainerStatus;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodStatus;
import io.fabric8.kubernetes.api.model.apps.StatefulSet;
import io.fabric8.kubernetes.api.model.apps.StatefulSetSpec;
import io.fabric8.kubernetes.api.model.batch.v1.Job;
import io.fabric8.kubernetes.api.model.batch.v1.JobCondition;
import io.fabric8.kubernetes.api.model.batch.v1.JobStatus;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.api.reconciler.UpdateControl;
import io.quarkus.logging.Log;

public class AutoUpdateLogic extends BaseUpdateLogic {

    /**
     * How long a finished Update Job without a compatibility result is kept before it is replaced by a new one.
     * Each attempt starts Keycloak twice, so the delay limits the load while the check keeps failing. It must be shorter
     * than {@link KeycloakUpdateJobDependentResource#JOB_TIME_TO_LIVE_SECONDS}, as it is measured from the finished job.
     */
    static final Duration RETRY_DELAY = Duration.ofMinutes(5);

    static final String NO_INSTANCES_REASON = "No instances to keep available.";

    private static final String RETRY_MESSAGE_PREFIX = "Update compatibility check did not produce a result, the update job will be retried: ";

    // exit codes of the update-compatibility commands, see org.keycloak.compatibility.CompatibilityResult.ExitCode
    private static final int EXIT_CODE_ROLLING = 0;
    private static final int EXIT_CODE_RECREATE = 3;
    private static final int EXIT_CODE_FEATURE_DISABLED = 4;

    enum Outcome {
        ROLLING,
        RECREATE,
        /**
         * The update-compatibility commands did not produce a result, e.g. the pod hit its deadline, was killed, or the
         * command failed. Neither update type can be decided, so the current StatefulSet is kept and the job is retried.
         */
        RETRY
    }

    record UpdateCheckResult(Outcome outcome, String reason) {
    }

    private final KeycloakUpdateJobDependentResource updateJobResource;

    public AutoUpdateLogic(Context<Keycloak> context, Keycloak keycloak, KeycloakUpdateJobDependentResource updateJobResource) {
        super(context, keycloak);
        this.updateJobResource = updateJobResource;
    }

    @Override
    Optional<UpdateControl<Keycloak>> onUpdate() {
        if (hasNoInstances(ContextUtils.getCurrentStatefulSet(context).orElse(null), ContextUtils.getDesiredStatefulSet(context))) {
            // no instances to keep available, and a pending update without a check result must not block scaling to zero.
            decideRecreateUpdate(noInstancesReason(ContextUtils.getCurrentStatefulSet(context).orElse(null)));
            return Optional.empty();
        }

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
            Log.warn("Pod for completed Update Job not found, will restart the Job");
            context.getClient().resource(existingJob.get()).lockResourceVersion().delete();
            return Optional.of(UpdateControl.noUpdate());
        }

        var result = checkUpdateType(pod.get());
        switch (result.outcome()) {
            case ROLLING -> decideRollingUpdate(result.reason());
            case RECREATE -> decideRecreateUpdate(result.reason());
            case RETRY -> {
                return Optional.of(retryJob(existingJob.get(), result.reason()));
            }
        }
        return Optional.empty();
    }

    /**
     * @return true if the current or the desired StatefulSet has no instances, so a recreate update causes no downtime
     */
    static boolean hasNoInstances(StatefulSet current, StatefulSet desired) {
        return replicas(current) == 0 || replicas(desired) == 0;
    }

    /**
     * A recreate update first scales the current StatefulSet down to zero. The reason of that recreate update is kept
     * until the update is finished.
     */
    static String noInstancesReason(StatefulSet current) {
        return Optional.ofNullable(current)
                .map(StatefulSet::getMetadata)
                .map(ObjectMeta::getAnnotations)
                .filter(annotations -> Boolean.parseBoolean(annotations.get(Constants.KEYCLOAK_MIGRATING_ANNOTATION)))
                .map(annotations -> annotations.get(Constants.KEYCLOAK_UPDATE_REASON_ANNOTATION))
                .orElse(NO_INSTANCES_REASON);
    }

    private static int replicas(StatefulSet statefulSet) {
        return Optional.ofNullable(statefulSet)
                .map(StatefulSet::getSpec)
                .map(StatefulSetSpec::getReplicas)
                // a StatefulSet without replicas defaults to one
                .orElse(1);
    }

    private UpdateControl<Keycloak> retryJob(Job job, String reason) {
        reportRetry(RETRY_MESSAGE_PREFIX + reason);
        var remaining = remainingRetryDelay(job, Instant.now());
        if (remaining.isPresent()) {
            Log.warnf("Update Job did not produce a result: %s Retrying in %d seconds.", reason, remaining.get().toSeconds());
            return UpdateControl.<Keycloak>noUpdate().rescheduleAfter(remaining.get());
        }
        Log.info("Deleting Update Job without a result, a new one will be created.");
        context.getClient().resource(job).lockResourceVersion().delete();
        return UpdateControl.noUpdate();
    }

    /**
     * The failed job is kept for {@link #RETRY_DELAY} after it finished, so it can be inspected, and to not run the job
     * in a tight loop.
     *
     * @return the time left until the job is replaced, or empty if it can be replaced now
     */
    static Optional<Duration> remainingRetryDelay(Job job, Instant now) {
        var remaining = Duration.between(now, jobFinishedTime(job).plus(RETRY_DELAY));
        return remaining.compareTo(Duration.ZERO) > 0 ? Optional.of(remaining) : Optional.empty();
    }

    private static Instant jobFinishedTime(Job job) {
        return Optional.ofNullable(job.getStatus())
                .map(JobStatus::getConditions)
                .flatMap(conditions -> conditions.stream()
                        .filter(AutoUpdateLogic::isFinishedCondition)
                        .map(JobCondition::getLastTransitionTime)
                        .filter(Objects::nonNull)
                        .findFirst())
                .flatMap(AutoUpdateLogic::parseInstant)
                .orElse(Instant.EPOCH);
    }

    private static Optional<Instant> parseInstant(String time) {
        try {
            return Optional.of(Instant.parse(time));
        } catch (DateTimeParseException e) {
            return Optional.empty();
        }
    }

    private static boolean isFinishedCondition(JobCondition condition) {
        return "True".equals(condition.getStatus()) && List.of("Complete", "Failed").contains(condition.getType());
    }

    private boolean isJobRunning(Job job) {
        var status = job.getStatus();
        Log.debugf("Update Job Status:%n%s", CRDUtils.toJsonNode(status, context).toPrettyString());
        return Optional.ofNullable(status)
                .map(s -> s.getConditions().stream().noneMatch(AutoUpdateLogic::isFinishedCondition))
                .orElse(true);
    }

    /**
     * Maps the exit codes of the Update Job pod to an update decision. Only the exit codes that are a result of the
     * update-compatibility commands decide the update type. Anything else means the check did not produce a result.
     */
    static UpdateCheckResult checkUpdateType(Pod pod) {
        // the init container runs the 'update-compatibility metadata' command with the current configuration.
        var initContainer = initContainer(pod);
        var initContainerExitCode = initContainer.flatMap(AutoUpdateLogic::terminatedExitCode);
        if (initContainerExitCode.isEmpty()) {
            return retry("The update-compatibility metadata command did not finish%s.".formatted(details(pod, initContainer)));
        }
        switch (initContainerExitCode.get()) {
            case EXIT_CODE_ROLLING:
                break;
            case EXIT_CODE_FEATURE_DISABLED:
                Log.warn("Feature 'rolling-update' not enabled.");
                return new UpdateCheckResult(Outcome.RECREATE, "Feature 'rolling-update' not enabled.");
            default:
                return retry("The update-compatibility metadata command failed%s. Check update job for details.".formatted(details(pod, initContainer)));
        }

        // the container runs the 'update-compatibility check' command with the desired configuration.
        var container = container(pod);
        var containerExitCode = container.flatMap(AutoUpdateLogic::terminatedExitCode);
        if (containerExitCode.isEmpty()) {
            return retry("The update-compatibility check command did not finish%s.".formatted(details(pod, container)));
        }
        return switch (containerExitCode.get()) {
            case EXIT_CODE_ROLLING -> new UpdateCheckResult(Outcome.ROLLING, "Compatible changes detected.");
            case EXIT_CODE_RECREATE -> {
                Log.warn("Rolling Update not possible.");
                yield new UpdateCheckResult(Outcome.RECREATE, "Incompatible changes detected. Check update job for details.");
            }
            case EXIT_CODE_FEATURE_DISABLED -> {
                Log.warn("Feature 'rolling-update' not enabled.");
                yield new UpdateCheckResult(Outcome.RECREATE, "Feature 'rolling-update' not enabled.");
            }
            default -> retry("The update-compatibility check command failed%s. Check update job for details.".formatted(details(pod, container)));
        };
    }

    private static UpdateCheckResult retry(String reason) {
        return new UpdateCheckResult(Outcome.RETRY, reason);
    }

    private static String details(Pod pod, Optional<ContainerStatus> containerStatus) {
        var details = Stream.of(
                        containerStatus.flatMap(AutoUpdateLogic::terminatedExitCode).map("exit code %s"::formatted),
                        containerStatus.map(ContainerStatus::getState).map(ContainerState::getTerminated).map(ContainerStateTerminated::getReason),
                        Optional.ofNullable(pod.getStatus()).map(PodStatus::getReason).map("pod %s"::formatted))
                .flatMap(Optional::stream)
                .toList();
        return details.isEmpty() ? "" : " (" + String.join(", ", details) + ")";
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

    private static Optional<Integer> terminatedExitCode(ContainerStatus containerStatus) {
        return Optional.ofNullable(containerStatus)
                .map(ContainerStatus::getState)
                .map(ContainerState::getTerminated)
                .map(ContainerStateTerminated::getExitCode);
    }

    public static int exitCode(ContainerStatus containerStatus) {
        return terminatedExitCode(containerStatus).orElse(1);
    }

}

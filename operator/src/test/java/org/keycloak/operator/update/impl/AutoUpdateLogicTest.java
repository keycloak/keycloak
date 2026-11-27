package org.keycloak.operator.update.impl;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.stream.Stream;

import org.keycloak.operator.Constants;
import org.keycloak.operator.controllers.KeycloakUpdateJobDependentResource;
import org.keycloak.operator.update.impl.AutoUpdateLogic.Outcome;

import io.fabric8.kubernetes.api.model.ContainerStatus;
import io.fabric8.kubernetes.api.model.ContainerStatusBuilder;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodBuilder;
import io.fabric8.kubernetes.api.model.apps.StatefulSet;
import io.fabric8.kubernetes.api.model.apps.StatefulSetBuilder;
import io.fabric8.kubernetes.api.model.batch.v1.Job;
import io.fabric8.kubernetes.api.model.batch.v1.JobBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class AutoUpdateLogicTest {

    static Stream<Arguments> exitCodes() {
        return Stream.of(
                // only results of the update-compatibility commands decide the update type
                Arguments.of(0, 0, Outcome.ROLLING),
                Arguments.of(0, 3, Outcome.RECREATE),
                Arguments.of(0, 4, Outcome.RECREATE),
                Arguments.of(4, null, Outcome.RECREATE),
                // anything else is retried
                Arguments.of(0, 1, Outcome.RETRY),
                Arguments.of(0, 2, Outcome.RETRY),
                Arguments.of(0, 137, Outcome.RETRY),
                // SIGTERM, e.g. when the pod reaches activeDeadlineSeconds
                Arguments.of(0, 143, Outcome.RETRY),
                Arguments.of(1, null, Outcome.RETRY),
                Arguments.of(2, null, Outcome.RETRY),
                Arguments.of(137, null, Outcome.RETRY));
    }

    @ParameterizedTest(name = "init={0}, container={1} -> {2}")
    @MethodSource("exitCodes")
    public void testExitCodes(Integer initExitCode, Integer containerExitCode, Outcome expected) {
        var pod = pod(null, terminated("actual", initExitCode, null), terminated("desired", containerExitCode, null));

        assertEquals(expected, AutoUpdateLogic.checkUpdateType(pod).outcome());
    }

    @Test
    public void testContainersAreSelectedByName() {
        // init containers of the pod template run before the 'actual' init container of the update job
        var podTemplateInitContainer = terminated("pod-template-init", 0, "Completed");

        var featureDisabled = new PodBuilder().withNewStatus()
                .addToInitContainerStatuses(podTemplateInitContainer, terminated("actual", 4, "Error"))
                .endStatus().build();
        assertEquals(Outcome.RECREATE, AutoUpdateLogic.checkUpdateType(featureDisabled).outcome());

        var compatible = new PodBuilder().withNewStatus()
                .addToInitContainerStatuses(podTemplateInitContainer, terminated("actual", 0, "Completed"))
                .addToContainerStatuses(terminated("desired", 0, "Completed"))
                .endStatus().build();
        assertEquals(Outcome.ROLLING, AutoUpdateLogic.checkUpdateType(compatible).outcome());
    }

    @Test
    public void testPodWithoutStatusIsRetried() {
        assertEquals(Outcome.RETRY, AutoUpdateLogic.checkUpdateType(new PodBuilder().build()).outcome());
    }

    @Test
    public void testDeadlineExceededIsRetried() {
        // the pod hit activeDeadlineSeconds while the check command was still running
        var running = new ContainerStatusBuilder().withName("desired").withNewState().withNewRunning().endRunning().endState().build();
        var pod = pod("DeadlineExceeded", terminated("actual", 0, "Completed"), running);

        var result = AutoUpdateLogic.checkUpdateType(pod);

        assertEquals(Outcome.RETRY, result.outcome());
        assertTrue(result.reason().contains("did not finish"), result.reason());
        assertTrue(result.reason().contains("pod DeadlineExceeded"), result.reason());
    }

    @Test
    public void testKilledContainerIsRetried() {
        var pod = pod(null, terminated("actual", 0, "Completed"), terminated("desired", 137, "OOMKilled"));

        var result = AutoUpdateLogic.checkUpdateType(pod);

        assertEquals(Outcome.RETRY, result.outcome());
        assertTrue(result.reason().contains("exit code 137, OOMKilled"), result.reason());
    }

    @Test
    public void testNoInstancesSkipsTheCheck() {
        // scaling to zero, or updating a StatefulSet without instances, causes no downtime
        assertTrue(AutoUpdateLogic.hasNoInstances(statefulSet(2), statefulSet(0)));
        assertTrue(AutoUpdateLogic.hasNoInstances(statefulSet(0), statefulSet(2)));
        assertTrue(AutoUpdateLogic.hasNoInstances(statefulSet(0), statefulSet(0)));
        assertFalse(AutoUpdateLogic.hasNoInstances(statefulSet(2), statefulSet(3)));
        // replicas default to one
        assertFalse(AutoUpdateLogic.hasNoInstances(statefulSet(null), new StatefulSetBuilder().build()));
        assertFalse(AutoUpdateLogic.hasNoInstances(null, statefulSet(1)));
    }

    @Test
    public void testRecreateReasonIsKeptWhileScaledDown() {
        var reason = "Incompatible changes detected. Check update job for details.";
        var migrating = new StatefulSetBuilder().withNewMetadata()
                .addToAnnotations(Constants.KEYCLOAK_MIGRATING_ANNOTATION, "true")
                .addToAnnotations(Constants.KEYCLOAK_UPDATE_REASON_ANNOTATION, reason)
                .endMetadata().build();
        var finished = new StatefulSetBuilder().withNewMetadata()
                .addToAnnotations(Constants.KEYCLOAK_UPDATE_REASON_ANNOTATION, reason)
                .endMetadata().build();

        assertEquals(reason, AutoUpdateLogic.noInstancesReason(migrating));
        // the reason of a finished update does not apply to the next one
        assertEquals(AutoUpdateLogic.NO_INSTANCES_REASON, AutoUpdateLogic.noInstancesReason(finished));
        assertEquals(AutoUpdateLogic.NO_INSTANCES_REASON, AutoUpdateLogic.noInstancesReason(new StatefulSetBuilder().build()));
        assertEquals(AutoUpdateLogic.NO_INSTANCES_REASON, AutoUpdateLogic.noInstancesReason(null));
    }

    @Test
    public void testRetryDelayIsShorterThanJobTimeToLive() {
        // the delay is measured from the finished job, so the job must still exist when it ends
        assertTrue(AutoUpdateLogic.RETRY_DELAY.toSeconds() < KeycloakUpdateJobDependentResource.JOB_TIME_TO_LIVE_SECONDS);
    }

    @Test
    public void testJobIsKeptDuringRetryDelay() {
        var finished = Instant.parse("2026-01-01T10:00:00Z");
        var job = finishedJob("Failed", finished.toString());

        assertEquals(Optional.of(AutoUpdateLogic.RETRY_DELAY), AutoUpdateLogic.remainingRetryDelay(job, finished));
        assertEquals(Optional.of(Duration.ofMinutes(1)),
                AutoUpdateLogic.remainingRetryDelay(job, finished.plus(AutoUpdateLogic.RETRY_DELAY).minus(Duration.ofMinutes(1))));
    }

    @Test
    public void testJobIsReplacedAfterRetryDelay() {
        var finished = Instant.parse("2026-01-01T10:00:00Z");
        var job = finishedJob("Complete", finished.toString());

        assertEquals(Optional.empty(), AutoUpdateLogic.remainingRetryDelay(job, finished.plus(AutoUpdateLogic.RETRY_DELAY)));
        assertEquals(Optional.empty(), AutoUpdateLogic.remainingRetryDelay(job, finished.plus(Duration.ofHours(1))));
    }

    @Test
    public void testJobWithoutFinishedTimeIsReplaced() {
        var now = Instant.now();

        assertEquals(Optional.empty(), AutoUpdateLogic.remainingRetryDelay(new JobBuilder().build(), now));
        assertEquals(Optional.empty(), AutoUpdateLogic.remainingRetryDelay(finishedJob("Failed", null), now));
        assertEquals(Optional.empty(), AutoUpdateLogic.remainingRetryDelay(finishedJob("Failed", "not a time"), now));
    }

    private static StatefulSet statefulSet(Integer replicas) {
        return new StatefulSetBuilder().withNewSpec().withReplicas(replicas).endSpec().build();
    }

    private static Job finishedJob(String conditionType, String lastTransitionTime) {
        return new JobBuilder()
                .withNewStatus()
                .addNewCondition().withType(conditionType).withStatus("True").withLastTransitionTime(lastTransitionTime).endCondition()
                .endStatus()
                .build();
    }

    private static Pod pod(String podReason, ContainerStatus initContainer, ContainerStatus container) {
        var status = new PodBuilder().withNewStatus().withReason(podReason).addToInitContainerStatuses(initContainer);
        if (container != null) {
            status.addToContainerStatuses(container);
        }
        return status.endStatus().build();
    }

    private static ContainerStatus terminated(String name, Integer exitCode, String reason) {
        if (exitCode == null) {
            return null;
        }
        return new ContainerStatusBuilder()
                .withName(name)
                .withNewState()
                .withNewTerminated().withExitCode(exitCode).withReason(reason).endTerminated()
                .endState()
                .build();
    }
}

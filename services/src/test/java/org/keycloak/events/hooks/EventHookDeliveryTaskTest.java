package org.keycloak.events.hooks;

import java.util.List;
import java.util.Map;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class EventHookDeliveryTaskTest {

    @Test
    public void shouldApplyExponentialRetryBackoff() {
        EventHookMessageModel message = message("msg-1", 0);

        EventHookDeliveryResult result = new EventHookDeliveryResult();
        result.setSuccess(false);
        result.setRetryable(true);
        result.setDetails("temporary failure");

        EventHookDeliveryTask.applyDeliveryResultToMessage(
                message,
                target(Map.of("maxAttempts", 5, "retryDelayMs", 1000)),
                result,
                1_000L);

        assertEquals(EventHookMessageStatus.PENDING, message.getStatus());
        assertEquals(1, message.getAttemptCount());
        assertEquals(2_000L, message.getNextAttemptAt());
        assertNull(message.getExecutionStartedAt());
        assertEquals(1_000L, message.getUpdatedAt());
        assertEquals("temporary failure", message.getLastError());
    }

    @Test
    public void shouldExhaustMessageWhenMaxAttemptsReached() {
        EventHookMessageModel message = message("msg-2", 2);

        EventHookDeliveryResult result = new EventHookDeliveryResult();
        result.setSuccess(false);
        result.setRetryable(true);
        result.setDetails("still failing");

        EventHookDeliveryTask.applyDeliveryResultToMessage(
                message,
                target(Map.of("maxAttempts", 2, "retryDelayMs", 1000)),
                result,
                5_000L);

        assertEquals(EventHookMessageStatus.EXHAUSTED, message.getStatus());
        assertEquals(3, message.getAttemptCount());
        assertEquals(5_000L, message.getNextAttemptAt());
        assertEquals("still failing", message.getLastError());
    }

    @Test
    public void shouldRespectRetryAfterWhenItExceedsExponentialDelay() {
        EventHookMessageModel message = message("msg-3", 0);

        EventHookDeliveryResult result = new EventHookDeliveryResult();
        result.setSuccess(false);
        result.setRetryable(true);
        result.setRetryAfterMillis(15_000L);

        EventHookDeliveryTask.applyDeliveryResultToMessage(
                message,
                target(Map.of("maxAttempts", 5, "retryDelayMs", 1000)),
                result,
                2_000L);

        assertEquals(EventHookMessageStatus.PENDING, message.getStatus());
        assertEquals(17_000L, message.getNextAttemptAt());
    }

    @Test
    public void shouldDelayInitialDeliveryWhenHttpTargetIsAutoDisabled() {
        EventHookTargetModel target = target(Map.of(EventHookAutoDisableSupport.LEGACY_AUTO_DISABLED_UNTIL, 9_000L));
        target.setEnabled(false);

        long nextAttemptAt = EventHookDeliveryTask.initialNextAttemptAt(
                target,
                new HttpEventHookTargetProviderFactory(),
                null,
                1_000L);

        assertEquals(9_000L, Math.max(nextAttemptAt, EventHookAutoDisableSupport.deliveryPausedUntil(target, 1_000L)));
    }

    @Test
    public void shouldSplitBulkMessagesByConfiguredBatchSize() {
        List<List<EventHookMessageModel>> batches = EventHookDeliveryTask.splitMessagesForTarget(
                List.of(
                        message("msg-1", 0),
                        message("msg-2", 0),
                        message("msg-3", 0),
                        message("msg-4", 0),
                        message("msg-5", 0)),
            target(Map.of("deliveryMode", "BULK", "maxEventsPerBatch", 2)),
            true);

        assertEquals(3, batches.size());
        assertEquals(List.of("msg-1", "msg-2"), ids(batches.get(0)));
        assertEquals(List.of("msg-3", "msg-4"), ids(batches.get(1)));
        assertEquals(List.of("msg-5"), ids(batches.get(2)));
    }

    @Test
    public void shouldKeepSingleModeAsSingleMessageBatches() {
        List<List<EventHookMessageModel>> batches = EventHookDeliveryTask.splitMessagesForTarget(
                List.of(message("msg-1", 0), message("msg-2", 0), message("msg-3", 0)),
                target(Map.of("deliveryMode", "SINGLE")),
                true);

        assertEquals(3, batches.size());
        assertEquals(List.of("msg-1"), ids(batches.get(0)));
        assertEquals(List.of("msg-2"), ids(batches.get(1)));
        assertEquals(List.of("msg-3"), ids(batches.get(2)));
    }

    @Test
    public void shouldKeepSingleMessageBatchesWhenProviderDoesNotSupportBatch() {
        List<List<EventHookMessageModel>> batches = EventHookDeliveryTask.splitMessagesForTarget(
                List.of(message("msg-1", 0), message("msg-2", 0)),
                target(Map.of("deliveryMode", "BULK", "maxEventsPerBatch", 50)),
                false);

        assertEquals(2, batches.size());
        assertEquals(List.of("msg-1"), ids(batches.get(0)));
        assertEquals(List.of("msg-2"), ids(batches.get(1)));
    }

    @Test
    public void shouldResolveSingleExecutionSizeWhenBatchDisabled() {
        assertEquals(1, EventHookDeliveryTask.maxMessagesPerExecution(target(Map.of("deliveryMode", "BULK", "maxEventsPerBatch", 25)), false));
        assertEquals(1, EventHookDeliveryTask.maxMessagesPerExecution(target(Map.of("deliveryMode", "SINGLE", "maxEventsPerBatch", 25)), true));
        assertEquals(25, EventHookDeliveryTask.maxMessagesPerExecution(target(Map.of("deliveryMode", "BULK", "maxEventsPerBatch", 25)), true));
    }

    @Test
    public void shouldUseUpdatedDefaultBatchSizeWhenMissing() {
        assertEquals(1000, EventHookDeliveryTask.maxMessagesPerExecution(target(Map.of("deliveryMode", "BULK")), true));
    }

    @Test
    public void shouldUseUpdatedDefaultRetryDelayWhenMissing() {
        EventHookMessageModel message = message("msg-default-retry", 0);

        EventHookDeliveryResult result = new EventHookDeliveryResult();
        result.setSuccess(false);
        result.setRetryable(true);

        EventHookDeliveryTask.applyDeliveryResultToMessage(
                message,
                target(Map.of("maxAttempts", 5)),
                result,
                1_000L);

        assertEquals(EventHookMessageStatus.PENDING, message.getStatus());
        assertEquals(6_000L, message.getNextAttemptAt());
    }

    @Test
    public void shouldDelayInitialBulkDeliveryWhenAggregationIsEnabled() {
        assertEquals(
                6_000L,
                EventHookDeliveryTask.initialNextAttemptAt(
                        target(Map.of("deliveryMode", "BULK", "aggregationTimeoutMs", 5_000)),
                        new HttpEventHookTargetProviderFactory(),
                        null,
                        1_000L));
    }

    @Test
    public void shouldReusePendingAggregationDeadlineForLaterMessages() {
        assertEquals(
                9_000L,
                EventHookDeliveryTask.initialNextAttemptAt(
                        target(Map.of("deliveryMode", "BULK", "aggregationTimeoutMs", 5_000)),
                        new HttpEventHookTargetProviderFactory(),
                        9_000L,
                        4_000L));
    }

    @Test
    public void shouldSkipAggregationForProvidersWithoutSupport() {
        assertEquals(
                2_000L,
                EventHookDeliveryTask.initialNextAttemptAt(
                        target(Map.of("deliveryMode", "BULK", "aggregationTimeoutMs", 5_000)),
                        new PullEventHookTargetProviderFactory(),
                        null,
                        2_000L));
    }

            @Test
            public void shouldUseUpdatedDefaultAggregationTimeoutWhenMissing() {
            assertEquals(
                8_000L,
                EventHookDeliveryTask.initialNextAttemptAt(
                    target(Map.of("deliveryMode", "BULK")),
                    new HttpEventHookTargetProviderFactory(),
                    null,
                    3_000L));
            }

    @Test
    public void shouldNotRetryWhenRetryDisabled() {
        EventHookMessageModel message = message("msg-4", 0);

        EventHookDeliveryResult result = new EventHookDeliveryResult();
        result.setSuccess(false);
        result.setRetryable(true);
        result.setDetails("temporary failure");

        EventHookDeliveryTask.applyDeliveryResultToMessage(
                message,
                target(Map.of("retryEnabled", false, "maxAttempts", 5, "retryDelayMs", 1000)),
                result,
                3_000L,
                true);

        assertEquals(EventHookMessageStatus.FAILED, message.getStatus());
        assertEquals(3_000L, message.getNextAttemptAt());
    }

    @Test
    public void shouldNotRetryWhenProviderDoesNotSupportRetry() {
        EventHookMessageModel message = message("msg-5", 0);

        EventHookDeliveryResult result = new EventHookDeliveryResult();
        result.setSuccess(false);
        result.setRetryable(true);

        EventHookDeliveryTask.applyDeliveryResultToMessage(
                message,
                target(Map.of("maxAttempts", 5, "retryDelayMs", 1000)),
                result,
                4_000L,
                false);

        assertEquals(EventHookMessageStatus.FAILED, message.getStatus());
        assertEquals(4_000L, message.getNextAttemptAt());
    }

    @Test
    public void shouldKeepMessageWaitingWhenAdditionalActionIsRequired() {
        EventHookMessageModel message = message("msg-6", 0);

        EventHookDeliveryResult result = new EventHookDeliveryResult();
        result.setSuccess(false);
        result.setWaiting(true);
        result.setRetryable(false);
        result.setDetails("Waiting for consumption");

        EventHookDeliveryTask.applyDeliveryResultToMessage(
                message,
                target(Map.of()),
                result,
                6_000L,
                false);

        assertEquals(EventHookMessageStatus.WAITING, message.getStatus());
        assertEquals(1, message.getAttemptCount());
        assertEquals(6_000L, message.getNextAttemptAt());
        assertNull(message.getExecutionStartedAt());
        assertNull(message.getLastError());
    }

    @Test
    public void shouldMarkUnavailableTargetTypeAsFailed() {
        EventHookMessageModel message = message("msg-7", 1);

        EventHookDeliveryTask.applyTerminalMessageState(
                message,
                EventHookMessageStatus.FAILED,
                EventHookDeliveryTask.unavailableTargetTypeReason("custom-target", new HttpEventHookTargetProviderFactory()),
                7_000L);

        assertEquals(EventHookMessageStatus.FAILED, message.getStatus());
        assertEquals(7_000L, message.getUpdatedAt());
        assertEquals(7_000L, message.getNextAttemptAt());
        assertEquals("Target type not available: custom-target", message.getLastError());
    }

    @Test
    public void shouldDescribeUnknownTargetTypeWhenNoFactoryExists() {
        assertEquals(
                "Unknown target type: custom-target",
                EventHookDeliveryTask.unavailableTargetTypeReason("custom-target", null));
    }

    @Test
    public void shouldAutoDisableHttpTargetAfterRepeated429Responses() {
        EventHookTargetModel target = target(Map.of());
        target.setEnabled(true);

        EventHookDeliveryResult result = new EventHookDeliveryResult();
        result.setSuccess(false);
        result.setRetryable(true);
        result.setAutoDisableEligible(true);

        EventHookAutoDisableSupport.applyAutoDisableState(target, result, 10_000L);
        assertTrue(target.isEnabled());

        long pausedUntil = EventHookAutoDisableSupport.applyAutoDisableState(target, result, 20_000L);
        assertTrue(target.isEnabled());

        pausedUntil = EventHookAutoDisableSupport.applyAutoDisableState(target, result, 30_000L);

        assertFalse(target.isEnabled());
        assertEquals(330_000L, pausedUntil);
        assertEquals(300_000L, result.getRetryAfterMillis().longValue());
        assertTrue(EventHookAutoDisableSupport.isAutoDisabled(target));
    }

    @Test
    public void shouldResumeAutoDisabledHttpTargetAfterSuccess() {
        EventHookTargetModel target = target(Map.of());
        target.setEnabled(false);
        target.setAutoDisabledUntil(50_000L);
        target.setConsecutive429Count(3);

        EventHookDeliveryResult result = new EventHookDeliveryResult();
        result.setSuccess(true);
        result.setStatusCode("200");

        EventHookAutoDisableSupport.applyAutoDisableState(target, result, 60_000L);

        assertTrue(target.isEnabled());
        assertFalse(EventHookAutoDisableSupport.isAutoDisabled(target));
    }

    private EventHookTargetModel target(Map<String, Object> settings) {
        EventHookTargetModel target = new EventHookTargetModel();
        target.setSettings(settings);
        return target;
    }

    private EventHookMessageModel message(String id, int attemptCount) {
        EventHookMessageModel message = new EventHookMessageModel();
        message.setId(id);
        message.setAttemptCount(attemptCount);
        message.setStatus(EventHookMessageStatus.PENDING);
        message.setExecutionStartedAt(500L);
        return message;
    }

    private List<String> ids(List<EventHookMessageModel> messages) {
        return messages.stream().map(EventHookMessageModel::getId).toList();
    }
}

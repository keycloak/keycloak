/*
 * Copyright 2026 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.events.hooks;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;

import org.keycloak.executors.ExecutorsProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.timer.ScheduledTask;

import static org.keycloak.common.util.Time.currentTimeMillis;
import static org.keycloak.models.utils.KeycloakModelUtils.runJobInTransaction;

public class EventHookDeliveryTask implements ScheduledTask {

    private static final String EXECUTOR_NAME = "event-hook-delivery";

    private final KeycloakSessionFactory sessionFactory;
    private final int maxPollBatchSize;
    private final long claimTimeoutMillis;

    public EventHookDeliveryTask(KeycloakSessionFactory sessionFactory, int maxPollBatchSize, long claimTimeoutMillis) {
        this.sessionFactory = sessionFactory;
        this.maxPollBatchSize = maxPollBatchSize;
        this.claimTimeoutMillis = claimTimeoutMillis;
    }

    @Override
    public void run(KeycloakSession session) {
        long now = currentTimeMillis();
        String claimOwner = UUID.randomUUID().toString();
        EventHookStoreProvider store = session.getProvider(EventHookStoreProvider.class);
        List<EventHookMessageModel> claimedMessages = store.claimAvailableMessages(maxPollBatchSize, now, now - claimTimeoutMillis, claimOwner);
        if (claimedMessages.isEmpty()) {
            return;
        }

        ExecutorService executor = session.getProvider(ExecutorsProvider.class).getExecutor(EXECUTOR_NAME);
        groupByTarget(claimedMessages).forEach(batch -> executor.submit(() -> processBatch(batch)));
    }

    private List<List<EventHookMessageModel>> groupByTarget(List<EventHookMessageModel> claimedMessages) {
        Map<String, List<EventHookMessageModel>> grouped = claimedMessages.stream()
                .sorted(Comparator.comparing(EventHookMessageModel::getCreatedAt))
                .collect(java.util.stream.Collectors.groupingBy(EventHookMessageModel::getTargetId, java.util.LinkedHashMap::new, java.util.stream.Collectors.toList()));

        List<List<EventHookMessageModel>> batches = new ArrayList<>();
        grouped.values().forEach(messages -> batches.addAll(splitByTargetMode(messages)));
        return batches;
    }

    private List<List<EventHookMessageModel>> splitByTargetMode(List<EventHookMessageModel> messages) {
        return runJobInTransactionWithResult(sessionFactory, session -> {
            EventHookTargetModel target = session.getProvider(EventHookStoreProvider.class).getTarget(messages.get(0).getRealmId(), messages.get(0).getTargetId());
            if (target == null) {
                return messages.stream().map(List::of).toList();
            }
            EventHookTargetProviderFactory providerFactory = providerFactory(session, target.getType());
            return splitMessagesForTarget(messages, target, providerFactory != null && providerFactory.supportsBatch());
        });
    }

    private void processBatch(List<EventHookMessageModel> batch) {
        runJobInTransaction(sessionFactory, session -> {
            EventHookStoreProvider store = session.getProvider(EventHookStoreProvider.class);
            EventHookMessageModel firstMessage = batch.get(0);
            EventHookTargetModel target = store.getTarget(firstMessage.getRealmId(), firstMessage.getTargetId());

            if (target == null || !target.isEnabled()) {
                batch.forEach(message -> markDead(store, message, "Target not found or disabled"));
                return;
            }

            EventHookTargetProvider provider = session.getProvider(EventHookTargetProvider.class, target.getType());
            if (provider == null) {
                batch.forEach(message -> markFailed(store, message, unavailableTargetTypeReason(target.getType(), providerFactory(session, target.getType()))));
                return;
            }

            EventHookTargetProviderFactory providerFactory = providerFactory(session, target.getType());
            boolean batchExecution = batch.size() > 1;
            if (batchExecution && (providerFactory == null || !providerFactory.supportsBatch())) {
                batch.forEach(message -> markDead(store, message, "Target provider does not support batch delivery: " + target.getType()));
                return;
            }

            String executionId = UUID.randomUUID().toString();

            EventHookDeliveryResult result;
            try {
                result = batchExecution
                        ? provider.deliverBatch(target, batch)
                        : provider.deliver(target, batch.get(0));
            } catch (IOException exception) {
                result = new EventHookDeliveryResult();
                result.setSuccess(false);
                result.setRetryable(true);
                result.setDetails(exception.getMessage());
                result.setDurationMillis(0);
            } catch (IllegalArgumentException exception) {
                result = new EventHookDeliveryResult();
                result.setSuccess(false);
                result.setRetryable(false);
                result.setDetails(exception.getMessage());
                result.setDurationMillis(0);
            }

            EventHookDeliveryResult deliveryResult = result;
            boolean retryAvailable = providerFactory == null || providerFactory.supportsRetry();
            batch.forEach(message -> applyDeliveryResult(store, message, target, executionId, batchExecution, retryAvailable, deliveryResult));
        });
    }

    private void applyDeliveryResult(EventHookStoreProvider store, EventHookMessageModel message, EventHookTargetModel target,
            String executionId, boolean batchExecution, boolean retryAvailable, EventHookDeliveryResult result) {
        long now = currentTimeMillis();
        applyDeliveryResultToMessage(message, target, result, now, retryAvailable);

        if (result.isSuccess()) {
            createLog(store, message, executionId, batchExecution, EventHookLogStatus.SUCCESS, result);
        } else if (result.isWaiting()) {
            createLog(store, message, executionId, batchExecution, EventHookLogStatus.WAITING, result);
        } else {
            createLog(store, message, executionId, batchExecution, EventHookLogStatus.FAILED, result);
        }

        store.updateMessage(message);
    }

    private void markDead(EventHookStoreProvider store, EventHookMessageModel message, String details) {
        long now = currentTimeMillis();
        applyTerminalMessageState(message, EventHookMessageStatus.DEAD, details, now);
        store.updateMessage(message);

        EventHookDeliveryResult result = new EventHookDeliveryResult();
        result.setSuccess(false);
        result.setRetryable(false);
        result.setDetails(details);
        result.setDurationMillis(0);
        createLog(store, message, UUID.randomUUID().toString(), false, EventHookLogStatus.FAILED, result);
    }

    private void markFailed(EventHookStoreProvider store, EventHookMessageModel message, String details) {
        long now = currentTimeMillis();
        applyTerminalMessageState(message, EventHookMessageStatus.FAILED, details, now);
        store.updateMessage(message);

        EventHookDeliveryResult result = new EventHookDeliveryResult();
        result.setSuccess(false);
        result.setRetryable(false);
        result.setDetails(details);
        result.setDurationMillis(0);
        createLog(store, message, UUID.randomUUID().toString(), false, EventHookLogStatus.FAILED, result);
    }

    private void createLog(EventHookStoreProvider store, EventHookMessageModel message, String executionId, boolean batchExecution,
            EventHookLogStatus status, EventHookDeliveryResult result) {
        EventHookLogModel log = new EventHookLogModel();
        log.setId(UUID.randomUUID().toString());
        log.setExecutionId(executionId);
        log.setBatchExecution(batchExecution);
        log.setMessageId(message.getId());
        log.setTargetId(message.getTargetId());
        log.setStatus(status);
        log.setAttemptNumber(message.getAttemptCount());
        log.setStatusCode(result.getStatusCode());
        log.setDurationMs(result.getDurationMillis());
        log.setDetails(truncate(result.getDetails(), 2048));
        log.setCreatedAt(currentTimeMillis());
        store.createLog(log);
    }

    static void applyDeliveryResultToMessage(EventHookMessageModel message, EventHookTargetModel target, EventHookDeliveryResult result, long now) {
        applyDeliveryResultToMessage(message, target, result, now, true);
    }

    static void applyDeliveryResultToMessage(EventHookMessageModel message, EventHookTargetModel target, EventHookDeliveryResult result, long now,
            boolean retryAvailable) {
        int attemptNumber = message.getAttemptCount() + 1;
        int maxAttempts = intSetting(target.getSettings(), "maxAttempts", 5);
        long retryDelayMs = intSetting(target.getSettings(), "retryDelayMs", 1000);
        boolean retryEnabled = retryAvailable && booleanSetting(target.getSettings(), "retryEnabled", true);

        message.setAttemptCount(attemptNumber);
        message.setUpdatedAt(now);
        message.setClaimOwner(null);
        message.setClaimedAt(null);
        message.setLastError(result.isSuccess() || result.isWaiting() ? null : result.getDetails());

        if (result.isSuccess()) {
            message.setStatus(EventHookMessageStatus.SUCCESS);
            message.setNextAttemptAt(now);
        } else if (result.isWaiting()) {
            message.setStatus(EventHookMessageStatus.WAITING);
            message.setNextAttemptAt(now);
        } else if (retryEnabled && result.isRetryable() && attemptNumber < maxAttempts) {
            message.setStatus(EventHookMessageStatus.PENDING);
            long nextAttemptAt = now + retryDelay(retryDelayMs, attemptNumber);
            Long retryAfterMillis = result.getRetryAfterMillis();
            if (retryAfterMillis != null && retryAfterMillis > 0) {
                nextAttemptAt = Math.max(nextAttemptAt, now + retryAfterMillis);
            }
            message.setNextAttemptAt(nextAttemptAt);
        } else if (retryEnabled && result.isRetryable()) {
            message.setStatus(EventHookMessageStatus.EXHAUSTED);
            message.setNextAttemptAt(now);
        } else {
            message.setStatus(EventHookMessageStatus.FAILED);
            message.setNextAttemptAt(now);
        }
    }

    static List<List<EventHookMessageModel>> splitMessagesForTarget(List<EventHookMessageModel> messages, EventHookTargetModel target,
            boolean supportsBatch) {
        int batchSize = maxMessagesPerExecution(target, supportsBatch);
        if (batchSize <= 1) {
            return messages.stream().map(List::of).toList();
        }
        List<List<EventHookMessageModel>> batches = new ArrayList<>();
        for (int index = 0; index < messages.size(); index += batchSize) {
            batches.add(messages.subList(index, Math.min(index + batchSize, messages.size())));
        }
        return batches;
    }

    static int maxMessagesPerExecution(EventHookTargetModel target, boolean supportsBatch) {
        if (!supportsBatch) {
            return 1;
        }

        String deliveryMode = stringSetting(target == null ? null : target.getSettings(), "deliveryMode", "SINGLE");
        if (!"BULK".equalsIgnoreCase(deliveryMode)) {
            return 1;
        }

        return intSetting(target == null ? null : target.getSettings(), "maxEventsPerBatch", 20);
    }

    static long retryDelay(long retryDelayMs, int attemptNumber) {
        return retryDelayMs * (1L << Math.max(0, Math.min(attemptNumber - 1, 10)));
    }

    static void applyTerminalMessageState(EventHookMessageModel message, EventHookMessageStatus status, String details, long now) {
        message.setStatus(status);
        message.setClaimOwner(null);
        message.setClaimedAt(null);
        message.setUpdatedAt(now);
        message.setNextAttemptAt(now);
        message.setLastError(details);
    }

    static String unavailableTargetTypeReason(String type, EventHookTargetProviderFactory providerFactory) {
        return providerFactory == null
                ? "Unknown target type: " + type
                : "Target type not available: " + type;
    }

    private static String stringSetting(Map<String, Object> settings, String key, String defaultValue) {
        Object value = settings == null ? null : settings.get(key);
        return value == null ? defaultValue : value.toString();
    }

    private static int intSetting(Map<String, Object> settings, String key, int defaultValue) {
        Object value = settings == null ? null : settings.get(key);
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        return Integer.parseInt(value.toString());
    }

    private static boolean booleanSetting(Map<String, Object> settings, String key, boolean defaultValue) {
        Object value = settings == null ? null : settings.get(key);
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Boolean booleanValue) {
            return booleanValue;
        }
        return Boolean.parseBoolean(value.toString());
    }

    private String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }

    private <T> T runJobInTransactionWithResult(KeycloakSessionFactory factory, java.util.function.Function<KeycloakSession, T> task) {
        final java.util.concurrent.atomic.AtomicReference<T> result = new java.util.concurrent.atomic.AtomicReference<>();
        runJobInTransaction(factory, session -> result.set(task.apply(session)));
        return result.get();
    }

    private EventHookTargetProviderFactory providerFactory(KeycloakSession session, String type) {
        return (EventHookTargetProviderFactory) session.getKeycloakSessionFactory().getProviderFactory(EventHookTargetProvider.class, type);
    }
}

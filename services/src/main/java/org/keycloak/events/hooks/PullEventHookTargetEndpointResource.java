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

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.InternalServerErrorException;
import jakarta.ws.rs.NotAuthorizedException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;

import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;

import static org.keycloak.common.util.Time.currentTimeMillis;

@Produces(MediaType.APPLICATION_JSON)
public class PullEventHookTargetEndpointResource {

    private static final long EXECUTION_TIMEOUT_MILLIS = java.util.concurrent.TimeUnit.MINUTES.toMillis(5);
    private static final String SECRET_HEADER = "X-Keycloak-Event-Hook-Secret";
    private static final Comparator<EventHookMessageModel> FIFO_MESSAGE_ORDER = Comparator
            .comparingLong(EventHookMessageModel::getCreatedAt)
            .thenComparing(EventHookMessageModel::getId, Comparator.nullsLast(String::compareTo));

    private final KeycloakSession session;
    private final EventHookTargetModel target;
    private final boolean testMode;

    public PullEventHookTargetEndpointResource(KeycloakSession session, EventHookTargetModel target, boolean testMode) {
        this.session = session;
        this.target = target;
        this.testMode = testMode;
    }

    @GET
    public Object consumeGet(@jakarta.ws.rs.core.Context HttpHeaders headers) {
        return consume(headers);
    }

    @POST
    public Object consumePost(@jakarta.ws.rs.core.Context HttpHeaders headers) {
        return consume(headers);
    }

    private Object consume(HttpHeaders headers) {
        RealmModel realm = session.getContext().getRealm();
        if (target == null || !target.isEnabled()) {
            throw new NotFoundException("Event hook target not found");
        }

        validateSecret(headers);

        EventHookStoreProvider store = session.getProvider(EventHookStoreProvider.class);
        long now = currentTimeMillis();
        int maxResults = EventHookDeliveryTask.maxMessagesPerExecution(target, true);
        boolean batchMode = maxResults > 1;
        String executionId = UUID.randomUUID().toString();
        List<EventHookMessageModel> messages = store.reserveAvailableMessagesForTarget(realm.getId(), target.getId(), maxResults, now,
            EXECUTION_TIMEOUT_MILLIS, executionId, testMode).stream()
                .sorted(FIFO_MESSAGE_ORDER)
                .toList();
        boolean batchExecution = messages.size() > 1;
        messages.forEach(message -> message.setExecutionBatch(batchExecution));

        List<EventHookPullEntryRepresentation> entries = messages.stream()
                .map(message -> toPullEntry(store, realm.getId(), message))
                .toList();

        EventHookPullRepresentation representation = new EventHookPullRepresentation();
        if (!batchMode) {
            representation.setEvent(messages.isEmpty() ? null : readPayload(messages.get(0).getPayload()));
            representation.setEntry(entries.isEmpty() ? null : entries.get(0));
        } else {
            representation.setEvents(messages.stream().map(message -> readPayload(message.getPayload())).toList());
            representation.setEntries(entries);
        }
        representation.setHasMoreEvents(store.hasAvailableMessages(realm.getId(), target.getId(), currentTimeMillis(), EXECUTION_TIMEOUT_MILLIS, testMode));

        if (!messages.isEmpty()) {
            if (EventHookBodyMappingSupport.isEnabled(target.getSettings())) {
                try {
                    Object renderedBody = EventHookBodyMappingSupport.render(target.getSettings(), pullBodyMappingModel(representation)).jsonBody();
                    messages.forEach(message -> completeMessage(store, message, message.getExecutionId(), batchExecution));
                    return renderedBody;
                } catch (EventHookBodyMappingSupport.EventHookBodyMappingException exception) {
                    failMessages(store, messages, executionId, batchExecution, exception.getMessage());
                    throw new InternalServerErrorException(truncate(exception.getMessage(), 1024));
                }
            }

            messages.forEach(message -> completeMessage(store, message, message.getExecutionId(), batchExecution));
        } else if (EventHookBodyMappingSupport.isEnabled(target.getSettings())) {
            try {
                return EventHookBodyMappingSupport.render(target.getSettings(), pullBodyMappingModel(representation)).jsonBody();
            } catch (EventHookBodyMappingSupport.EventHookBodyMappingException exception) {
                throw new InternalServerErrorException(truncate(exception.getMessage(), 1024));
            }
        }

        return representation;
    }

    private void validateSecret(HttpHeaders headers) {
        String configuredSecret = stringSetting(target.getSettings(), "pullSecret");
        if (configuredSecret == null) {
            return;
        }

        String suppliedSecret = suppliedSecret(headers);
        if (suppliedSecret == null) {
            throw new NotAuthorizedException("Missing " + SECRET_HEADER + " header");
        }

        if (!MessageDigest.isEqual(configuredSecret.getBytes(StandardCharsets.UTF_8), suppliedSecret.getBytes(StandardCharsets.UTF_8))) {
            throw new NotAuthorizedException("Invalid API secret");
        }
    }

    private String suppliedSecret(HttpHeaders headers) {
        String headerSecret = headers == null ? null : headers.getHeaderString(SECRET_HEADER);
        return headerSecret == null || headerSecret.isBlank() ? null : headerSecret.trim();
    }

    private void completeMessage(EventHookStoreProvider store, EventHookMessageModel message, String executionId, boolean batchExecution) {
        long now = currentTimeMillis();
        message.setExecutionId(executionId);
        message.setExecutionBatch(batchExecution);
        message.setStatus(EventHookMessageStatus.SUCCESS);
        message.setUpdatedAt(now);
        message.setNextAttemptAt(now);
        message.setExecutionStartedAt(null);
        message.setLastError(null);
        store.updateMessage(message);

        EventHookLogModel log = new EventHookLogModel();
        log.setId(UUID.randomUUID().toString());
        log.setExecutionId(executionId);
        log.setStatus(EventHookLogStatus.SUCCESS);
        log.setMessageStatus(message.getStatus());
        log.setAttemptNumber(message.getAttemptCount());
        log.setStatusCode(testMode ? "PULL_TEST_CONSUMED" : "PULL_CONSUMED");
        log.setDurationMs(0L);
        log.setDetails(testMode ? "Consumed through the pull test endpoint" : "Consumed through the pull endpoint");
        log.setCreatedAt(now);
        log.setTest(message.isTest());
        store.createLog(log);
    }

    private EventHookPullEntryRepresentation toPullEntry(EventHookStoreProvider store, String realmId, EventHookMessageModel message) {
        EventHookLogModel latestLog = store.getLogsStream(realmId, message.getId(), message.getTargetId(), null, null, null, 0, 1)
            .findFirst()
            .orElse(null);

        EventHookPullEntryRepresentation entry = new EventHookPullEntryRepresentation();
        entry.setLogId(latestLog == null ? null : latestLog.getId());
        entry.setExecutionId(message.getExecutionId());
        entry.setBatchExecution(message.isExecutionBatch());
        entry.setMessageId(message.getId());
        entry.setTargetId(message.getTargetId());
        entry.setSourceEventId(message.getSourceEventId());
        entry.setStatus(latestLog == null ? EventHookLogStatus.WAITING.name() : latestLog.getStatus().name());
        entry.setAttemptNumber(message.getAttemptCount());
        entry.setStatusCode(latestLog == null ? null : latestLog.getStatusCode());
        entry.setDurationMs(latestLog == null ? null : latestLog.getDurationMs());
        entry.setDetails(latestLog == null ? null : latestLog.getDetails());
        entry.setCreatedAt(latestLog == null ? message.getUpdatedAt() : latestLog.getCreatedAt());
        entry.setTest(message.isTest());
        entry.setData(readPayload(message.getPayload()));
        return entry;
    }

    private Object readPayload(String payload) {
        try {
            return EventHookPayloadNormalizer.readPayload(payload);
        } catch (Exception exception) {
            return payload;
        }
    }

    private String stringSetting(Map<String, Object> settings, String key) {
        Object value = settings == null ? null : settings.get(key);
        if (value == null) {
            return null;
        }
        String stringValue = value.toString().trim();
        return stringValue.isEmpty() ? null : stringValue;
    }

    private Map<String, Object> pullBodyMappingModel(EventHookPullRepresentation representation) {
        Map<String, Object> model = representation.getEvents() == null
                ? EventHookBodyMappingSupport.singleEventModel(representation.getEvent())
                : EventHookBodyMappingSupport.batchEventModel(representation.getEvents());
        return EventHookBodyMappingSupport.withPullMetadata(model, representation.getEntry(), representation.getEntries(), representation.isHasMoreEvents());
    }

    private void failMessages(EventHookStoreProvider store, List<EventHookMessageModel> messages, String executionId, boolean batchExecution, String details) {
        EventHookDeliveryResult result = new EventHookDeliveryResult();
        result.setSuccess(false);
        result.setRetryable(false);
        result.setStatusCode(EventHookBodyMappingSupport.PARSE_FAILED_STATUS_CODE);
        result.setDetails(truncate(details, 1024));
        result.setDurationMillis(0L);

        long now = currentTimeMillis();
        for (EventHookMessageModel message : messages) {
            message.setExecutionId(executionId);
            message.setExecutionBatch(batchExecution);
            EventHookDeliveryTask.applyDeliveryResultToMessage(message, target, result, now, false);
            store.updateMessage(message);
            createFailureLog(store, message, executionId, result, now);
        }
    }

    private void createFailureLog(EventHookStoreProvider store, EventHookMessageModel message, String executionId,
            EventHookDeliveryResult result, long now) {
        EventHookLogModel log = new EventHookLogModel();
        log.setId(UUID.randomUUID().toString());
        log.setExecutionId(executionId);
        log.setStatus(EventHookLogStatus.FAILED);
        log.setMessageStatus(message.getStatus());
        log.setAttemptNumber(message.getAttemptCount());
        log.setStatusCode(result.getStatusCode());
        log.setDurationMs(result.getDurationMillis());
        log.setDetails(result.getDetails());
        log.setCreatedAt(now);
        log.setTest(message.isTest());
        store.createLog(log);
    }

    private String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }
}

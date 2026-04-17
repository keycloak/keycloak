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
import java.util.List;
import java.util.Map;
import java.util.UUID;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotAuthorizedException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;

import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.util.JsonSerialization;

import static org.keycloak.common.util.Time.currentTimeMillis;

@Produces(MediaType.APPLICATION_JSON)
public class PullEventHookTargetEndpointResource {

    private static final long CLAIM_TIMEOUT_MILLIS = java.util.concurrent.TimeUnit.MINUTES.toMillis(5);
    private static final String SECRET_HEADER = "X-Keycloak-Event-Hook-Secret";

    private final KeycloakSession session;
    private final EventHookTargetModel target;

    public PullEventHookTargetEndpointResource(KeycloakSession session, EventHookTargetModel target) {
        this.session = session;
        this.target = target;
    }

    @GET
    public EventHookPullRepresentation consumeGet(@jakarta.ws.rs.core.Context HttpHeaders headers) {
        return consume(headers);
    }

    @POST
    public EventHookPullRepresentation consumePost(@jakarta.ws.rs.core.Context HttpHeaders headers) {
        return consume(headers);
    }

    private EventHookPullRepresentation consume(HttpHeaders headers) {
        RealmModel realm = session.getContext().getRealm();
        if (target == null || !target.isEnabled()) {
            throw new NotFoundException("Event hook target not found");
        }

        validateSecret(headers);

        EventHookStoreProvider store = session.getProvider(EventHookStoreProvider.class);
        long now = currentTimeMillis();
        int maxResults = EventHookDeliveryTask.maxMessagesPerExecution(target, true);
        boolean batchMode = maxResults > 1;
        List<EventHookMessageModel> messages = store.claimAvailableMessagesForTarget(realm.getId(), target.getId(), maxResults, now,
                now - CLAIM_TIMEOUT_MILLIS, UUID.randomUUID().toString());

        List<EventHookPullEntryRepresentation> entries = messages.stream()
                .map(message -> toPullEntry(store, realm.getId(), message))
                .toList();

        if (!messages.isEmpty()) {
            String executionId = UUID.randomUUID().toString();
            boolean batchExecution = messages.size() > 1;
            messages.forEach(message -> completeMessage(store, message, executionId, batchExecution));
        }

        EventHookPullRepresentation representation = new EventHookPullRepresentation();
        if (!batchMode) {
            representation.setEvent(messages.isEmpty() ? null : readPayload(messages.get(0).getPayload()));
            representation.setEntry(entries.isEmpty() ? null : entries.get(0));
        } else {
            representation.setEvents(messages.stream().map(message -> readPayload(message.getPayload())).toList());
            representation.setEntries(entries);
        }
        representation.setHasMoreEvents(store.hasAvailableMessages(realm.getId(), target.getId(), currentTimeMillis(), currentTimeMillis() - CLAIM_TIMEOUT_MILLIS));
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
        message.setStatus(EventHookMessageStatus.SUCCESS);
        message.setClaimOwner(null);
        message.setClaimedAt(null);
        message.setUpdatedAt(now);
        message.setNextAttemptAt(now);
        message.setLastError(null);
        store.updateMessage(message);

        EventHookLogModel log = new EventHookLogModel();
        log.setId(UUID.randomUUID().toString());
        log.setExecutionId(executionId);
        log.setBatchExecution(batchExecution);
        log.setMessageId(message.getId());
        log.setTargetId(message.getTargetId());
        log.setStatus(EventHookLogStatus.SUCCESS);
        log.setAttemptNumber(message.getAttemptCount());
        log.setStatusCode("PULL_CONSUMED");
        log.setDurationMs(0L);
        log.setDetails("Consumed through the pull endpoint");
        log.setCreatedAt(now);
        store.createLog(log);
    }

    private EventHookPullEntryRepresentation toPullEntry(EventHookStoreProvider store, String realmId, EventHookMessageModel message) {
        EventHookLogModel latestLog = store.getLogsStream(realmId, message.getId(), message.getTargetId(), null, null, null, 0, 1)
                .findFirst()
                .orElse(null);

        EventHookPullEntryRepresentation entry = new EventHookPullEntryRepresentation();
        entry.setLogId(latestLog == null ? null : latestLog.getId());
        entry.setExecutionId(latestLog == null ? null : latestLog.getExecutionId());
        entry.setBatchExecution(latestLog == null ? null : latestLog.isBatchExecution());
        entry.setMessageId(message.getId());
        entry.setTargetId(message.getTargetId());
        entry.setSourceEventId(message.getSourceEventId());
        entry.setStatus(latestLog == null ? EventHookLogStatus.WAITING.name() : latestLog.getStatus().name());
        entry.setAttemptNumber(message.getAttemptCount());
        entry.setStatusCode(latestLog == null ? null : latestLog.getStatusCode());
        entry.setDurationMs(latestLog == null ? null : latestLog.getDurationMs());
        entry.setDetails(latestLog == null ? null : latestLog.getDetails());
        entry.setCreatedAt(latestLog == null ? message.getUpdatedAt() : latestLog.getCreatedAt());
        entry.setData(readPayload(message.getPayload()));
        return entry;
    }

    private Object readPayload(String payload) {
        try {
            return JsonSerialization.readValue(payload, Object.class);
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
}

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

package org.keycloak.events.hooks.jpa;

import java.io.IOException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;

import org.keycloak.events.hooks.EventHookLogModel;
import org.keycloak.events.hooks.EventHookLogStatus;
import org.keycloak.events.hooks.EventHookMessageModel;
import org.keycloak.events.hooks.EventHookMessageStatus;
import org.keycloak.events.hooks.EventHookSourceType;
import org.keycloak.events.hooks.EventHookStoreProvider;
import org.keycloak.events.hooks.EventHookTargetModel;
import org.keycloak.events.hooks.EventHookTargetStatus;
import org.keycloak.util.JsonSerialization;

public class JpaEventHookStoreProvider implements EventHookStoreProvider {

    private final EntityManager em;

    public JpaEventHookStoreProvider(EntityManager em) {
        this.em = em;
    }

    @Override
    public Stream<EventHookTargetModel> getTargetsStream(String realmId, Boolean enabled) {
        StringBuilder query = new StringBuilder("from EventHookTargetEntity where realmId = :realmId");
        if (enabled != null) {
            query.append(" and enabled = :enabled");
        }
        query.append(" order by name asc");

        TypedQuery<EventHookTargetEntity> typedQuery = em.createQuery(query.toString(), EventHookTargetEntity.class)
                .setParameter("realmId", realmId);
        if (enabled != null) {
            typedQuery.setParameter("enabled", enabled.booleanValue());
        }

        return typedQuery.getResultStream().map(this::toModel);
    }

    @Override
    public EventHookTargetModel getTarget(String realmId, String targetId) {
        return em.createQuery("from EventHookTargetEntity where realmId = :realmId and id = :id", EventHookTargetEntity.class)
                .setParameter("realmId", realmId)
                .setParameter("id", targetId)
                .getResultStream()
                .findFirst()
                .map(this::toModel)
                .orElse(null);
    }

    @Override
    public EventHookTargetModel createTarget(EventHookTargetModel target) {
        EventHookTargetEntity entity = new EventHookTargetEntity();
        applyTarget(entity, target, true);
        em.persist(entity);
        return toModel(entity);
    }

    @Override
    public EventHookTargetModel updateTarget(EventHookTargetModel target) {
        EventHookTargetEntity entity = em.find(EventHookTargetEntity.class, target.getId());
        if (entity == null || !entity.getRealmId().equals(target.getRealmId())) {
            return null;
        }

        applyTarget(entity, target, false);
        return toModel(entity);
    }

    @Override
    public boolean deleteTarget(String realmId, String targetId) {
        EventHookTargetEntity entity = em.find(EventHookTargetEntity.class, targetId);
        if (entity == null || !entity.getRealmId().equals(realmId)) {
            return false;
        }

        em.remove(entity);
        return true;
    }

    @Override
    public void createMessages(List<EventHookMessageModel> messages) {
        messages.forEach(message -> em.persist(toEntity(message)));
    }

    @Override
    public Stream<EventHookMessageModel> getMessagesStream(String realmId, String status, String targetId, Integer first, Integer max) {
        StringBuilder query = new StringBuilder("from EventHookMessageEntity where realmId = :realmId");
        if (status != null) {
            query.append(" and status = :status");
        }
        if (targetId != null) {
            query.append(" and targetId = :targetId");
        }
        query.append(" order by createdAt desc");

        TypedQuery<EventHookMessageEntity> typedQuery = em.createQuery(query.toString(), EventHookMessageEntity.class)
                .setParameter("realmId", realmId);
        if (status != null) {
            typedQuery.setParameter("status", status);
        }
        if (targetId != null) {
            typedQuery.setParameter("targetId", targetId);
        }
        if (first != null && first >= 0) {
            typedQuery.setFirstResult(first);
        }
        if (max != null && max > 0) {
            typedQuery.setMaxResults(max);
        }
        return typedQuery.getResultStream().map(this::toModel);
    }

    @Override
    public EventHookMessageModel getMessage(String realmId, String messageId) {
        return em.createQuery("from EventHookMessageEntity where realmId = :realmId and id = :id", EventHookMessageEntity.class)
                .setParameter("realmId", realmId)
                .setParameter("id", messageId)
                .getResultStream()
                .findFirst()
                .map(this::toModel)
                .orElse(null);
    }

    @Override
    public Stream<EventHookLogModel> getLogsStream(String realmId, String messageId, String targetId, String targetType,
            String sourceType, String event, String client, String user, String ipAddress,
            String resourceType, String resourcePath, String status, String messageStatus,
            Long dateFrom, Long dateTo, String executionId, String search, Integer first, Integer max) {
        StringBuilder query = new StringBuilder("select log, message from EventHookLogEntity log join EventHookMessageEntity message on message.id = log.messageId join EventHookTargetEntity target on target.id = log.targetId where message.realmId = :realmId");
        if (messageId != null) {
            query.append(" and log.messageId = :messageId");
        }
        if (targetId != null) {
            query.append(" and log.targetId = :targetId");
        }
        if (targetType != null) {
            query.append(" and target.type = :targetType");
        }
        if (sourceType != null) {
            query.append(" and message.sourceType = :sourceType");
        }
        if (event != null && !event.isBlank()) {
            query.append(" and (lower(coalesce(message.payload, '')) like :userEvent or lower(coalesce(message.payload, '')) like :adminEvent)");
        }
        if (client != null && !client.isBlank()) {
            query.append(" and lower(coalesce(message.payload, '')) like :client");
        }
        if (user != null && !user.isBlank()) {
            query.append(" and lower(coalesce(message.payload, '')) like :user");
        }
        if (ipAddress != null && !ipAddress.isBlank()) {
            query.append(" and lower(coalesce(message.payload, '')) like :ipAddress");
        }
        if (resourceType != null && !resourceType.isBlank()) {
            query.append(" and lower(coalesce(message.payload, '')) like :resourceType");
        }
        if (resourcePath != null && !resourcePath.isBlank()) {
            query.append(" and lower(coalesce(message.payload, '')) like :resourcePath");
        }
        if (status != null) {
            query.append(" and log.status = :status");
        }
        if (messageStatus != null) {
            query.append(" and message.status = :messageStatus");
        }
        if (dateFrom != null) {
            query.append(" and log.createdAt >= :dateFrom");
        }
        if (dateTo != null) {
            query.append(" and log.createdAt <= :dateTo");
        }
        if (executionId != null) {
            query.append(" and log.executionId = :executionId");
        }
        if (search != null && !search.isBlank()) {
            query.append(" and (lower(target.name) like :search or lower(target.type) like :search or lower(log.executionId) like :search or lower(log.messageId) like :search or lower(log.targetId) like :search or lower(log.status) like :search or lower(coalesce(message.sourceEventId, '')) like :search or lower(coalesce(message.payload, '')) like :search or lower(coalesce(log.statusCode, '')) like :search or lower(coalesce(log.details, '')) like :search)");
        }
        query.append(" order by log.createdAt desc");

        TypedQuery<Object[]> typedQuery = em.createQuery(query.toString(), Object[].class)
                .setParameter("realmId", realmId);
        if (messageId != null) {
            typedQuery.setParameter("messageId", messageId);
        }
        if (targetId != null) {
            typedQuery.setParameter("targetId", targetId);
        }
        if (targetType != null) {
            typedQuery.setParameter("targetType", targetType);
        }
        if (sourceType != null) {
            typedQuery.setParameter("sourceType", sourceType);
        }
        if (event != null && !event.isBlank()) {
            typedQuery.setParameter("userEvent", containsJsonValue("eventType", event));
            typedQuery.setParameter("adminEvent", containsJsonValue("operationType", event));
        }
        if (client != null && !client.isBlank()) {
            typedQuery.setParameter("client", containsJsonValue("clientId", client));
        }
        if (user != null && !user.isBlank()) {
            typedQuery.setParameter("user", containsJsonValue("userId", user));
        }
        if (ipAddress != null && !ipAddress.isBlank()) {
            typedQuery.setParameter("ipAddress", containsJsonValue("ipAddress", ipAddress));
        }
        if (resourceType != null && !resourceType.isBlank()) {
            typedQuery.setParameter("resourceType", containsJsonValue("resourceType", resourceType));
        }
        if (resourcePath != null && !resourcePath.isBlank()) {
            typedQuery.setParameter("resourcePath", containsJsonValue("resourcePath", resourcePath));
        }
        if (status != null) {
            typedQuery.setParameter("status", status);
        }
        if (messageStatus != null) {
            typedQuery.setParameter("messageStatus", messageStatus);
        }
        if (dateFrom != null) {
            typedQuery.setParameter("dateFrom", dateFrom.longValue());
        }
        if (dateTo != null) {
            typedQuery.setParameter("dateTo", dateTo.longValue());
        }
        if (executionId != null) {
            typedQuery.setParameter("executionId", executionId);
        }
        if (search != null && !search.isBlank()) {
            typedQuery.setParameter("search", "%" + search.toLowerCase() + "%");
        }
        if (first != null && first >= 0) {
            typedQuery.setFirstResult(first);
        }
        if (max != null && max > 0) {
            typedQuery.setMaxResults(max);
        }
        return typedQuery.getResultStream().map(result -> toLogModel((EventHookLogEntity) result[0], (EventHookMessageEntity) result[1]));
    }

    @Override
    public Map<String, EventHookTargetStatus> getLatestTargetStatuses(String realmId) {
        List<EventHookLogEntity> latestLogs = em.createQuery(
                "select log from EventHookLogEntity log join EventHookMessageEntity message on message.id = log.messageId where message.realmId = :realmId and log.createdAt = (select max(innerLog.createdAt) from EventHookLogEntity innerLog join EventHookMessageEntity innerMessage on innerMessage.id = innerLog.messageId where innerMessage.realmId = :realmId and innerLog.targetId = log.targetId) order by log.targetId asc, log.attemptNumber desc",
                EventHookLogEntity.class)
                .setParameter("realmId", realmId)
                .getResultList();

        Map<String, EventHookTargetStatus> statuses = new LinkedHashMap<>();
        for (EventHookLogEntity latestLog : latestLogs) {
            statuses.putIfAbsent(latestLog.getTargetId(), toTargetStatus(EventHookLogStatus.valueOf(latestLog.getStatus())));
        }

        return statuses;
    }

    private EventHookTargetStatus toTargetStatus(EventHookLogStatus status) {
        return switch (status) {
            case SUCCESS -> EventHookTargetStatus.OK;
            case FAILED, PENDING -> EventHookTargetStatus.HAS_PROBLEMS;
            case WAITING -> EventHookTargetStatus.OK;
        };
    }

    @Override
    public List<EventHookMessageModel> claimAvailableMessages(int maxResults, long now, long staleClaimBefore, String claimOwner) {
        return em.createQuery("from EventHookMessageEntity where (status = :pending and nextAttemptAt <= :now) or (status = :claimed and claimedAt is not null and claimedAt < :staleClaimBefore) order by nextAttemptAt asc, createdAt asc", EventHookMessageEntity.class)
                .setParameter("pending", EventHookMessageStatus.PENDING.name())
                .setParameter("claimed", EventHookMessageStatus.CLAIMED.name())
                .setParameter("now", now)
                .setParameter("staleClaimBefore", staleClaimBefore)
                .setMaxResults(maxResults)
                .getResultStream()
                .map(entity -> claim(entity, now, claimOwner))
                .map(this::toModel)
                .toList();
    }

    @Override
    public List<EventHookMessageModel> claimAvailableMessages(int maxResults, long now, long staleClaimBefore, String claimOwner,
            List<String> targetTypes) {
        if (targetTypes == null || targetTypes.isEmpty()) {
            return List.of();
        }

        return em.createQuery("select message from EventHookMessageEntity message join EventHookTargetEntity target on target.id = message.targetId where target.type in :targetTypes and ((message.status = :pending and message.nextAttemptAt <= :now) or (message.status = :claimed and message.claimedAt is not null and message.claimedAt < :staleClaimBefore)) order by message.nextAttemptAt asc, message.createdAt asc", EventHookMessageEntity.class)
                .setParameter("targetTypes", targetTypes)
                .setParameter("pending", EventHookMessageStatus.PENDING.name())
                .setParameter("claimed", EventHookMessageStatus.CLAIMED.name())
                .setParameter("now", now)
                .setParameter("staleClaimBefore", staleClaimBefore)
                .setMaxResults(maxResults)
                .getResultStream()
                .map(entity -> claim(entity, now, claimOwner))
                .map(this::toModel)
                .toList();
    }

    @Override
    public List<EventHookMessageModel> claimAvailableMessagesForTarget(String realmId, String targetId, int maxResults, long now,
            long staleClaimBefore, String claimOwner) {
        return em.createQuery("from EventHookMessageEntity where realmId = :realmId and targetId = :targetId and (status = :waiting or (status = :claimed and claimedAt is not null and claimedAt < :staleClaimBefore)) order by createdAt asc", EventHookMessageEntity.class)
                .setParameter("realmId", realmId)
                .setParameter("targetId", targetId)
            .setParameter("waiting", EventHookMessageStatus.WAITING.name())
                .setParameter("claimed", EventHookMessageStatus.CLAIMED.name())
                .setParameter("staleClaimBefore", staleClaimBefore)
                .setMaxResults(maxResults)
                .getResultStream()
                .map(entity -> claim(entity, now, claimOwner))
                .map(this::toModel)
                .toList();
    }

    @Override
    public Long getPendingAggregationDeadline(String realmId, String targetId, long now) {
        return em.createQuery("select min(message.nextAttemptAt) from EventHookMessageEntity message where message.realmId = :realmId and message.targetId = :targetId and message.status = :pending and message.nextAttemptAt > :now", Long.class)
                .setParameter("realmId", realmId)
                .setParameter("targetId", targetId)
                .setParameter("pending", EventHookMessageStatus.PENDING.name())
                .setParameter("now", now)
                .getSingleResult();
    }

    @Override
    public boolean hasAvailableMessages(String realmId, String targetId, long now, long staleClaimBefore) {
        Long count = em.createQuery("select count(message.id) from EventHookMessageEntity message where message.realmId = :realmId and message.targetId = :targetId and (message.status = :waiting or (message.status = :claimed and message.claimedAt is not null and message.claimedAt < :staleClaimBefore))", Long.class)
                .setParameter("realmId", realmId)
                .setParameter("targetId", targetId)
            .setParameter("waiting", EventHookMessageStatus.WAITING.name())
                .setParameter("claimed", EventHookMessageStatus.CLAIMED.name())
                .setParameter("staleClaimBefore", staleClaimBefore)
                .getSingleResult();
        return count != null && count.longValue() > 0;
    }

    @Override
    public EventHookMessageModel updateMessage(EventHookMessageModel message) {
        EventHookMessageEntity entity = em.find(EventHookMessageEntity.class, message.getId());
        if (entity == null) {
            return null;
        }

        applyMessage(entity, message);
        return toModel(entity);
    }

    @Override
    public void createLog(EventHookLogModel log) {
        EventHookLogEntity entity = new EventHookLogEntity();
        entity.setId(log.getId() == null ? UUID.randomUUID().toString() : log.getId());
        entity.setExecutionId(log.getExecutionId());
        entity.setBatchExecution(log.isBatchExecution());
        entity.setMessageId(log.getMessageId());
        entity.setTargetId(log.getTargetId());
        entity.setStatus(log.getStatus().name());
        entity.setAttemptNumber(log.getAttemptNumber());
        entity.setStatusCode(log.getStatusCode());
        entity.setDurationMs(log.getDurationMs());
        entity.setDetails(log.getDetails());
        entity.setCreatedAt(log.getCreatedAt());
        em.persist(entity);
    }

    @Override
    public void close() {
    }

    private EventHookMessageEntity claim(EventHookMessageEntity entity, long now, String claimOwner) {
        entity.setStatus(EventHookMessageStatus.CLAIMED.name());
        entity.setClaimOwner(claimOwner);
        entity.setClaimedAt(now);
        entity.setUpdatedAt(now);
        return entity;
    }

    private void applyTarget(EventHookTargetEntity entity, EventHookTargetModel model, boolean create) {
        entity.setId(create ? (model.getId() == null ? UUID.randomUUID().toString() : model.getId()) : model.getId());
        entity.setRealmId(model.getRealmId());
        entity.setName(model.getName());
        entity.setType(model.getType());
        entity.setEnabled(model.isEnabled());
        entity.setCreatedAt(create ? model.getCreatedAt() : entity.getCreatedAt());
        entity.setUpdatedAt(model.getUpdatedAt());
        entity.setSettingsJson(writeJson(model.getSettings()));
    }

    private EventHookTargetModel toModel(EventHookTargetEntity entity) {
        EventHookTargetModel model = new EventHookTargetModel();
        model.setId(entity.getId());
        model.setRealmId(entity.getRealmId());
        model.setName(entity.getName());
        model.setType(entity.getType());
        model.setEnabled(entity.isEnabled());
        model.setCreatedAt(entity.getCreatedAt());
        model.setUpdatedAt(entity.getUpdatedAt());
        model.setSettings(readJson(entity.getSettingsJson()));
        return model;
    }

    private EventHookMessageModel toModel(EventHookMessageEntity entity) {
        EventHookMessageModel model = new EventHookMessageModel();
        model.setId(entity.getId());
        model.setRealmId(entity.getRealmId());
        model.setTargetId(entity.getTargetId());
        model.setSourceType(EventHookSourceType.valueOf(entity.getSourceType()));
        model.setSourceEventId(entity.getSourceEventId());
        model.setStatus(EventHookMessageStatus.valueOf(entity.getStatus()));
        model.setPayload(entity.getPayload());
        model.setAttemptCount(entity.getAttemptCount());
        model.setNextAttemptAt(entity.getNextAttemptAt());
        model.setCreatedAt(entity.getCreatedAt());
        model.setUpdatedAt(entity.getUpdatedAt());
        model.setClaimOwner(entity.getClaimOwner());
        model.setClaimedAt(entity.getClaimedAt());
        model.setLastError(entity.getLastError());
        return model;
    }

    private EventHookLogModel toLogModel(EventHookLogEntity entity, EventHookMessageEntity messageEntity) {
        EventHookLogModel model = new EventHookLogModel();
        model.setId(entity.getId());
        model.setExecutionId(entity.getExecutionId());
        model.setBatchExecution(entity.isBatchExecution());
        model.setMessageId(entity.getMessageId());
        model.setTargetId(entity.getTargetId());
        model.setSourceType(EventHookSourceType.valueOf(messageEntity.getSourceType()));
        model.setSourceEventId(messageEntity.getSourceEventId());
        model.setSourceEventName(extractSourceEventName(messageEntity.getPayload(), EventHookSourceType.valueOf(messageEntity.getSourceType())));
        model.setStatus(EventHookLogStatus.valueOf(entity.getStatus()));
        model.setMessageStatus(EventHookMessageStatus.valueOf(messageEntity.getStatus()));
        model.setAttemptNumber(entity.getAttemptNumber());
        model.setStatusCode(entity.getStatusCode());
        model.setDurationMs(entity.getDurationMs());
        model.setDetails(entity.getDetails());
        model.setCreatedAt(entity.getCreatedAt());
        return model;
    }

    private EventHookMessageEntity toEntity(EventHookMessageModel model) {
        EventHookMessageEntity entity = new EventHookMessageEntity();
        entity.setId(model.getId() == null ? UUID.randomUUID().toString() : model.getId());
        applyMessage(entity, model);
        return entity;
    }

    private void applyMessage(EventHookMessageEntity entity, EventHookMessageModel model) {
        entity.setRealmId(model.getRealmId());
        entity.setTargetId(model.getTargetId());
        entity.setSourceType(model.getSourceType() == null ? EventHookSourceType.USER.name() : model.getSourceType().name());
        entity.setSourceEventId(model.getSourceEventId());
        entity.setStatus(model.getStatus().name());
        entity.setPayload(model.getPayload());
        entity.setAttemptCount(model.getAttemptCount());
        entity.setNextAttemptAt(model.getNextAttemptAt());
        entity.setCreatedAt(model.getCreatedAt());
        entity.setUpdatedAt(model.getUpdatedAt());
        entity.setClaimOwner(model.getClaimOwner());
        entity.setClaimedAt(model.getClaimedAt());
        entity.setLastError(model.getLastError());
    }

    private String writeJson(Map<String, Object> value) {
        if (value == null || value.isEmpty()) {
            return null;
        }

        try {
            return JsonSerialization.writeValueAsString(value);
        } catch (IOException exception) {
            throw new IllegalArgumentException("Failed to serialize event hook settings", exception);
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> readJson(String value) {
        if (value == null) {
            return Collections.emptyMap();
        }

        try {
            return JsonSerialization.readValue(value, Map.class);
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to deserialize event hook settings", exception);
        }
    }

    @SuppressWarnings("unchecked")
    private String extractSourceEventName(String payload, EventHookSourceType sourceType) {
        if (payload == null || sourceType == null) {
            return null;
        }

        try {
            Map<String, Object> payloadMap = JsonSerialization.readValue(payload, Map.class);
            Object value = sourceType == EventHookSourceType.ADMIN ? payloadMap.get("operationType") : payloadMap.get("eventType");
            return value == null ? null : value.toString();
        } catch (IOException exception) {
            return null;
        }
    }

    private String containsJsonValue(String key, String value) {
        return "%\"" + key.toLowerCase() + "\":\"" + value.toLowerCase() + "\"%";
    }
}

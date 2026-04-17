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

package org.keycloak.services.resources.admin;

import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.keycloak.events.admin.OperationType;
import org.keycloak.events.hooks.EventHookDeliveryResult;
import org.keycloak.events.hooks.EventHookDeliveryTask;
import org.keycloak.events.hooks.EventHookLogModel;
import org.keycloak.events.hooks.EventHookLogStatus;
import org.keycloak.events.hooks.EventHookMessageModel;
import org.keycloak.events.hooks.EventHookMessageStatus;
import org.keycloak.events.hooks.EventHookPayloadNormalizer;
import org.keycloak.events.hooks.EventHookStoreProvider;
import org.keycloak.events.hooks.EventHookTestExample;
import org.keycloak.events.hooks.EventHookTestExamples;
import org.keycloak.events.hooks.EventHookTargetModel;
import org.keycloak.events.hooks.EventHookTargetProvider;
import org.keycloak.events.hooks.EventHookTargetProviderFactory;
import org.keycloak.events.hooks.EventHookTargetRepresentationUtil;
import org.keycloak.events.hooks.EventHookTargetStatus;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ProviderFactory;
import org.keycloak.representations.idm.ConfigPropertyRepresentation;
import org.keycloak.representations.idm.EventHookLogRepresentation;
import org.keycloak.representations.idm.EventHookMessageRepresentation;
import org.keycloak.representations.idm.EventHookProviderRepresentation;
import org.keycloak.representations.idm.EventHookTargetRepresentation;
import org.keycloak.representations.idm.EventHookTestExampleRepresentation;
import org.keycloak.representations.idm.EventHookTestResultRepresentation;
import org.keycloak.services.ErrorResponse;
import org.keycloak.services.util.DateUtil;
import org.keycloak.services.resources.admin.fgap.AdminPermissionEvaluator;
import org.keycloak.util.JsonSerialization;

import static org.keycloak.common.util.Time.currentTimeMillis;

@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class EventHooksResource {

    private static final EnumSet<EventHookMessageStatus> RETRYABLE_MESSAGE_STATUSES = EnumSet.of(
            EventHookMessageStatus.FAILED,
            EventHookMessageStatus.EXHAUSTED,
            EventHookMessageStatus.DEAD);

    private final RealmModel realm;
    private final AdminPermissionEvaluator auth;
    private final AdminEventBuilder adminEvent;
    private final KeycloakSession session;

    public EventHooksResource(KeycloakSession session, AdminPermissionEvaluator auth, AdminEventBuilder adminEvent) {
        this.session = session;
        this.auth = auth;
        this.realm = session.getContext().getRealm();
        this.adminEvent = adminEvent;
    }

    @GET
    @Path("targets")
    public List<EventHookTargetRepresentation> getTargets() {
        auth.realm().requireManageRealm();
        EventHookStoreProvider store = session.getProvider(EventHookStoreProvider.class);
        Map<String, EventHookTargetStatus> latestStatuses = store.getLatestTargetStatuses(realm.getId());
        return store.getTargetsStream(realm.getId())
            .map(target -> toRepresentation(target, latestStatuses))
                .toList();
    }

    @POST
    @Path("targets")
    public Response createTarget(EventHookTargetRepresentation representation) {
        auth.realm().requireManageRealm();
        validateRepresentation(representation);
        EventHookTargetModel target = toTargetModel(representation, null);

        EventHookTargetModel created = session.getProvider(EventHookStoreProvider.class).createTarget(target);
        EventHookTargetRepresentation response = toRepresentation(created);
        adminEvent.operation(OperationType.CREATE).resourcePath(session.getContext().getUri(), created.getId()).representation(response).success();
        return Response.status(Response.Status.CREATED)
                .location(session.getContext().getUri().getAbsolutePathBuilder().path(created.getId()).build())
                .entity(response)
                .build();
    }

    @GET
    @Path("targets/{targetId}")
    public EventHookTargetRepresentation getTarget(@PathParam("targetId") String targetId) {
        auth.realm().requireManageRealm();
        EventHookTargetModel target = getTargetOrThrow(targetId);
        return toRepresentation(target);
    }

    @POST
    @Path("targets/test")
    public EventHookTestResultRepresentation testTarget(EventHookTargetRepresentation representation,
            @QueryParam("exampleId") String exampleId) {
        auth.realm().requireManageRealm();
        validateRepresentation(representation);

        EventHookTargetModel existing = null;
        if (representation.getId() != null && !representation.getId().isBlank()) {
            existing = getTargetOrThrow(representation.getId());
            if (!existing.getType().equals(representation.getType())) {
                throw new BadRequestException("Target type cannot be changed during test");
            }
        }

        EventHookTargetModel target = toTargetModel(representation, existing);
        if (target.getId() == null || target.getId().isBlank()) {
            target.setId(UUID.randomUUID().toString());
        }
        EventHookTargetProviderFactory providerFactory = getTargetProviderFactory(target.getType());
        EventHookStoreProvider store = session.getProvider(EventHookStoreProvider.class);
        List<EventHookMessageModel> messages;
        try {
            messages = providerFactory.createTestMessages(session, realm, target, exampleId);
        } catch (Exception exception) {
            throw ErrorResponse.error("Failed to create test event hook message: " + exception.getMessage(), Response.Status.INTERNAL_SERVER_ERROR);
        }

        store.createMessages(messages);
        EventHookDeliveryResult result = providerFactory.test(session, realm, target, messages);
        createTestExecution(store, target, messages, result);

        adminEvent.operation(OperationType.ACTION)
                .resourcePath(session.getContext().getUri())
                .representation(toRepresentation(target))
                .success();

        return toRepresentation(result);
    }

    @PUT
    @Path("targets/{targetId}")
    public EventHookTargetRepresentation updateTarget(@PathParam("targetId") String targetId, EventHookTargetRepresentation representation) {
        auth.realm().requireManageRealm();
        validateRepresentation(representation);

        EventHookTargetModel existing = getTargetOrThrow(targetId);
        EventHookTargetModel updated = session.getProvider(EventHookStoreProvider.class).updateTarget(toTargetModel(representation, existing));
        EventHookTargetRepresentation response = toRepresentation(updated);
        adminEvent.operation(OperationType.UPDATE).resourcePath(session.getContext().getUri()).representation(response).success();
        return response;
    }

    @DELETE
    @Path("targets/{targetId}")
    public void deleteTarget(@PathParam("targetId") String targetId) {
        auth.realm().requireManageRealm();
        boolean deleted = session.getProvider(EventHookStoreProvider.class).deleteTarget(realm.getId(), targetId);
        if (!deleted) {
            throw new NotFoundException("Event hook target not found");
        }

        adminEvent.operation(OperationType.DELETE).resourcePath(session.getContext().getUri()).success();
    }

    @GET
    @Path("providers")
    public List<EventHookProviderRepresentation> getProviders() {
        auth.realm().requireManageRealm();
        return session.getKeycloakSessionFactory().getProviderFactoriesStream(EventHookTargetProvider.class)
                .map(EventHookTargetProviderFactory.class::cast)
                .sorted(Comparator.comparing(ProviderFactory::getId))
                .map(this::toRepresentation)
                .toList();
    }

    @GET
    @Path("test-examples")
    public List<EventHookTestExampleRepresentation> getTestExamples() {
        auth.realm().requireManageRealm();
        EventHookTargetModel target = new EventHookTargetModel();
        target.setRealmId(realm.getId());
        return EventHookTestExamples.all(realm, target).stream().map(this::toRepresentation).toList();
    }

    @GET
    @Path("messages")
    public List<EventHookMessageRepresentation> getMessages(@QueryParam("status") String status,
            @QueryParam("targetId") String targetId,
            @QueryParam("targetType") String targetType,
            @QueryParam("sourceType") String sourceType,
            @QueryParam("event") String event,
            @QueryParam("client") String client,
            @QueryParam("user") String user,
            @QueryParam("ipAddress") String ipAddress,
            @QueryParam("resourceType") String resourceType,
            @QueryParam("resourcePath") String resourcePath,
            @QueryParam("executionId") String executionId,
            @QueryParam("search") String search,
            @QueryParam("first") Integer first,
            @QueryParam("max") Integer max) {
        auth.realm().requireManageRealm();
        return session.getProvider(EventHookStoreProvider.class)
            .getMessagesStream(realm.getId(), status, targetId, targetType, sourceType, event, client, user,
                ipAddress, resourceType, resourcePath, executionId, search, first, max)
                .map(this::toRepresentation)
                .toList();
    }

    @GET
    @Path("messages/{messageId}")
    public EventHookMessageRepresentation getMessage(@PathParam("messageId") String messageId) {
        auth.realm().requireManageRealm();
        EventHookMessageModel message = session.getProvider(EventHookStoreProvider.class).getMessage(realm.getId(), messageId);
        if (message == null) {
            throw new NotFoundException("Event hook message not found");
        }
        return toRepresentation(message);
    }

    @POST
    @Path("executions/{executionId}/retry")
    public List<EventHookMessageRepresentation> retryExecution(@PathParam("executionId") String executionId) {
        auth.realm().requireManageRealm();

        EventHookStoreProvider store = session.getProvider(EventHookStoreProvider.class);
        List<EventHookMessageModel> executionMessages = store
                .getMessagesStream(realm.getId(), null, null, null, null, null, null, null, null, null, null,
                        executionId, null, null, null)
                .toList();
        if (executionMessages.isEmpty()) {
            throw new NotFoundException("Event hook execution not found");
        }

        List<EventHookMessageModel> retryableMessages = executionMessages.stream()
                .filter(message -> RETRYABLE_MESSAGE_STATUSES.contains(message.getStatus()))
                .toList();
        if (retryableMessages.isEmpty()) {
            throw ErrorResponse.error("Event hook execution cannot be retried from current message statuses",
                    Response.Status.BAD_REQUEST);
        }

        int previousAttemptCount = retryableMessages.stream()
                .mapToInt(EventHookMessageModel::getAttemptCount)
                .max()
                .orElse(0);
        long now = currentTimeMillis();
        String retriedExecutionId = UUID.randomUUID().toString();
        boolean executionBatch = retryableMessages.size() > 1;

        List<EventHookMessageRepresentation> representations = retryableMessages.stream()
                .map(message -> {
                    queueMessageForRetry(message, retriedExecutionId, now);
                message.setExecutionBatch(executionBatch);
                    EventHookMessageModel updated = store.updateMessage(message);
                    return toRepresentation(updated == null ? message : updated);
                })
                .toList();

        createRetryLog(store, retryableMessages.get(0), retriedExecutionId, previousAttemptCount, now);

        if (adminEvent != null) {
            adminEvent.operation(OperationType.ACTION)
                    .resourcePath(session.getContext().getUri())
                    .representation(representations)
                    .success();
        }
        return representations;
    }

    @GET
    @Path("messages/{messageId}/logs")
    public List<EventHookLogRepresentation> getMessageLogs(@PathParam("messageId") String messageId,
            @QueryParam("first") Integer first,
            @QueryParam("max") Integer max) {
        auth.realm().requireManageRealm();
        return session.getProvider(EventHookStoreProvider.class)
                .getLogsStream(realm.getId(), messageId, null, first, max)
                .map(this::toRepresentation)
                .toList();
    }

    @GET
    @Path("logs")
    public List<EventHookLogRepresentation> getLogs(@QueryParam("targetId") String targetId,
            @QueryParam("targetType") String targetType,
            @QueryParam("sourceType") String sourceType,
            @QueryParam("event") String event,
            @QueryParam("client") String client,
            @QueryParam("user") String user,
            @QueryParam("ipAddress") String ipAddress,
            @QueryParam("resourceType") String resourceType,
            @QueryParam("resourcePath") String resourcePath,
            @QueryParam("status") String status,
            @QueryParam("messageStatus") String messageStatus,
            @QueryParam("dateFrom") String dateFrom,
            @QueryParam("dateTo") String dateTo,
            @QueryParam("executionId") String executionId,
            @QueryParam("search") String search,
            @QueryParam("messageId") String messageId,
            @QueryParam("first") Integer first,
            @QueryParam("max") Integer max) {
        auth.realm().requireManageRealm();

        Long fromTime = null;
        Long toTime = null;

        if (dateFrom != null) {
            try {
                fromTime = DateUtil.toStartOfDay(dateFrom);
            } catch (Throwable throwable) {
                throw new BadRequestException("Invalid value for 'dateFrom', expected format is yyyy-MM-dd or an Epoch timestamp");
            }
        }

        if (dateTo != null) {
            try {
                toTime = DateUtil.toEndOfDay(dateTo);
            } catch (Throwable throwable) {
                throw new BadRequestException("Invalid value for 'dateTo', expected format is yyyy-MM-dd or an Epoch timestamp");
            }
        }

        return session.getProvider(EventHookStoreProvider.class)
                .getLogsStream(realm.getId(), messageId, targetId, targetType, sourceType, event, client, user,
                        ipAddress, resourceType, resourcePath, status, messageStatus, fromTime, toTime,
                        executionId, search, first, max)
                .map(this::toRepresentation)
                .toList();
    }

    @Path("{targetId}/{endpoint}")
    public Object getTargetEndpoint(@PathParam("targetId") String targetId, @PathParam("endpoint") String endpoint) {
        EventHookTargetModel target = getTargetOrThrow(targetId);
        EventHookTargetProviderFactory providerFactory = getTargetProviderFactory(target.getType());
        Object resource = providerFactory.getTargetEndpointResource(session, realm, target, endpoint);
        if (resource != null) {
            return resource;
        }

        throw new NotFoundException("Event hook target endpoint not found");
    }

    private EventHookTargetModel getTargetOrThrow(String targetId) {
        EventHookTargetModel target = session.getProvider(EventHookStoreProvider.class).getTarget(realm.getId(), targetId);
        if (target == null) {
            throw new NotFoundException("Event hook target not found");
        }
        return target;
    }

    private EventHookTargetProviderFactory getTargetProviderFactory(String type) {
        if (type == null || type.isBlank()) {
            throw ErrorResponse.error("Target type is required", Response.Status.BAD_REQUEST);
        }

        EventHookTargetProviderFactory factory = (EventHookTargetProviderFactory) session.getKeycloakSessionFactory()
                .getProviderFactory(EventHookTargetProvider.class, type);
        if (factory == null) {
            throw ErrorResponse.error("Unknown event hook target type: " + type, Response.Status.BAD_REQUEST);
        }
        return factory;
    }

    private EventHookTargetRepresentation toRepresentation(EventHookTargetModel model) {
        return toRepresentation(model, Map.of());
    }

    private EventHookTargetRepresentation toRepresentation(EventHookTargetModel model,
            Map<String, EventHookTargetStatus> latestStatuses) {
        EventHookTargetRepresentation representation = EventHookTargetRepresentationUtil.toRepresentation(session, model, true);
        representation.setStatus(latestStatuses.getOrDefault(model.getId(), EventHookTargetStatus.NOT_USED).name());
        return representation;
    }

    private EventHookTestResultRepresentation toRepresentation(EventHookDeliveryResult result) {
        EventHookTestResultRepresentation representation = new EventHookTestResultRepresentation();
        representation.setSuccess(result.isSuccess());
        representation.setRetryable(result.isRetryable());
        representation.setStatusCode(result.getStatusCode());
        representation.setDetails(result.getDetails());
        representation.setDurationMs(result.getDurationMillis());
        return representation;
    }

    private EventHookProviderRepresentation toRepresentation(EventHookTargetProviderFactory factory) {
        EventHookProviderRepresentation representation = new EventHookProviderRepresentation();
        representation.setId(factory.getId());
        representation.setSupportsRetry(factory.supportsRetry());
        representation.setSupportsBatch(factory.supportsBatch());
        representation.setSupportsAggregation(factory.supportsAggregation());
        representation.setConfigMetadata(factory.getConfigMetadata().stream().map(this::toRepresentation).toList());
        return representation;
    }

    private EventHookMessageRepresentation toRepresentation(EventHookMessageModel model) {
        EventHookMessageRepresentation representation = new EventHookMessageRepresentation();
        representation.setId(model.getId());
        representation.setTargetId(model.getTargetId());
        representation.setExecutionId(model.getExecutionId());
        representation.setSourceType(model.getSourceType().name());
        representation.setSourceEventId(model.getSourceEventId());
        representation.setSourceEventName(model.getSourceEventName());
        representation.setUserId(model.getUserId());
        representation.setResourcePath(model.getResourcePath());
        representation.setExecutionBatch(model.isExecutionBatch());
        representation.setStatus(model.getStatus().name());
        representation.setAttemptCount(model.getAttemptCount());
        representation.setNextAttemptAt(model.getNextAttemptAt());
        representation.setExecutionStartedAt(model.getExecutionStartedAt());
        representation.setCreatedAt(model.getCreatedAt());
        representation.setUpdatedAt(model.getUpdatedAt());
        representation.setLastError(model.getLastError());
        representation.setTest(model.isTest());
        representation.setPayload(readPayload(model.getPayload()));
        return representation;
    }

    private EventHookTestExampleRepresentation toRepresentation(EventHookTestExample model) {
        EventHookTestExampleRepresentation representation = new EventHookTestExampleRepresentation();
        representation.setId(model.getId());
        representation.setSourceType(model.getSourceType().name());
        representation.setEventName(model.getEventName());
        representation.setPayload(model.getPayload());
        return representation;
    }

    private EventHookLogRepresentation toRepresentation(EventHookLogModel model) {
        EventHookLogRepresentation representation = new EventHookLogRepresentation();
        representation.setId(model.getId());
        representation.setExecutionId(model.getExecutionId());
        representation.setStatus(model.getStatus().name());
        representation.setMessageStatus(model.getMessageStatus() == null ? null : model.getMessageStatus().name());
        representation.setAttemptNumber(model.getAttemptNumber());
        representation.setStatusCode(model.getStatusCode());
        representation.setDurationMs(model.getDurationMs());
        representation.setDetails(model.getDetails());
        representation.setCreatedAt(model.getCreatedAt());
        representation.setTest(model.isTest());
        return representation;
    }

    private ConfigPropertyRepresentation toRepresentation(ProviderConfigProperty property) {
        ConfigPropertyRepresentation representation = new ConfigPropertyRepresentation();
        representation.setName(property.getName());
        representation.setLabel(property.getLabel());
        representation.setHelpText(property.getHelpText());
        representation.setType(property.getType());
        representation.setDefaultValue(property.getDefaultValue());
        representation.setOptions(property.getOptions());
        representation.setSecret(property.isSecret());
        representation.setRequired(property.isRequired());
        representation.setReadOnly(property.isReadOnly());
        return representation;
    }

    private void validateRepresentation(EventHookTargetRepresentation representation) {
        if (representation == null) {
            throw ErrorResponse.error("Target representation is required", Response.Status.BAD_REQUEST);
        }
        if (representation.getSettings() == null) {
            representation.setSettings(Map.of());
        }
    }

    private EventHookTargetModel toTargetModel(EventHookTargetRepresentation representation, EventHookTargetModel existing) {
        try {
            return EventHookTargetRepresentationUtil.toModel(session, realm, representation, existing, currentTimeMillis(), false);
        } catch (IllegalArgumentException exception) {
            throw ErrorResponse.error(exception.getMessage(), Response.Status.BAD_REQUEST);
        }
    }

    private Object readPayload(String payload) {
        if (payload == null) {
            return null;
        }
        try {
            return EventHookPayloadNormalizer.readPayload(payload);
        } catch (Exception exception) {
            return payload;
        }
    }

    private void queueMessageForRetry(EventHookMessageModel message, String executionId, long now) {
        message.setExecutionId(executionId);
        message.setExecutionBatch(false);
        message.setStatus(EventHookMessageStatus.PENDING);
        message.setAttemptCount(0);
        message.setNextAttemptAt(now);
        message.setExecutionStartedAt(null);
        message.setUpdatedAt(now);
        message.setLastError(null);
    }

        private void createRetryLog(EventHookStoreProvider store, EventHookMessageModel message, String executionId,
            int previousAttemptCount, long now) {
        EventHookLogModel log = new EventHookLogModel();
        log.setId(UUID.randomUUID().toString());
        log.setExecutionId(executionId);
        log.setStatus(EventHookLogStatus.PENDING);
        log.setMessageStatus(message.getStatus());
        log.setAttemptNumber(previousAttemptCount);
        log.setStatusCode("MANUAL_RETRY");
        log.setDetails("Manual retry requested");
        log.setCreatedAt(now);
        log.setTest(message.isTest());
        store.createLog(log);
    }

    private void createTestExecution(EventHookStoreProvider store, EventHookTargetModel target, List<EventHookMessageModel> messages,
            EventHookDeliveryResult result) {
        String executionId = UUID.randomUUID().toString();
        boolean batchExecution = messages.size() > 1;
        long now = currentTimeMillis();
        messages.forEach(message -> {
            message.setExecutionId(executionId);
            message.setExecutionBatch(batchExecution);
            EventHookDeliveryTask.applyDeliveryResultToMessage(message, target, result, now);
            store.updateMessage(message);
        });

        EventHookMessageModel representativeMessage = messages.get(0);
        EventHookLogModel log = new EventHookLogModel();
        log.setId(UUID.randomUUID().toString());
        log.setExecutionId(executionId);
        log.setStatus(result.isSuccess() ? EventHookLogStatus.SUCCESS : result.isWaiting() ? EventHookLogStatus.WAITING : EventHookLogStatus.FAILED);
        log.setAttemptNumber(representativeMessage.getAttemptCount());
        log.setStatusCode(result.getStatusCode());
        log.setDurationMs(result.getDurationMillis());
        log.setDetails(result.getDetails());
        log.setCreatedAt(now);
        log.setTest(representativeMessage.isTest());
        store.createLog(log);
    }

}

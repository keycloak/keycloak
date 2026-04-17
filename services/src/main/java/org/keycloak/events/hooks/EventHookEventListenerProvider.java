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
import java.util.List;
import java.util.UUID;

import org.keycloak.events.Event;
import org.keycloak.events.EventListenerProvider;
import org.keycloak.events.EventListenerTransaction;
import org.keycloak.events.admin.AdminEvent;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.util.JsonSerialization;

import static org.keycloak.common.util.Time.currentTimeMillis;
import static org.keycloak.models.utils.KeycloakModelUtils.runJobInTransaction;

public class EventHookEventListenerProvider implements EventListenerProvider {

    private final EventListenerTransaction transaction;
    private final KeycloakSession session;
    private final KeycloakSessionFactory sessionFactory;

    public EventHookEventListenerProvider(KeycloakSession session) {
        this.session = session;
        this.sessionFactory = session.getKeycloakSessionFactory();
        this.transaction = new EventListenerTransaction(this::enqueueAdminEvent, this::enqueueUserEvent);
        this.session.getTransactionManager().enlistAfterCompletion(transaction);
    }

    @Override
    public void onEvent(Event event) {
        if (event.getRealmId() != null) {
            transaction.addEvent(event);
        }
    }

    @Override
    public void onEvent(AdminEvent event, boolean includeRepresentation) {
        if (event.getRealmId() != null) {
            transaction.addAdminEvent(event, includeRepresentation);
        }
    }

    @Override
    public void close() {
    }

    private void enqueueUserEvent(Event event) {
        runJobInTransaction(sessionFactory, targetSession -> {
            EventHookStoreProvider store = targetSession.getProvider(EventHookStoreProvider.class);
            List<EventHookTargetModel> targets = store.getEnabledTargets(event.getRealmId()).stream()
                    .filter(target -> EventHookTargetEventFilter.matchesUserEvent(target, event))
                    .toList();
            if (targets.isEmpty()) {
                return;
            }

            String payload = serialize(EventHookPayloadBuilder.buildUserEventPayload(event));
            long now = currentTimeMillis();
            List<EventHookMessageModel> messages = targets.stream()
                    .map(target -> createMessage(target, event.getRealmId(), EventHookSourceType.USER, event.getId(), payload, now))
                    .toList();
            store.createMessages(messages);
        });
    }

    private void enqueueAdminEvent(AdminEvent event, boolean includeRepresentation) {
        runJobInTransaction(sessionFactory, targetSession -> {
            EventHookStoreProvider store = targetSession.getProvider(EventHookStoreProvider.class);
            List<EventHookTargetModel> targets = store.getEnabledTargets(event.getRealmId()).stream()
                    .filter(target -> EventHookTargetEventFilter.matchesAdminEvent(target, event))
                    .toList();
            if (targets.isEmpty()) {
                return;
            }

            String payload = serialize(EventHookPayloadBuilder.buildAdminEventPayload(event, includeRepresentation));
            long now = currentTimeMillis();
            List<EventHookMessageModel> messages = targets.stream()
                    .map(target -> createMessage(target, event.getRealmId(), EventHookSourceType.ADMIN, event.getId(), payload, now))
                    .toList();
            store.createMessages(messages);
        });
    }

    private EventHookMessageModel createMessage(EventHookTargetModel target, String realmId, EventHookSourceType sourceType,
            String sourceEventId, String payload, long now) {
        EventHookMessageModel message = new EventHookMessageModel();
        EventHookStoreProvider store = session.getProvider(EventHookStoreProvider.class);
        EventHookTargetProviderFactory providerFactory = (EventHookTargetProviderFactory) session.getKeycloakSessionFactory()
            .getProviderFactory(EventHookTargetProvider.class, target.getType());
        message.setId(UUID.randomUUID().toString());
        message.setRealmId(realmId);
        message.setTargetId(target.getId());
        message.setSourceType(sourceType);
        message.setSourceEventId(sourceEventId);
        message.setStatus(EventHookMessageStatus.PENDING);
        message.setPayload(payload);
        message.setAttemptCount(0);
        message.setNextAttemptAt(EventHookDeliveryTask.initialNextAttemptAt(
            target,
            providerFactory,
            store.getPendingAggregationDeadline(realmId, target.getId(), now),
            now));
        message.setCreatedAt(now);
        message.setUpdatedAt(now);
        return message;
    }

    private String serialize(Object value) {
        try {
            return JsonSerialization.writeValueAsString(value);
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to serialize event hook payload", exception);
        }
    }
}

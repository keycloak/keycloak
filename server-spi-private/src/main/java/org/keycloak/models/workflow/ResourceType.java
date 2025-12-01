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

package org.keycloak.models.workflow;

import java.util.List;
import java.util.Objects;
import java.util.function.BiFunction;

import org.keycloak.events.Event;
import org.keycloak.events.EventType;
import org.keycloak.events.admin.AdminEvent;
import org.keycloak.events.admin.OperationType;
import org.keycloak.models.KeycloakSession;
import org.keycloak.provider.ProviderEvent;

import static org.keycloak.models.workflow.ResourceOperationType.toOperationType;

public enum ResourceType {

    USERS(
            org.keycloak.events.admin.ResourceType.USER,
            List.of(OperationType.CREATE),
            List.of(EventType.LOGIN, EventType.REGISTER),
            (session, id) -> session.users().getUserById(session.getContext().getRealm(), id)
    );

    private final org.keycloak.events.admin.ResourceType supportedAdminResourceType;
    private final List<OperationType> supportedAdminOperationTypes;
    private final List<EventType> supportedEventTypes;
    private final BiFunction<KeycloakSession, String, ?> resourceResolver;

    ResourceType(org.keycloak.events.admin.ResourceType supportedAdminResourceType,
                 List<OperationType> supportedAdminOperationTypes,
                 List<EventType> supportedEventTypes,
                 BiFunction<KeycloakSession, String, ?> resourceResolver) {
        this.supportedAdminResourceType = supportedAdminResourceType;
        this.supportedAdminOperationTypes = supportedAdminOperationTypes;
        this.supportedEventTypes = supportedEventTypes;
        this.resourceResolver = resourceResolver;
    }

    public WorkflowEvent toEvent(AdminEvent event) {
        if (Objects.equals(this.supportedAdminResourceType, event.getResourceType())
                && this.supportedAdminOperationTypes.contains(event.getOperationType())) {

            ResourceOperationType resourceOperationType = toOperationType(event.getOperationType());
            if (resourceOperationType != null && event.getResourceId() != null) {
                return new WorkflowEvent(this, resourceOperationType, event.getResourceId(), event);
            }
        }
        return null;
    }

    public WorkflowEvent toEvent(Event event) {
        if (this.supportedEventTypes.contains(event.getType())) {
            ResourceOperationType resourceOperationType = toOperationType(event.getType());
            String resourceId = switch (this) {
                case USERS -> event.getUserId();
            };
            if (resourceOperationType != null && resourceId != null) {
                return new WorkflowEvent(this, resourceOperationType, event.getUserId(), event);
            }
        }
        return null;
    }

    public WorkflowEvent toEvent(ProviderEvent event) {
        ResourceOperationType resourceOperationType = toOperationType(event.getClass());

        if (resourceOperationType == null) {
            return null;
        }

        String resourceId = resourceOperationType.getResourceId(event);

        if (resourceId == null) {
            return null;
        }

        return new WorkflowEvent(this, resourceOperationType, resourceId, event);
    }

    public Object resolveResource(KeycloakSession session, String id) {
        return resourceResolver.apply(session, id);
    }
}

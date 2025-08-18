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

package org.keycloak.models.policy;

import org.keycloak.events.Event;
import org.keycloak.events.EventType;
import org.keycloak.events.admin.AdminEvent;
import org.keycloak.events.admin.OperationType;

import java.util.List;
import java.util.Objects;

public enum ResourceType {

    USERS(org.keycloak.events.admin.ResourceType.USER, List.of(OperationType.CREATE), List.of(EventType.LOGIN, EventType.REGISTER));

    private final org.keycloak.events.admin.ResourceType supportedAdminResourceType;
    private final List<OperationType> supportedAdminOperationTypes;
    private final List<EventType> supportedEventTypes;

    ResourceType(org.keycloak.events.admin.ResourceType supportedAdminResourceType,
                 List<OperationType> supportedAdminOperationTypes,
                 List<EventType> supportedEventTypes) {
        this.supportedAdminResourceType = supportedAdminResourceType;
        this.supportedAdminOperationTypes = supportedAdminOperationTypes;
        this.supportedEventTypes = supportedEventTypes;
    }

    public ResourcePolicyEvent toEvent(AdminEvent event) {
        if (Objects.equals(this.supportedAdminResourceType, event.getResourceType())
                && this.supportedAdminOperationTypes.contains(event.getOperationType())) {

            ResourceOperationType resourceOperationType = toOperationType(event.getOperationType());
            if (resourceOperationType != null) {
                return new ResourcePolicyEvent(this, resourceOperationType, event.getResourceId());
            }
        }
        return null;
    }

    public ResourcePolicyEvent toEvent(Event event) {
        if (this.supportedEventTypes.contains(event.getType())) {
            ResourceOperationType resourceOperationType = toOperationType(event.getType());
            String resourceId = switch (this) {
                case USERS -> event.getUserId();
            };
            if (resourceOperationType != null && resourceId != null) {
                return new ResourcePolicyEvent(this, resourceOperationType, event.getUserId());
            }
        }
        return null;
    }

    private ResourceOperationType toOperationType(OperationType operation) {
        return switch (operation) {
            case CREATE -> ResourceOperationType.CREATE;
            default -> null;
        };
    }

    private ResourceOperationType toOperationType(EventType type) {
        return switch (type) {
            case REGISTER -> ResourceOperationType.CREATE;
            case LOGIN -> ResourceOperationType.LOGIN;
            default -> null;
        };
    }

}

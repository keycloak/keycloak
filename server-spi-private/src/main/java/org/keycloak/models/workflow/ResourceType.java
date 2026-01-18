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
import java.util.function.BiFunction;

import org.keycloak.events.EventType;
import org.keycloak.events.admin.OperationType;
import org.keycloak.models.KeycloakSession;

public enum ResourceType {

    USERS(
            org.keycloak.events.admin.ResourceType.USER,
            List.of(OperationType.CREATE),
            List.of(EventType.LOGIN, EventType.REGISTER),
            (session, id) -> session.users().getUserById(session.getContext().getRealm(), id)
    ),
    CLIENTS(
            org.keycloak.events.admin.ResourceType.CLIENT,
            List.of(OperationType.CREATE),
            List.of(EventType.CLIENT_LOGIN, EventType.CLIENT_REGISTER),
            (session, id) -> session.clients().getClientById(session.getContext().getRealm(), id)
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

    public boolean supportsEvent(EventType eventType) {
        return supportedEventTypes.contains(eventType);
    }

    public boolean supportsAdminEvent(org.keycloak.events.admin.ResourceType resourceType, OperationType operationType) {
        return supportedAdminResourceType.equals(resourceType) && supportedAdminOperationTypes.contains(operationType);
    }

    public Object resolveResource(KeycloakSession session, String id) {
        return resourceResolver.apply(session, id);
    }
}

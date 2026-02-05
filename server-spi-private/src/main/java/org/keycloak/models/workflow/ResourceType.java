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


import java.util.function.BiFunction;

import org.keycloak.events.Event;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;

public enum ResourceType {

    USERS(
            (session, id) -> session.users().getUserById(session.getContext().getRealm(), id),
            (session, event) -> event.getUserId()
    ),
    CLIENTS(
            (session, id) -> session.clients().getClientById(session.getContext().getRealm(), id),
            (session, event) -> findClientResourceId(session, event.getClientId())
    );

    private final BiFunction<KeycloakSession, String, ?> resourceResolver;
    private final BiFunction<KeycloakSession, Event, String> resourceIdResolver;

    ResourceType(BiFunction<KeycloakSession, String, ?> resourceResolver,
                 BiFunction<KeycloakSession, Event, String> resourceIdResolver) {
        this.resourceResolver = resourceResolver;
        this.resourceIdResolver = resourceIdResolver;
    }

    public Object resolveResource(KeycloakSession session, String id) {
        return resourceResolver.apply(session, id);
    }

    public String resolveResourceId(KeycloakSession session, Event event) {
        return resourceIdResolver.apply(session, event);
    }

    private static String findClientResourceId(KeycloakSession session, String clientClientId) {
        RealmModel realm = session.getContext().getRealm();
        if (realm == null) {
            return null;
        }

        ClientModel client = realm.getClientByClientId(clientClientId);
        if (client == null) {
            return null;
        }

        return client.getId();
    }
}

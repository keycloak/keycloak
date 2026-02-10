/*
 * Copyright 2024 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.events.admin.v2;

import java.io.IOException;

import org.keycloak.common.ClientConnection;
import org.keycloak.events.admin.AdminEvent;
import org.keycloak.events.admin.ResourceType;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.services.resources.admin.AdminAuth;
import org.keycloak.services.resources.admin.AdminEventBuilder;
import org.keycloak.util.JsonSerialization;

/**
 * Builder for Admin API v2 events.
 * <p>
 * Extends the v1 AdminEventBuilder with v2-specific behavior:
 * - Events are marked with an "apiVersion" detail set to "v2" to distinguish them from v1 events.
 * - Representation is serialized without stripping secrets (v2 representations handle this differently).
 */
public class AdminEventV2Builder extends AdminEventBuilder {

    private static final String API_VERSION_DETAIL_KEY = "apiVersion";
    private static final String API_VERSION_V2 = "v2";

    public AdminEventV2Builder(RealmModel realm, AdminAuth auth, KeycloakSession session, ClientConnection clientConnection) {
        super(realm, auth, session, clientConnection);
        // Mark this as a v2 API event
        detail(API_VERSION_DETAIL_KEY, API_VERSION_V2);
    }

    private AdminEventV2Builder(RealmModel realm, AdminAuth auth, KeycloakSession session, String ipAddress, AdminEvent adminEvent) {
        super(realm, auth, session, ipAddress, adminEvent);
        // The v2 detail is already in the adminEvent copy
    }

    /**
     * Create a new instance of the {@link AdminEventV2Builder} that is bound to a new session.
     * Use this when starting, for example, a nested transaction.
     * @param session new session where the {@link AdminEventV2Builder} should be bound to.
     * @return a new instance of {@link AdminEventV2Builder}
     */
    @Override
    public AdminEventV2Builder clone(KeycloakSession session) {
        RealmModel newEventRealm = session.realms().getRealm(realm.getId());
        RealmModel newAuthRealm = session.realms().getRealm(this.auth.getRealm().getId());
        UserModel newAuthUser = session.users().getUserById(newAuthRealm, this.auth.getUser().getId());
        ClientModel newAuthClient = session.clients().getClientById(newAuthRealm, this.auth.getClient().getId());

        return new AdminEventV2Builder(
                newEventRealm,
                new AdminAuth(newAuthRealm, this.auth.getToken(), newAuthUser, newAuthClient),
                session,
                ipAddress,
                adminEvent
        );
    }

    @Override
    public AdminEventV2Builder resource(ResourceType resourceType) {
        super.resource(resourceType);
        return this;
    }

    @Override
    public AdminEventV2Builder resource(String resourceType) {
        super.resource(resourceType);
        return this;
    }

    /**
     * Sets the v2 representation to be included in the event.
     * The representation is serialized to JSON without stripping secrets
     * (v2 representations handle sensitive data differently).
     * 
     * @param value the v2 representation object (e.g., BaseClientRepresentation)
     * @return this builder
     */
    @Override
    public AdminEventV2Builder representation(Object value) {
        if (value == null || value.equals("")) {
            return this;
        }

        try {
            adminEvent.setRepresentation(JsonSerialization.writeValueAsString(value));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return this;
    }
}

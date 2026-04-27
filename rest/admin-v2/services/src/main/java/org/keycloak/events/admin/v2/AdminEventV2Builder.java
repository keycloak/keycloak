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


import jakarta.ws.rs.core.UriInfo;

import org.keycloak.common.ClientConnection;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.utils.StripSecretsUtilsV2;
import org.keycloak.services.resources.admin.AdminAuth;
import org.keycloak.services.resources.admin.AdminEventBuilder;

/**
 * Builder for Admin API v2 events.
 * <p>
 * Extends the v1 AdminEventBuilder with v2-specific behavior:
 * - Events are marked with an "apiVersion" detail set to "v2" to distinguish them from v1 events.
 * - Representation is serialized without stripping secrets (v2 representations handle this differently).
 */
public class AdminEventV2Builder extends AdminEventBuilder {
    public static final String API_VERSION_DETAIL_KEY = "apiVersion";
    public static final String API_VERSION_V2 = "v2";

    public AdminEventV2Builder(RealmModel realm, AdminAuth auth, KeycloakSession session, ClientConnection clientConnection) {
        super(realm, auth, session, clientConnection);
        // Mark this as a v2 API event
        detail(API_VERSION_DETAIL_KEY, API_VERSION_V2);
    }

    @Override
    protected String getResourcePath(UriInfo uriInfo) {
        String path = uriInfo.getPath();
        String realmRelative = "/admin/api/%s/".formatted(realm.getName());
        return path.substring(path.indexOf(realmRelative) + realmRelative.length());
    }

    @Override
    protected void stripSecretsFromRepresentation(Object value) {
        StripSecretsUtilsV2.stripSecrets(session, value);
    }
}

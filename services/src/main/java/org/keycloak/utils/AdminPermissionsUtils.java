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
package org.keycloak.utils;

import jakarta.ws.rs.core.Response;
import java.util.Set;
import java.util.stream.Collectors;
import org.keycloak.authorization.AdminPermissionsAuthorizationSchema;
import org.keycloak.authorization.model.ResourceServer;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.representations.idm.authorization.AuthorizationSchema;
import org.keycloak.representations.idm.authorization.ResourceRepresentation;
import org.keycloak.representations.idm.authorization.ScopeRepresentation;
import org.keycloak.services.ErrorResponse;

public class AdminPermissionsUtils {

    public static void checkIsAdminPermissionClient(KeycloakSession session, String clientUuid) {
        RealmModel realm = session.getContext().getRealm();
        if (realm.getAdminPermissionsClient() != null && realm.getAdminPermissionsClient().getId().equals(clientUuid)) {
            throw ErrorResponse.error("Not supported for this client.", Response.Status.BAD_REQUEST);
        }
    }

    public static void resourceRepresentationValidation(RealmModel realm, ResourceServer resourceServer, ResourceRepresentation rep) {
        if (realm.getAdminPermissionsClient() != null && realm.getAdminPermissionsClient().getId().equals(resourceServer.getId())) {
            AuthorizationSchema schema = AdminPermissionsAuthorizationSchema.INSTANCE;
            if (rep.getType() == null || schema.getResourceTypes().get(rep.getType()) == null) {
                throw ErrorResponse.error("Resource type not found.", Response.Status.NOT_FOUND);
            }

            if (rep.getOwner() != null && !rep.getOwner().getId().equals(resourceServer.getClientId())) {
                throw ErrorResponse.error("Owner should reference admin permission client.", Response.Status.BAD_REQUEST);
            }

            Set<String> schemaScopes = schema.getResourceTypes().get(rep.getType()).getScopes();
            Set<String> resourceScopes = rep.getScopes().stream().map(ScopeRepresentation::getName).collect(Collectors.toSet());
            
            if (!schemaScopes.containsAll(resourceScopes)) {
                throw ErrorResponse.error("Unexpected scopes found in the request.", Response.Status.BAD_REQUEST);
            }

            // fields below are not expected
            rep.setAttributes(null);
            rep.setDisplayName(null);
            rep.setIconUri(null);
            rep.setUris(null);
        }
        
    }
}

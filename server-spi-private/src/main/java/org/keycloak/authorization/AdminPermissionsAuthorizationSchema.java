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
package org.keycloak.authorization;

import java.util.Set;

import org.keycloak.authorization.model.Resource;
import org.keycloak.authorization.model.ResourceServer;
import org.keycloak.authorization.store.StoreFactory;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.representations.idm.authorization.AuthorizationSchema;
import org.keycloak.representations.idm.authorization.ResourceType;

public class AdminPermissionsAuthorizationSchema extends AuthorizationSchema {

    public static final ResourceType USERS = new ResourceType("Users", Set.of("manage"));
    public static final ResourceType GROUPS = new ResourceType("Groups", Set.of("manage", "manage-members", "manage-membership", "view", "view-members"));
    public static final AdminPermissionsAuthorizationSchema INSTANCE = new AdminPermissionsAuthorizationSchema();

    private AdminPermissionsAuthorizationSchema() {
        super(USERS, GROUPS);
    }

    public Resource getOrCreateResource(KeycloakSession session, ResourceServer resourceServer, String type, String id) {
        if (!supportsAuthorizationSchema(session, resourceServer)) {
            return null;
        }

        String resourceName = null;

        if (USERS.getType().equals(type)) {
            resourceName = resolveUser(session, id);
        }

        if (resourceName == null) {
            throw new IllegalStateException("Could not map resource object with type [" + type + "] and id [" + id + "]");
        }

        return getOrCreateResource(session, resourceServer, resourceName);
    }

    public boolean isSupportedPolicyType(KeycloakSession session, ResourceServer resourceServer, String type) {
        if (!supportsAuthorizationSchema(session, resourceServer)) {
            return true;
        }

        return !type.equals("resource");
    }

    private boolean supportsAuthorizationSchema(KeycloakSession session, ResourceServer resourceServer) {
        RealmModel realm = session.getContext().getRealm();

        if (!realm.isAdminPermissionsEnabled()) {
            return false;
        }

        ClientModel permissionClient = realm.getAdminPermissionsClient();

        if (permissionClient == null) {
            throw new IllegalStateException("Permission client not found");
        }

        return resourceServer.getId().equals(permissionClient.getId());
    }

    private Resource getOrCreateResource(KeycloakSession session, ResourceServer resourceServer, String id) {
        StoreFactory storeFactory = getStoreFactory(session);
        Resource resource = storeFactory.getResourceStore().findByName(resourceServer, id);

        if (resource == null) {
            return storeFactory.getResourceStore().create(resourceServer, id, resourceServer.getClientId());
        }

        return resource;
    }

    private String resolveUser(KeycloakSession session, String id) {
        RealmModel realm = session.getContext().getRealm();
        UserModel user = session.users().getUserById(realm, id);

        if (user == null) {
            user = session.users().getUserByUsername(realm, id);
        }

        return user == null ? null : user.getId();
    }

    private StoreFactory getStoreFactory(KeycloakSession session) {
        AuthorizationProvider authzProvider = session.getProvider(AuthorizationProvider.class);
        return authzProvider.getStoreFactory();
    }
}

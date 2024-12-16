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

import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.keycloak.authorization.model.Resource;
import org.keycloak.authorization.model.ResourceServer;
import org.keycloak.authorization.model.Scope;
import org.keycloak.authorization.store.StoreFactory;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ModelValidationException;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.representations.idm.authorization.AbstractPolicyRepresentation;
import org.keycloak.representations.idm.authorization.AuthorizationSchema;
import org.keycloak.representations.idm.authorization.ResourceType;
import org.keycloak.representations.idm.authorization.ScopePermissionRepresentation;

public class AdminPermissionsSchema extends AuthorizationSchema {

    public static final String USERS_RESOURCE_TYPE = "Users";
    public static final ResourceType USERS = new ResourceType(USERS_RESOURCE_TYPE, Set.of("manage"));
    public static final AdminPermissionsSchema SCHEMA = new AdminPermissionsSchema();

    private AdminPermissionsSchema() {
        super(Map.of(USERS_RESOURCE_TYPE, USERS));
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

        return isAdminPermissionClient(realm, resourceServer.getId());
    }

    private boolean isAdminPermissionClient(RealmModel realm, String id) {
        return realm.getAdminPermissionsClient() != null && realm.getAdminPermissionsClient().getId().equals(id);
    }

    public void throwExceptionIfAdminPermissionClient(KeycloakSession session, String id) {
        if (isAdminPermissionClient(session.getContext().getRealm(), id)) {
            throw new ModelValidationException("Not supported for this client.");
        }
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

    public void throwExceptionIfResourceTypeOrScopesNotProvided(KeycloakSession session, ResourceServer resourceServer, AbstractPolicyRepresentation rep) {
        if (!supportsAuthorizationSchema(session, resourceServer)) {
            return;
        }
        if (rep instanceof ScopePermissionRepresentation) {
            if (rep.getResourceType() == null || SCHEMA.getResourceTypes().get(rep.getResourceType()) == null) {
                throw new ModelValidationException("Resource type not provided.");
            }
            if (rep.getScopes() == null || rep.getScopes().isEmpty()) {
                throw new ModelValidationException("Scopes not provided.");
            }
        }
    }

    public Scope getScope(KeycloakSession session, ResourceServer resourceServer, String resourceType, String id) {
        StoreFactory storeFactory = getStoreFactory(session);

        Scope scope = Optional.ofNullable(storeFactory.getScopeStore().findById(resourceServer, id))
            .or(() -> Optional.ofNullable(storeFactory.getScopeStore().findByName(resourceServer, id)))
            .orElseThrow(() -> new ModelValidationException(String.format("Scope [%s] does not exist.", id)));

        if (supportsAuthorizationSchema(session, resourceServer)) {
            //validations for schema
            if (!SCHEMA.getResourceTypes().get(resourceType).getScopes().contains(scope.getName())) {
                throw new ModelValidationException(String.format("Scope %s was not found for resource type %s.", scope.getName(), resourceType));
            }
        }

        return scope;
    }
}

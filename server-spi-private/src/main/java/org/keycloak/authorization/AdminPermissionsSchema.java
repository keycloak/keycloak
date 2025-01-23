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

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.keycloak.authorization.model.Policy;

import org.keycloak.authorization.model.Resource;
import org.keycloak.authorization.model.ResourceServer;
import org.keycloak.authorization.model.Scope;
import org.keycloak.authorization.store.ResourceStore;
import org.keycloak.authorization.store.ScopeStore;
import org.keycloak.authorization.store.StoreFactory;
import org.keycloak.common.Profile;
import org.keycloak.models.ClientModel;
import org.keycloak.models.Constants;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ModelException;
import org.keycloak.models.ModelIllegalStateException;
import org.keycloak.models.ModelValidationException;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.ModelToRepresentation;
import org.keycloak.models.utils.RepresentationToModel;
import org.keycloak.representations.idm.authorization.AbstractPolicyRepresentation;
import org.keycloak.representations.idm.authorization.AuthorizationSchema;
import org.keycloak.representations.idm.authorization.ResourceRepresentation;
import org.keycloak.representations.idm.authorization.ResourceServerRepresentation;
import org.keycloak.representations.idm.authorization.ResourceType;
import org.keycloak.representations.idm.authorization.ScopePermissionRepresentation;
import org.keycloak.representations.idm.authorization.ScopeRepresentation;

public class AdminPermissionsSchema extends AuthorizationSchema {

    public static final String MANAGE = "manage";
    public static final String VIEW = "view";

    public static final String USERS_RESOURCE_TYPE = "Users";

    public static final ResourceType USERS = new ResourceType(USERS_RESOURCE_TYPE, Set.of(MANAGE, VIEW));

    public static final AdminPermissionsSchema SCHEMA = new AdminPermissionsSchema();

    private AdminPermissionsSchema() {
        super(Map.of(USERS_RESOURCE_TYPE, USERS));
    }

    public Resource getOrCreateResource(KeycloakSession session, ResourceServer resourceServer, String type, String id) {
        if (!supportsAuthorizationSchema(session, resourceServer)) {
            return null;
        }

        StoreFactory storeFactory = getStoreFactory(session);
        ResourceStore resourceStore = storeFactory.getResourceStore();
        String name = null;

        if (USERS.getType().equals(type)) {
            name = resolveUser(session, id);
            if (name == null) {
                Resource resource = resourceStore.findById(resourceServer, id);

                if (resource != null) {
                    name = resource.getName();
                }
            }
        }

        if (name == null) {
            throw new IllegalStateException("Could not map resource object with type [" + type + "] and id [" + id + "]");
        }

        Resource resource = resourceStore.findByName(resourceServer, name);

        if (resource == null) {
            resource = resourceStore.create(resourceServer, name, resourceServer.getClientId());
            ScopeStore scopeStore = storeFactory.getScopeStore();
            resource.updateScopes(getResourceTypes().get(type).getScopes().stream().map(scopeName -> {
                Scope findByName = scopeStore.findByName(resourceServer, scopeName);
                if (findByName == null) throw new ModelException("No scopes found.");
                return findByName;
            }).collect(Collectors.toSet()));
            return resource;
        }

        return resource;
    }

    public boolean isSupportedPolicyType(KeycloakSession session, ResourceServer resourceServer, String type) {
        if (!supportsAuthorizationSchema(session, resourceServer)) {
            return true;
        }

        return !type.equals("resource");
    }

    private boolean supportsAuthorizationSchema(KeycloakSession session, ResourceServer resourceServer) {
        RealmModel realm = session.getContext().getRealm();

        if (!isAdminPermissionsEnabled(realm)) {
            return false;
        }

        return isAdminPermissionClient(realm, resourceServer.getId());
    }

    public boolean isAdminPermissionClient(RealmModel realm, String id) {
        return realm.getAdminPermissionsClient() != null && realm.getAdminPermissionsClient().getId().equals(id);
    }

    public void throwExceptionIfAdminPermissionClient(KeycloakSession session, String id) {
        if (isAdminPermissionClient(session.getContext().getRealm(), id)) {
            throw new ModelValidationException("Not supported for this client.");
        }
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

    public void init(KeycloakSession session, RealmModel realm) {
        ClientModel client = session.clients().addClient(realm, Constants.ADMIN_PERMISSIONS_CLIENT_ID);
        realm.setAdminPermissionsClient(client);

        ResourceServer resourceServer = RepresentationToModel.createResourceServer(client, session, false);
        ResourceServerRepresentation resourceServerRep = ModelToRepresentation.toRepresentation(resourceServer, client);

        //create all scopes defined in the schema
        //there is no way how to map scopes to the resourceType, we need to collect all scopes from all resourceTypes 
        Set<ScopeRepresentation> scopes = SCHEMA.getResourceTypes().values().stream()
                .flatMap((resourceType) -> resourceType.getScopes().stream())
                .map(scope -> new ScopeRepresentation(scope))
                .collect(Collectors.toSet());//collecting to set to get rid of duplicities

        resourceServerRep.setScopes(List.copyOf(scopes));

        //create 'all-resource' resources defined in the schema
        resourceServerRep.setResources(SCHEMA.getResourceTypes().keySet().stream()
                .map(type -> {
                    ResourceRepresentation resource = new ResourceRepresentation(type, SCHEMA.getResourceTypes().get(type).getScopes().toArray(String[]::new));
                    resource.setType(type);
                    return resource;
                }).collect(Collectors.toList()));

        RepresentationToModel.toModel(resourceServerRep, session.getProvider(AuthorizationProvider.class), client);
    }

    public boolean isAdminPermissionsEnabled(RealmModel realm) {
        return Profile.isFeatureEnabled(Profile.Feature.ADMIN_FINE_GRAINED_AUTHZ_V2) && realm.isAdminPermissionsEnabled();
    }

    public AuthorizationSchema getAuthorizationSchema(ClientModel client) {
        if (isAdminPermissionsEnabled(client.getRealm()) && 
            isAdminPermissionClient(client.getRealm(), client.getId())) {
            return SCHEMA;
        }
        return null;
    }

    public void removeOrphanResources(Policy policy, AuthorizationProvider authorization) {
        if (isAdminPermissionClient(authorization.getRealm(), policy.getResourceServer().getId())) {
            Set<Resource> resources = policy.getResources();
            for (Resource resource : resources) {
                if (getResourceTypes().get(resource.getName()) == null) {
                    List<Policy> policies = authorization.getStoreFactory().getPolicyStore().findByResource(policy.getResourceServer(), resource);
                    // if there is single resource remaining delete it
                    if (policies.size() == 1 && policy.equals(policies.get(0))) {
                        authorization.getStoreFactory().getResourceStore().delete(resource.getId());
                    }
                }
            }
        }
    }

    public String getResourceName(KeycloakSession session, Policy policy, Resource resource) {
        ResourceServer resourceServer = policy.getResourceServer();

        if (supportsAuthorizationSchema(session, resourceServer)) {
            String resourceType = policy.getResourceType();

            if (USERS.getType().equals(resourceType)) {
                if (resource.getName().equals(USERS_RESOURCE_TYPE)) {
                    return "All users";
                }

                UserModel user = session.users().getUserById(session.getContext().getRealm(), resource.getName());

                if (user == null) {
                    throw new ModelIllegalStateException("User not found for resource [" + resource.getId() + "]");
                }

                return user.getUsername();
            }
        }

        return resource.getDisplayName();
    }
}

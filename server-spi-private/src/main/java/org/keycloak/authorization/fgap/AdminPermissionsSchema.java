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
package org.keycloak.authorization.fgap;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;

import org.keycloak.authorization.AuthorizationProvider;
import org.keycloak.authorization.fgap.evaluation.FGAPPolicyEvaluator;
import org.keycloak.authorization.fgap.evaluation.partial.PartialEvaluationStorageProvider;
import org.keycloak.authorization.fgap.evaluation.partial.PartialEvaluator;
import org.keycloak.authorization.model.Policy;
import org.keycloak.authorization.model.Policy.FilterOption;
import org.keycloak.authorization.model.Resource;
import org.keycloak.authorization.model.ResourceServer;
import org.keycloak.authorization.model.Scope;
import org.keycloak.authorization.policy.evaluation.PolicyEvaluator;
import org.keycloak.authorization.store.ResourceStore;
import org.keycloak.authorization.store.ScopeStore;
import org.keycloak.authorization.store.StoreFactory;
import org.keycloak.common.Profile;
import org.keycloak.models.ClientModel;
import org.keycloak.models.ClientModel.ClientRemovedEvent;
import org.keycloak.models.ClientProvider;
import org.keycloak.models.Constants;
import org.keycloak.models.GroupModel;
import org.keycloak.models.GroupModel.GroupRemovedEvent;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ModelException;
import org.keycloak.models.ModelValidationException;
import org.keycloak.models.OrganizationModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleContainerModel.RoleRemovedEvent;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserModel.UserRemovedEvent;
import org.keycloak.models.utils.ModelToRepresentation;
import org.keycloak.models.utils.RepresentationToModel;
import org.keycloak.organization.OrganizationProvider;
import org.keycloak.provider.ProviderEvent;
import org.keycloak.representations.idm.authorization.AbstractPolicyRepresentation;
import org.keycloak.representations.idm.authorization.AuthorizationSchema;
import org.keycloak.representations.idm.authorization.DecisionStrategy;
import org.keycloak.representations.idm.authorization.ResourceRepresentation;
import org.keycloak.representations.idm.authorization.ResourceServerRepresentation;
import org.keycloak.representations.idm.authorization.ResourceType;
import org.keycloak.representations.idm.authorization.ScopePermissionRepresentation;
import org.keycloak.representations.idm.authorization.ScopeRepresentation;

public class AdminPermissionsSchema extends AuthorizationSchema {

    public static final String REALMS_RESOURCE_TYPE = "Realms";

    public static final String CLIENTS_RESOURCE_TYPE = "Clients";
    public static final String GROUPS_RESOURCE_TYPE = "Groups";
    public static final String ROLES_RESOURCE_TYPE = "Roles";
    public static final String USERS_RESOURCE_TYPE = "Users";
    public static final String ORGANIZATIONS_RESOURCE_TYPE = "Organizations";

    // common scopes
    public static final String MANAGE = "manage";
    public static final String VIEW = "view";
    public static String QUERY = "query";

    // client specific scopes
    public static final String MAP_ROLES = "map-roles";
    public static final String MAP_ROLES_CLIENT_SCOPE = "map-roles-client-scope";
    public static final String MAP_ROLES_COMPOSITE = "map-roles-composite";

    // group specific scopes
    public static final String MANAGE_MEMBERSHIP = "manage-membership";
    public static final String MANAGE_MEMBERSHIP_OF_MEMBERS = "manage-membership-of-members";
    public static final String MANAGE_MEMBERS = "manage-members";
    public static final String VIEW_MEMBERS = "view-members";
    public static final String IMPERSONATE_MEMBERS = "impersonate-members";

    // role specific scopes
    public static final String MAP_ROLE = "map-role";
    public static final String MAP_ROLE_CLIENT_SCOPE = "map-role-client-scope";
    public static final String MAP_ROLE_COMPOSITE = "map-role-composite";

    // user specific scopes
    public static final String IMPERSONATE = "impersonate";
    public static final String RESET_PASSWORD = "reset-password";

    public static final String MANAGE_GROUP_MEMBERSHIP = "manage-group-membership";

    public static final ResourceType CLIENTS = new ResourceType(CLIENTS_RESOURCE_TYPE, Set.of(MANAGE, MAP_ROLES, MAP_ROLES_CLIENT_SCOPE, MAP_ROLES_COMPOSITE, VIEW));
    public static final ResourceType GROUPS = new ResourceType(GROUPS_RESOURCE_TYPE, Set.of(MANAGE, VIEW, MANAGE_MEMBERSHIP, MANAGE_MEMBERSHIP_OF_MEMBERS, MANAGE_MEMBERS, VIEW_MEMBERS, IMPERSONATE_MEMBERS));
    public static final ResourceType ROLES = new ResourceType(ROLES_RESOURCE_TYPE, Set.of(MAP_ROLE, MAP_ROLE_CLIENT_SCOPE, MAP_ROLE_COMPOSITE));
    public static final ResourceType USERS = new ResourceType(USERS_RESOURCE_TYPE, Set.of(MANAGE, VIEW, IMPERSONATE, MAP_ROLES, MANAGE_GROUP_MEMBERSHIP, RESET_PASSWORD), Map.of(VIEW, Set.of(VIEW_MEMBERS), MANAGE, Set.of(MANAGE_MEMBERS), IMPERSONATE, Set.of(IMPERSONATE_MEMBERS), MANAGE_GROUP_MEMBERSHIP, Set.of(MANAGE_MEMBERSHIP_OF_MEMBERS)), GROUPS.getType());
    public static final ResourceType ORGANIZATIONS = new ResourceType(ORGANIZATIONS_RESOURCE_TYPE, Set.of(MANAGE, VIEW));
    private static final String SKIP_EVALUATION = "kc.authz.fgap.skip";
    public static final AdminPermissionsSchema SCHEMA = new AdminPermissionsSchema();

    private final PartialEvaluator partialEvaluator = new PartialEvaluator();
    private final PolicyEvaluator policyEvaluator = new FGAPPolicyEvaluator();

    private AdminPermissionsSchema() {
        super(Map.of(
            CLIENTS_RESOURCE_TYPE, CLIENTS,
            GROUPS_RESOURCE_TYPE, GROUPS,
            ROLES_RESOURCE_TYPE, ROLES,
            USERS_RESOURCE_TYPE, USERS,
            ORGANIZATIONS_RESOURCE_TYPE, ORGANIZATIONS
        ));
    }

    public Resource getOrCreateResource(KeycloakSession session, ResourceServer resourceServer, String resourceType, String id) {
        if (!supportsAuthorizationSchema(session, resourceServer)) {
            return null;
        }

        StoreFactory storeFactory = getStoreFactory(session);
        ResourceStore resourceStore = storeFactory.getResourceStore();
        Resource resource = resourceStore.findById(resourceServer, id);

        if (resource != null) {
            String resourceName = resource.getName();
            if (!Objects.equals(resourceName, resourceType)) {
                // instance resource — validate the name resolves as the expected entity type
                getResourceName(session, resourceType, resourceName);
            }
            return resource;
        }

        String resourceName = getResourceName(session, resourceType, id);
        resource = resourceStore.findByName(resourceServer, resourceName);

        if (resource == null) {
            resource = resourceStore.create(resourceServer, resourceName, resourceServer.getClientId());
            ScopeStore scopeStore = storeFactory.getScopeStore();
            resource.updateScopes(getResourceTypes().get(resourceType).getScopes().stream().map(scopeName -> {
                Scope findByName = scopeStore.findByName(resourceServer, scopeName);
                if (findByName == null) throw new ModelException("No scopes found.");
                return findByName;
            }).collect(Collectors.toSet()));
        }

        return resource;
    }

    private String getResourceName(KeycloakSession session, String resourceType, String id) {
        if(Objects.equals(resourceType, id)) {
            return id;
        }
        return switch (resourceType) {
            case CLIENTS_RESOURCE_TYPE -> resolveClient(session, id).map(ClientModel::getId)
                    .orElseThrow(() -> new ModelValidationException("Resource [" + id + "] does not exist for type [" + resourceType + "]"));
            case GROUPS_RESOURCE_TYPE -> resolveGroup(session, id).map(GroupModel::getId)
                    .orElseThrow(() -> new ModelValidationException("Resource [" + id + "] does not exist for type [" + resourceType + "]"));
            case ROLES_RESOURCE_TYPE -> resolveRole(session, id).map(RoleModel::getId)
                    .orElseThrow(() -> new ModelValidationException("Resource [" + id + "] does not exist for type [" + resourceType + "]"));
            case USERS_RESOURCE_TYPE -> resolveUser(session, id).map(UserModel::getId)
                    .orElseThrow(() -> new ModelValidationException("Resource [" + id + "] does not exist for type [" + resourceType + "]"));
            case ORGANIZATIONS_RESOURCE_TYPE -> resolveOrganization(session, id).map(OrganizationModel::getId)
                    .orElseThrow(() -> new ModelValidationException("Resource [" + id + "] does not exist for type [" + resourceType + "]"));
            default -> throw new IllegalStateException("Resource type [" + resourceType + "] not found.");
        };
    }

    public Resource getResourceTypeResource(KeycloakSession session, ResourceServer resourceServer, String resourceType) {
        if (!supportsAuthorizationSchema(session, resourceServer)) {
            return null;
        }

        if (resourceType == null) {
            return null;
        }

        ResourceType type = getResourceTypes().get(resourceType);

        if (type == null) {
            return null;
        }

        ResourceStore resourceStore = getStoreFactory(session).getResourceStore();

        return resourceStore.findByName(resourceServer, type.getType());
    }

    public boolean isSupportedPolicyType(KeycloakSession session, ResourceServer resourceServer, String type) {
        if (!supportsAuthorizationSchema(session, resourceServer)) {
            return true;
        }

        return !type.equals("resource");
    }

    public boolean isAdminPermissionClient(RealmModel realm, String id) {
        return realm.getAdminPermissionsClient() != null && realm.getAdminPermissionsClient().getId().equals(id);
    }

    private boolean supportsAuthorizationSchema(KeycloakSession session, ResourceServer resourceServer) {
        RealmModel realm = session.getContext().getRealm();

        if (!isAdminPermissionsEnabled(realm)) {
            return false;
        }

        return isAdminPermissionClient(realm, resourceServer.getId());
    }

    public void throwExceptionIfAdminPermissionClient(KeycloakSession session, String id) {
        if (isAdminPermissionClient(session.getContext().getRealm(), id)) {
            throw new ModelValidationException("Not supported for this client.");
        }
    }

    private Optional<GroupModel> resolveGroup(KeycloakSession session, String id) {
        RealmModel realm = session.getContext().getRealm();

        return Optional.ofNullable(session.groups().getGroupById(realm, id));
    }

    private Optional<RoleModel> resolveRole(KeycloakSession session, String id) {
        RealmModel realm = session.getContext().getRealm();
        RoleModel role = session.roles().getRoleById(realm, id);

        return Optional.ofNullable(role);
    }

    private Optional<UserModel> resolveUser(KeycloakSession session, String id) {
        RealmModel realm = session.getContext().getRealm();
        UserModel user = session.users().getUserById(realm, id);

        if (user == null) {
            user = session.users().getUserByUsername(realm, id);
        }

        return Optional.ofNullable(user);
    }

    private Optional<OrganizationModel> resolveOrganization(KeycloakSession session, String id) {
        return Optional.ofNullable(session.getProvider(OrganizationProvider.class).getById(id));
    }

    private Optional<ClientModel> resolveClient(KeycloakSession session, String id) {
        RealmModel realm = session.getContext().getRealm();
        ClientModel client = session.clients().getClientById(realm, id);

        if (client == null) {
            client = session.clients().getClientByClientId(realm, id);
        }

        return Optional.ofNullable(client);
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
            DecisionStrategy decisionStrategy = rep.getDecisionStrategy();
            if (decisionStrategy != null && !DecisionStrategy.UNANIMOUS.equals(decisionStrategy)) {
                throw new ModelValidationException("Only UNANIMOUS decision strategy is supported for admin permissions.");
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
        ClientProvider clients = session.clients();
        ClientModel client = realm.getAdminPermissionsClient();

        if (client != null) {
            ensureSchemaUpToDate(session, client);
            return;
        }

        client = clients.addClient(realm, Constants.ADMIN_PERMISSIONS_CLIENT_ID);
        client.setProtocol("openid-connect");

        realm.setAdminPermissionsClient(client);

        ResourceServer resourceServer = RepresentationToModel.createResourceServer(client, session, false);
        ResourceServerRepresentation resourceServerRep = ModelToRepresentation.toRepresentation(resourceServer, client);

        //create all scopes defined in the schema
        //there is no way how to map scopes to the resourceType, we need to collect all scopes from all resourceTypes
        Set<ScopeRepresentation> scopes = SCHEMA.getResourceTypes().values().stream()
                .flatMap((resourceType) -> resourceType.getScopes().stream())
                .map(ScopeRepresentation::new)
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

    private void ensureSchemaUpToDate(KeycloakSession session, ClientModel client) {
        AuthorizationProvider authzProvider = session.getProvider(AuthorizationProvider.class);
        StoreFactory storeFactory = authzProvider.getStoreFactory();
        ResourceServer resourceServer = storeFactory.getResourceServerStore().findByClient(client);

        if (resourceServer == null) {
            return;
        }

        ResourceStore resourceStore = storeFactory.getResourceStore();
        ScopeStore scopeStore = storeFactory.getScopeStore();

        for (Entry<String, ResourceType> entry : SCHEMA.getResourceTypes().entrySet()) {
            String typeName = entry.getKey();
            ResourceType type = entry.getValue();

            for (String scopeName : type.getScopes()) {
                if (scopeStore.findByName(resourceServer, scopeName) == null) {
                    scopeStore.create(resourceServer, scopeName);
                }
            }

            if (resourceStore.findByName(resourceServer, typeName) == null) {
                Resource resource = resourceStore.create(resourceServer, typeName, resourceServer.getClientId());
                resource.updateScopes(type.getScopes().stream()
                        .map(s -> scopeStore.findByName(resourceServer, s))
                        .filter(Objects::nonNull)
                        .collect(Collectors.toSet()));
                resource.setType(typeName);
            }
        }
    }

    public boolean isAdminPermissionsEnabled(RealmModel realm) {
        return Profile.isFeatureEnabled(Profile.Feature.ADMIN_FINE_GRAINED_AUTHZ_V2) && realm != null && realm.isAdminPermissionsEnabled();
    }

    public AuthorizationSchema getAuthorizationSchema(ClientModel client) {
        if (isAdminPermissionsEnabled(client.getRealm()) && 
            isAdminPermissionClient(client.getRealm(), client.getId())) {
            return SCHEMA;
        }
        return null;
    }

    // for updates
    public void removeResource(Resource resource, Policy policy, AuthorizationProvider authorization) {
        ResourceServer resourceServer = resource.getResourceServer();

        if (isAdminPermissionClient(authorization.getRealm(), resourceServer.getId())) {
            // for admin permissions remove resource in the FGAP context (if resource is becoming on orphan, we remove the resource from DB)
            if (getResourceTypes().get(resource.getName()) == null) {
                List<Policy> policies = authorization.getStoreFactory().getPolicyStore().findByResource(resourceServer, resource);
                // if there is single resource remaining delete it
                if (policies.size() == 1 && policy.equals(policies.get(0))) {
                    authorization.getStoreFactory().getResourceStore().delete(resource.getId());
                } else {
                    policy.removeResource(resource);
                }
            }
        } else {
            policy.removeResource(resource);
        }
    }

    //for deletion
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
            return getResourceName(session, resourceServer, policy.getResourceType(), resource.getName());
        }

        return resource.getDisplayName();
    }

    public String getResourceName(KeycloakSession session, ResourceServer resourceServer, String resourceType, String resourceName) {
        if (resourceType == null) {
            return resourceName;
        }

        if (supportsAuthorizationSchema(session, resourceServer)) {
            switch (resourceType) {
                case CLIENTS_RESOURCE_TYPE -> {
                    return resolveClient(session, resourceName).map(ClientModel::getClientId).orElse(resourceType);
                }
                case GROUPS_RESOURCE_TYPE -> {
                    return resolveGroup(session, resourceName).map(GroupModel::getName).orElse(resourceType);
                }
                case ROLES_RESOURCE_TYPE -> {
                    return resolveRole(session, resourceName).map(RoleModel::getName).orElse(resourceType);
                }
                case USERS_RESOURCE_TYPE -> {
                    return resolveUser(session, resourceName).map(UserModel::getUsername).orElse(resourceType);
                }
                case ORGANIZATIONS_RESOURCE_TYPE -> {
                    return resolveOrganization(session, resourceName).map(OrganizationModel::getName).orElse(resourceType);
                }
                default -> throw new IllegalStateException("Resource type [" + resourceType + "] not found.");
            }
        }

        return resourceName;
    }

    public void addUResourceTypeResource(KeycloakSession session, ResourceServer resourceServer, Policy policy, String resourceType) {
        Resource resourceTypeResource = getResourceTypeResource(session, resourceServer, resourceType);

        if (resourceTypeResource != null) {
            Set<Resource> resources = policy.getResources();

            if (resources.isEmpty()) {
                policy.addResource(resourceTypeResource);
            } else if (resources.size() > 1) {
                policy.removeResource(resourceTypeResource);
            }
        }
    }

    public void removeResourceObject(AuthorizationProvider authorization, ProviderEvent event) {
        if (!isAdminPermissionsEnabled(authorization.getRealm()) || authorization.getRealm().getAdminPermissionsClient() == null) return;

        String id;
        if (event instanceof UserRemovedEvent userRemovedEvent) {
            id = userRemovedEvent.getUser().getId();
        } else if (event instanceof ClientRemovedEvent clientRemovedEvent) {
            id = clientRemovedEvent.getClient().getId();
        } else if (event instanceof GroupRemovedEvent groupRemovedEvent) {
            id = groupRemovedEvent.getGroup().getId();
        } else if (event instanceof RoleRemovedEvent roleRemovedEvent) {
            id = roleRemovedEvent.getRole().getId();
        } else if (event instanceof OrganizationModel.OrganizationRemovedEvent orgRemovedEvent) {
            id = orgRemovedEvent.getOrganization().getId();
        } else {
            return;
        }

        ResourceServer server = authorization.getStoreFactory().getResourceServerStore().findByClient(authorization.getRealm().getAdminPermissionsClient());

        Resource resource = authorization.getStoreFactory().getResourceStore().findByName(server, id);
        if (resource != null) {
            List<Policy> permissions = authorization.getStoreFactory().getPolicyStore().findByResource(server, resource);
            //remove object from permission if there is more than one resource, remove the permission if there is only the removed object
            for (Policy permission : permissions) {
                if (permission.getResources().size() == 1) {
                    authorization.getStoreFactory().getPolicyStore().delete(permission.getId());
                } else {
                    permission.removeResource(resource);
                }
            }

            //remove the resource associated with the object
            authorization.getStoreFactory().getResourceStore().delete(resource.getId());
        }
    }

    public List<Predicate> applyAuthorizationFilters(KeycloakSession session, ResourceType resourceType, RealmModel realm, CriteriaBuilder builder, CriteriaQuery<?> queryBuilder, Path<?> path) {
        return applyAuthorizationFilters(session, resourceType, null, realm, builder, queryBuilder, path);
    }

    public List<Predicate> applyAuthorizationFilters(KeycloakSession session, ResourceType resourceType, PartialEvaluationStorageProvider evaluator, RealmModel realm, CriteriaBuilder builder, CriteriaQuery<?> queryBuilder, Path<?> path) {
        return partialEvaluator.getPredicates(session, resourceType, evaluator, realm, builder, queryBuilder, path);
    }

    public PolicyEvaluator getPolicyEvaluator(KeycloakSession session, ResourceServer resourceServer) {
        if (resourceServer == null) {
            return null;
        }

        RealmModel realm = session.getContext().getRealm();

        if (isAdminPermissionClient(realm, resourceServer.getId())) {
            return policyEvaluator;
        }

        return null;
    }

    public Set<String> getScopeAliases(String resourceType, Scope scope) {
        ResourceType type = getResourceTypes().get(resourceType);
        Set<String> aliases = type.getScopeAliases().get(scope.getName());

        if (aliases == null) {
            aliases = new HashSet<>();
            for (Entry<String, Set<String>> entry : type.getScopeAliases().entrySet()) {
                if (entry.getValue().contains(scope.getName())) {
                    aliases.add(entry.getKey());
                }
            }
        }

        return aliases;
    }

    /**
     * <p>Disables authorization and evaluation of permissions for realm resource types when executing the given {@code runnable}
     * in the context of the given {@code session}.
     *
     * <p>This method should be used whenever a code block should be executed without any evaluation or filtering based on
     * the permissions set to a realm. For instance, when caching realm resources where access enforcement does not apply.
     *
     * @param session the session. If {@code null}, authorization is enabled when executing the code block
     * @param runnable the runnable to execute
     */
    public static void runWithoutAuthorization(KeycloakSession session, Runnable runnable) {
        if (isSkipEvaluation(session)) {
            runnable.run();
            return;
        }

        try {
            session.setAttribute(SKIP_EVALUATION, Boolean.TRUE.toString());
            runnable.run();
        } finally {
            session.removeAttribute(SKIP_EVALUATION);
        }
    }

    /**
     * Returns if authorization is disabled in the context of the given {@code session} at the moment that this method is called.
     *
     * @param session the session
     * @return {@code true} if authorization is disabled. Otherwise, returns {@code false}.
     * Otherwise, {@code false}.
     * @see AdminPermissionsSchema#runWithoutAuthorization(KeycloakSession, Runnable)
     */
    public static boolean isSkipEvaluation(KeycloakSession session) {
        if (session == null) {
            return true;
        }

        RealmModel realm = session.getContext().getRealm();

        if (realm == null) {
            return true;
        }

        if (!AdminPermissionsSchema.SCHEMA.isAdminPermissionsEnabled(realm)) {
            return true;
        }

        return Boolean.parseBoolean(session.getAttributeOrDefault(SKIP_EVALUATION, Boolean.FALSE.toString()));
    }

    public void addResourceTypeScope(KeycloakSession session, RealmModel realm, String resourceType, String scopeName) {
        ClientModel client = realm.getAdminPermissionsClient();

        if (client == null) {
            return;
        }

        AuthorizationProvider authorizationProvider = session.getProvider(AuthorizationProvider.class);
        StoreFactory storeFactory = authorizationProvider.getStoreFactory();
        ResourceServer resourceServer = storeFactory.getResourceServerStore().findByClient(client);

        if (resourceServer == null) {
            return;
        }

        ScopeStore scopeStore = storeFactory.getScopeStore();
        Scope newScope = scopeStore.findByName(resourceServer, scopeName);

        if (newScope == null) {
            newScope = scopeStore.create(resourceServer, scopeName);
        }

        ResourceStore resourceStore = storeFactory.getResourceStore();
        Resource resourceTypeResource = resourceStore.findByName(resourceServer, resourceType);
        Set<Scope> newScopes = new HashSet<>(resourceTypeResource.getScopes());

        newScopes.add(newScope);

        resourceTypeResource.updateScopes(newScopes);

        for (Policy policy : storeFactory.getPolicyStore().find(resourceServer, Map.of(FilterOption.CONFIG, new String[]{"defaultResourceType", resourceType}), -1, -1)) {
            for (Resource resource : policy.getResources()) {
                resource.updateScopes(newScopes);
            }
        }
    }
}

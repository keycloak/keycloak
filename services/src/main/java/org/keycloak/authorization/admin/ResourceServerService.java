/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2016 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.keycloak.authorization.admin;

import org.jboss.resteasy.spi.ResteasyProviderFactory;
import org.keycloak.authorization.AuthorizationProvider;
import org.keycloak.authorization.admin.util.Models;
import org.keycloak.authorization.model.Policy;
import org.keycloak.authorization.model.Resource;
import org.keycloak.authorization.model.ResourceServer;
import org.keycloak.authorization.model.Scope;
import org.keycloak.authorization.store.PolicyStore;
import org.keycloak.authorization.store.ResourceStore;
import org.keycloak.authorization.store.ScopeStore;
import org.keycloak.authorization.store.StoreFactory;
import org.keycloak.models.ClientModel;
import org.keycloak.models.Constants;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserFederationManager;
import org.keycloak.models.UserModel;
import org.keycloak.representations.idm.authorization.DecisionStrategy;
import org.keycloak.representations.idm.authorization.Logic;
import org.keycloak.representations.idm.authorization.PolicyRepresentation;
import org.keycloak.representations.idm.authorization.ResourceOwnerRepresentation;
import org.keycloak.representations.idm.authorization.ResourceRepresentation;
import org.keycloak.representations.idm.authorization.ResourceServerRepresentation;
import org.keycloak.representations.idm.authorization.ScopeRepresentation;
import org.keycloak.services.resources.admin.RealmAuth;
import org.keycloak.util.JsonSerialization;

import javax.management.relation.Role;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public class ResourceServerService {

    private final AuthorizationProvider authorization;
    private final RealmAuth auth;
    private final RealmModel realm;
    private final KeycloakSession session;
    private ResourceServer resourceServer;
    private final ClientModel client;

    public ResourceServerService(AuthorizationProvider authorization, ResourceServer resourceServer, ClientModel client, RealmAuth auth) {
        this.authorization = authorization;
        this.session = authorization.getKeycloakSession();
        this.client = client;
        this.resourceServer = resourceServer;
        this.realm = client.getRealm();
        this.auth = auth;
    }

    public void create() {
        this.auth.requireManage();
        this.resourceServer = this.authorization.getStoreFactory().getResourceServerStore().create(this.client.getId());
        createDefaultRoles();
        createDefaultPermission(createDefaultResource(), createDefaultPolicy());
    }

    @PUT
    @Consumes("application/json")
    @Produces("application/json")
    public Response update(ResourceServerRepresentation server) {
        this.auth.requireManage();
        this.resourceServer.setAllowRemoteResourceManagement(server.isAllowRemoteResourceManagement());
        this.resourceServer.setPolicyEnforcementMode(server.getPolicyEnforcementMode());

        return Response.noContent().build();
    }

    public void delete() {
        this.auth.requireManage();
        StoreFactory storeFactory = authorization.getStoreFactory();
        ResourceStore resourceStore = storeFactory.getResourceStore();
        String id = resourceServer.getId();

        resourceStore.findByResourceServer(id).forEach(resource -> resourceStore.delete(resource.getId()));

        ScopeStore scopeStore = storeFactory.getScopeStore();

        scopeStore.findByResourceServer(id).forEach(scope -> scopeStore.delete(scope.getId()));

        PolicyStore policyStore = storeFactory.getPolicyStore();

        policyStore.findByResourceServer(id).forEach(scope -> policyStore.delete(scope.getId()));

        storeFactory.getResourceServerStore().delete(id);
    }

    @GET
    @Produces("application/json")
    public Response findById() {
        this.auth.requireView();
        return Response.ok(Models.toRepresentation(this.resourceServer, this.realm)).build();
    }

    @Path("/settings")
    @GET
    @Produces("application/json")
    public Response exportSettings() {
        this.auth.requireManage();
        StoreFactory storeFactory = authorization.getStoreFactory();
        ResourceServerRepresentation settings = Models.toRepresentation(resourceServer, this.realm);

        settings.setId(null);
        settings.setName(null);
        settings.setClientId(null);

        List<ResourceRepresentation> resources = storeFactory.getResourceStore().findByResourceServer(resourceServer.getId())
                .stream().map(resource -> {
                    ResourceRepresentation rep = Models.toRepresentation(resource, resourceServer, authorization);

                    if (rep.getOwner().getId().equals(this.resourceServer.getClientId())) {
                        rep.setOwner(null);
                    } else {
                        rep.getOwner().setId(null);
                    }
                    rep.setId(null);
                    rep.setPolicies(null);
                    rep.getScopes().forEach(scopeRepresentation -> {
                        scopeRepresentation.setId(null);
                        scopeRepresentation.setIconUri(null);
                    });

                    return rep;
                }).collect(Collectors.toList());

        settings.setResources(resources);

        List<PolicyRepresentation> policies = new ArrayList<>();
        PolicyStore policyStore = storeFactory.getPolicyStore();

        policies.addAll(policyStore.findByResourceServer(resourceServer.getId())
                .stream().filter(policy -> !policy.getType().equals("resource") && !policy.getType().equals("scope"))
                .map(policy -> createPolicyRepresentation(storeFactory, policy)).collect(Collectors.toList()));
        policies.addAll(policyStore.findByResourceServer(resourceServer.getId())
                .stream().filter(policy -> policy.getType().equals("resource") || policy.getType().equals("scope"))
                .map(policy -> createPolicyRepresentation(storeFactory, policy)).collect(Collectors.toList()));

        settings.setPolicies(policies);

        List<ScopeRepresentation> scopes = storeFactory.getScopeStore().findByResourceServer(resourceServer.getId()).stream().map(scope -> {
            ScopeRepresentation rep = Models.toRepresentation(scope, authorization);

            rep.setId(null);
            rep.setPolicies(null);
            rep.setResources(null);

            return rep;
        }).collect(Collectors.toList());

        settings.setScopes(scopes);

        return Response.ok(settings).build();
    }

    @Path("/import")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response importSettings(@Context final UriInfo uriInfo, ResourceServerRepresentation rep) throws IOException {
        this.auth.requireManage();

        resourceServer.setPolicyEnforcementMode(rep.getPolicyEnforcementMode());
        resourceServer.setAllowRemoteResourceManagement(rep.isAllowRemoteResourceManagement());

        StoreFactory storeFactory = authorization.getStoreFactory();
        ResourceStore resourceStore = storeFactory.getResourceStore();
        ScopeStore scopeStore = storeFactory.getScopeStore();
        ScopeService scopeResource = new ScopeService(resourceServer, this.authorization, this.auth);

        ResteasyProviderFactory.getInstance().injectProperties(scopeResource);

        rep.getScopes().forEach(scope -> {
            Scope existing = scopeStore.findByName(scope.getName(), resourceServer.getId());

            if (existing != null) {
                scopeResource.update(existing.getId(), scope);
            } else {
                scopeResource.create(scope);
            }
        });

        ResourceSetService resourceSetResource = new ResourceSetService(resourceServer, this.authorization, this.auth);

        rep.getResources().forEach(resourceRepresentation -> {
            ResourceOwnerRepresentation owner = resourceRepresentation.getOwner();

            if (owner == null) {
                owner = new ResourceOwnerRepresentation();
            }

            owner.setId(resourceServer.getClientId());

            if (owner.getName() != null) {
                UserModel user = this.session.users().getUserByUsername(owner.getName(), this.realm);

                if (user != null) {
                    owner.setId(user.getId());
                }
            }

            Resource existing = resourceStore.findByName(resourceRepresentation.getName(), this.resourceServer.getId());

            if (existing != null) {
                resourceSetResource.update(existing.getId(), resourceRepresentation);
            } else {
                resourceSetResource.create(resourceRepresentation);
            }
        });

        PolicyStore policyStore = storeFactory.getPolicyStore();
        PolicyService policyResource = new PolicyService(resourceServer, this.authorization, this.auth);

        ResteasyProviderFactory.getInstance().injectProperties(policyResource);

        rep.getPolicies().forEach(policyRepresentation -> {
            Map<String, String> config = policyRepresentation.getConfig();

            String roles = config.get("roles");

            if (roles != null && !roles.isEmpty()) {
                try {
                    List<Map> rolesMap = JsonSerialization.readValue(roles, List.class);
                    config.put("roles", JsonSerialization.writeValueAsString(rolesMap.stream().map(roleConfig -> {
                        String roleName = roleConfig.get("id").toString();
                        String clientId = null;
                        int clientIdSeparator = roleName.indexOf("/");

                        if (clientIdSeparator != -1) {
                            clientId = roleName.substring(0, clientIdSeparator);
                            roleName = roleName.substring(clientIdSeparator + 1);
                        }

                        RoleModel role;

                        if (clientId == null) {
                            role = realm.getRole(roleName);
                        } else {
                            role = realm.getClientByClientId(clientId).getRole(roleName);
                        }

                        // fallback to find any client role with the given name
                        if (role == null) {
                            String finalRoleName = roleName;
                            role = realm.getClients().stream().map(clientModel -> clientModel.getRole(finalRoleName)).filter(roleModel -> roleModel != null)
                                    .findFirst().orElse(null);
                        }

                        if (role == null) {
                            throw new RuntimeException("Error while importing configuration. Role [" + role + "] could not be found.");
                        }

                        roleConfig.put("id", role.getId());
                        return roleConfig;
                    }).collect(Collectors.toList())));
                } catch (Exception e) {
                    throw new RuntimeException("Error while exporting policy [" + policyRepresentation.getName() + "].", e);
                }
            }

            String users = config.get("users");

            if (users != null && !users.isEmpty()) {
                try {
                    List<String> usersMap = JsonSerialization.readValue(users, List.class);
                    config.put("users", JsonSerialization.writeValueAsString(usersMap.stream().map(userName -> this.session.users().getUserByUsername(userName, this.realm).getId()).collect(Collectors.toList())));
                } catch (Exception e) {
                    throw new RuntimeException("Error while exporting policy [" + policyRepresentation.getName() + "].", e);
                }
            }

            String scopes = config.get("scopes");

            if (scopes != null && !scopes.isEmpty()) {
                try {
                    List<String> scopesMap = JsonSerialization.readValue(scopes, List.class);
                    config.put("scopes", JsonSerialization.writeValueAsString(scopesMap.stream().map(scopeName -> {
                        Scope newScope = scopeStore.findByName(scopeName, resourceServer.getId());

                        if (newScope == null) {
                            throw new RuntimeException("Scope with name [" + scopeName + "] not defined.");
                        }

                        return newScope.getId();
                    }).collect(Collectors.toList())));
                } catch (Exception e) {
                    throw new RuntimeException("Error while exporting policy [" + policyRepresentation.getName() + "].", e);
                }
            }

            String policyResources = config.get("resources");

            if (policyResources != null && !policyResources.isEmpty()) {
                try {
                    List<String> resources = JsonSerialization.readValue(policyResources, List.class);
                    config.put("resources", JsonSerialization.writeValueAsString(resources.stream().map(resourceName -> storeFactory.getResourceStore().findByName(resourceName, resourceServer.getId()).getId()).collect(Collectors.toList())));
                } catch (Exception e) {
                    throw new RuntimeException("Error while exporting policy [" + policyRepresentation.getName() + "].", e);
                }
            }

            String applyPolicies = config.get("applyPolicies");

            if (applyPolicies != null && !applyPolicies.isEmpty()) {
                try {
                    List<String> policies = JsonSerialization.readValue(applyPolicies, List.class);
                    config.put("applyPolicies", JsonSerialization.writeValueAsString(policies.stream().map(policyName -> {
                        Policy policy = policyStore.findByName(policyName, resourceServer.getId());

                        if (policy == null) {
                            throw new RuntimeException("Policy with name [" + policyName + "] not defined.");
                        }

                        return policy.getId();
                    }).collect(Collectors.toList())));
                } catch (Exception e) {
                    throw new RuntimeException("Error while exporting policy [" + policyRepresentation.getName() + "].", e);
                }
            }

            Policy existing = policyStore.findByName(policyRepresentation.getName(), this.resourceServer.getId());

            if (existing != null) {
                policyResource.update(existing.getId(), policyRepresentation);
            } else {
                policyResource.create(policyRepresentation);
            }
        });

        return Response.noContent().build();
    }

    @Path("/resource")
    public ResourceSetService getResourceSetResource() {
        ResourceSetService resource = new ResourceSetService(this.resourceServer, this.authorization, this.auth);

        ResteasyProviderFactory.getInstance().injectProperties(resource);

        return resource;
    }

    @Path("/scope")
    public ScopeService getScopeResource() {
        ScopeService resource = new ScopeService(this.resourceServer, this.authorization, this.auth);

        ResteasyProviderFactory.getInstance().injectProperties(resource);

        return resource;
    }

    @Path("/policy")
    public PolicyService getPolicyResource() {
        PolicyService resource = new PolicyService(this.resourceServer, this.authorization, this.auth);

        ResteasyProviderFactory.getInstance().injectProperties(resource);

        return resource;
    }

    private void createDefaultPermission(ResourceRepresentation resource, PolicyRepresentation policy) {
        PolicyRepresentation defaultPermission = new PolicyRepresentation();

        defaultPermission.setName("Default Permission");
        defaultPermission.setType("resource");
        defaultPermission.setDescription("A permission that applies to the default resource type");
        defaultPermission.setDecisionStrategy(DecisionStrategy.UNANIMOUS);
        defaultPermission.setLogic(Logic.POSITIVE);

        HashMap<String, String> defaultPermissionConfig = new HashMap<>();

        defaultPermissionConfig.put("default", "true");
        defaultPermissionConfig.put("defaultResourceType", resource.getType());
        defaultPermissionConfig.put("applyPolicies", "[\"" + policy.getName() + "\"]");

        defaultPermission.setConfig(defaultPermissionConfig);

        getPolicyResource().create(defaultPermission);
    }

    private PolicyRepresentation createDefaultPolicy() {
        PolicyRepresentation defaultPolicy = new PolicyRepresentation();

        defaultPolicy.setName("Default Policy");
        defaultPolicy.setDescription("A policy that grants access only for users within this realm");
        defaultPolicy.setType("js");
        defaultPolicy.setDecisionStrategy(DecisionStrategy.AFFIRMATIVE);
        defaultPolicy.setLogic(Logic.POSITIVE);

        HashMap<String, String> defaultPolicyConfig = new HashMap<>();

        defaultPolicyConfig.put("code", "// by default, grants any permission associated with this policy\n$evaluation.grant();\n");

        defaultPolicy.setConfig(defaultPolicyConfig);

        getPolicyResource().create(defaultPolicy);

        return defaultPolicy;
    }

    private ResourceRepresentation createDefaultResource() {
        ResourceRepresentation defaultResource = new ResourceRepresentation();

        defaultResource.setName("Default Resource");
        defaultResource.setUri("/*");
        defaultResource.setType("urn:" + this.client.getClientId() + ":resources:default");

        getResourceSetResource().create(defaultResource);
        return defaultResource;
    }

    private void createDefaultRoles() {
        RoleModel umaProtectionRole = client.getRole(Constants.AUTHZ_UMA_PROTECTION);

        if (umaProtectionRole == null) {
            umaProtectionRole = client.addRole(Constants.AUTHZ_UMA_PROTECTION);
        }

        UserModel serviceAccount = this.session.users().getServiceAccount(client);

        if (!serviceAccount.hasRole(umaProtectionRole)) {
            serviceAccount.grantRole(umaProtectionRole);
        }
    }

    private PolicyRepresentation createPolicyRepresentation(StoreFactory storeFactory, Policy policy) {
        try {
            PolicyRepresentation rep = Models.toRepresentation(policy, authorization);

            rep.setId(null);
            rep.setDependentPolicies(null);

            Map<String, String> config = rep.getConfig();

            String roles = config.get("roles");

            if (roles != null && !roles.isEmpty()) {
                List<Map> rolesMap = JsonSerialization.readValue(roles, List.class);
                config.put("roles", JsonSerialization.writeValueAsString(rolesMap.stream().map(roleMap -> {
                    roleMap.put("id", realm.getRoleById(roleMap.get("id").toString()).getName());
                    return roleMap;
                }).collect(Collectors.toList())));
            }

            String users = config.get("users");

            if (users != null && !users.isEmpty()) {
                UserFederationManager userManager = this.session.users();
                List<String> userIds = JsonSerialization.readValue(users, List.class);
                config.put("users", JsonSerialization.writeValueAsString(userIds.stream().map(userId -> userManager.getUserById(userId, this.realm).getUsername()).collect(Collectors.toList())));
            }

            String scopes = config.get("scopes");

            if (scopes != null && !scopes.isEmpty()) {
                ScopeStore scopeStore = storeFactory.getScopeStore();
                List<String> scopeIds = JsonSerialization.readValue(scopes, List.class);
                config.put("scopes", JsonSerialization.writeValueAsString(scopeIds.stream().map(scopeId -> scopeStore.findById(scopeId).getName()).collect(Collectors.toList())));
            }

            String policyResources = config.get("resources");

            if (policyResources != null && !policyResources.isEmpty()) {
                ResourceStore resourceStore = storeFactory.getResourceStore();
                List<String> resourceIds = JsonSerialization.readValue(policyResources, List.class);
                config.put("resources", JsonSerialization.writeValueAsString(resourceIds.stream().map(resourceId -> resourceStore.findById(resourceId).getName()).collect(Collectors.toList())));
            }

            Set<Policy> associatedPolicies = policy.getAssociatedPolicies();

            if (!associatedPolicies.isEmpty()) {
                config.put("applyPolicies", JsonSerialization.writeValueAsString(associatedPolicies.stream().map(associated -> associated.getName()).collect(Collectors.toList())));
            }

            rep.setAssociatedPolicies(null);

            return rep;
        } catch (Exception e) {
            throw new RuntimeException("Error while exporting policy [" + policy.getName() + "].", e);
        }
    }
}

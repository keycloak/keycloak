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

import org.jboss.resteasy.plugins.providers.multipart.InputPart;
import org.jboss.resteasy.plugins.providers.multipart.MultipartFormDataInput;
import org.jboss.resteasy.spi.ResteasyProviderFactory;
import org.keycloak.authorization.AuthorizationProvider;
import org.keycloak.authorization.admin.representation.PolicyRepresentation;
import org.keycloak.authorization.admin.representation.ResourceOwnerRepresentation;
import org.keycloak.authorization.admin.representation.ResourceRepresentation;
import org.keycloak.authorization.admin.representation.ResourceServerRepresentation;
import org.keycloak.authorization.admin.representation.ScopeRepresentation;
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
import org.keycloak.services.resources.admin.RealmAuth;
import org.keycloak.util.JsonSerialization;

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

                    rep.getOwner().setId(null);
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

            rep.getPolicies().forEach(policyRepresentation -> {
                policyRepresentation.setId(null);
                policyRepresentation.setConfig(null);
                policyRepresentation.setType(null);
                policyRepresentation.setDecisionStrategy(null);
                policyRepresentation.setDescription(null);
                policyRepresentation.setDependentPolicies(null);
            });

            return rep;
        }).collect(Collectors.toList());

        settings.setScopes(scopes);

        return Response.ok(settings).build();
    }

    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public Response importSettings(@Context final UriInfo uriInfo, MultipartFormDataInput input) throws IOException {
        this.auth.requireManage();
        Map<String, List<InputPart>> uploadForm = input.getFormDataMap();
        List<InputPart> inputParts = uploadForm.get("file");

        for (InputPart inputPart : inputParts) {
            ResourceServerRepresentation rep = JsonSerialization.readValue(inputPart.getBodyAsString(), ResourceServerRepresentation.class);

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
                    roles = roles.replace("[", "");
                    roles = roles.replace("]", "");

                    if (!roles.isEmpty()) {
                        String roleNames = "";

                        for (String role : roles.split(",")) {
                            if (!roleNames.isEmpty()) {
                                roleNames = roleNames + ",";
                            }

                            role = role.replace("\"", "");

                            roleNames = roleNames + "\"" + this.realm.getRole(role).getId() + "\"";
                        }

                        config.put("roles", "[" + roleNames + "]");
                    }
                }

                String users = config.get("users");

                if (users != null) {
                    users = users.replace("[", "");
                    users = users.replace("]", "");

                    if (!users.isEmpty()) {
                        String userNames = "";

                        for (String user : users.split(",")) {
                            if (!userNames.isEmpty()) {
                                userNames =  userNames + ",";
                            }

                            user = user.replace("\"", "");

                            userNames = userNames + "\"" + this.session.users().getUserByUsername(user, this.realm).getId() + "\"";
                        }

                        config.put("users", "[" + userNames + "]");
                    }
                }

                String scopes = config.get("scopes");

                if (scopes != null && !scopes.isEmpty()) {
                    scopes = scopes.replace("[", "");
                    scopes = scopes.replace("]", "");

                    if (!scopes.isEmpty()) {
                        String scopeNames = "";

                        for (String scope : scopes.split(",")) {
                            if (!scopeNames.isEmpty()) {
                                scopeNames =  scopeNames + ",";
                            }

                            scope = scope.replace("\"", "");

                            Scope newScope = scopeStore.findByName(scope, resourceServer.getId());

                            if (newScope == null) {
                                throw new RuntimeException("Scope with name [" + scope + "] not defined.");
                            }

                            scopeNames = scopeNames + "\"" + newScope.getId() + "\"";
                        }

                        config.put("scopes", "[" + scopeNames + "]");
                    }
                }

                String policyResources = config.get("resources");

                if (policyResources != null && !policyResources.isEmpty()) {
                    policyResources = policyResources.replace("[", "");
                    policyResources = policyResources.replace("]", "");

                    if (!policyResources.isEmpty()) {
                        String resourceNames = "";

                        for (String resource : policyResources.split(",")) {
                            if (!resourceNames.isEmpty()) {
                                resourceNames =  resourceNames + ",";
                            }

                            resource = resource.replace("\"", "");

                            if ("".equals(resource)) {
                                continue;
                            }

                            resourceNames = resourceNames + "\"" + storeFactory.getResourceStore().findByName(resource, resourceServer.getId()).getId() + "\"";
                        }

                        config.put("resources", "[" + resourceNames + "]");
                    }
                }

                String applyPolicies = config.get("applyPolicies");

                if (applyPolicies != null && !applyPolicies.isEmpty()) {
                    applyPolicies = applyPolicies.replace("[", "");
                    applyPolicies = applyPolicies.replace("]", "");

                    if (!applyPolicies.isEmpty()) {
                        String policyNames = "";

                        for (String pId : applyPolicies.split(",")) {
                            if (!policyNames.isEmpty()) {
                                policyNames = policyNames + ",";
                            }

                            pId = pId.replace("\"", "").trim();

                            Policy policy = policyStore.findByName(pId, resourceServer.getId());

                            if (policy == null) {
                                throw new RuntimeException("Policy with name [" + pId + "] not defined.");
                            }

                            policyNames = policyNames + "\"" + policy.getId() + "\"";
                        }

                        config.put("applyPolicies", "[" + policyNames + "]");
                    }
                }

                Policy existing = policyStore.findByName(policyRepresentation.getName(), this.resourceServer.getId());

                if (existing != null) {
                    policyResource.update(existing.getId(), policyRepresentation);
                } else {
                    policyResource.create(policyRepresentation);
                }
            });
        }

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
        defaultPermission.setDecisionStrategy(Policy.DecisionStrategy.UNANIMOUS);
        defaultPermission.setLogic(Policy.Logic.POSITIVE);

        HashMap<String, String> defaultPermissionConfig = new HashMap<>();

        defaultPermissionConfig.put("default", "true");
        defaultPermissionConfig.put("defaultResourceType", resource.getType());
        defaultPermissionConfig.put("applyPolicies", "[\"" + policy.getName() + "\"]");

        defaultPermission.setConfig(defaultPermissionConfig);

        getPolicyResource().create(defaultPermission);
    }

    private PolicyRepresentation createDefaultPolicy() {
        PolicyRepresentation defaultPolicy = new PolicyRepresentation();

        defaultPolicy.setName("Only From Realm Policy");
        defaultPolicy.setDescription("A policy that grants access only for users within this realm");
        defaultPolicy.setType("js");
        defaultPolicy.setDecisionStrategy(Policy.DecisionStrategy.AFFIRMATIVE);
        defaultPolicy.setLogic(Policy.Logic.POSITIVE);

        HashMap<String, String> defaultPolicyConfig = new HashMap<>();

        defaultPolicyConfig.put("code", "var context = $evaluation.getContext();\n" +
                "\n" +
                "// using attributes from the evaluation context to obtain the realm\n" +
                "var contextAttributes = context.getAttributes();\n" +
                "var realmName = contextAttributes.getValue('kc.realm.name').asString(0);\n" +
                "\n" +
                "// using attributes from the identity to obtain the issuer\n" +
                "var identity = context.getIdentity();\n" +
                "var identityAttributes = identity.getAttributes();\n" +
                "var issuer = identityAttributes.getValue('iss').asString(0);\n" +
                "\n" +
                "// only users from the realm have access granted \n" +
                "if (issuer.endsWith(realmName)) {\n" +
                "    $evaluation.grant();\n" +
                "}");

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

        UserModel serviceAccount = this.session.users().getUserByServiceAccountClient(client);

        if (!serviceAccount.hasRole(umaProtectionRole)) {
            serviceAccount.grantRole(umaProtectionRole);
        }
    }

    private PolicyRepresentation createPolicyRepresentation(StoreFactory storeFactory, Policy policy) {
        PolicyRepresentation rep = Models.toRepresentation(policy, authorization);

        rep.setId(null);
        rep.setDependentPolicies(null);

        Map<String, String> config = rep.getConfig();

        String roles = config.get("roles");

        if (roles != null && !roles.isEmpty()) {
            roles = roles.replace("[", "");
            roles = roles.replace("]", "");

            if (!roles.isEmpty()) {
                String roleNames = "";

                for (String role : roles.split(",")) {
                    if (!roleNames.isEmpty()) {
                        roleNames = roleNames + ",";
                    }

                    role = role.replace("\"", "");

                    roleNames = roleNames + "\"" + this.realm.getRoleById(role).getName() + "\"";
                }

                config.put("roles", "[" + roleNames + "]");
            }
        }

        String users = config.get("users");

        if (users != null) {
            users = users.replace("[", "");
            users = users.replace("]", "");

            if (!users.isEmpty()) {
                UserFederationManager userManager = this.session.users();
                String userNames = "";

                for (String user : users.split(",")) {
                    if (!userNames.isEmpty()) {
                        userNames =  userNames + ",";
                    }

                    user = user.replace("\"", "");

                    userNames = userNames + "\"" + userManager.getUserById(user, this.realm).getUsername() + "\"";
                }

                config.put("users", "[" + userNames + "]");
            }
        }

        String scopes = config.get("scopes");

        if (scopes != null && !scopes.isEmpty()) {
            scopes = scopes.replace("[", "");
            scopes = scopes.replace("]", "");

            if (!scopes.isEmpty()) {
                ScopeStore scopeStore = storeFactory.getScopeStore();
                String scopeNames = "";

                for (String scope : scopes.split(",")) {
                    if (!scopeNames.isEmpty()) {
                        scopeNames =  scopeNames + ",";
                    }

                    scope = scope.replace("\"", "");

                    scopeNames = scopeNames + "\"" + scopeStore.findById(scope).getName() + "\"";
                }

                config.put("scopes", "[" + scopeNames + "]");
            }
        }

        String policyResources = config.get("resources");

        if (policyResources != null && !policyResources.isEmpty()) {
            policyResources = policyResources.replace("[", "");
            policyResources = policyResources.replace("]", "");

            if (!policyResources.isEmpty()) {
                ResourceStore resourceStore = storeFactory.getResourceStore();
                String resourceNames = "";

                for (String resource : policyResources.split(",")) {
                    if (!resourceNames.isEmpty()) {
                        resourceNames =  resourceNames + ",";
                    }

                    resource = resource.replace("\"", "");

                    resourceNames = resourceNames + "\"" + resourceStore.findById(resource).getName() + "\"";
                }

                config.put("resources", "[" + resourceNames + "]");
            }
        }

        String policyNames = "";
        Set<Policy> associatedPolicies = policy.getAssociatedPolicies();

        if (!associatedPolicies.isEmpty()) {
            for (Policy associatedPolicy : associatedPolicies) {
                if (!policyNames.isEmpty()) {
                    policyNames = policyNames + ",";
                }

                policyNames = policyNames + "\"" + associatedPolicy.getName() + "\"";
            }

            config.put("applyPolicies", "[" + policyNames + "]");
        }

        return rep;
    }
}

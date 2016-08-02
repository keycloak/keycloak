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

package org.keycloak.authorization.admin.util;

import org.keycloak.authorization.AuthorizationProvider;
import org.keycloak.authorization.ErrorCode;
import org.keycloak.authorization.model.Policy;
import org.keycloak.authorization.model.Resource;
import org.keycloak.authorization.model.ResourceServer;
import org.keycloak.authorization.model.Scope;
import org.keycloak.authorization.store.PolicyStore;
import org.keycloak.authorization.store.ResourceServerStore;
import org.keycloak.authorization.store.ResourceStore;
import org.keycloak.authorization.store.StoreFactory;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.representations.idm.authorization.PolicyRepresentation;
import org.keycloak.representations.idm.authorization.ResourceOwnerRepresentation;
import org.keycloak.representations.idm.authorization.ResourceRepresentation;
import org.keycloak.representations.idm.authorization.ResourceServerRepresentation;
import org.keycloak.representations.idm.authorization.ScopeRepresentation;
import org.keycloak.services.ErrorResponseException;
import org.keycloak.util.JsonSerialization;

import javax.ws.rs.core.Response.Status;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Some utility methods to transform models to representations and vice-versa.
 *
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public final class Models {

    public static ScopeRepresentation toRepresentation(Scope model, AuthorizationProvider authorizationProvider) {
        ScopeRepresentation scope = new ScopeRepresentation();

        scope.setId(model.getId());
        scope.setName(model.getName());
        scope.setIconUri(model.getIconUri());

        StoreFactory storeFactory = authorizationProvider.getStoreFactory();

        scope.setResources(new ArrayList<>());

        storeFactory.getResourceStore().findByScope(model.getId()).forEach(resource -> scope.getResources().add(toRepresentation(resource, resource.getResourceServer(), authorizationProvider)));

        PolicyStore policyStore = storeFactory.getPolicyStore();

        scope.setPolicies(new ArrayList<>());

        policyStore.findByScopeIds(Arrays.asList(model.getId()), model.getResourceServer().getId()).forEach(policyModel -> {
            PolicyRepresentation policy = new PolicyRepresentation();

            policy.setId(policyModel.getId());
            policy.setName(policyModel.getName());
            policy.setType(policyModel.getType());

            if (!scope.getPolicies().contains(policy)) {
                scope.getPolicies().add(policy);
            }
        });

        return scope;
    }

    public static Scope toModel(ScopeRepresentation scope, ResourceServer resourceServer, AuthorizationProvider authorization) {
        StoreFactory storeFactory = authorization.getStoreFactory();
        Scope model = storeFactory.getScopeStore().findByName(scope.getName(), resourceServer.getId());

        if (model == null) {
            model = storeFactory.getScopeStore().create(scope.getName(), resourceServer);

            model.setIconUri(scope.getIconUri());
        }

        return model;
    }

    public static ResourceServerRepresentation toRepresentation(ResourceServer model, RealmModel realm) {
        ResourceServerRepresentation server = new ResourceServerRepresentation();

        server.setId(model.getId());
        server.setClientId(model.getClientId());
        ClientModel clientById = realm.getClientById(model.getClientId());
        server.setName(clientById.getClientId());
        server.setAllowRemoteResourceManagement(model.isAllowRemoteResourceManagement());
        server.setPolicyEnforcementMode(model.getPolicyEnforcementMode());

        return server;
    }

    public static ResourceServer toModel(ResourceServerRepresentation server, AuthorizationProvider authorization) {
        RealmModel realm = authorization.getKeycloakSession().getContext().getRealm();
        ClientModel client = realm.getClientById(server.getClientId());

        if (client == null) {
            throw new ErrorResponseException(ErrorCode.INVALID_CLIENT_ID, "Client with id [" + server.getClientId() + "] not found in realm [" + realm.getName()  + "].", Status.BAD_REQUEST);
        }

        if (!client.isServiceAccountsEnabled()) {
            throw new ErrorResponseException(ErrorCode.INVALID_CLIENT_ID, "Client with id [" + server.getClientId() + "] must have a service account.", Status.BAD_REQUEST);
        }

        ResourceServer existingResourceServer = authorization.getStoreFactory().getResourceServerStore().findByClient(client.getId());

        if (existingResourceServer != null) {
            throw new ErrorResponseException(ErrorCode.INVALID_CLIENT_ID, "Resource server already exists with client id [" + server.getClientId() + "].", Status.BAD_REQUEST);
        }

        if (server.getName() == null) {
            server.setName(client.getName());
        }

        ResourceServer model = authorization.getStoreFactory().getResourceServerStore().create(client.getId());

        model.setAllowRemoteResourceManagement(server.isAllowRemoteResourceManagement());
        model.setPolicyEnforcementMode(server.getPolicyEnforcementMode());

        return model;
    }

    public static PolicyRepresentation toRepresentation(Policy model, AuthorizationProvider authorization) {
        PolicyRepresentation representation = new PolicyRepresentation();

        representation.setId(model.getId());
        representation.setName(model.getName());
        representation.setDescription(model.getDescription());
        representation.setType(model.getType());
        representation.setDecisionStrategy(model.getDecisionStrategy());
        representation.setLogic(model.getLogic());
        representation.setConfig(new HashMap<>(model.getConfig()));

        List<Policy> policies = authorization.getStoreFactory().getPolicyStore().findDependentPolicies(model.getId());

        representation.setDependentPolicies(policies.stream().map(policy -> {
            PolicyRepresentation representation1 = new PolicyRepresentation();

            representation1.setId(policy.getId());
            representation1.setName(policy.getName());

            return representation1;
        }).collect(Collectors.toList()));

        List<PolicyRepresentation> associatedPolicies = new ArrayList<>();

        List<String> obj = model.getAssociatedPolicies().stream().map(policy -> {
            PolicyRepresentation representation1 = new PolicyRepresentation();

            representation1.setId(policy.getId());
            representation1.setName(policy.getName());
            representation1.setType(policy.getType());

            associatedPolicies.add(representation1);

            return policy.getId();
        }).collect(Collectors.toList());

        representation.setAssociatedPolicies(associatedPolicies);

        try {
            representation.getConfig().put("applyPolicies", JsonSerialization.writeValueAsString(obj));
        } catch (IOException e) {
            e.printStackTrace();
        }

        return representation;
    }

    public static Policy toModel(PolicyRepresentation policy, ResourceServer resourceServer, AuthorizationProvider authorizationManager) {
        Policy model = authorizationManager.getStoreFactory().getPolicyStore().create(policy.getName(), policy.getType(), resourceServer);

        model.setDescription(policy.getDescription());
        model.setDecisionStrategy(policy.getDecisionStrategy());
        model.setLogic(policy.getLogic());
        model.setConfig(policy.getConfig());

        return model;
    }

    public static ResourceRepresentation toRepresentation(Resource model, ResourceServer resourceServer, AuthorizationProvider authorization) {
        ResourceRepresentation resource = new ResourceRepresentation();

        resource.setId(model.getId());
        resource.setType(model.getType());
        resource.setName(model.getName());
        resource.setUri(model.getUri());
        resource.setIconUri(model.getIconUri());

        ResourceOwnerRepresentation owner = new ResourceOwnerRepresentation();

        owner.setId(model.getOwner());

        KeycloakSession keycloakSession = authorization.getKeycloakSession();
        RealmModel realm = keycloakSession.getContext().getRealm();

        if (owner.getId().equals(resourceServer.getClientId())) {
            ClientModel clientModel = realm.getClientById(resourceServer.getClientId());
            owner.setName(clientModel.getClientId());
        } else {
            UserModel userModel = keycloakSession.users().getUserById(owner.getId(), realm);

            if (userModel == null) {
                throw new ErrorResponseException("invalid_owner", "Could not find the user [" + owner.getId() + "] who owns the Resource [" + resource.getId() + "].", Status.BAD_REQUEST);
            }

            owner.setName(userModel.getUsername());
        }

        resource.setOwner(owner);

        resource.setScopes(model.getScopes().stream().map(model1 -> {
            ScopeRepresentation scope = new ScopeRepresentation();
            scope.setId(model1.getId());
            scope.setName(model1.getName());
            String iconUri = model1.getIconUri();
            if (iconUri != null) {
                scope.setIconUri(iconUri);
            }
            return scope;
        }).collect(Collectors.toSet()));

        resource.setTypedScopes(new ArrayList<>());

        if (resource.getType() != null) {
            ResourceStore resourceStore = authorization.getStoreFactory().getResourceStore();
            for (Resource typed : resourceStore.findByType(resource.getType())) {
                if (typed.getOwner().equals(resourceServer.getClientId()) && !typed.getId().equals(resource.getId())) {
                    resource.setTypedScopes(typed.getScopes().stream().map(model1 -> {
                        ScopeRepresentation scope = new ScopeRepresentation();
                        scope.setId(model1.getId());
                        scope.setName(model1.getName());
                        String iconUri = model1.getIconUri();
                        if (iconUri != null) {
                            scope.setIconUri(iconUri);
                        }
                        return scope;
                    }).filter(scopeRepresentation -> !resource.getScopes().contains(scopeRepresentation)).collect(Collectors.toList()));
                }
            }
        }

        resource.setPolicies(new ArrayList<>());

        Set<Policy> policies = new HashSet<>();
        PolicyStore policyStore = authorization.getStoreFactory().getPolicyStore();

        policies.addAll(policyStore.findByResource(resource.getId()));
        policies.addAll(policyStore.findByResourceType(resource.getType(), resourceServer.getId()));
        policies.addAll(policyStore.findByScopeIds(resource.getScopes().stream().map(scope -> scope.getId()).collect(Collectors.toList()), resourceServer.getId()));

        for (Policy policyModel : policies) {
            PolicyRepresentation policy = new PolicyRepresentation();

            policy.setId(policyModel.getId());
            policy.setName(policyModel.getName());
            policy.setType(policyModel.getType());

            if (!resource.getPolicies().contains(policy)) {
                resource.getPolicies().add(policy);
            }
        }

        return resource;
    }

    public static Resource toModel(ResourceRepresentation resource, ResourceServer resourceServer, AuthorizationProvider authorization) {
        ResourceOwnerRepresentation owner = resource.getOwner();

        if (owner == null) {
            owner = new ResourceOwnerRepresentation();
            owner.setId(resourceServer.getClientId());
        }

        if (owner.getId() == null) {
            throw new ErrorResponseException("invalid_owner", "No owner specified for resource [" + resource.getName() + "].", Status.BAD_REQUEST);
        }

        ResourceStore resourceStore = authorization.getStoreFactory().getResourceStore();
        Resource model = resourceStore.create(resource.getName(), resourceServer, owner.getId());

        model.setType(resource.getType());
        model.setUri(resource.getUri());
        model.setIconUri(resource.getIconUri());

        Set<ScopeRepresentation> scopes = resource.getScopes();

        if (scopes != null) {
            model.updateScopes(scopes.stream().map((Function<ScopeRepresentation, Scope>) scope -> toModel(scope, resourceServer, authorization)).collect(Collectors.toSet()));
        }

        return model;
    }
}

/*
 * Copyright 2022 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.authorization.policy.provider.permission;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import org.keycloak.Config;
import org.keycloak.authorization.AuthorizationProvider;
import org.keycloak.authorization.model.Policy;
import org.keycloak.authorization.model.Scope;
import org.keycloak.authorization.policy.provider.PolicyProvider;
import org.keycloak.authorization.policy.provider.PolicyProviderFactory;
import org.keycloak.authorization.policy.provider.client.ClientPolicyProviderFactory;
import org.keycloak.authorization.policy.provider.role.RolePolicyProviderFactory;
import org.keycloak.authorization.store.PolicyStore;
import org.keycloak.models.ClientModel;
import org.keycloak.models.GroupModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.models.utils.ModelToRepresentation;
import org.keycloak.models.utils.RepresentationToModel;
import org.keycloak.representations.idm.authorization.AbstractPolicyRepresentation;
import org.keycloak.representations.idm.authorization.ClientPolicyRepresentation;
import org.keycloak.representations.idm.authorization.GroupPolicyRepresentation;
import org.keycloak.representations.idm.authorization.GroupPolicyRepresentation.GroupDefinition;
import org.keycloak.representations.idm.authorization.JSPolicyRepresentation;
import org.keycloak.representations.idm.authorization.PolicyRepresentation;
import org.keycloak.representations.idm.authorization.RolePolicyRepresentation;
import org.keycloak.representations.idm.authorization.RolePolicyRepresentation.RoleDefinition;
import org.keycloak.representations.idm.authorization.UmaPermissionRepresentation;
import org.keycloak.representations.idm.authorization.UserPolicyRepresentation;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public class UMAPolicyProviderFactory implements PolicyProviderFactory<UmaPermissionRepresentation> {

    private UMAPolicyProvider provider = new UMAPolicyProvider();

    @Override
    public String getName() {
        return "UMA";
    }

    @Override
    public String getGroup() {
        return "Others";
    }

    @Override
    public boolean isInternal() {
        return true;
    }

    @Override
    public PolicyProvider create(AuthorizationProvider authorization) {
        return provider;
    }

    @Override
    public PolicyProvider create(KeycloakSession session) {
        return provider;
    }

    @Override
    public void onCreate(Policy policy, UmaPermissionRepresentation representation, AuthorizationProvider authorization) {
        policy.setOwner(representation.getOwner());
        PolicyStore policyStore = authorization.getStoreFactory().getPolicyStore();
        Set<String> roles = representation.getRoles();

        if (roles != null) {
            for (String role : roles) {
                createRolePolicy(policy, policyStore, role, representation.getOwner());
            }
        }

        Set<String> groups = representation.getGroups();

        if (groups != null) {
            for (String group : groups) {
                createGroupPolicy(policy, policyStore, group, representation.getOwner());
            }
        }

        Set<String> clients = representation.getClients();

        if (clients != null) {
            for (String client : clients) {
                createClientPolicy(policy, policyStore, client, representation.getOwner());
            }
        }

        Set<String> users = representation.getUsers();

        if (users != null) {
            for (String user : users) {
                createUserPolicy(policy, policyStore, user, representation.getOwner());
            }
        }

        String condition = representation.getCondition();

        if (condition != null) {
            createJSPolicy(policy, policyStore, condition, representation.getOwner());
        }
    }

    @Override
    public void onUpdate(Policy policy, UmaPermissionRepresentation representation, AuthorizationProvider authorization) {
        PolicyStore policyStore = authorization.getStoreFactory().getPolicyStore();
        Set<Policy> associatedPolicies = policy.getAssociatedPolicies();

        for (Policy associatedPolicy : associatedPolicies) {
            AbstractPolicyRepresentation associatedRep = ModelToRepresentation.toRepresentation(associatedPolicy, authorization, false, false);

            if ("role".equals(associatedRep.getType())) {
                Set<String> updatedRoles = representation.getRoles();

                if (updatedRoles == null || updatedRoles.isEmpty()) {
                    policyStore.delete(associatedPolicy.getId());
                } else if (authorization.getProviderFactory(associatedPolicy.getType()) instanceof RolePolicyProviderFactory factory) {
                    factory.updateFromUma(associatedPolicy, updatedRoles, authorization);
                } else {
                    RolePolicyRepresentation rep = RolePolicyRepresentation.class.cast(associatedRep);
                    rep.setRoles(new HashSet<>());
                    updatedRoles.forEach(rep::addRole);
                    RepresentationToModel.toModel(rep, authorization, associatedPolicy);
                }
            } else if (associatedRep instanceof JSPolicyRepresentation) {
                JSPolicyRepresentation rep = JSPolicyRepresentation.class.cast(associatedRep);

                if (representation.getCondition() != null) {
                    rep.setType(representation.getCondition());
                    RepresentationToModel.toModel(rep, authorization, associatedPolicy);
                } else {
                    policyStore.delete(associatedPolicy.getId());
                }
            } else if ("group".equals(associatedRep.getType())) {
                GroupPolicyRepresentation rep = GroupPolicyRepresentation.class.cast(associatedRep);

                Set<String> updatedGroups = representation.getGroups();

                if (updatedGroups == null || updatedGroups.isEmpty()) {
                    policyStore.delete(associatedPolicy.getId());
                } else {
                    Set<GroupDefinition> retained = new HashSet<>();
                    Set<String> newPaths = new HashSet<>(updatedGroups);
                    for (GroupDefinition definition : rep.getGroups()) {
                        String reference = getGroupReference(definition, authorization.getRealm());
                        if (reference != null && updatedGroups.contains(reference)) {
                            retained.add(definition);
                            newPaths.remove(reference);
                        }
                    }
                    rep.setGroups(retained);
                    newPaths.forEach(rep::addGroupPath);
                    RepresentationToModel.toModel(rep, authorization, associatedPolicy);
                }
            } else if ("client".equals(associatedRep.getType())) {
                Set<String> updatedClients = representation.getClients();

                if (updatedClients == null || updatedClients.isEmpty()) {
                    policyStore.delete(associatedPolicy.getId());
                } else if (authorization.getProviderFactory(associatedPolicy.getType()) instanceof ClientPolicyProviderFactory factory) {
                    factory.updateFromUma(associatedPolicy, updatedClients, authorization);
                } else {
                    ClientPolicyRepresentation rep = ClientPolicyRepresentation.class.cast(associatedRep);
                    rep.setClients(new HashSet<>(updatedClients));
                    RepresentationToModel.toModel(rep, authorization, associatedPolicy);
                }
            } else if ("user".equals(associatedRep.getType())) {
                UserPolicyRepresentation rep = UserPolicyRepresentation.class.cast(associatedRep);

                rep.setUsers(new HashSet<>());

                Set<String> updatedUsers = representation.getUsers();

                if (updatedUsers != null) {
                    for (String user : updatedUsers) {
                        rep.addUser(user);
                    }
                }

                if (rep.getUsers().isEmpty()) {
                    policyStore.delete(associatedPolicy.getId());
                } else {
                    RepresentationToModel.toModel(rep, authorization, associatedPolicy);
                }
            }
        }

        Set<String> updatedRoles = representation.getRoles();

        if (updatedRoles != null) {
            boolean createPolicy = true;

            for (Policy associatedPolicy : associatedPolicies) {
                if ("role".equals(associatedPolicy.getType())) {
                    createPolicy = false;
                }
            }

            if (createPolicy) {
                for (String role : updatedRoles) {
                    createRolePolicy(policy, policyStore, role, policy.getOwner());
                }
            }
        }

        Set<String> updatedGroups = representation.getGroups();

        if (updatedGroups != null) {
            boolean createPolicy = true;

            for (Policy associatedPolicy : associatedPolicies) {
                if ("group".equals(associatedPolicy.getType())) {
                    createPolicy = false;
                }
            }

            if (createPolicy) {
                for (String group : updatedGroups) {
                    createGroupPolicy(policy, policyStore, group, policy.getOwner());
                }
            }
        }

        Set<String> updatedClients = representation.getClients();

        if (updatedClients != null) {
            boolean createPolicy = true;

            for (Policy associatedPolicy : associatedPolicies) {
                if ("client".equals(associatedPolicy.getType())) {
                    createPolicy = false;
                }
            }

            if (createPolicy) {
                for (String client : updatedClients) {
                    createClientPolicy(policy, policyStore, client, policy.getOwner());
                }
            }
        }

        Set<String> updatedUsers = representation.getUsers();

        if (updatedUsers != null) {
            boolean createPolicy = true;

            for (Policy associatedPolicy : associatedPolicies) {
                if ("user".equals(associatedPolicy.getType())) {
                    createPolicy = false;
                }
            }

            if (createPolicy) {
                for (String user : updatedUsers) {
                    createUserPolicy(policy, policyStore, user, policy.getOwner());
                }
            }
        }

        String condition = representation.getCondition();

        if (condition != null) {
            boolean createPolicy = true;

            for (Policy associatedPolicy : associatedPolicies) {
                if (associatedPolicy.getType().startsWith("script-")) {
                    createPolicy = false;
                }
            }

            if (createPolicy) {
                createJSPolicy(policy, policyStore, condition, policy.getOwner());
            }
        }
    }

    @Override
    public void onImport(Policy policy, PolicyRepresentation representation, AuthorizationProvider authorization) {
    }

    @Override
    public UmaPermissionRepresentation toRepresentation(Policy policy, AuthorizationProvider authorization) {
        UmaPermissionRepresentation representation = new UmaPermissionRepresentation();

        representation.setScopes(policy.getScopes().stream().map(Scope::getName).collect(Collectors.toSet()));
        representation.setOwner(policy.getOwner());

        for (Policy associatedPolicy : policy.getAssociatedPolicies()) {
            AbstractPolicyRepresentation associatedRep = ModelToRepresentation.toRepresentation(associatedPolicy, authorization, false, false);
            RealmModel realm = authorization.getRealm();

            if ("role".equals(associatedRep.getType())) {
                RolePolicyRepresentation rep = RolePolicyRepresentation.class.cast(associatedRep);

                for (RoleDefinition definition : rep.getRoles()) {
                    RoleModel role = realm.getRoleById(definition.getId());

                    if (role == null) {
                        representation.addRole(definition.getId());
                    } else if (role.isClientRole()) {
                        representation.addClientRole(ClientModel.class.cast(role.getContainer()).getClientId(),role.getName());
                    } else {
                        representation.addRole(role.getName());
                    }
                }
            } else if (associatedRep instanceof JSPolicyRepresentation) {
                JSPolicyRepresentation rep = JSPolicyRepresentation.class.cast(associatedRep);
                representation.setCondition(rep.getType());
            } else if ("group".equals(associatedRep.getType())) {
                GroupPolicyRepresentation rep = GroupPolicyRepresentation.class.cast(associatedRep);

                for (GroupDefinition definition : rep.getGroups()) {
                    representation.addGroup(getGroupReference(definition, realm));
                }
            } else if ("client".equals(associatedRep.getType())) {
                ClientPolicyRepresentation rep = ClientPolicyRepresentation.class.cast(associatedRep);

                for (String client : rep.getClients()) {
                    ClientModel model = realm.getClientById(client);
                    representation.addClient(model == null ? client : model.getClientId());
                }
            } else if ("user".equals(associatedPolicy.getType())) {
                UserPolicyRepresentation rep = UserPolicyRepresentation.class.cast(associatedRep);

                for (String user : rep.getUsers()) {
                    UserModel model = authorization.getKeycloakSession().users().getUserById(realm, user);
                    representation.addUser(model == null ? user : model.getUsername());
                }
            }
        }

        return representation;
    }

    private String getGroupReference(GroupDefinition definition, RealmModel realm) {
        if (definition.getId() == null) {
            return definition.getPath();
        }
        GroupModel group = realm.getGroupById(definition.getId());
        return group == null ? definition.getId() : ModelToRepresentation.buildGroupPath(group);
    }

    @Override
    public Class<UmaPermissionRepresentation> getRepresentationType() {
        return UmaPermissionRepresentation.class;
    }

    @Override
    public void onRemove(Policy policy, AuthorizationProvider authorization) {
        PolicyStore policyStore = authorization.getStoreFactory().getPolicyStore();

        for (Policy associatedPolicy : policy.getAssociatedPolicies()) {
            policyStore.delete(associatedPolicy.getId());
        }
    }

    @Override
    public void init(Config.Scope config) {

    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {

    }

    @Override
    public void close() {

    }

    @Override
    public String getId() {
        return "uma";
    }

    private void createJSPolicy(Policy policy, PolicyStore policyStore, String condition, String owner) {
        JSPolicyRepresentation rep = new JSPolicyRepresentation();

        rep.setName(KeycloakModelUtils.generateId());
        rep.setType(condition);

        Policy associatedPolicy = policyStore.create(policy.getResourceServer(), rep);

        associatedPolicy.setOwner(owner);

        policy.addAssociatedPolicy(associatedPolicy);
    }

    private void createClientPolicy(Policy policy, PolicyStore policyStore, String client, String owner) {
        ClientPolicyRepresentation rep = new ClientPolicyRepresentation();

        rep.setName(KeycloakModelUtils.generateId());
        rep.addClient(client);

        Policy associatedPolicy = policyStore.create(policy.getResourceServer(), rep);

        associatedPolicy.setOwner(owner);

        policy.addAssociatedPolicy(associatedPolicy);
    }

    private void createGroupPolicy(Policy policy, PolicyStore policyStore, String group, String owner) {
        GroupPolicyRepresentation rep = new GroupPolicyRepresentation();

        rep.setName(KeycloakModelUtils.generateId());
        rep.addGroupPath(group);

        Policy associatedPolicy = policyStore.create(policy.getResourceServer(), rep);

        associatedPolicy.setOwner(owner);

        policy.addAssociatedPolicy(associatedPolicy);
    }

    private void createRolePolicy(Policy policy, PolicyStore policyStore, String role, String owner) {
        RolePolicyRepresentation rep = new RolePolicyRepresentation();

        rep.setName(KeycloakModelUtils.generateId());
        rep.addRole(role, false);

        Policy associatedPolicy = policyStore.create(policy.getResourceServer(), rep);

        associatedPolicy.setOwner(owner);

        policy.addAssociatedPolicy(associatedPolicy);
    }

    private void createUserPolicy(Policy policy, PolicyStore policyStore, String user, String owner) {
        UserPolicyRepresentation rep = new UserPolicyRepresentation();

        rep.setName(KeycloakModelUtils.generateId());
        rep.addUser(user);

        Policy associatedPolicy = policyStore.create(policy.getResourceServer(), rep);

        associatedPolicy.setOwner(owner);

        policy.addAssociatedPolicy(associatedPolicy);
    }
}

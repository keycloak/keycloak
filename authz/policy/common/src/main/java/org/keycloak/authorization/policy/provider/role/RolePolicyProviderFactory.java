/*
 *  Copyright 2016 Red Hat, Inc. and/or its affiliates
 *  and other contributors as indicated by the @author tags.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package org.keycloak.authorization.policy.provider.role;

import org.keycloak.Config;
import org.keycloak.authorization.AuthorizationProvider;
import org.keycloak.authorization.model.Policy;
import org.keycloak.authorization.model.ResourceServer;
import org.keycloak.authorization.policy.provider.PolicyProvider;
import org.keycloak.authorization.policy.provider.PolicyProviderFactory;
import org.keycloak.authorization.store.PolicyStore;
import org.keycloak.authorization.store.ResourceServerStore;
import org.keycloak.authorization.store.StoreFactory;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleContainerModel;
import org.keycloak.models.RoleContainerModel.RoleRemovedEvent;
import org.keycloak.models.RoleModel;
import org.keycloak.representations.idm.authorization.PolicyRepresentation;
import org.keycloak.representations.idm.authorization.RolePolicyRepresentation;
import org.keycloak.util.JsonSerialization;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public class RolePolicyProviderFactory implements PolicyProviderFactory<RolePolicyRepresentation> {

    private RolePolicyProvider provider = new RolePolicyProvider(this::toRepresentation);

    @Override
    public String getName() {
        return "Role";
    }

    @Override
    public String getGroup() {
        return "Identity Based";
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
    public RolePolicyRepresentation toRepresentation(Policy policy, AuthorizationProvider authorization) {
        RolePolicyRepresentation representation = new RolePolicyRepresentation();

        try {
            representation.setRoles(new HashSet<>(Arrays.asList(JsonSerialization.readValue(policy.getConfig().get("roles"), RolePolicyRepresentation.RoleDefinition[].class))));
        } catch (IOException cause) {
            throw new RuntimeException("Failed to deserialize roles", cause);
        }

        return representation;
    }

    @Override
    public Class<RolePolicyRepresentation> getRepresentationType() {
        return RolePolicyRepresentation.class;
    }

    @Override
    public void onCreate(Policy policy, RolePolicyRepresentation representation, AuthorizationProvider authorization) {
        updateRoles(policy, representation, authorization);
    }

    @Override
    public void onUpdate(Policy policy, RolePolicyRepresentation representation, AuthorizationProvider authorization) {
        updateRoles(policy, representation, authorization);
    }

    @Override
    public void onImport(Policy policy, PolicyRepresentation representation, AuthorizationProvider authorization) {
        try {
            updateRoles(policy, authorization, new HashSet<>(Arrays.asList(JsonSerialization.readValue(representation.getConfig().get("roles"), RolePolicyRepresentation.RoleDefinition[].class))));
        } catch (IOException cause) {
            throw new RuntimeException("Failed to deserialize roles during import", cause);
        }
    }

    @Override
    public void onExport(Policy policy, PolicyRepresentation representation, AuthorizationProvider authorizationProvider) {
        Map<String, String> config = new HashMap<>();
        Set<RolePolicyRepresentation.RoleDefinition> roles = toRepresentation(policy, authorizationProvider).getRoles();

        for (RolePolicyRepresentation.RoleDefinition roleDefinition : roles) {
            RoleModel role = authorizationProvider.getRealm().getRoleById(roleDefinition.getId());

            if (role.isClientRole()) {
                roleDefinition.setId(ClientModel.class.cast(role.getContainer()).getClientId() + "/" + role.getName());
            } else {
                roleDefinition.setId(role.getName());
            }
        }

        try {
            config.put("roles", JsonSerialization.writeValueAsString(roles));
        } catch (IOException cause) {
            throw new RuntimeException("Failed to export role policy [" + policy.getName() + "]", cause);
        }

        representation.setConfig(config);
    }

    private void updateRoles(Policy policy, RolePolicyRepresentation representation, AuthorizationProvider authorization) {
        updateRoles(policy, authorization, representation.getRoles());
    }

    private void updateRoles(Policy policy, AuthorizationProvider authorization, Set<RolePolicyRepresentation.RoleDefinition> roles) {
        KeycloakSession session = authorization.getKeycloakSession();
        RealmModel realm = authorization.getRealm();
        Set<RolePolicyRepresentation.RoleDefinition> updatedRoles = new HashSet<>();

        if (roles != null) {
            for (RolePolicyRepresentation.RoleDefinition definition : roles) {
                String roleName = definition.getId();
                String clientId = null;
                int clientIdSeparator = roleName.indexOf("/");

                if (clientIdSeparator != -1) {
                    clientId = roleName.substring(0, clientIdSeparator);
                    roleName = roleName.substring(clientIdSeparator + 1);
                }

                RoleModel role;

                if (clientId == null) {
                    role = realm.getRole(roleName);

                    if (role == null) {
                        role = realm.getRoleById(roleName);
                    }
                } else {
                    ClientModel client = realm.getClientByClientId(clientId);

                    if (client == null) {
                        throw new RuntimeException("Client with id [" + clientId + "] not found.");
                    }

                    role = client.getRole(roleName);
                }

                if (role == null) {
                    throw new RuntimeException("Error while updating policy [" + policy.getName()  + "]. Role [" + roleName + "] could not be found.");
                }

                definition.setId(role.getId());

                updatedRoles.add(definition);
            }
        }

        try {
            policy.putConfig("roles", JsonSerialization.writeValueAsString(updatedRoles));
        } catch (IOException cause) {
            throw new RuntimeException("Failed to serialize roles", cause);
        }
    }

    @Override
    public void init(Config.Scope config) {

    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
        factory.register(event -> {
            if (event instanceof RoleRemovedEvent) {
                KeycloakSession keycloakSession = ((RoleRemovedEvent) event).getKeycloakSession();
                AuthorizationProvider provider = keycloakSession.getProvider(AuthorizationProvider.class);
                StoreFactory storeFactory = provider.getStoreFactory();
                PolicyStore policyStore = storeFactory.getPolicyStore();
                RoleModel removedRole = ((RoleRemovedEvent) event).getRole();
                RoleContainerModel container = removedRole.getContainer();
                ResourceServerStore resourceServerStore = storeFactory.getResourceServerStore();

                if (container instanceof RealmModel) {
                    RealmModel realm = (RealmModel) container;
                    realm.getClientsStream()
                            .forEach(clientModel -> updateResourceServer(clientModel, removedRole, resourceServerStore, policyStore));
                } else {
                    ClientModel clientModel = (ClientModel) container;
                    updateResourceServer(clientModel, removedRole, resourceServerStore, policyStore);
                }
            }
        });
    }

    private void updateResourceServer(ClientModel clientModel, RoleModel removedRole, ResourceServerStore resourceServerStore, PolicyStore policyStore) {
        ResourceServer resourceServer = resourceServerStore.findById(clientModel.getId());

        if (resourceServer != null) {
            policyStore.findByType(getId(), resourceServer.getId()).forEach(policy -> {
                List<Map> roles = new ArrayList<>();

                for (Map<String,Object> role : getRoles(policy)) {
                    if (!role.get("id").equals(removedRole.getId())) {
                        Map updated = new HashMap();
                        updated.put("id", role.get("id"));
                        Object required = role.get("required");
                        if (required != null) {
                            updated.put("required", required);
                        }
                        roles.add(updated);
                    }
                }

                try {
                    if (roles.isEmpty()) {
                        policyStore.delete(policy.getId());
                    } else {
                        policy.putConfig("roles", JsonSerialization.writeValueAsString(roles));
                    }
                } catch (IOException e) {
                    throw new RuntimeException("Error while synchronizing roles with policy [" + policy.getName() + "].", e);
                }
            });
        }
    }

    @Override
    public void close() {

    }

    @Override
    public String getId() {
        return "role";
    }

    private Map<String, Object>[] getRoles(Policy policy) {
        String roles = policy.getConfig().get("roles");

        if (roles != null) {
            try {
                return JsonSerialization.readValue(roles.getBytes(), Map[].class);
            } catch (IOException e) {
                throw new RuntimeException("Could not parse roles [" + roles + "] from policy config [" + policy.getName() + ".", e);
            }
        }

        return new Map[] {};
    }
}

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
package org.keycloak.authorization.policy.provider.clientscope;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import org.keycloak.Config.Scope;
import org.keycloak.authorization.AuthorizationProvider;
import org.keycloak.authorization.model.Policy;
import org.keycloak.authorization.policy.provider.PolicyProvider;
import org.keycloak.authorization.policy.provider.PolicyProviderFactory;
import org.keycloak.authorization.store.PolicyStore;
import org.keycloak.authorization.store.StoreFactory;
import org.keycloak.models.ClientScopeModel;
import org.keycloak.models.ClientScopeModel.ClientScopeRemovedEvent;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.RealmModel;
import org.keycloak.representations.idm.authorization.ClientScopePolicyRepresentation;
import org.keycloak.representations.idm.authorization.ClientScopePolicyRepresentation.ClientScopeDefinition;
import org.keycloak.representations.idm.authorization.PolicyRepresentation;
import org.keycloak.util.JsonSerialization;

/**
 * @author <a href="mailto:yoshiyuki.tabata.jy@hitachi.com">Yoshiyuki Tabata</a>
 */
public class ClientScopePolicyProviderFactory implements PolicyProviderFactory<ClientScopePolicyRepresentation> {

    private ClientScopePolicyProvider provider = new ClientScopePolicyProvider(this::toRepresentation);

    @Override
    public PolicyProvider create(KeycloakSession session) {
        return provider;
    }

    @Override
    public void init(Scope config) {
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
        factory.register(event -> {
            if (event instanceof ClientScopeRemovedEvent) {
                KeycloakSession keycloakSession = ((ClientScopeRemovedEvent) event).getKeycloakSession();
                AuthorizationProvider provider = keycloakSession.getProvider(AuthorizationProvider.class);
                StoreFactory storeFactory = provider.getStoreFactory();
                PolicyStore policyStore = storeFactory.getPolicyStore();
                ClientScopeModel removedClientScope = ((ClientScopeRemovedEvent) event).getClientScope();

                Map<Policy.FilterOption, String[]> filters = new HashMap<>();

                filters.put(Policy.FilterOption.TYPE, new String[] { getId() });

                policyStore.find(null, filters, null, null).forEach(new Consumer<Policy>() {

                    @Override
                    public void accept(Policy policy) {
                        List<Map<String, Object>> clientScopes = new ArrayList<>();

                        for (Map<String, Object> clientScope : getClientScopes(policy)) {
                            if (!clientScope.get("id").equals(removedClientScope.getId())) {
                                Map<String, Object> updated = new HashMap<>();
                                updated.put("id", clientScope.get("id"));
                                Object required = clientScope.get("required");
                                if (required != null) {
                                    updated.put("required", required);
                                }
                                clientScopes.add(updated);
                            }
                        }

                        if (clientScopes.isEmpty()) {
                            policyStore.delete(policy.getId());
                        } else {
                            try {
                                policy.putConfig("clientScopes", JsonSerialization.writeValueAsString(clientScopes));
                            } catch (IOException e) {
                                throw new RuntimeException(
                                    "Error while synchronizing client scopes with policy [" + policy.getName() + "].", e);
                            }
                        }
                    }
                });
            }
        });
    }

    private Map<String, Object>[] getClientScopes(Policy policy) {
        String clientScopes = policy.getConfig().get("clientScopes");

        if (clientScopes != null) {
            try {
                return JsonSerialization.readValue(clientScopes.getBytes(), Map[].class);
            } catch (IOException e) {
                throw new RuntimeException(
                    "Could not parse client scopes [" + clientScopes + "] from policy config [" + policy.getName() + "].", e);
            }
        }
        return new Map[] {};
    }

    @Override
    public void close() {
    }

    @Override
    public String getId() {
        return "client-scope";
    }

    @Override
    public String getName() {
        return "Client Scope";
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
    public ClientScopePolicyRepresentation toRepresentation(Policy policy, AuthorizationProvider authorization) {
        ClientScopePolicyRepresentation representation = new ClientScopePolicyRepresentation();

        try {
            String clientScopes = policy.getConfig().get("clientScopes");

            if (clientScopes == null) {
                representation.setClientScopes(Collections.emptySet());
            } else {
                representation
                        .setClientScopes(new HashSet<>(Arrays.asList(JsonSerialization.readValue(clientScopes,
                                ClientScopePolicyRepresentation.ClientScopeDefinition[].class))));
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to deserialize client scopes", e);
        }

        return representation;
    }

    @Override
    public Class<ClientScopePolicyRepresentation> getRepresentationType() {
        return ClientScopePolicyRepresentation.class;
    }

    @Override
    public void onCreate(Policy policy, ClientScopePolicyRepresentation representation, AuthorizationProvider authorization) {
        updateClientScopes(policy, representation, authorization);
    }

    @Override
    public void onUpdate(Policy policy, ClientScopePolicyRepresentation representation, AuthorizationProvider authorization) {
        updateClientScopes(policy, representation, authorization);
    }

    @Override
    public void onImport(Policy policy, PolicyRepresentation representation, AuthorizationProvider authorization) {
        try {
            updateClientScopes(policy, authorization,
                new HashSet<>(Arrays.asList(JsonSerialization.readValue(representation.getConfig().get("clientScopes"),
                    ClientScopePolicyRepresentation.ClientScopeDefinition[].class))));
        } catch (IOException e) {
            throw new RuntimeException("Failed to deserialize client scopes during import", e);
        }
    }

    @Override
    public void onExport(Policy policy, PolicyRepresentation representation, AuthorizationProvider authorizationProvider) {
        Map<String, String> config = new HashMap<>();
        Set<ClientScopePolicyRepresentation.ClientScopeDefinition> clientScopes = toRepresentation(policy,
            authorizationProvider).getClientScopes();

        for (ClientScopePolicyRepresentation.ClientScopeDefinition clientScopeDefinition : clientScopes) {
            ClientScopeModel clientScope = authorizationProvider.getRealm().getClientScopeById(clientScopeDefinition.getId());

            clientScopeDefinition.setId(clientScope.getName());
        }

        try {
            config.put("clientScopes", JsonSerialization.writeValueAsString(clientScopes));
        } catch (IOException e) {
            throw new RuntimeException("Failed to export client scope policy [" + policy.getName() + "]", e);
        }

        representation.setConfig(config);
    }

    private void updateClientScopes(Policy policy, ClientScopePolicyRepresentation representation,
        AuthorizationProvider authorization) {
        updateClientScopes(policy, authorization, representation.getClientScopes());
    }

    private void updateClientScopes(Policy policy, AuthorizationProvider authorization,
        Set<ClientScopeDefinition> clientScopes) {
        RealmModel realm = authorization.getRealm();
        Set<ClientScopePolicyRepresentation.ClientScopeDefinition> updatedClientScopes = new HashSet<>();

        if (clientScopes != null) {
            for (ClientScopePolicyRepresentation.ClientScopeDefinition definition : clientScopes) {
                String clientScopeName = definition.getId();
                ClientScopeModel clientScope = realm.getClientScopesStream()
                    .filter(scope -> scope.getName().equals(clientScopeName)).findAny().orElse(null);

                if (clientScope == null) {
                    clientScope = realm.getClientScopeById(clientScopeName);
                }

                if (clientScope == null) {
                    throw new RuntimeException(
                        "Error while updating policy [" + policy.getName() + "]. Client Scope [" + "] could not be found.");
                }

                definition.setId(clientScope.getId());
                updatedClientScopes.add(definition);
            }
        }

        try {
            policy.putConfig("clientScopes", JsonSerialization.writeValueAsString(updatedClientScopes));
        } catch (IOException e) {
            throw new RuntimeException("Failed to serialize client scopes", e);
        }
    }
}

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

package org.keycloak.authorization.policy.provider.client;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.keycloak.Config;
import org.keycloak.authorization.AuthorizationProvider;
import org.keycloak.authorization.model.Policy;
import org.keycloak.authorization.policy.provider.PolicyProvider;
import org.keycloak.authorization.policy.provider.PolicyProviderFactory;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.RealmModel;
import org.keycloak.representations.idm.authorization.ClientPolicyRepresentation;
import org.keycloak.representations.idm.authorization.PolicyRepresentation;
import org.keycloak.util.JsonSerialization;

public class ClientPolicyProviderFactory implements PolicyProviderFactory<ClientPolicyRepresentation> {

    private static final String UNRESOLVED_CLIENTS = "unresolvedClients";

    private ClientPolicyProvider provider = new ClientPolicyProvider(this::toRepresentation);

    @Override
    public String getName() {
        return "Client";
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
    public ClientPolicyRepresentation toRepresentation(Policy policy, AuthorizationProvider authorization) {
        ClientPolicyRepresentation representation = new ClientPolicyRepresentation();
        representation.setClients(getClients(policy));
        return representation;
    }

    @Override
    public Class<ClientPolicyRepresentation> getRepresentationType() {
        return ClientPolicyRepresentation.class;
    }

    @Override
    public void onCreate(Policy policy, ClientPolicyRepresentation representation, AuthorizationProvider authorization) {
        updateClients(policy, representation.getClients(), authorization, Set.of(), Set.of());
    }

    @Override
    public void onUpdate(Policy policy, ClientPolicyRepresentation representation, AuthorizationProvider authorization) {
        updateClients(policy, representation.getClients(), authorization, getClients(policy), Set.of());
    }

    public void updateFromUma(Policy policy, Set<String> clients, AuthorizationProvider authorization) {
        RealmModel realm = authorization.getRealm();
        Set<String> unresolvedIds = getClients(policy).stream()
                .filter(id -> clients.contains(id) && realm.getClientById(id) == null)
                .collect(Collectors.toSet());
        updateClients(policy, clients, authorization, unresolvedIds, Set.of());
    }

    @Override
    public void onImport(Policy policy, PolicyRepresentation representation, AuthorizationProvider authorization) {
        onImport(policy, representation, authorization, Map.of());
    }

    @Override
    public void onImport(Policy policy, PolicyRepresentation representation, AuthorizationProvider authorization, Map<String, String> previousConfig) {
        String policyName = policy.getName();
        updateClients(policy, getClients(policy), authorization, readClients(previousConfig.get("clients"), policyName),
                readClients(representation.getConfig().get(UNRESOLVED_CLIENTS), policyName));
        policy.removeConfig(UNRESOLVED_CLIENTS);
    }

    @Override
    public void onExport(Policy policy, PolicyRepresentation representation, AuthorizationProvider authorization) {
        Map<String, String> config = new HashMap<>();
        Set<String> resolved = new HashSet<>();
        Set<String> unresolved = new HashSet<>();
        RealmModel realm = authorization.getRealm();
        for (String id : getClients(policy)) {
            ClientModel client = realm.getClientById(id);
            if (client == null) {
                unresolved.add(id);
            } else {
                resolved.add(client.getClientId());
            }
        }

        try {
            config.put("clients", JsonSerialization.writeValueAsString(resolved));
            if (!unresolved.isEmpty()) {
                config.put(UNRESOLVED_CLIENTS, JsonSerialization.writeValueAsString(unresolved));
            }
        } catch (IOException cause) {
            throw new RuntimeException("Failed to export client policy [" + policy.getName() + "]", cause);
        }

        representation.setConfig(config);
    }

    @Override
    public PolicyProvider create(KeycloakSession session) {
        return provider;
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
        return "client";
    }

    private void updateClients(Policy policy, Set<String> clients, AuthorizationProvider authorization,
            Set<String> existingClients, Set<String> unresolvedClients) {
        RealmModel realm = authorization.getRealm();

        if (clients == null) {
            throw new RuntimeException("No client provided.");
        }

        Set<String> updatedClients = new HashSet<>(unresolvedClients);

        for (String id : clients) {
            if (existingClients.contains(id)) {
                updatedClients.add(id);
                continue;
            }
            ClientModel client = realm.getClientByClientId(id);

            if (client == null) {
                client = realm.getClientById(id);
            }

            if (client == null) {
                throw new RuntimeException("Error while updating policy [" + policy.getName()  + "]. Client [" + id + "] could not be found.");
            }

            updatedClients.add(client.getId());
        }

        try {
            policy.putConfig("clients", JsonSerialization.writeValueAsString(updatedClients));
        } catch (IOException cause) {
            throw new RuntimeException("Failed to serialize clients", cause);
        }
    }

    private Set<String> getClients(Policy policy) {
        return readClients(policy.getConfig().get("clients"), policy.getName());
    }

    private Set<String> readClients(String clients, String policyName) {

        if (clients != null) {
            try {
                return JsonSerialization.readValue(clients, Set.class);
            } catch (IOException e) {
                throw new RuntimeException("Could not parse clients [" + clients + "] from policy config [" + policyName + "].", e);
            }
        }

        return Collections.emptySet();
    }
}

package org.keycloak.authorization.policy.provider.client;

import org.keycloak.Config;
import org.keycloak.authorization.AuthorizationProvider;
import org.keycloak.authorization.model.Policy;
import org.keycloak.authorization.model.ResourceServer;
import org.keycloak.authorization.policy.provider.PolicyProvider;
import org.keycloak.authorization.policy.provider.PolicyProviderAdminService;
import org.keycloak.authorization.policy.provider.PolicyProviderFactory;
import org.keycloak.authorization.store.PolicyStore;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.RealmModel.ClientRemovedEvent;
import org.keycloak.util.JsonSerialization;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ClientPolicyProviderFactory implements PolicyProviderFactory {

    @Override
    public String getName() {
        return "Client";
    }

    @Override
    public String getGroup() {
        return "Identity Based";
    }

    @Override
    public PolicyProvider create(Policy policy, AuthorizationProvider authorization) {
        return new ClientPolicyProvider(policy, authorization);
    }

    @Override
    public PolicyProviderAdminService getAdminResource(ResourceServer resourceServer) {
        return null;
    }

    @Override
    public PolicyProvider create(KeycloakSession session) {
        return null;
    }

    @Override
    public void init(Config.Scope config) {

    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
        factory.register(event -> {
            if (event instanceof ClientRemovedEvent) {
                KeycloakSession keycloakSession = ((ClientRemovedEvent) event).getKeycloakSession();
                AuthorizationProvider provider = keycloakSession.getProvider(AuthorizationProvider.class);
                PolicyStore policyStore = provider.getStoreFactory().getPolicyStore();
                ClientModel removedClient = ((ClientRemovedEvent) event).getClient();

                policyStore.findByType(getId()).forEach(policy -> {
                    List<String> clients = new ArrayList<>();

                    for (String clientId : getClients(policy)) {
                        if (!clientId.equals(removedClient.getId())) {
                            clients.add(clientId);
                        }
                    }

                    try {
                        if (clients.isEmpty()) {
                            policyStore.findDependentPolicies(policy.getId()).forEach(dependentPolicy -> {
                                dependentPolicy.removeAssociatedPolicy(policy);
                            });
                            policyStore.delete(policy.getId());
                        } else {
                            policy.getConfig().put("clients", JsonSerialization.writeValueAsString(clients));
                        }
                    } catch (IOException e) {
                        throw new RuntimeException("Error while synchronizing clients with policy [" + policy.getName() + "].", e);
                    }
                });
            }
        });
    }

    @Override
    public void close() {

    }

    @Override
    public String getId() {
        return "client";
    }

    static String[] getClients(Policy policy) {
        String clients = policy.getConfig().get("clients");

        if (clients != null) {
            try {
                return JsonSerialization.readValue(clients.getBytes(), String[].class);
            } catch (IOException e) {
                throw new RuntimeException("Could not parse clients [" + clients + "] from policy config [" + policy.getName() + "].", e);
            }
        }

        return new String[]{};
    }
}

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

package org.keycloak.authorization.store.syncronization;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.keycloak.authorization.AuthorizationProvider;
import org.keycloak.authorization.model.Policy;
import org.keycloak.authorization.model.ResourceServer;
import org.keycloak.authorization.policy.provider.PolicyProviderFactory;
import org.keycloak.authorization.store.ResourceServerStore;
import org.keycloak.authorization.store.StoreFactory;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.ClientModel.ClientRemovedEvent;
import org.keycloak.provider.ProviderFactory;
import org.keycloak.representations.idm.authorization.ClientPolicyRepresentation;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public class ClientApplicationSynchronizer implements Synchronizer<ClientRemovedEvent> {

    @Override
    public void synchronize(ClientRemovedEvent event, KeycloakSessionFactory factory) {
        ProviderFactory<AuthorizationProvider> providerFactory = factory.getProviderFactory(AuthorizationProvider.class);
        AuthorizationProvider authorizationProvider = providerFactory.create(event.getKeycloakSession());

        removeFromClientPolicies(event, authorizationProvider);
    }

    private void removeFromClientPolicies(ClientRemovedEvent event, AuthorizationProvider authorizationProvider) {
        StoreFactory storeFactory = authorizationProvider.getStoreFactory();
        ResourceServerStore store = storeFactory.getResourceServerStore();
        ResourceServer resourceServer = store.findById(event.getClient().getId());

        if (resourceServer != null) {
            storeFactory.getResourceServerStore().delete(resourceServer.getId());
        }

        Map<Policy.FilterOption, String[]> attributes = new EnumMap<>(Policy.FilterOption.class);

        attributes.put(Policy.FilterOption.TYPE, new String[] {"client"});
        attributes.put(Policy.FilterOption.CONFIG, new String[] {"clients", event.getClient().getId()});

        List<Policy> search = storeFactory.getPolicyStore().findByResourceServer(attributes, null, -1, -1);

        for (Policy policy : search) {
            PolicyProviderFactory policyFactory = authorizationProvider.getProviderFactory(policy.getType());
            ClientPolicyRepresentation representation = ClientPolicyRepresentation.class.cast(policyFactory.toRepresentation(policy, authorizationProvider));
            Set<String> clients = representation.getClients();

            clients.remove(event.getClient().getId());

            if (clients.isEmpty()) {
                policyFactory.onRemove(policy, authorizationProvider);
                authorizationProvider.getStoreFactory().getPolicyStore().delete(policy.getId());
            } else {
                policyFactory.onUpdate(policy, representation, authorizationProvider);
            }
        }
    }
}

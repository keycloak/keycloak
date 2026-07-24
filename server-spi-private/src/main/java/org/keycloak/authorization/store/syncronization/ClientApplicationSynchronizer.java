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

package org.keycloak.authorization.store.syncronization;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.keycloak.authorization.AuthorizationProvider;
import org.keycloak.authorization.fgap.AdminPermissionsSchema;
import org.keycloak.authorization.model.Policy;
import org.keycloak.authorization.model.ResourceServer;
import org.keycloak.authorization.policy.provider.PolicyProviderFactory;
import org.keycloak.authorization.store.ResourceServerStore;
import org.keycloak.authorization.store.StoreFactory;
import org.keycloak.models.ClientModel.ClientRemovedEvent;
import org.keycloak.models.KeycloakSessionFactory;
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

        AdminPermissionsSchema.SCHEMA.removeResourceObject(authorizationProvider, event);

        removeFromClientPolicies(event, authorizationProvider);
    }

    private void removeFromClientPolicies(ClientRemovedEvent event, AuthorizationProvider authorizationProvider) {
        StoreFactory storeFactory = authorizationProvider.getStoreFactory();
        ResourceServerStore store = storeFactory.getResourceServerStore();
        ResourceServer resourceServer = store.findByClient(event.getClient());

        if (resourceServer != null) {
            storeFactory.getResourceServerStore().delete(event.getClient());
        }

        Map<Policy.FilterOption, String[]> attributes = new EnumMap<>(Policy.FilterOption.class);

        attributes.put(Policy.FilterOption.TYPE, new String[] {"client"});
        attributes.put(Policy.FilterOption.CONFIG, new String[] {"clients", event.getClient().getId()});
        attributes.put(Policy.FilterOption.ANY_OWNER, Policy.FilterOption.EMPTY_FILTER);

        List<Policy> search = storeFactory.getPolicyStore().find(null, attributes, null, null);

        for (Policy policy : search) {
            PolicyProviderFactory policyFactory = authorizationProvider.getProviderFactory(policy.getType());
            ClientPolicyRepresentation representation = ClientPolicyRepresentation.class.cast(policyFactory.toRepresentation(policy, authorizationProvider));
            Set<String> clients = representation.getClients();

            clients.remove(event.getClient().getId());

            policyFactory.onUpdate(policy, representation, authorizationProvider);
        }
    }
}

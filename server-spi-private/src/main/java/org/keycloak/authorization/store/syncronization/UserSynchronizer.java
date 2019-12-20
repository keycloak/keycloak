/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.keycloak.authorization.AuthorizationProvider;
import org.keycloak.authorization.model.PermissionTicket;
import org.keycloak.authorization.model.Policy;
import org.keycloak.authorization.model.ResourceServer;
import org.keycloak.authorization.policy.provider.PolicyProviderFactory;
import org.keycloak.authorization.store.PermissionTicketStore;
import org.keycloak.authorization.store.PolicyStore;
import org.keycloak.authorization.store.ResourceServerStore;
import org.keycloak.authorization.store.ResourceStore;
import org.keycloak.authorization.store.StoreFactory;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserModel.UserRemovedEvent;
import org.keycloak.provider.ProviderFactory;
import org.keycloak.representations.idm.authorization.UserPolicyRepresentation;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public class UserSynchronizer implements Synchronizer<UserRemovedEvent> {

    @Override
    public void synchronize(UserRemovedEvent event, KeycloakSessionFactory factory) {
        ProviderFactory<AuthorizationProvider> providerFactory = factory.getProviderFactory(AuthorizationProvider.class);
        AuthorizationProvider authorizationProvider = providerFactory.create(event.getKeycloakSession());

        removeFromUserPermissionTickets(event, authorizationProvider);
        removeUserResources(event, authorizationProvider);
        removeFromUserPolicies(event, authorizationProvider);
    }

    private void removeFromUserPolicies(UserRemovedEvent event, AuthorizationProvider authorizationProvider) {
        StoreFactory storeFactory = authorizationProvider.getStoreFactory();
        PolicyStore policyStore = storeFactory.getPolicyStore();
        UserModel userModel = event.getUser();
        Map<String, String[]> attributes = new HashMap<>();

        attributes.put("type", new String[] {"user"});
        attributes.put("config:users", new String[] {userModel.getId()});

        List<Policy> search = policyStore.findByResourceServer(attributes, null, -1, -1);

        for (Policy policy : search) {
            PolicyProviderFactory policyFactory = authorizationProvider.getProviderFactory(policy.getType());
            UserPolicyRepresentation representation = UserPolicyRepresentation.class.cast(policyFactory.toRepresentation(policy, authorizationProvider));
            Set<String> users = representation.getUsers();

            users.remove(userModel.getId());

            if (users.isEmpty()) {
                policyFactory.onRemove(policy, authorizationProvider);
                policyStore.delete(policy.getId());
            } else {
                policyFactory.onUpdate(policy, representation, authorizationProvider);
            }
        }
    }

    private void removeUserResources(UserRemovedEvent event, AuthorizationProvider authorizationProvider) {
        StoreFactory storeFactory = authorizationProvider.getStoreFactory();
        PolicyStore policyStore = storeFactory.getPolicyStore();
        ResourceStore resourceStore = storeFactory.getResourceStore();
        ResourceServerStore resourceServerStore = storeFactory.getResourceServerStore();
        RealmModel realm = event.getRealm();
        UserModel userModel = event.getUser();

        realm.getClients().forEach(clientModel -> {
            ResourceServer resourceServer = resourceServerStore.findById(clientModel.getId());

            if (resourceServer != null) {
                resourceStore.findByOwner(userModel.getId(), resourceServer.getId()).forEach(resource -> {
                    String resourceId = resource.getId();
                    policyStore.findByResource(resourceId, resourceServer.getId()).forEach(policy -> {
                        if (policy.getResources().size() == 1) {
                            policyStore.delete(policy.getId());
                        } else {
                            policy.removeResource(resource);
                        }
                    });
                    resourceStore.delete(resourceId);
                });
            }
        });
    }

    private void removeFromUserPermissionTickets(UserRemovedEvent event, AuthorizationProvider authorizationProvider) {
        StoreFactory storeFactory = authorizationProvider.getStoreFactory();
        PermissionTicketStore ticketStore = storeFactory.getPermissionTicketStore();
        UserModel userModel = event.getUser();
        Map<String, String> attributes = new HashMap<>();

        attributes.put(PermissionTicket.OWNER, userModel.getId());

        for (PermissionTicket ticket : ticketStore.find(attributes, null, -1, -1)) {
            ticketStore.delete(ticket.getId());
        }

        attributes = new HashMap<>();
        
        attributes.put(PermissionTicket.REQUESTER, userModel.getId());

        for (PermissionTicket ticket : ticketStore.find(attributes, null, -1, -1)) {
            ticketStore.delete(ticket.getId());
        }
    }
}

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
import org.keycloak.authorization.model.PermissionTicket;
import org.keycloak.authorization.model.Policy;
import org.keycloak.authorization.policy.provider.PolicyProviderFactory;
import org.keycloak.authorization.store.PermissionTicketStore;
import org.keycloak.authorization.store.PolicyStore;
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
        RealmModel realm = event.getRealm();
        Map<Policy.FilterOption, String[]> attributes = new EnumMap<>(Policy.FilterOption.class);

        attributes.put(Policy.FilterOption.TYPE, new String[] {"user"});
        attributes.put(Policy.FilterOption.CONFIG, new String[] {"users", userModel.getId()});
        attributes.put(Policy.FilterOption.ANY_OWNER, new String[] {Boolean.TRUE.toString()});

        List<Policy> search = policyStore.find(realm, null, attributes, null, null);

        for (Policy policy : search) {
            PolicyProviderFactory policyFactory = authorizationProvider.getProviderFactory(policy.getType());
            UserPolicyRepresentation representation = UserPolicyRepresentation.class.cast(policyFactory.toRepresentation(policy, authorizationProvider));
            Set<String> users = representation.getUsers();

            users.remove(userModel.getId());

            if (users.isEmpty()) {
                policyFactory.onRemove(policy, authorizationProvider);
                policyStore.delete(realm, policy.getId());
            } else {
                policyFactory.onUpdate(policy, representation, authorizationProvider);
            }
        }
    }

    private void removeUserResources(UserRemovedEvent event, AuthorizationProvider authorizationProvider) {
        StoreFactory storeFactory = authorizationProvider.getStoreFactory();
        PolicyStore policyStore = storeFactory.getPolicyStore();
        ResourceStore resourceStore = storeFactory.getResourceStore();
        UserModel userModel = event.getUser();
        RealmModel realm = event.getRealm();

        resourceStore.findByOwner(realm, null, userModel.getId(), resource -> {
            String resourceId = resource.getId();
            policyStore.findByResource(resource.getResourceServer(), resource).forEach(policy -> {
                if (policy.getResources().size() == 1) {
                    policyStore.delete(realm, policy.getId());
                } else {
                    policy.removeResource(resource);
                }
            });
            resourceStore.delete(realm, resourceId);
        });
    }

    private void removeFromUserPermissionTickets(UserRemovedEvent event, AuthorizationProvider authorizationProvider) {
        StoreFactory storeFactory = authorizationProvider.getStoreFactory();
        PermissionTicketStore ticketStore = storeFactory.getPermissionTicketStore();
        UserModel userModel = event.getUser();
        RealmModel realm = event.getRealm();
        Map<PermissionTicket.FilterOption, String> attributes = new EnumMap<>(PermissionTicket.FilterOption.class);

        attributes.put(PermissionTicket.FilterOption.OWNER, userModel.getId());

        for (PermissionTicket ticket : ticketStore.find(realm, null, attributes, null, null)) {
            ticketStore.delete(realm, ticket.getId());
        }

        attributes.clear();
        
        attributes.put(PermissionTicket.FilterOption.REQUESTER, userModel.getId());

        for (PermissionTicket ticket : ticketStore.find(realm, null, attributes, null, null)) {
            ticketStore.delete(realm, ticket.getId());
        }
    }
}

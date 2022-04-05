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
package org.keycloak.authorization;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.keycloak.authorization.model.PermissionTicket;
import org.keycloak.authorization.model.Policy;
import org.keycloak.authorization.model.ResourceServer;
import org.keycloak.authorization.model.Scope;
import org.keycloak.authorization.store.PolicyStore;
import org.keycloak.authorization.store.StoreFactory;
import org.keycloak.models.RealmModel;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.representations.idm.authorization.PolicyRepresentation;
import org.keycloak.representations.idm.authorization.UserPolicyRepresentation;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public class UserManagedPermissionUtil {

    public static void updatePolicy(PermissionTicket ticket, StoreFactory storeFactory) {
        Scope scope = ticket.getScope();
        Policy policy = ticket.getPolicy();
        ResourceServer resourceServer = ticket.getResourceServer();

        if (policy == null) {
            Map<PermissionTicket.FilterOption, String> filter = new EnumMap<>(PermissionTicket.FilterOption.class);

            filter.put(PermissionTicket.FilterOption.OWNER, ticket.getOwner());
            filter.put(PermissionTicket.FilterOption.REQUESTER, ticket.getRequester());
            filter.put(PermissionTicket.FilterOption.RESOURCE_ID, ticket.getResource().getId());
            filter.put(PermissionTicket.FilterOption.POLICY_IS_NOT_NULL, Boolean.TRUE.toString());

            List<PermissionTicket> tickets = storeFactory.getPermissionTicketStore().find(resourceServer.getRealm(), resourceServer, filter, null, null);

            if (!tickets.isEmpty()) {
                policy = tickets.iterator().next().getPolicy();
            }
        }

        if (ticket.isGranted()) {
            if (policy == null) {
                policy = createUserManagedPermission(ticket, storeFactory);
            }

            if (scope != null && !policy.getScopes().contains(scope)) {
                policy.addScope(scope);
            }

            ticket.setPolicy(policy);
        } else if (scope != null) {
            policy.removeScope(scope);
            ticket.setPolicy(null);
        }
    }

    public static void removePolicy(PermissionTicket ticket, StoreFactory storeFactory) {
        Policy policy = ticket.getPolicy();
        RealmModel realm = ticket.getResourceServer().getRealm();

        if (policy != null) {
            Map<PermissionTicket.FilterOption, String> filter = new EnumMap<>(PermissionTicket.FilterOption.class);

            filter.put(PermissionTicket.FilterOption.OWNER, ticket.getOwner());
            filter.put(PermissionTicket.FilterOption.REQUESTER, ticket.getRequester());
            filter.put(PermissionTicket.FilterOption.RESOURCE_ID, ticket.getResource().getId());
            filter.put(PermissionTicket.FilterOption.GRANTED, Boolean.TRUE.toString());

            List<PermissionTicket> tickets = storeFactory.getPermissionTicketStore().find(realm, ticket.getResourceServer(), filter, null, null);

            if (tickets.isEmpty()) {
                PolicyStore policyStore = storeFactory.getPolicyStore();

                for (Policy associatedPolicy : policy.getAssociatedPolicies()) {
                    policyStore.delete(realm, associatedPolicy.getId());
                }

                policyStore.delete(realm, policy.getId());
            } else if (ticket.getScope() != null) {
                policy.removeScope(ticket.getScope());
            }
        }
    }

    private static Policy createUserManagedPermission(PermissionTicket ticket, StoreFactory storeFactory) {
        PolicyStore policyStore = storeFactory.getPolicyStore();
        UserPolicyRepresentation userPolicyRep = new UserPolicyRepresentation();

        userPolicyRep.setName(KeycloakModelUtils.generateId());
        userPolicyRep.addUser(ticket.getRequester());

        Policy userPolicy = policyStore.create(ticket.getResourceServer(), userPolicyRep);

        userPolicy.setOwner(ticket.getOwner());

        PolicyRepresentation policyRep = new PolicyRepresentation();

        policyRep.setName(KeycloakModelUtils.generateId());
        policyRep.setType("uma");
        policyRep.addPolicy(userPolicy.getId());

        Policy policy = policyStore.create(ticket.getResourceServer(), policyRep);

        policy.setOwner(ticket.getOwner());
        policy.addResource(ticket.getResource());

        Scope scope = ticket.getScope();

        if (scope != null) {
            policy.addScope(scope);
        }

        return policy;
    }

}

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
package org.keycloak.authorization.policy.evaluation;

import java.util.EnumMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.keycloak.authorization.AuthorizationProvider;
import org.keycloak.authorization.identity.Identity;
import org.keycloak.authorization.model.PermissionTicket;
import org.keycloak.authorization.model.Resource;
import org.keycloak.authorization.model.ResourceServer;
import org.keycloak.authorization.model.Scope;
import org.keycloak.authorization.store.ResourceStore;
import org.keycloak.authorization.store.ScopeStore;
import org.keycloak.authorization.store.StoreFactory;
import org.keycloak.models.RealmModel;
import org.keycloak.representations.idm.authorization.AuthorizationRequest;
import org.keycloak.representations.idm.authorization.Permission;
import org.keycloak.representations.idm.authorization.PermissionTicketToken;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public class PermissionTicketAwareDecisionResultCollector extends DecisionPermissionCollector {

    private final AuthorizationRequest request;
    private PermissionTicketToken ticket;
    private final Identity identity;
    private ResourceServer resourceServer;
    private final AuthorizationProvider authorization;

    public PermissionTicketAwareDecisionResultCollector(AuthorizationRequest request, PermissionTicketToken ticket, Identity identity, ResourceServer resourceServer, AuthorizationProvider authorization) {
        super(authorization, resourceServer, request);
        this.request = request;
        this.ticket = ticket;
        this.identity = identity;
        this.resourceServer = resourceServer;
        this.authorization = authorization;
    }

    @Override
    protected void onGrant(Permission grantedPermission) {
        // Removes permissions (represented by {@code ticket}) granted by any user-managed policy so we don't create unnecessary permission tickets.
        List<Permission> permissions = ticket.getPermissions();
        Iterator<Permission> itPermissions = permissions.iterator();

        while (itPermissions.hasNext()) {
            Permission permission = itPermissions.next();

            if (permission.getResourceId() == null || permission.getResourceId().equals(grantedPermission.getResourceId())) {
                Set<String> scopes = permission.getScopes();
                Iterator<String> itScopes = scopes.iterator();

                while (itScopes.hasNext()) {
                    if (grantedPermission.getScopes().contains(itScopes.next())) {
                        itScopes.remove();
                    }
                }

                if (scopes.isEmpty()) {
                    itPermissions.remove();
                }
            }
        }
    }

    @Override
    public void onComplete() {
        super.onComplete();

        if (request.isSubmitRequest()) {
            StoreFactory storeFactory = authorization.getStoreFactory();
            ResourceStore resourceStore = storeFactory.getResourceStore();
            List<Permission> permissions = ticket.getPermissions();
            RealmModel realm = resourceServer.getRealm();

            if (permissions != null) {
                for (Permission permission : permissions) {
                    Resource resource = resourceStore.findById(realm, resourceServer, permission.getResourceId());

                    if (resource == null) {
                        resource = resourceStore.findByName(resourceServer, permission.getResourceId(), identity.getId());
                    }

                    if (resource == null || !resource.isOwnerManagedAccess() || resource.getOwner().equals(identity.getId()) || resource.getOwner().equals(resourceServer.getClientId())) {
                        continue;
                    }

                    Set<String> scopes = permission.getScopes();

                    if (scopes.isEmpty()) {
                        scopes = resource.getScopes().stream().map(Scope::getName).collect(Collectors.toSet());
                    }

                    if (scopes.isEmpty()) {
                        Map<PermissionTicket.FilterOption, String> filters = new EnumMap<>(PermissionTicket.FilterOption.class);

                        filters.put(PermissionTicket.FilterOption.RESOURCE_ID, resource.getId());
                        filters.put(PermissionTicket.FilterOption.REQUESTER, identity.getId());
                        filters.put(PermissionTicket.FilterOption.SCOPE_IS_NULL, Boolean.TRUE.toString());

                        List<PermissionTicket> tickets = authorization.getStoreFactory().getPermissionTicketStore().find(realm, resourceServer, filters, null, null);

                        if (tickets.isEmpty()) {
                            authorization.getStoreFactory().getPermissionTicketStore().create(resourceServer, resource, null, identity.getId());
                        }
                    } else {
                        ScopeStore scopeStore = authorization.getStoreFactory().getScopeStore();

                        for (String scopeId : scopes) {
                            Scope scope = scopeStore.findByName(resourceServer, scopeId);

                            if (scope == null) {
                                scope = scopeStore.findById(realm, resourceServer, scopeId);
                            }

                            Map<PermissionTicket.FilterOption, String> filters = new EnumMap<>(PermissionTicket.FilterOption.class);

                            filters.put(PermissionTicket.FilterOption.RESOURCE_ID, resource.getId());
                            filters.put(PermissionTicket.FilterOption.REQUESTER, identity.getId());
                            filters.put(PermissionTicket.FilterOption.SCOPE_ID, scope.getId());

                            List<PermissionTicket> tickets = authorization.getStoreFactory().getPermissionTicketStore().find(realm, resourceServer, filters, null, null);

                            if (tickets.isEmpty()) {
                                authorization.getStoreFactory().getPermissionTicketStore().create(resourceServer, resource, scope, identity.getId());
                            }
                        }
                    }
                }
            }
        }
    }
}

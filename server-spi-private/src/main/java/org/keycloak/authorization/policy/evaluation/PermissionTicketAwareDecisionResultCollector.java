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
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.utils.KeycloakModelUtils;
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
            // At this point, onGrant() has already removed granted permissions from the ticket,
            // so ticket.getPermissions() contains only the denied ones that need permission tickets.
            List<Permission> permissions = ticket.getPermissions();
            if (permissions == null || permissions.isEmpty()) return;

            String resourceServerId = resourceServer.getId();
            String identityId = identity.getId();
            KeycloakSession session = authorization.getKeycloakSession();

            // Create tickets in a separate transaction so they survive the rollback
            KeycloakModelUtils.enlistAfterCompletion(session, ctx -> {
                AuthorizationProvider authz = ctx.session().getProvider(AuthorizationProvider.class);
                StoreFactory sf = authz.getStoreFactory();
                ResourceServer rs = sf.getResourceServerStore().findById(resourceServerId);
                if (rs == null) return;

                ResourceStore resourceStore = sf.getResourceStore();
                ScopeStore scopeStore = sf.getScopeStore();

                for (Permission permission : permissions) {
                    Resource resource = resourceStore.findById(rs, permission.getResourceId());
                    if (resource == null) {
                        resource = resourceStore.findByName(rs, permission.getResourceId(), identityId);
                    }
                    if (resource == null || !resource.isOwnerManagedAccess()
                            || resource.getOwner().equals(identityId)
                            || resource.getOwner().equals(rs.getClientId())) {
                        continue;
                    }

                    Set<String> scopes = permission.getScopes();
                    if (scopes.isEmpty()) {
                        scopes = resource.getScopes().stream().map(Scope::getName).collect(Collectors.toSet());
                    }

                    if (scopes.isEmpty()) {
                        createTicketIfNotExists(sf, rs, resource, null, identityId);
                    } else {
                        for (String scopeName : scopes) {
                            Scope scope = scopeStore.findByName(rs, scopeName);
                            if (scope == null) scope = scopeStore.findById(rs, scopeName);
                            if (scope != null) createTicketIfNotExists(sf, rs, resource, scope, identityId);
                        }
                    }
                }
            });
        }
    }

    private static void createTicketIfNotExists(StoreFactory sf, ResourceServer rs, Resource resource, Scope scope, String requester) {
        Map<PermissionTicket.FilterOption, String> filters = new EnumMap<>(PermissionTicket.FilterOption.class);
        filters.put(PermissionTicket.FilterOption.RESOURCE_ID, resource.getId());
        filters.put(PermissionTicket.FilterOption.REQUESTER, requester);
        if (scope != null) {
            filters.put(PermissionTicket.FilterOption.SCOPE_ID, scope.getId());
        } else {
            filters.put(PermissionTicket.FilterOption.SCOPE_IS_NULL, Boolean.TRUE.toString());
        }

        if (sf.getPermissionTicketStore().find(rs, filters, null, null).isEmpty()) {
            sf.getPermissionTicketStore().create(rs, resource, scope, requester);
        }
    }
}

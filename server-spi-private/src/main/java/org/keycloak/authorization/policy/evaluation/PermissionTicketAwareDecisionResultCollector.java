/*
 * Copyright 2017 Red Hat, Inc. and/or its affiliates
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

import java.util.ArrayList;
import java.util.HashMap;
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
import org.keycloak.authorization.permission.ResourcePermission;
import org.keycloak.authorization.policy.evaluation.Result.PolicyResult;
import org.keycloak.authorization.store.ResourceStore;
import org.keycloak.authorization.store.ScopeStore;
import org.keycloak.authorization.store.StoreFactory;
import org.keycloak.representations.idm.authorization.AuthorizationRequest;
import org.keycloak.representations.idm.authorization.PermissionTicketToken;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public class PermissionTicketAwareDecisionResultCollector extends DecisionResultCollector {

    private final AuthorizationRequest request;
    private PermissionTicketToken ticket;
    private final Identity identity;
    private ResourceServer resourceServer;
    private final AuthorizationProvider authorization;
    private List<Result> results;

    public PermissionTicketAwareDecisionResultCollector(AuthorizationRequest request, PermissionTicketToken ticket, Identity identity, ResourceServer resourceServer, AuthorizationProvider authorization) {
        this.request = request;
        this.ticket = ticket;
        this.identity = identity;
        this.resourceServer = resourceServer;
        this.authorization = authorization;
    }

    @Override
    protected void onDeny(Result result) {
        ResourcePermission permission = result.getPermission();
        Resource resource = permission.getResource();

        if (resource != null && resource.isOwnerManagedAccess()) {
            if (!resource.getOwner().equals(identity.getId())) {
                Map<String, String> filters = new HashMap<>();

                filters.put(PermissionTicket.RESOURCE, resource.getId());
                filters.put(PermissionTicket.REQUESTER, identity.getId());
                filters.put(PermissionTicket.GRANTED, Boolean.TRUE.toString());

                List<PermissionTicket> permissions = authorization.getStoreFactory().getPermissionTicketStore().find(filters, resource.getResourceServer().getId(), -1, -1);

                if (!permissions.isEmpty()) {
                    List<Scope> grantedScopes = new ArrayList<>();

                    for (PolicyResult policyResult : result.getResults()) {
                        for (PermissionTicket ticket : permissions) {
                            Scope grantedScope = ticket.getScope();

                            if ("resource".equals(policyResult.getPolicy().getType())) {
                                policyResult.setStatus(Effect.PERMIT);
                            }

                            if (grantedScope != null) {
                                grantedScopes.add(grantedScope);

                                for (Scope policyScope : policyResult.getPolicy().getScopes()) {
                                    if (policyScope.equals(grantedScope)) {
                                        policyResult.setStatus(Effect.PERMIT);
                                    }
                                }
                            }
                        }
                    }

                    permission.getScopes().clear();
                    permission.getScopes().addAll(grantedScopes);
                }
            }
        }

        super.onDeny(result);
    }

    @Override
    public void onComplete() {
        super.onComplete();

        if (request.isSubmitRequest()) {
            StoreFactory storeFactory = authorization.getStoreFactory();
            ResourceStore resourceStore = storeFactory.getResourceStore();

            if (ticket.getResources() != null) {
                for (PermissionTicketToken.ResourcePermission permission : ticket.getResources()) {
                    Resource resource = resourceStore.findById(permission.getResourceId(), resourceServer.getId());

                    if (resource == null) {
                        resource = resourceStore.findByName(permission.getResourceId(), identity.getId(), resourceServer.getId());
                    }

                    if (!resource.isOwnerManagedAccess() || resource.getOwner().equals(identity.getId()) || resource.getOwner().equals(resourceServer.getId())) {
                        continue;
                    }

                    Set<String> scopes = permission.getScopes();

                    if (scopes.isEmpty()) {
                        scopes = resource.getScopes().stream().map(Scope::getName).collect(Collectors.toSet());
                    }

                    if (scopes.isEmpty()) {
                        Map<String, String> filters = new HashMap<>();

                        filters.put(PermissionTicket.RESOURCE, resource.getId());
                        filters.put(PermissionTicket.REQUESTER, identity.getId());
                        filters.put(PermissionTicket.SCOPE_IS_NULL, Boolean.TRUE.toString());

                        List<PermissionTicket> permissions = authorization.getStoreFactory().getPermissionTicketStore().find(filters, resource.getResourceServer().getId(), -1, -1);

                        if (permissions.isEmpty()) {
                            authorization.getStoreFactory().getPermissionTicketStore().create(resource.getId(), null, identity.getId(), resource.getResourceServer());
                        }
                    } else {
                        ScopeStore scopeStore = authorization.getStoreFactory().getScopeStore();

                        for (String scopeId : scopes) {
                            Scope scope = scopeStore.findByName(scopeId, resourceServer.getId());

                            if (scope == null) {
                                scope = scopeStore.findById(scopeId, resourceServer.getId());
                            }

                            Map<String, String> filters = new HashMap<>();

                            filters.put(PermissionTicket.RESOURCE, resource.getId());
                            filters.put(PermissionTicket.REQUESTER, identity.getId());
                            filters.put(PermissionTicket.SCOPE, scope.getId());

                            List<PermissionTicket> permissions = authorization.getStoreFactory().getPermissionTicketStore().find(filters, resource.getResourceServer().getId(), -1, -1);

                            if (permissions.isEmpty()) {
                                authorization.getStoreFactory().getPermissionTicketStore().create(resource.getId(), scope.getId(), identity.getId(), resource.getResourceServer());
                            }
                        }
                    }
                }
            }
        }
    }

    @Override
    protected void onComplete(List<Result> results) {
        this.results = results;
    }

    public List<Result> results() {
        return results;
    }
}

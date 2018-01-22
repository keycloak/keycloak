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

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.keycloak.authorization.AuthorizationProvider;
import org.keycloak.authorization.identity.Identity;
import org.keycloak.authorization.model.PermissionTicket;
import org.keycloak.authorization.model.Resource;
import org.keycloak.authorization.model.ResourceServer;
import org.keycloak.authorization.model.Scope;
import org.keycloak.authorization.permission.ResourcePermission;
import org.keycloak.authorization.policy.evaluation.Result.PolicyResult;
import org.keycloak.authorization.store.ResourceStore;
import org.keycloak.authorization.store.StoreFactory;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public abstract class PermissionTicketAwareDecisionResultCollector extends DecisionResultCollector {

    private Map<String, List<String>> ticketResourceScopes;
    private final Identity identity;
    private ResourceServer resourceServer;
    private final AuthorizationProvider authorization;

    public PermissionTicketAwareDecisionResultCollector(Map<String, List<String>> ticketResourceScopes, Identity identity, ResourceServer resourceServer, AuthorizationProvider authorization) {
        this.ticketResourceScopes = ticketResourceScopes;
        this.identity = identity;
        this.resourceServer = resourceServer;
        this.authorization = authorization;
    }

    @Override
    protected void onGrant(Result result) {
        ResourcePermission permission = result.getPermission();
        Resource resource = permission.getResource();
        List<String> scopes = ticketResourceScopes.get(resource.getId());

        for (PolicyResult policyResult : result.getResults()) {
            if ("resource".equals(policyResult.getPolicy().getType())) {
                if (scopes != null && scopes.isEmpty()) {
                    ticketResourceScopes.remove(resource.getId());
                }
            } else {
                for (Scope policyScope : policyResult.getPolicy().getScopes()) {
                    scopes.remove(policyScope.getId());
                    if (scopes.isEmpty()) {
                        ticketResourceScopes.remove(resource.getId());
                    }
                }
            }
        }

        super.onGrant(result);
    }

    @Override
    protected void onDeny(Result result) {
        ResourcePermission permission = result.getPermission();
        Resource resource = permission.getResource();
        List<String> requestedScopes = ticketResourceScopes.get(resource.getId());

        if (resource != null && resource.isOwnerManagedAccess()) {
            if (!resource.getOwner().equals(identity.getId())) {
                Map<String, String> filters = new HashMap<>();

                filters.put(PermissionTicket.RESOURCE, resource.getId());
                filters.put(PermissionTicket.REQUESTER, identity.getId());

                List<PermissionTicket> permissions = authorization.getStoreFactory().getPermissionTicketStore().find(filters, resource.getResourceServer().getId(), -1, -1);

                if (permissions.isEmpty()) {
                    createPermissionTickets(resource);
                } else {
                    for (PolicyResult policyResult : result.getResults()) {
                        for (PermissionTicket ticket : permissions) {
                            if (ticket.isGranted()) {
                                if ("resource".equals(policyResult.getPolicy().getType())) {
                                    policyResult.setStatus(Effect.PERMIT);
                                    if (requestedScopes != null && requestedScopes.isEmpty()) {
                                        ticketResourceScopes.remove(resource.getId());
                                    }
                                } else if (ticket.getScope() != null) {
                                    for (Scope policyScope : policyResult.getPolicy().getScopes()) {
                                        if (policyScope.equals(ticket.getScope())) {
                                            policyResult.setStatus(Effect.PERMIT);
                                        }
                                    }
                                    if (requestedScopes != null) {
                                        requestedScopes.remove(ticket.getScope().getId());
                                        if (requestedScopes.isEmpty()) {
                                            ticketResourceScopes.remove(resource.getId());
                                        }
                                    }
                                }
                            }
                            if (requestedScopes != null && ticket.getScope() != null) {
                                requestedScopes.remove(ticket.getScope().getId());
                                if (requestedScopes.isEmpty()) {
                                    ticketResourceScopes.remove(resource.getId());
                                }
                            }
                        }
                    }
                }
            }

            for (String requestedScope : requestedScopes) {
                Iterator<Scope> iterator = permission.getScopes().iterator();
                while (iterator.hasNext()) {
                    if (iterator.next().getId().equals(requestedScope)) {
                        iterator.remove();
                    }
                }
            }
        }

        super.onDeny(result);
    }

    @Override
    public void onComplete() {
        super.onComplete();
        StoreFactory storeFactory = authorization.getStoreFactory();
        ResourceStore resourceStore = storeFactory.getResourceStore();

        for (Entry<String, List<String>> entry : ticketResourceScopes.entrySet()) {
            Resource resource = resourceStore.findById(entry.getKey(), resourceServer.getId());

            if (!resource.getOwner().equals(identity.getId()) && !resource.getOwner().equals(resourceServer.getId())) {
                createPermissionTickets(resource);
            }
        }
    }

    private void createPermissionTickets(Resource resource) {
        if (ticketResourceScopes.containsKey(resource.getId())) {
            List<String> scopeIds = ticketResourceScopes.get(resource.getId());

            if (scopeIds.isEmpty()) {
                authorization.getStoreFactory().getPermissionTicketStore().create(resource.getId(), null, identity.getId(), resource.getResourceServer());
            } else {
                for (String scopeId : scopeIds) {
                    authorization.getStoreFactory().getPermissionTicketStore().create(resource.getId(), scopeId, identity.getId(), resource.getResourceServer());
                }
            }
        }
        ticketResourceScopes.remove(resource.getId());
    }
}

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

package org.keycloak.forms.account.freemarker.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.ws.rs.core.UriInfo;

import org.keycloak.authorization.AuthorizationProvider;
import org.keycloak.authorization.model.PermissionTicket;
import org.keycloak.authorization.model.Policy;
import org.keycloak.authorization.model.Resource;
import org.keycloak.authorization.model.ResourceServer;
import org.keycloak.authorization.model.Scope;
import org.keycloak.authorization.store.PermissionTicketStore;
import org.keycloak.common.util.Time;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.ModelToRepresentation;
import org.keycloak.representations.idm.authorization.ScopeRepresentation;
import org.keycloak.services.util.ResolveRelative;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class AuthorizationBean {

    private final KeycloakSession session;
    private final RealmModel realm;
    private final UserModel user;
    private final AuthorizationProvider authorization;
    private final UriInfo uriInfo;
    private ResourceBean resource;
    private List<ResourceBean> resources;
    private Collection<ResourceBean> userSharedResources;
    private Collection<ResourceBean> requestsWaitingPermission;
    private Collection<ResourceBean> resourcesWaitingOthersApproval;

    public AuthorizationBean(KeycloakSession session, RealmModel realm, UserModel user, UriInfo uriInfo) {
        this.session = session;
        this.realm = realm;
        this.user = user;
        this.uriInfo = uriInfo;
        authorization = session.getProvider(AuthorizationProvider.class);
        List<String> pathParameters = uriInfo.getPathParameters().get("resource_id");

        if (pathParameters != null && !pathParameters.isEmpty()) {
            Resource resource = authorization.getStoreFactory().getResourceStore().findById(realm, null, pathParameters.get(0));

            if (resource != null && !resource.getOwner().equals(user.getId())) {
                throw new RuntimeException("User [" + user.getUsername() + "] can not access resource [" + resource.getId() + "]");
            }
        }
    }

    public Collection<ResourceBean> getResourcesWaitingOthersApproval() {
        if (resourcesWaitingOthersApproval == null) {
            Map<PermissionTicket.FilterOption, String> filters = new EnumMap<>(PermissionTicket.FilterOption.class);

            filters.put(PermissionTicket.FilterOption.REQUESTER, user.getId());
            filters.put(PermissionTicket.FilterOption.GRANTED, Boolean.FALSE.toString());

            resourcesWaitingOthersApproval = toResourceRepresentation(findPermissions(filters));
        }

        return resourcesWaitingOthersApproval;
    }

    public Collection<ResourceBean> getResourcesWaitingApproval() {
        if (requestsWaitingPermission == null) {
            Map<PermissionTicket.FilterOption, String> filters = new EnumMap<>(PermissionTicket.FilterOption.class);

            filters.put(PermissionTicket.FilterOption.OWNER, user.getId());
            filters.put(PermissionTicket.FilterOption.GRANTED, Boolean.FALSE.toString());

            requestsWaitingPermission = toResourceRepresentation(findPermissions(filters));
        }

        return requestsWaitingPermission;
    }

    public List<ResourceBean> getResources() {
        if (resources == null) {
            resources = authorization.getStoreFactory().getResourceStore().findByOwner(realm, null, user.getId()).stream()
                    .filter(Resource::isOwnerManagedAccess)
                    .map(ResourceBean::new)
                    .collect(Collectors.toList());
        }
        return resources;
    }

    public Collection<ResourceBean> getSharedResources() {
        if (userSharedResources == null) {
            Map<PermissionTicket.FilterOption, String> filters = new EnumMap<>(PermissionTicket.FilterOption.class);

            filters.put(PermissionTicket.FilterOption.REQUESTER, user.getId());
            filters.put(PermissionTicket.FilterOption.GRANTED, Boolean.TRUE.toString());

            PermissionTicketStore ticketStore = authorization.getStoreFactory().getPermissionTicketStore();

            userSharedResources = toResourceRepresentation(ticketStore.find(realm,null, filters, null, null));
        }
        return userSharedResources;
    }

    public ResourceBean getResource() {
        if (resource == null) {
            String resourceId = uriInfo.getPathParameters().getFirst("resource_id");

            if (resourceId != null) {
                resource = getResource(resourceId);
            }
        }

        return resource;
    }

    private ResourceBean getResource(String id) {
        return new ResourceBean(authorization.getStoreFactory().getResourceStore().findById(realm, null, id));
    }

    public static class RequesterBean {

        private final Long createdTimestamp;
        private final Long grantedTimestamp;
        private UserModel requester;
        private List<PermissionScopeBean> scopes = new ArrayList<>();
        private boolean granted;

        public RequesterBean(PermissionTicket ticket, AuthorizationProvider authorization) {
            this.requester = authorization.getKeycloakSession().users().getUserById(authorization.getRealm(), ticket.getRequester());
            granted = ticket.isGranted();
            createdTimestamp = ticket.getCreatedTimestamp();
            grantedTimestamp = ticket.getGrantedTimestamp();
        }

        public UserModel getRequester() {
            return requester;
        }

        public List<PermissionScopeBean> getScopes() {
            return scopes;
        }

        private void addScope(PermissionTicket ticket) {
            if (ticket != null) {
                scopes.add(new PermissionScopeBean(ticket));
            }
        }

        public boolean isGranted() {
            return (granted && scopes.isEmpty()) || scopes.stream().filter(permissionScopeBean -> permissionScopeBean.isGranted()).count() > 0;
        }

        public Date getCreatedDate() {
            return Time.toDate(createdTimestamp);
        }

        public Date getGrantedDate() {
            if (grantedTimestamp == null) {
                PermissionScopeBean permission = scopes.stream().filter(permissionScopeBean -> permissionScopeBean.isGranted()).findFirst().orElse(null);

                if (permission == null) {
                    return null;
                }

                return permission.getGrantedDate();
            }
            return Time.toDate(grantedTimestamp);
        }
    }

    public static class PermissionScopeBean {

        private final Scope scope;
        private final PermissionTicket ticket;

        public PermissionScopeBean(PermissionTicket ticket) {
            this.ticket = ticket;
            scope = ticket.getScope();
        }

        public String getId() {
            return ticket.getId();
        }

        public Scope getScope() {
            return scope;
        }

        public boolean isGranted() {
            return ticket.isGranted();
        }

        private Date getGrantedDate() {
            if (isGranted()) {
                return Time.toDate(ticket.getGrantedTimestamp());
            }
            return null;
        }
    }

    public class ResourceBean {

        private final ResourceServerBean resourceServer;
        private final String ownerName;
        private final UserModel userOwner;
        private ClientModel clientOwner;
        private Resource resource;
        private Map<String, RequesterBean> permissions = new HashMap<>();
        private Collection<RequesterBean> shares;

        public ResourceBean(Resource resource) {
            RealmModel realm = authorization.getRealm();
            ResourceServer resourceServerModel = resource.getResourceServer();
            resourceServer = new ResourceServerBean(realm.getClientById(resourceServerModel.getClientId()), resourceServerModel);
            this.resource = resource;
            userOwner = authorization.getKeycloakSession().users().getUserById(realm, resource.getOwner());
            if (userOwner == null) {
                clientOwner = realm.getClientById(resource.getOwner());
                ownerName = clientOwner.getClientId();
            } else if (userOwner.getEmail() != null) {
                ownerName = userOwner.getEmail();
            } else {
                ownerName = userOwner.getUsername();
            }
        }

        public String getId() {
            return resource.getId();
        }

        public String getName() {
            return resource.getName();
        }

        public String getDisplayName() {
            return resource.getDisplayName();
        }

        public String getIconUri() {
            return resource.getIconUri();
        }

        public String getOwnerName() {
            return ownerName;
        }

        public UserModel getUserOwner() {
            return userOwner;
        }

        public ClientModel getClientOwner() {
            return clientOwner;
        }

        public List<ScopeRepresentation> getScopes() {
            return resource.getScopes().stream().map(ModelToRepresentation::toRepresentation).collect(Collectors.toList());
        }

        public Collection<RequesterBean> getShares() {
            if (shares == null) {
                Map<PermissionTicket.FilterOption, String> filters = new EnumMap<>(PermissionTicket.FilterOption.class);

                filters.put(PermissionTicket.FilterOption.RESOURCE_ID, this.resource.getId());
                filters.put(PermissionTicket.FilterOption.GRANTED, Boolean.TRUE.toString());

                shares = toPermissionRepresentation(findPermissions(filters));
            }

            return shares;
        }

        public Collection<ManagedPermissionBean> getPolicies() {
            ResourceServer resourceServer = getResourceServer().getResourceServerModel();
            RealmModel realm = resourceServer.getRealm();
            Map<Policy.FilterOption, String[]> filters = new EnumMap<>(Policy.FilterOption.class);

            filters.put(Policy.FilterOption.TYPE, new String[] {"uma"});
            filters.put(Policy.FilterOption.RESOURCE_ID, new String[] {this.resource.getId()});
            if (getUserOwner() != null) {
                filters.put(Policy.FilterOption.OWNER, new String[] {getUserOwner().getId()});
            } else {
                filters.put(Policy.FilterOption.OWNER, new String[] {getClientOwner().getId()});
            }

            List<Policy> policies = authorization.getStoreFactory().getPolicyStore().find(realm, resourceServer, filters, null, null);

            if (policies.isEmpty()) {
                return Collections.emptyList();
            }

            return policies.stream()
                    .filter(policy -> {
                        Map<PermissionTicket.FilterOption, String> filters1 = new EnumMap<>(PermissionTicket.FilterOption.class);

                        filters1.put(PermissionTicket.FilterOption.POLICY_ID, policy.getId());

                        return authorization.getStoreFactory().getPermissionTicketStore().find(realm, resourceServer, filters1, -1, 1)
                                .isEmpty();
                    })
                    .map(ManagedPermissionBean::new).collect(Collectors.toList());
        }

        public ResourceServerBean getResourceServer() {
            return resourceServer;
        }

        public Collection<RequesterBean> getPermissions() {
            return permissions.values();
        }

        private void addPermission(PermissionTicket ticket, AuthorizationProvider authorization) {
            permissions.computeIfAbsent(ticket.getRequester(), key -> new RequesterBean(ticket, authorization)).addScope(ticket);
        }
    }

    private Collection<RequesterBean> toPermissionRepresentation(List<PermissionTicket> permissionRequests) {
        Map<String, RequesterBean> requests = new HashMap<>();

        for (PermissionTicket ticket : permissionRequests) {
            Resource resource = ticket.getResource();

            if (!resource.isOwnerManagedAccess()) {
                continue;
            }

            requests.computeIfAbsent(ticket.getRequester(), resourceId -> new RequesterBean(ticket, authorization)).addScope(ticket);
        }

        return requests.values();
    }

    private Collection<ResourceBean> toResourceRepresentation(List<PermissionTicket> tickets) {
        Map<String, ResourceBean> requests = new HashMap<>();

        for (PermissionTicket ticket : tickets) {
            Resource resource = ticket.getResource();

            if (!resource.isOwnerManagedAccess()) {
                continue;
            }

            requests.computeIfAbsent(resource.getId(), resourceId -> getResource(resourceId)).addPermission(ticket, authorization);
        }

        return requests.values();
    }

    private List<PermissionTicket> findPermissions(Map<PermissionTicket.FilterOption, String> filters) {
        return authorization.getStoreFactory().getPermissionTicketStore().find(realm, null, filters, null, null);
    }

    public class ResourceServerBean {

        private ClientModel clientModel;
        private ResourceServer resourceServer;

        public ResourceServerBean(ClientModel clientModel, ResourceServer resourceServer) {
            this.clientModel = clientModel;
            this.resourceServer = resourceServer;
        }

        public String getId() {
            return resourceServer.getId();
        }

        public String getName() {
            String name = clientModel.getName();

            if (name != null) {
                return name;
            }

            return clientModel.getClientId();
        }

        public String getClientId() {
            return clientModel.getClientId();
        }

        public String getRedirectUri() {
            Set<String> redirectUris = clientModel.getRedirectUris();

            if (redirectUris.isEmpty()) {
                return null;
            }

            return redirectUris.iterator().next();
        }

        public String getBaseUri() {
            return ResolveRelative.resolveRelativeUri(session, clientModel.getRootUrl(), clientModel.getBaseUrl());
        }

        public ResourceServer getResourceServerModel() {
            return resourceServer;
        }
    }

    public class ManagedPermissionBean {

        private final Policy policy;
        private List<ManagedPermissionBean> policies;

        public ManagedPermissionBean(Policy policy) {
            this.policy = policy;
        }

        public String getId() {
            return policy.getId();
        }

        public Collection<ScopeRepresentation> getScopes() {
            return policy.getScopes().stream().map(ModelToRepresentation::toRepresentation).collect(Collectors.toList());
        }

        public String getDescription() {
            return this.policy.getDescription();
        }

        public Collection<ManagedPermissionBean> getPolicies() {
            if (this.policies == null) {
                this.policies = policy.getAssociatedPolicies().stream().map(ManagedPermissionBean::new).collect(Collectors.toList());
            }

            return this.policies;
        }
    }
}

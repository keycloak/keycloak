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

package org.keycloak.forms.account.freemarker.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.ws.rs.core.UriInfo;

import org.keycloak.authorization.AuthorizationProvider;
import org.keycloak.authorization.model.PermissionTicket;
import org.keycloak.authorization.model.Resource;
import org.keycloak.authorization.model.Scope;
import org.keycloak.authorization.store.PermissionTicketStore;
import org.keycloak.common.util.Time;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.ModelToRepresentation;
import org.keycloak.representations.idm.authorization.ScopeRepresentation;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class AuthorizationBean {

    private final UserModel user;
    private final AuthorizationProvider authorization;
    private final UriInfo uriInfo;
    private ResourceBean resource;
    private List<ResourceBean> resources;
    private Collection<ResourceBean> userSharedResources;
    private Collection<ResourceBean> requestsWaitingPermission;
    private Collection<ResourceBean> resourcesWaitingOthersApproval;

    public AuthorizationBean(KeycloakSession session, UserModel user, UriInfo uriInfo) {
        this.user = user;
        this.uriInfo = uriInfo;
        authorization = session.getProvider(AuthorizationProvider.class);
        List<String> pathParameters = uriInfo.getPathParameters().get("resource_id");

        if (pathParameters != null && !pathParameters.isEmpty()) {
            Resource resource = authorization.getStoreFactory().getResourceStore().findById(pathParameters.get(0), null);

            if (resource != null && !resource.getOwner().equals(user.getId())) {
                throw new RuntimeException("User [" + user.getUsername() + "] can not access resource [" + resource.getId() + "]");
            }
        }
    }

    public Collection<ResourceBean> getResourcesWaitingOthersApproval() {
        if (resourcesWaitingOthersApproval == null) {
            HashMap<String, String> filters = new HashMap<>();

            filters.put(PermissionTicket.REQUESTER, user.getId());
            filters.put(PermissionTicket.GRANTED, Boolean.FALSE.toString());

            resourcesWaitingOthersApproval = toResourceRepresentation(findPermissions(filters));
        }

        return resourcesWaitingOthersApproval;
    }

    public Collection<ResourceBean> getResourcesWaitingApproval() {
        if (requestsWaitingPermission == null) {
            HashMap<String, String> filters = new HashMap<>();

            filters.put(PermissionTicket.OWNER, user.getId());
            filters.put(PermissionTicket.GRANTED, Boolean.FALSE.toString());

            requestsWaitingPermission = toResourceRepresentation(findPermissions(filters));
        }

        return requestsWaitingPermission;
    }

    public List<ResourceBean> getResources() {
        if (resources == null) {
            resources = authorization.getStoreFactory().getResourceStore().findByOwner(user.getId(), null).stream()
                    .filter(Resource::isOwnerManagedAccess)
                    .map(ResourceBean::new)
                    .collect(Collectors.toList());
        }
        return resources;
    }

    public Collection<ResourceBean> getSharedResources() {
        if (userSharedResources == null) {
            HashMap<String, String> filters = new HashMap<>();

            filters.put(PermissionTicket.REQUESTER, user.getId());
            filters.put(PermissionTicket.GRANTED, Boolean.TRUE.toString());

            PermissionTicketStore ticketStore = authorization.getStoreFactory().getPermissionTicketStore();

            userSharedResources = toResourceRepresentation(ticketStore.find(filters, null, -1, -1));
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
        return new ResourceBean(authorization.getStoreFactory().getResourceStore().findById(id, null));
    }

    public static class RequesterBean {

        private final Long createdTimestamp;
        private final Long grantedTimestamp;
        private UserModel requester;
        private List<PermissionScopeBean> scopes = new ArrayList<>();
        private boolean granted;

        public RequesterBean(PermissionTicket ticket, AuthorizationProvider authorization) {
            this.requester = authorization.getKeycloakSession().users().getUserById(ticket.getRequester(), authorization.getRealm());
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
        private final UserModel owner;
        private Resource resource;
        private Map<String, RequesterBean> permissions = new HashMap<>();
        private Collection<RequesterBean> shares;

        public ResourceBean(Resource resource) {
            RealmModel realm = authorization.getRealm();
            resourceServer = new ResourceServerBean(realm.getClientById(resource.getResourceServer().getId()));
            this.resource = resource;
            owner = authorization.getKeycloakSession().users().getUserById(resource.getOwner(), realm);
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

        public UserModel getOwner() {
            return owner;
        }

        public List<ScopeRepresentation> getScopes() {
            return resource.getScopes().stream().map(ModelToRepresentation::toRepresentation).collect(Collectors.toList());
        }

        public Collection<RequesterBean> getShares() {
            if (shares == null) {
                Map<String, String> filters = new HashMap<>();

                filters.put(PermissionTicket.RESOURCE, resource.getId());
                filters.put(PermissionTicket.GRANTED, Boolean.TRUE.toString());

                shares = toPermissionRepresentation(findPermissions(filters));
            }

            return shares;
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

    private List<PermissionTicket> findPermissions(Map<String, String> filters) {
        return authorization.getStoreFactory().getPermissionTicketStore().find(filters, null, -1, -1);
    }

    public class ResourceServerBean {

        private ClientModel clientModel;

        public ResourceServerBean(ClientModel clientModel) {
            this.clientModel = clientModel;
        }

        public String getName() {
            String name = clientModel.getName();

            if (name != null) {
                return name;
            }

            return clientModel.getClientId();
        }

        public String getRedirectUri() {
            Set<String> redirectUris = clientModel.getRedirectUris();

            if (redirectUris.isEmpty()) {
                return null;
            }

            return redirectUris.iterator().next();
        }
    }
}

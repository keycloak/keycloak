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
import java.util.stream.Collectors;

import javax.ws.rs.core.UriInfo;

import org.keycloak.authorization.AuthorizationProvider;
import org.keycloak.authorization.model.PermissionTicket;
import org.keycloak.authorization.model.Resource;
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
    private List<ResourceBean> userResources;
    private List<ResourceBean> userSharedResources;
    private List<PermissionResourceBean> permissionRequests;
    private List<PermissionResourceBean> userPermissionRequests;

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

    public List<PermissionResourceBean> getPermissionRequests() {
        if (permissionRequests == null) {
            HashMap<String, String> filters = new HashMap<>();

            filters.put(PermissionTicket.OWNER, user.getId());

            permissionRequests = toRepresentation(findPermissions(filters)).values().stream().collect(Collectors.toList());
        }

        return permissionRequests;
    }

    public List<PermissionResourceBean> getUserPermissionRequests() {
        if (userPermissionRequests == null) {
            HashMap<String, String> filters = new HashMap<>();

            filters.put(PermissionTicket.REQUESTER, user.getId());
            filters.put(PermissionTicket.GRANTED, Boolean.FALSE.toString());

            userPermissionRequests = toRepresentation(findPermissions(filters)).values().stream().collect(Collectors.toList());
        }

        return userPermissionRequests;
    }

    public List<ResourceBean> getUserResources() {
        if (userResources == null) {
            userResources = authorization.getStoreFactory().getResourceStore().findByOwner(user.getId(), null).stream()
                    .map(ResourceBean::new)
                    .collect(Collectors.toList());
        }
        return userResources;
    }

    public List<ResourceBean> getUserSharedResources() {
        if (userSharedResources == null) {
            HashMap<String, String> filters = new HashMap<>();

            filters.put(PermissionTicket.REQUESTER, user.getId());
            filters.put(PermissionTicket.GRANTED, Boolean.TRUE.toString());

            PermissionTicketStore ticketStore = authorization.getStoreFactory().getPermissionTicketStore();

            userSharedResources = toRepresentation(ticketStore.find(filters, null, -1, -1)).values().stream().map(PermissionResourceBean::getResource).collect(Collectors.toList());
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

    public static class PermissionResourceBean {

        private ResourceBean resource;
        private Map<String, PermissionRequestBean> permissions = new HashMap<>();

        public PermissionResourceBean(ResourceBean resource) {
            this.resource = resource;
        }

        public ResourceBean getResource() {
            return resource;
        }

        private void addRequester(PermissionTicket ticket, AuthorizationProvider authorization) {
            UserModel requester = authorization.getKeycloakSession().users().getUserById(ticket.getRequester(), authorization.getRealm());
            PermissionRequestBean permission = permissions.computeIfAbsent(ticket.getRequester(), key -> new PermissionRequestBean(requester, ticket));

            if (ticket.getScope() != null) {
                permission.addScope(new PermissionScopeBean(ticket));
            }
        }

        public Collection<PermissionRequestBean> getGranted() {
            return permissions.values().stream().filter(PermissionRequestBean::isGranted).collect(Collectors.toList());
        }

        public Collection<PermissionRequestBean> getPending() {
            return permissions.values().stream().filter(requester -> !requester.isGranted()).collect(Collectors.toList());
        }

        public Date getCreatedDate() {
            return permissions.values().iterator().next().getCreatedDate();
        }

        public Date getGrantedDate() {
            return permissions.values().stream().filter(permission -> permission.getGrantedDate() != null).findFirst().get().getGrantedDate();
        }
    }

    public static class PermissionRequestBean {

        private final Long createdTimestamp;
        private final Long grantedTimestamp;
        private String id;
        private String requester;
        private List<PermissionScopeBean> scopes = new ArrayList<>();
        private boolean granted;

        public PermissionRequestBean(UserModel requester, PermissionTicket ticket) {
            this.id = ticket.getId();
            this.requester = requester.getUsername();
            granted = ticket.isGranted();
            createdTimestamp = ticket.getCreatedTimestamp();
            grantedTimestamp = ticket.getGrantedTimestamp();
        }

        public String getId() {
            return id;
        }

        public String getRequester() {
            return requester;
        }

        public List<PermissionScopeBean> getScopes() {
            return scopes;
        }

        private void addScope(PermissionScopeBean scope) {
            scopes.add(scope);
        }

        public boolean isGranted() {
            return (granted && scopes.isEmpty()) || scopes.stream().filter(permissionScopeBean -> permissionScopeBean.isGranted()).count() == scopes.size();
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

        private final ScopeRepresentation scope;
        private final PermissionTicket ticket;

        public PermissionScopeBean(PermissionTicket ticket) {
            this.ticket = ticket;
            if (ticket.getScope() != null) {
                scope = ModelToRepresentation.toRepresentation(ticket.getScope());
            } else {
                scope = null;
            }
        }

        public String getId() {
            return ticket.getId();
        }

        public ScopeRepresentation getScope() {
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

        private final ClientModel resourceServer;
        private final UserModel owner;
        private Resource resource;
        private PermissionResourceBean permission;

        public ResourceBean(Resource resource) {
            RealmModel realm = authorization.getRealm();
            resourceServer = realm.getClientById(resource.getResourceServer().getId());
            this.resource = resource;
            owner = authorization.getKeycloakSession().users().getUserById(resource.getOwner(), realm);
        }

        public String getId() {
            return resource.getId();
        }

        public String getName() {
            return resource.getName();
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

        public long getApprovalRequests() {
            HashMap<String, String> filters = new HashMap<>();

            filters.put(PermissionTicket.RESOURCE, resource.getId());
            filters.put(PermissionTicket.GRANTED, Boolean.FALSE.toString());

            PermissionResourceBean permission = toRepresentation(findPermissions(filters)).get(resource.getId());

            if (permission != null) {
                return permission.getPending().size();
            }

            return 0;
        }

        public PermissionResourceBean getPermission() {
            if (permission == null) {
                HashMap<String, String> filters = new HashMap<>();

                filters.put(PermissionTicket.RESOURCE, resource.getId());

                permission = toRepresentation(findPermissions(filters)).getOrDefault(resource.getId(), null);
            }

            return permission;
        }

        public String getResourceServerName() {
            return resourceServer.getClientId();
        }
    }

    private Map<String, PermissionResourceBean> toRepresentation(List<PermissionTicket> permissionRequests) {
        Map<String, PermissionResourceBean> requests = new HashMap<>();

        for (PermissionTicket request : permissionRequests) {
            PermissionResourceBean bean = requests.computeIfAbsent(request.getResource().getId(), resourceId -> new PermissionResourceBean(getResource(resourceId)));
            bean.addRequester(request, authorization);
        }

        return requests;
    }

    private List<PermissionTicket> findPermissions(HashMap<String, String> filters) {
        return authorization.getStoreFactory().getPermissionTicketStore().find(filters, null, -1, -1);
    }

}

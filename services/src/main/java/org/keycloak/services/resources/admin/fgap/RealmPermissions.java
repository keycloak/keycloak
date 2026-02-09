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
package org.keycloak.services.resources.admin.fgap;

import jakarta.ws.rs.ForbiddenException;

import org.keycloak.authorization.model.ResourceServer;
import org.keycloak.models.AdminRoles;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
class RealmPermissions implements RealmPermissionEvaluator {

    protected final MgmtPermissions root;

    public RealmPermissions(MgmtPermissions root) {
        this.root = root;
    }

    public boolean canManageRealmDefault() {
        return root.hasOneAdminRole(AdminRoles.MANAGE_REALM);

    }
    public boolean canViewRealmDefault() {
        return root.hasOneAdminRole(AdminRoles.MANAGE_REALM, AdminRoles.VIEW_REALM);
    }

    public boolean canManageIdentityProvidersDefault() {
        return root.hasOneAdminRole(AdminRoles.MANAGE_IDENTITY_PROVIDERS);

    }
    public boolean canViewIdentityProvidersDefault() {
        return root.hasOneAdminRole(AdminRoles.MANAGE_IDENTITY_PROVIDERS, AdminRoles.VIEW_IDENTITY_PROVIDERS);
    }

    public boolean canManageAuthorizationDefault(ResourceServer resourceServer) {
        return root.hasOneAdminRole(AdminRoles.MANAGE_AUTHORIZATION, AdminRoles.MANAGE_CLIENTS);

    }
    public boolean canViewAuthorizationDefault(ResourceServer resourceServer) {
        return root.hasOneAdminRole(AdminRoles.MANAGE_AUTHORIZATION, AdminRoles.VIEW_AUTHORIZATION);
    }
    public boolean canManageEventsDefault() {
        return root.hasOneAdminRole(AdminRoles.MANAGE_EVENTS);
    }
    public boolean canViewEventsDefault() {
        return root.hasOneAdminRole(AdminRoles.MANAGE_EVENTS, AdminRoles.VIEW_EVENTS);
    }

    @Override
    public boolean canListRealms() {
        return root.isAdmin();
    }

    @Override
    public void requireViewRealmNameList() {
        if (!canListRealms()) {
            throw new ForbiddenException();
        }
    }

    @Override
    public boolean canManageRealm() {
        return canManageRealmDefault();
    }

    @Override
    public void requireManageRealm() {
        if (!canManageRealm()) {
            throw new ForbiddenException();
        }
    }
    @Override
    public boolean canViewRealm() {
        return canViewRealmDefault();
    }

    @Override
    public void requireViewRealm() {
        if (!canViewRealm()) {
            throw new ForbiddenException();
        }
    }

    @Override
    public boolean canManageIdentityProviders() {
        return canManageIdentityProvidersDefault();
    }

    @Override
    public boolean canViewIdentityProviders() {
        return canViewIdentityProvidersDefault();
    }

    @Override
    public void requireViewIdentityProviders() {
        if (!canViewIdentityProviders()) {
            throw new ForbiddenException();
        }
    }


    @Override
    public void requireManageIdentityProviders() {
        if (!canManageIdentityProviders()) {
            throw new ForbiddenException();
        }
    }


    @Override
    public boolean canManageAuthorization(ResourceServer resourceServer) {
        return canManageAuthorizationDefault(resourceServer);
    }

    @Override
    public boolean canViewAuthorization(ResourceServer resourceServer) {
        return canViewAuthorizationDefault(resourceServer);
    }

    @Override
    public void requireManageAuthorization(ResourceServer resourceServer) {
        if (!canManageAuthorization(resourceServer)) {
            throw new ForbiddenException();
        }
    }
    @Override
    public void requireViewAuthorization(ResourceServer resourceServer) {
        if (!canViewAuthorization(resourceServer)) {
            throw new ForbiddenException();
        }
    }

    @Override
    public boolean canManageEvents() {
        return canManageEventsDefault();
    }

    @Override
    public void requireManageEvents() {
        if (!canManageEvents()) {
            throw new ForbiddenException();
        }
    }
    @Override
    public boolean canViewEvents() {
        return canViewEventsDefault();
    }

    @Override
    public void requireViewEvents() {
        if (!canViewEvents()) {
            throw new ForbiddenException();
        }
    }

    @Override
    public void requireViewRequiredActions() {
        if (!(canViewRealm() || root.hasOneAdminRole(AdminRoles.QUERY_USERS))) {
            throw new ForbiddenException();
        }
    }

    @Override
    public void requireViewAuthenticationFlows() {
        if (!(canViewRealm() || root.hasOneAdminRole(AdminRoles.QUERY_CLIENTS))) {
            throw new ForbiddenException();
        }
    }

    @Override
    public void requireViewClientAuthenticatorProviders() {
        if (!(canViewRealm() || root.hasOneAdminRole(AdminRoles.QUERY_CLIENTS, AdminRoles.VIEW_CLIENTS, AdminRoles.MANAGE_CLIENTS))) {
            throw new ForbiddenException();
        }
    }

}

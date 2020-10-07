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
package org.keycloak.services.resources.admin.permissions;

import org.jboss.logging.Logger;
import org.keycloak.authorization.AuthorizationProvider;
import org.keycloak.models.AdminRoles;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.services.ForbiddenException;

/**
 * Manages default policies for all users.
 *
 *
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
class RealmPermissions implements RealmPermissionEvaluator {
    private static final Logger logger = Logger.getLogger(RealmPermissions.class);
    protected final KeycloakSession session;
    protected final RealmModel realm;
    protected final AuthorizationProvider authz;
    protected final MgmtPermissions root;

    public RealmPermissions(KeycloakSession session, RealmModel realm, AuthorizationProvider authz, MgmtPermissions root) {
        this.session = session;
        this.realm = realm;
        this.authz = authz;
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

    public boolean canManageAuthorizationDefault() {
        return root.hasOneAdminRole(AdminRoles.MANAGE_AUTHORIZATION, AdminRoles.MANAGE_CLIENTS);

    }
    public boolean canViewAuthorizationDefault() {
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
        return canViewRealm() || root.hasOneAdminRole(AdminRoles.ALL_QUERY_ROLES);
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
    public boolean canManageAuthorization() {
        return canManageAuthorizationDefault();
    }

    @Override
    public boolean canViewAuthorization() {
        return canViewAuthorizationDefault();
    }

    @Override
    public void requireManageAuthorization() {
        if (!canManageAuthorization()) {
            throw new ForbiddenException();
        }
    }
    @Override
    public void requireViewAuthorization() {
        if (!canViewAuthorization()) {
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

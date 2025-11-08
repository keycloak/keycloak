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

import org.keycloak.models.AdminRoles;
import org.keycloak.models.ClientModel;
import org.keycloak.services.resources.admin.AdminAuth;


/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
class RealmAuth {

    private AdminAuth.Resource resource;

    private AdminAuth auth;
    private ClientModel realmAdminApp;

    public RealmAuth(AdminAuth auth, ClientModel realmAdminApp) {
        this.auth = auth;
        this.realmAdminApp = realmAdminApp;
    }

    public RealmAuth init(AdminAuth.Resource resource) {
        this.resource = resource;
        return this;
    }

    public AdminAuth getAuth() {
        return auth;
    }

    public void requireAny() {
        if (!hasAny()) {
            throw new ForbiddenException();
        }
    }

    public boolean hasAny() {
        return auth.hasOneOfAppRole(realmAdminApp, AdminRoles.ALL_REALM_ROLES);
    }

    public boolean hasView() {
        return auth.hasOneOfAppRole(realmAdminApp, getViewRole(resource), getManageRole(resource));
    }

    public boolean hasManage() {
        return auth.hasOneOfAppRole(realmAdminApp, getManageRole(resource));
    }

    public void requireView() {
        if (!hasView()) {
            throw new ForbiddenException();
        }
    }

    public void requireManage() {
        if (!hasManage()) {
            throw new ForbiddenException();
        }
    }

    private String getViewRole(AdminAuth.Resource resource) {
        switch (resource) {
            case CLIENT:
                return AdminRoles.VIEW_CLIENTS;
            case USER:
                return AdminRoles.VIEW_USERS;
            case REALM:
                return AdminRoles.VIEW_REALM;
            case EVENTS:
                return AdminRoles.VIEW_EVENTS;
            case IDENTITY_PROVIDER:
                return AdminRoles.VIEW_IDENTITY_PROVIDERS;
            case AUTHORIZATION:
                return AdminRoles.VIEW_AUTHORIZATION;
            default:
                throw new IllegalStateException();
        }
    }

    private String getManageRole(AdminAuth.Resource resource) {
        switch (resource) {
            case CLIENT:
                return AdminRoles.MANAGE_CLIENTS;
            case USER:
                return AdminRoles.MANAGE_USERS;
            case REALM:
                return AdminRoles.MANAGE_REALM;
            case EVENTS:
                return AdminRoles.MANAGE_EVENTS;
            case IDENTITY_PROVIDER:
                return AdminRoles.MANAGE_IDENTITY_PROVIDERS;
            case IMPERSONATION:
                return AdminRoles.IMPERSONATION;
            case AUTHORIZATION:
                return AdminRoles.MANAGE_AUTHORIZATION;
            default:
                throw new IllegalStateException();
        }
    }

}

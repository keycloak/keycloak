/*
 * Copyright 2026 Red Hat, Inc. and/or its affiliates
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

import org.keycloak.authorization.AuthorizationProvider;
import org.keycloak.authorization.fgap.AdminPermissionsSchema;
import org.keycloak.authorization.store.PolicyStore;
import org.keycloak.authorization.store.ResourceStore;
import org.keycloak.models.AdminRoles;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.OrganizationModel;
import org.keycloak.services.resources.admin.fgap.ModelRecord.OrganizationModelRecord;

class OrganizationPermissions implements OrganizationPermissionEvaluator {

    private final FineGrainedAdminPermissionEvaluator eval;
    private final MgmtPermissions root;

    OrganizationPermissions(KeycloakSession session, AuthorizationProvider authz, MgmtPermissions root) {
        this.root = root;
        ResourceStore resourceStore = (authz == null) ? null : authz.getStoreFactory().getResourceStore();
        PolicyStore policyStore = (authz == null) ? null : authz.getStoreFactory().getPolicyStore();
        this.eval = new FineGrainedAdminPermissionEvaluator(session, root, resourceStore, policyStore);
    }

    @Override
    public boolean canManage() {
        if (root.hasOneAdminRole(AdminRoles.MANAGE_ORGANIZATIONS, AdminRoles.MANAGE_REALM)) {
            return true;
        }

        return eval.hasPermission(new OrganizationModelRecord(null), null, AdminPermissionsSchema.MANAGE);
    }

    @Override
    public boolean canManage(OrganizationModel organization) {
        if (root.hasOneAdminRole(AdminRoles.MANAGE_ORGANIZATIONS, AdminRoles.MANAGE_REALM)) {
            return true;
        }

        return eval.hasPermission(new OrganizationModelRecord(organization), null, AdminPermissionsSchema.MANAGE);
    }

    @Override
    public void requireManage() {
        if (!canManage()) {
            throw new ForbiddenException();
        }
    }

    @Override
    public void requireManage(OrganizationModel organization) {
        if (!canManage(organization)) {
            throw new ForbiddenException();
        }
    }

    @Override
    public boolean canView() {
        if (root.hasOneAdminRole(AdminRoles.MANAGE_ORGANIZATIONS, AdminRoles.VIEW_ORGANIZATIONS, AdminRoles.MANAGE_REALM)) {
            return true;
        }

        return eval.hasPermission(new OrganizationModelRecord(null), null, AdminPermissionsSchema.VIEW);
    }

    @Override
    public boolean canView(OrganizationModel organization) {
        if (root.hasOneAdminRole(AdminRoles.MANAGE_ORGANIZATIONS, AdminRoles.VIEW_ORGANIZATIONS, AdminRoles.MANAGE_REALM)) {
            return true;
        }

        return eval.hasPermission(new OrganizationModelRecord(organization), null, AdminPermissionsSchema.VIEW);
    }

    @Override
    public void requireView() {
        if (!canView()) {
            throw new ForbiddenException();
        }
    }

    @Override
    public void requireView(OrganizationModel organization) {
        if (!canView(organization)) {
            throw new ForbiddenException();
        }
    }

    @Override
    public boolean canQuery() {
        return root.hasOneAdminRole(AdminRoles.QUERY_ORGANIZATIONS) || canView();
    }

    @Override
    public void requireQuery() {
        if (!canQuery()) {
            throw new ForbiddenException();
        }
    }
}

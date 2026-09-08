/*
 * Copyright 2024 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.organization.validation;

import jakarta.ws.rs.BadRequestException;

import org.keycloak.models.GroupModel;
import org.keycloak.models.GroupModel.Type;
import org.keycloak.models.ModelException;
import org.keycloak.models.OrganizationModel;
import org.keycloak.models.RoleContainerModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserModel;
import org.keycloak.validate.BuiltinValidators;

import static org.keycloak.models.AdminRoles.isAdminRoleOrComposite;

public class OrganizationsValidation {

    public static void validateUrl(String redirectUrl) {
        if (!BuiltinValidators.uriValidator().validate(redirectUrl).isValid()) {
            throw new OrganizationValidationException("Organization redirect URL is not valid.");
        }
    }

    public static void validateOrganizationRoleMapping(UserModel user, RoleModel role) {
        if (role == null || !role.isType(RoleModel.Type.ORGANIZATION)) {
            return;
        }

        OrganizationModel organization = getOrganizationRoleContainer(role);

        if (user == null || !organization.isMember(user)) {
            throw new ModelException("Organization roles can only be assigned to members of the organization");
        }

        if (isAdminRoleOrComposite(role)) {
            throw new ModelException("Organization members can not be granted with admin roles");
        }
    }

    public static void validateOrganizationRoleGroupMapping(GroupModel group, RoleModel role) {
        if (role == null) {
            return;
        }

        boolean isOrgGroup = Type.ORGANIZATION.equals(group.getType());
        boolean isOrgRole = role.isType(RoleModel.Type.ORGANIZATION);

        if (isOrgRole) {
            OrganizationModel organization = getOrganizationRoleContainer(role);

            if (organization.isDefaultRole(role)) {
                throw new BadRequestException("The default organization role cannot be assigned to groups");
            }
        }

        if (isOrgGroup) {
            if (isAdminRoleOrComposite(role)) {
                throw new ModelException("Admin roles cannot be assigned to organization groups");
            }
            return;
        }

        if (isOrgRole) {
            throw new ModelException("Organization roles can only be assigned to organization groups");
        }
    }

    public static void validateOrganizationRoleScopeMapping(RoleModel role) {
        if (role != null && role.isType(RoleModel.Type.ORGANIZATION)) {
            throw new ModelException("Organization roles cannot be used in scope mappings");
        }
    }

    public static void validateOrganizationRoleComposite(RoleModel parent, RoleModel child) {
        if (parent == null || child == null) {
            return;
        }

        boolean isParentOrgRole = parent.isType(RoleModel.Type.ORGANIZATION);
        boolean isChildOrgRole = child.isType(RoleModel.Type.ORGANIZATION);

        if (!isParentOrgRole && !isChildOrgRole) {
            return;
        }

        if (isChildOrgRole) {
            if (!isParentOrgRole || !parent.getContainer().equals(child.getContainer())) {
                throw new ModelException("Organization roles can only be added as composites to other organization roles");
            }

            OrganizationModel organization = getOrganizationRoleContainer(child);

            if (organization.isDefaultRole(child)) {
                throw new ModelException("The default organization role cannot be added as a composite role");
            }
        }

        if (isAdminRoleOrComposite(child)) {
            throw new ModelException("Organization roles can not have admin roles as composites");
        }
    }

    private static OrganizationModel getOrganizationRoleContainer(RoleModel role) {
        RoleContainerModel container = role.getContainer();

        if (container instanceof OrganizationModel organization) {
            return organization;
        }

        throw new ModelException("Organization role must belong to an organization");
    }

    public static class OrganizationValidationException extends RuntimeException {
        public OrganizationValidationException(String message) {
            super(message);
        }
    }
}

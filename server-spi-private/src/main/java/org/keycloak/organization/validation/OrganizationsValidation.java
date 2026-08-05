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

import java.util.Objects;

import org.keycloak.models.ModelException;
import org.keycloak.models.OrganizationModel;
import org.keycloak.models.RoleContainerModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserModel;
import org.keycloak.validate.BuiltinValidators;

public class OrganizationsValidation {
    public static void validateUrl(String redirectUrl) {
        if (!BuiltinValidators.uriValidator().validate(redirectUrl).isValid()) {
            throw new OrganizationValidationException("Organization redirect URL is not valid.");
        }
    }

    public static void validateOrganizationRoleMapping(UserModel user, RoleModel role) {
        if (role == null || !role.isOrganizationRole()) {
            return;
        }
        if (user == null) {
            throw new ModelException("Organization roles can only be assigned to organization members");
        }

        OrganizationModel organization = getOrganizationRoleContainer(role);

        if (!organization.isMember(user)) {
            throw new ModelException("Organization roles can only be assigned to members of the organization");
        }
    }

    public static void validateOrganizationRoleGroupMapping(RoleModel role) {
        if (role != null && role.isOrganizationRole()) {
            throw new ModelException("Organization roles cannot be assigned to groups");
        }
    }

    public static void validateOrganizationRoleScopeMapping(RoleModel role) {
        if (role != null && role.isOrganizationRole()) {
            throw new ModelException("Organization roles cannot be used in scope mappings");
        }
    }

    public static void validateOrganizationRoleComposite(RoleModel parent, RoleModel child) {
        if (parent == null || child == null || (!parent.isOrganizationRole() && !child.isOrganizationRole())) {
            return;
        }

        OrganizationModel parentOrganization = parent.isOrganizationRole() ? getOrganizationRoleContainer(parent) : null;
        OrganizationModel childOrganization = child.isOrganizationRole() ? getOrganizationRoleContainer(child) : null;

        if (parentOrganization == null) {
            throw new ModelException("Organization roles cannot be composites of realm or client roles");
        }

        if (childOrganization != null && !Objects.equals(parentOrganization.getId(), childOrganization.getId())) {
            throw new ModelException("Organization role composites cannot cross organizations");
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

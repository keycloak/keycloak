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

import java.util.HashSet;
import java.util.Set;

import jakarta.ws.rs.BadRequestException;

import org.keycloak.models.GroupModel;
import org.keycloak.models.GroupModel.Type;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ModelException;
import org.keycloak.models.ModelValidationException;
import org.keycloak.models.OrganizationModel;
import org.keycloak.models.RoleContainerModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserModel;
import org.keycloak.organization.OrganizationProvider;
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

        if (organization.isDefaultRole(role)) {
            throw new ModelException("The default organization role is granted through organization membership");
        }

        if (isAdminRoleOrComposite(role)) {
            throw new ModelException("Organization members can not be granted with admin roles");
        }
    }

    public static void validateOrganizationRoleGroupMapping(KeycloakSession session, GroupModel group, RoleModel role) {
        if (role == null) {
            return;
        }

        boolean isOrgGroup = Type.ORGANIZATION.equals(group.getType());
        boolean isOrgRole = role.isType(RoleModel.Type.ORGANIZATION);

        if (!isOrgGroup && isOrgRole) {
            throw new ModelException("Organization roles cannot be assigned to realm groups");
        }

        if (isOrgGroup) {
            OrganizationModel organization = getCurrentOrganizationOrOwner(session, group);
            GroupModel root = getOrganizationRoot(session, organization);
            RoleContainerModel container = role.getContainer();

            if (container == null || container.getRealm() == null
                    || !organization.getRealm().getId().equals(container.getRealm().getId())) {
                throw new ModelException("Roles can only be assigned to organization groups from the same realm");
            }

            if (root.getId().equals(group.getId())) {
                validateOrganizationRoot(group, root, organization);
                if (!isOrgRole || !organization.getId().equals(role.getContainerId()) || !organization.isDefaultRole(role)) {
                    throw new ModelException("The internal organization group can only be assigned its default role");
                }
                validateInternalOrganizationContext(session, organization);
                return;
            }

            validateOrganizationGroupHierarchy(group, root, organization);
            if (isOrgRole) {
                OrganizationModel roleOrganization = getOrganizationRoleContainer(role);
                boolean sameOrganization = organization.getId().equals(roleOrganization.getId())
                        && organization.getRealm().getId().equals(roleOrganization.getRealm().getId());
                if (!sameOrganization) throw new ModelException("Organization roles can only be assigned to groups from the same organization");
                if (organization.isDefaultRole(role)) {
                    throw new ModelException("The default organization role is granted through organization membership");
                }
            }

            if (isAdminRoleOrComposite(role)) {
                throw new ModelException("Admin roles cannot be assigned to organization groups");
            }
        }
    }

    public static void validateOrganizationRoleGroupMappingRemoval(KeycloakSession session, GroupModel group, RoleModel role) {
        if (group == null || role == null || !Type.ORGANIZATION.equals(group.getType())) {
            return;
        }

        OrganizationModel organization = getCurrentOrganizationOrOwner(session, group);
        GroupModel root = getOrganizationRoot(session, organization);
        if (!root.getId().equals(group.getId())) {
            return;
        }

        validateOrganizationRoot(group, root, organization);
        if (organization.isDefaultRole(role)) {
            throw new BadRequestException("The default organization role cannot be removed from the internal organization group");
        }
        validateInternalOrganizationContext(session, organization);
    }

    public static void validateOrganizationGroupMembership(KeycloakSession session, UserModel user, GroupModel group, boolean joining) {
        if (group == null || !Type.ORGANIZATION.equals(group.getType())) {
            return;
        }

        OrganizationModel organization = getCurrentOrganizationOrOwner(session, group);
        GroupModel root = getOrganizationRoot(session, organization);
        if (root.getId().equals(group.getId())) {
            validateOrganizationRoot(group, root, organization);
            validateInternalOrganizationContext(session, organization);
        } else if (joining && (user == null || !organization.isMember(user))) {
            throw new ModelException("Only organization members can join organization groups");
        }
    }

    public static void validateOrganizationGroupRemoval(KeycloakSession session, GroupModel group) {
        if (group == null || !Type.ORGANIZATION.equals(group.getType())) {
            return;
        }

        OrganizationModel organization = getCurrentOrganizationOrOwner(session, group);
        GroupModel root = getOrganizationRoot(session, organization);
        if (root.getId().equals(group.getId())) {
            validateOrganizationRoot(group, root, organization);
            validateInternalOrganizationContext(session, organization);
        }
    }

    public static void validateOrganizationGroupParent(KeycloakSession session, GroupModel group, GroupModel parent) {
        if (group == null) {
            return;
        }

        boolean isOrganizationGroup = Type.ORGANIZATION.equals(group.getType());
        if (parent == null) {
            if (isOrganizationGroup) {
                throw new ModelValidationException("Organization groups cannot be moved outside their organization hierarchy");
            }
            return;
        }

        boolean isOrganizationParent = Type.ORGANIZATION.equals(parent.getType());

        if (!isOrganizationGroup && !isOrganizationParent) {
            return;
        }

        if (!isOrganizationGroup || !isOrganizationParent) {
            throw new ModelValidationException("Organization groups can only be moved within their organization hierarchy");
        }

        OrganizationModel organization = getOrganizationGroupOwner(group);
        OrganizationModel parentOrganization = getOrganizationGroupOwner(parent);

        if (!organization.getId().equals(parentOrganization.getId())
                || !organization.getRealm().getId().equals(parentOrganization.getRealm().getId())) {
            throw new ModelValidationException("Organization groups can only be moved within the same organization and realm");
        }

        GroupModel root = getOrganizationRoot(session, organization);
        validateOrganizationRoot(root, root, organization);
        if (root.getId().equals(group.getId())) {
            throw new ModelValidationException("The internal organization group cannot be moved");
        }

        Set<String> visited = new HashSet<>();
        GroupModel current = parent;
        while (current != null && visited.add(current.getId())) {
            if (group.getId().equals(current.getId())) {
                throw new ModelValidationException("Organization groups cannot be moved below their own descendants");
            }
            if (root.getId().equals(current.getId())) {
                return;
            }
            if (!Type.ORGANIZATION.equals(current.getType())) {
                break;
            }
            OrganizationModel currentOrganization = getOrganizationGroupOwner(current);
            if (!organization.getId().equals(currentOrganization.getId())
                    || !organization.getRealm().getId().equals(currentOrganization.getRealm().getId())) {
                throw new ModelValidationException(
                        "Organization group parent is not anchored to the internal organization group");
            }
            current = current.getParent();
        }

        throw new ModelValidationException("Organization group parent is not anchored to the internal organization group");
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

    private static OrganizationModel getOrganizationGroupOwner(GroupModel group) {
        OrganizationModel organization = group.getOrganization();

        if (organization == null || organization.getRealm() == null) {
            throw new ModelException("Organization group must belong to an organization");
        }

        return organization;
    }

    private static OrganizationModel getCurrentOrganizationOrOwner(KeycloakSession session, GroupModel group) {
        OrganizationModel owner = getOrganizationGroupOwner(group);
        OrganizationModel current = session.getContext().getOrganization();
        if (current != null && owner.getId().equals(current.getId())
                && owner.getRealm().getId().equals(current.getRealm().getId())) {
            return current;
        }
        return owner;
    }

    private static GroupModel getOrganizationRoot(KeycloakSession session, OrganizationModel organization) {
        OrganizationProvider provider = session.getProvider(OrganizationProvider.class);
        GroupModel root = provider == null ? null : provider.getOrganizationGroup(organization);
        if (root == null) {
            throw new ModelException("Internal organization group does not exist");
        }
        return root;
    }

    private static void validateOrganizationRoot(GroupModel group, GroupModel root, OrganizationModel organization) {
        OrganizationModel rootOrganization = getOrganizationGroupOwner(root);
        if (!Type.ORGANIZATION.equals(root.getType()) || root.getParent() != null
                || !root.getId().equals(group.getId())
                || !organization.getId().equals(rootOrganization.getId())
                || !organization.getRealm().getId().equals(rootOrganization.getRealm().getId())) {
            throw new ModelException("Invalid internal organization group");
        }
    }

    private static void validateOrganizationGroupHierarchy(GroupModel group, GroupModel root, OrganizationModel organization) {
        Set<String> visited = new HashSet<>();
        GroupModel current = group;
        while (current != null && visited.add(current.getId())) {
            OrganizationModel currentOrganization = getOrganizationGroupOwner(current);
            if (!Type.ORGANIZATION.equals(current.getType())
                    || !organization.getId().equals(currentOrganization.getId())
                    || !organization.getRealm().getId().equals(currentOrganization.getRealm().getId())) break;
            if (root.getId().equals(current.getId())) {
                validateOrganizationRoot(current, root, organization);
                return;
            }
            current = current.getParent();
        }
        throw new ModelException("Organization group is not anchored to its internal organization group");
    }

    private static void validateInternalOrganizationContext(KeycloakSession session, OrganizationModel organization) {
        OrganizationModel current = session.getContext().getOrganization();
        if (current == null || !organization.getId().equals(current.getId())
                || !organization.getRealm().getId().equals(current.getRealm().getId())) {
            throw new ModelValidationException("The internal organization group can only be changed by the organization provider");
        }
    }

    public static class OrganizationValidationException extends RuntimeException {
        public OrganizationValidationException(String message) {
            super(message);
        }
    }
}

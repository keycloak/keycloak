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
package org.keycloak.exportimport.util;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.keycloak.models.ClientModel;
import org.keycloak.models.GroupModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ModelException;
import org.keycloak.models.OrganizationModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleContainerModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.ModelToRepresentation;
import org.keycloak.organization.OrganizationProvider;
import org.keycloak.organization.validation.OrganizationsValidation;
import org.keycloak.representations.idm.IdentityProviderRepresentation;
import org.keycloak.representations.idm.MemberRepresentation;
import org.keycloak.representations.idm.MembershipType;
import org.keycloak.representations.idm.OrganizationRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.utils.StringUtil;

public final class OrganizationExportImportUtils {

    private OrganizationExportImportUtils() {
    }

    public static void exportOrganizations(KeycloakSession session, RealmRepresentation representation) {
        OrganizationProvider orgProvider = session.getProvider(OrganizationProvider.class);
        orgProvider.getAllStream().map(model -> {
            OrganizationRepresentation organization = ModelToRepresentation.toRepresentation(model, false);
            exportOrganizationRoles(model, organization);

            orgProvider.getMembersStream(model, (Map<String, String>) null, null, null, null)
                    .forEach(user -> {
                        MemberRepresentation member = new MemberRepresentation();
                        member.setUsername(user.getUsername());
                        member.setMembershipType(orgProvider.isManagedMember(model, user) ? MembershipType.MANAGED : MembershipType.UNMANAGED);
                        List<String> organizationRoles = exportOrganizationMemberRoleMappings(model, user);
                        if (!organizationRoles.isEmpty()) {
                            member.setOrganizationRoles(organizationRoles);
                        }

                        List<String> groupIds = orgProvider.getOrganizationGroupsByMember(model, user).map(GroupModel::getId).collect(Collectors.toList());
                        if (!groupIds.isEmpty()) {
                            member.setGroups(groupIds);
                        }

                        organization.addMember(member);
                    });

            orgProvider.getIdentityProviders(model)
                    .map(broker -> {
                        IdentityProviderRepresentation representationBroker = new IdentityProviderRepresentation();
                        representationBroker.setAlias(broker.getAlias());
                        return representationBroker;
                    }).forEach(organization::addIdentityProvider);

            orgProvider.getTopLevelGroups(model, null, null)
                    .map(group -> ModelToRepresentation.toGroupHierarchy(group, true))
                    .forEach(organization::addGroup);

            return organization;
        }).forEach(representation::addOrganization);
    }

    public static void exportOrganizationRoles(OrganizationModel organization, OrganizationRepresentation representation) {
        List<RoleRepresentation> roles = organization.getRolesStream()
                .map(role -> exportOrganizationRole(organization, role))
                .collect(Collectors.toList());
        if (!roles.isEmpty()) {
            representation.setRoles(roles);
        }

        RoleModel defaultRole = organization.getDefaultRole();
        if (defaultRole != null) {
            representation.setDefaultRole(ModelToRepresentation.toBriefRepresentation(defaultRole));
        }
    }

    private static RoleRepresentation exportOrganizationRole(OrganizationModel organization, RoleModel role) {
        RoleRepresentation roleRep = ExportUtils.exportRole(role);
        Set<String> compositeOrganizationRoles = role.getCompositesStream()
                .filter(composite -> isRoleFromOrganization(composite, organization))
                .map(RoleModel::getName)
                .collect(Collectors.toSet());

        if (!compositeOrganizationRoles.isEmpty()) {
            RoleRepresentation.Composites composites = roleRep.getComposites();
            if (composites == null) {
                composites = new RoleRepresentation.Composites();
                roleRep.setComposites(composites);
            }
            composites.setOrganization(compositeOrganizationRoles);
        }

        return roleRep;
    }

    public static List<String> exportOrganizationMemberRoleMappings(OrganizationModel organization, UserModel user) {
        return user.getRoleMappingsStream()
                .filter(role -> isRoleFromOrganization(role, organization))
                .map(RoleModel::getName)
                .sorted()
                .collect(Collectors.toList());
    }

    public static void importOrganizationRoles(KeycloakSession session, RealmModel realm, OrganizationModel organization,
            OrganizationRepresentation representation) {
        List<RoleRepresentation> roleReps = Optional.ofNullable(representation.getRoles()).orElse(Collections.emptyList())
                .stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        RoleRepresentation defaultRoleRep = representation.getDefaultRole();
        boolean defaultRoleImported = false;
        RoleModel generatedDefaultRole = organization.getDefaultRole();
        boolean generatedDefaultRoleImported = roleReps.stream()
                .anyMatch(roleRep -> matchesRoleReference(generatedDefaultRole, roleRep));

        for (RoleRepresentation roleRep : roleReps) {
            boolean defaultRole = isDefaultRoleRepresentation(organization, roleRep, defaultRoleRep);
            importOrganizationRole(session, organization, roleRep, defaultRole, defaultRole && !generatedDefaultRoleImported);
            defaultRoleImported = defaultRoleImported || defaultRole;
        }

        if (!defaultRoleImported && defaultRoleRep != null && StringUtil.isNotBlank(defaultRoleRep.getName())) {
            RoleModel role = importOrganizationRole(session, organization, defaultRoleRep, true, !generatedDefaultRoleImported);
            addComposites(realm, organization, role, defaultRoleRep);
        }

        for (RoleRepresentation roleRep : roleReps) {
            RoleModel role = resolveOrganizationRole(session, organization, roleRep);
            addComposites(realm, organization, role, roleRep);
        }
    }

    public static void importOrganizationMemberRoleMappings(OrganizationModel organization, MemberRepresentation member, UserModel user) {
        if (member.getOrganizationRoles() == null) {
            return;
        }
        if (user == null) {
            throw new ModelException("Unable to find organization member specified by username: " + member.getUsername());
        }

        for (String roleName : member.getOrganizationRoles()) {
            RoleModel role = getRequiredOrganizationRole(organization, roleName, "organization role mapping");
            OrganizationsValidation.validateOrganizationRoleMapping(user, role);
            user.grantRole(role);
        }
    }

    private static boolean isRoleFromOrganization(RoleModel role, OrganizationModel organization) {
        RoleContainerModel container = role.getContainer();
        return container instanceof OrganizationModel && Objects.equals(container.getId(), organization.getId());
    }

    private static RoleModel importOrganizationRole(KeycloakSession session, OrganizationModel organization, RoleRepresentation roleRep,
            boolean defaultRole, boolean reuseGeneratedDefaultRole) {
        if (StringUtil.isBlank(roleRep.getName())) {
            throw new ModelException("Organization role has no name");
        }

        RoleModel role = getExistingOrganizationRole(session, organization, roleRep);
        if (role == null && defaultRole && reuseGeneratedDefaultRole) {
            role = organization.getDefaultRole();
        }
        if (role == null) {
            role = StringUtil.isNotBlank(roleRep.getId()) ? organization.addRole(roleRep.getId(), roleRep.getName()) : organization.addRole(roleRep.getName());
        }

        updateRole(role, roleRep);
        if (defaultRole) {
            organization.setDefaultRole(role);
        }
        return role;
    }

    private static RoleModel getExistingOrganizationRole(KeycloakSession session, OrganizationModel organization, RoleRepresentation roleRep) {
        RoleModel role = null;
        if (StringUtil.isNotBlank(roleRep.getId())) {
            role = session.roles().getRoleById(organization, roleRep.getId());
        }
        if (role == null && StringUtil.isNotBlank(roleRep.getName())) {
            role = organization.getRole(roleRep.getName());
        }
        return role;
    }

    private static RoleModel resolveOrganizationRole(KeycloakSession session, OrganizationModel organization, RoleRepresentation roleRep) {
        RoleModel role = getExistingOrganizationRole(session, organization, roleRep);
        if (role == null) {
            throw new ModelException("Unable to find organization role specified by name: " + roleRep.getName());
        }
        return role;
    }

    private static boolean isDefaultRoleRepresentation(OrganizationModel organization, RoleRepresentation roleRep,
            RoleRepresentation defaultRoleRep) {
        if (defaultRoleRep != null) {
            return matchesRoleReference(roleRep, defaultRoleRep);
        }

        RoleModel defaultRole = organization.getDefaultRole();
        return defaultRole != null && Objects.equals(defaultRole.getName(), roleRep.getName());
    }

    private static boolean matchesRoleReference(RoleRepresentation roleRep, RoleRepresentation reference) {
        if (roleRep == null || reference == null) {
            return false;
        }
        if (StringUtil.isNotBlank(reference.getId()) && Objects.equals(reference.getId(), roleRep.getId())) {
            return true;
        }
        return StringUtil.isNotBlank(reference.getName()) && Objects.equals(reference.getName(), roleRep.getName());
    }

    private static boolean matchesRoleReference(RoleModel role, RoleRepresentation reference) {
        if (role == null || reference == null) {
            return false;
        }
        if (StringUtil.isNotBlank(reference.getId()) && Objects.equals(reference.getId(), role.getId())) {
            return true;
        }
        return StringUtil.isNotBlank(reference.getName()) && Objects.equals(reference.getName(), role.getName());
    }

    private static void updateRole(RoleModel role, RoleRepresentation roleRep) {
        if (!Objects.equals(role.getName(), roleRep.getName())) {
            role.setName(roleRep.getName());
        }
        if (roleRep.getDescription() != null) {
            role.setDescription(roleRep.getDescription());
        }
        setAttributes(role, roleRep.getAttributes());
    }

    private static void setAttributes(RoleModel role, Map<String, List<String>> attributes) {
        if (attributes == null) {
            return;
        }

        Set<String> attrsToRemove = new HashSet<>(role.getAttributes().keySet());
        attrsToRemove.removeAll(attributes.keySet());
        attributes.forEach(role::setAttribute);
        attrsToRemove.forEach(role::removeAttribute);
    }

    private static void addComposites(RealmModel realm, OrganizationModel organization, RoleModel role, RoleRepresentation roleRep) {
        RoleRepresentation.Composites composites = roleRep.getComposites();
        if (composites == null) {
            return;
        }

        if (composites.getRealm() != null) {
            for (String roleName : composites.getRealm()) {
                addComposite(role, getRequiredRealmRole(realm, roleName));
            }
        }
        if (composites.getClient() != null) {
            for (Map.Entry<String, List<String>> entry : composites.getClient().entrySet()) {
                ClientModel client = realm.getClientByClientId(entry.getKey());
                if (client == null) {
                    throw new ModelException("Unable to find composite client: " + entry.getKey());
                }
                for (String roleName : Optional.ofNullable(entry.getValue()).orElse(Collections.emptyList())) {
                    addComposite(role, getRequiredClientRole(client, roleName));
                }
            }
        }
        if (composites.getOrganization() != null) {
            for (String roleName : composites.getOrganization()) {
                addComposite(role, getRequiredOrganizationRole(organization, roleName, "composite organization role"));
            }
        }
    }

    private static RoleModel getRequiredRealmRole(RealmModel realm, String roleName) {
        String name = trimRoleName(roleName);
        RoleModel role = StringUtil.isBlank(name) ? null : realm.getRole(name);
        if (role == null) {
            throw new ModelException("Unable to find composite realm role: " + roleName);
        }
        return role;
    }

    private static RoleModel getRequiredClientRole(ClientModel client, String roleName) {
        String name = trimRoleName(roleName);
        RoleModel role = StringUtil.isBlank(name) ? null : client.getRole(name);
        if (role == null) {
            throw new ModelException("Unable to find composite client role: " + roleName);
        }
        return role;
    }

    private static RoleModel getRequiredOrganizationRole(OrganizationModel organization, String roleName, String description) {
        String name = trimRoleName(roleName);
        RoleModel role = StringUtil.isBlank(name) ? null : organization.getRole(name);
        if (role == null) {
            throw new ModelException("Unable to find " + description + ": " + roleName);
        }
        return role;
    }

    private static void addComposite(RoleModel role, RoleModel composite) {
        OrganizationsValidation.validateOrganizationRoleComposite(role, composite);
        role.addCompositeRole(composite);
    }

    private static String trimRoleName(String roleName) {
        return roleName == null ? null : roleName.trim();
    }
}

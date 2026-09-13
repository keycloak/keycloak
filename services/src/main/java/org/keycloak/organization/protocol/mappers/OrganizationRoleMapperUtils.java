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
package org.keycloak.organization.protocol.mappers;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.keycloak.models.ClientModel;
import org.keycloak.models.GroupModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ModelException;
import org.keycloak.models.OrganizationModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserModel;
import org.keycloak.organization.OrganizationProvider;
import org.keycloak.organization.utils.Organizations;
import org.keycloak.organization.validation.OrganizationsValidation;

import static org.keycloak.models.AdminRoles.isAdminRoleOrComposite;

public final class OrganizationRoleMapperUtils {

    public static final String REALM_ACCESS = "realm_access";
    public static final String RESOURCE_ACCESS = "resource_access";
    public static final String ROLES = "roles";

    private OrganizationRoleMapperUtils() {
    }

    public static OrganizationRoleClaims resolveRoleClaims(OrganizationModel organization, UserModel user, KeycloakSession session) {
        return resolveRoleClaims(organization, user, session, role -> true);
    }

    public static OrganizationRoleClaims resolveRoleClaims(OrganizationModel organization, UserModel user, KeycloakSession session,
            Predicate<RoleModel> roleFilter) {
        if (organization == null || user == null || !organization.isEnabled() || !organization.isMember(user)) {
            return OrganizationRoleClaims.empty();
        }

        Set<RoleModel> organizationRoles = new LinkedHashSet<>();
        try (var directMappings = user.getRoleMappingsStream()) {
            directMappings.filter(Objects::nonNull)
                    .filter(role -> role.isType(RoleModel.Type.ORGANIZATION))
                    .filter(role -> Objects.equals(organization.getId(), role.getContainerId()))
                    .forEach(role -> {
                        validateOrganizationRole(organization, role, false);
                        organizationRoles.add(role);
                    });
        }

        OrganizationProvider orgProvider = Organizations.getProvider(session);
        GroupModel root = orgProvider.getOrganizationGroup(organization);
        OrganizationModel rootOwner = root == null ? null : root.getOrganization();
        if (root == null || !GroupModel.Type.ORGANIZATION.equals(root.getType()) || root.getParent() != null
                || rootOwner == null || !Objects.equals(organization.getId(), rootOwner.getId())
                || rootOwner.getRealm() == null || !Objects.equals(organization.getRealm().getId(), rootOwner.getRealm().getId())) {
            throw new ModelException("Invalid internal organization group");
        }

        List<RoleModel> rootMappings = root.getRoleMappingsStream().filter(Objects::nonNull).toList();
        RoleModel defaultRole = organization.getDefaultRole();
        if (defaultRole == null || rootMappings.size() != 1
                || !Objects.equals(defaultRole.getId(), rootMappings.get(0).getId())) {
            throw new ModelException("Invalid default role mapping on the internal organization group");
        }
        validateOrganizationRole(organization, defaultRole, true);

        Set<String> validatedGroupIds = new HashSet<>();
        Set<String> collectedGroupIds = new HashSet<>();
        try (var memberships = user.getRoleMappingsGroupsStream()) {
            memberships.filter(Objects::nonNull)
                    .filter(group -> GroupModel.Type.ORGANIZATION.equals(group.getType()))
                    .filter(group -> isGroupFromOrganization(group, organization))
                    .forEach(group -> collectOrganizationGroupRoles(session, group, root, organization, organizationRoles,
                            validatedGroupIds, collectedGroupIds));
        }
        organizationRoles.add(rootMappings.get(0));

        if (organizationRoles.isEmpty()) {
            return OrganizationRoleClaims.empty();
        }

        return OrganizationRoleClaims.from(organization, expandOrganizationRoleGraph(organization, organizationRoles).stream()
                .filter(roleFilter).collect(Collectors.toSet()));
    }

    private static void collectOrganizationGroupRoles(KeycloakSession session, GroupModel group, GroupModel root,
            OrganizationModel organization,
            Set<RoleModel> roles, Set<String> validatedGroupIds, Set<String> collectedGroupIds) {
        List<GroupModel> chain = new ArrayList<>();
        Set<String> chainIds = new HashSet<>();
        GroupModel current = group;
        while (current != null && chainIds.add(current.getId())) {
            validateOrganizationGroup(current, organization);
            chain.add(current);
            if (Objects.equals(root.getId(), current.getId()) || validatedGroupIds.contains(current.getId())) {
                break;
            }
            current = current.getParent();
        }
        GroupModel last = chain.isEmpty() ? null : chain.get(chain.size() - 1);
        if (last == null || !Objects.equals(root.getId(), last.getId()) && !validatedGroupIds.contains(last.getId())) {
            throw new ModelException("Organization group is not anchored to its internal organization group");
        }

        validatedGroupIds.addAll(chainIds);
        for (GroupModel source : chain) {
            if (Objects.equals(root.getId(), source.getId()) || !collectedGroupIds.add(source.getId())) {
                continue;
            }
            try (var mappings = source.getRoleMappingsStream()) {
                mappings.filter(Objects::nonNull)
                        .filter(role -> role.isType(RoleModel.Type.ORGANIZATION))
                        .forEach(role -> {
                            validateOrganizationRole(organization, role, false);
                            OrganizationsValidation.validateOrganizationRoleGroupMapping(session, source, role);
                            roles.add(role);
                        });
            }
        }
    }

    private static boolean isGroupFromOrganization(GroupModel group, OrganizationModel organization) {
        OrganizationModel owner = group.getOrganization();
        return owner != null && Objects.equals(organization.getId(), owner.getId());
    }

    private static void validateOrganizationGroup(GroupModel group, OrganizationModel organization) {
        OrganizationModel owner = group.getOrganization();
        if (!GroupModel.Type.ORGANIZATION.equals(group.getType()) || owner == null || owner.getRealm() == null
                || !Objects.equals(organization.getId(), owner.getId())
                || !Objects.equals(organization.getRealm().getId(), owner.getRealm().getId())) {
            throw new ModelException("Invalid organization group hierarchy");
        }
    }

    private static void validateOrganizationRole(OrganizationModel organization, RoleModel role, boolean allowDefault) {
        if (!(role.getContainer() instanceof OrganizationModel owner) || owner.getRealm() == null
                || !Objects.equals(organization.getId(), owner.getId())
                || !Objects.equals(organization.getRealm().getId(), owner.getRealm().getId())) {
            throw new ModelException("Invalid organization role mapping");
        }
        if (!allowDefault && organization.isDefaultRole(role)) {
            throw new ModelException("The default organization role is granted through organization membership");
        }
        if (isAdminRoleOrComposite(role)) {
            throw new ModelException("Organization roles cannot inherit admin roles");
        }
    }

    private static Set<RoleModel> expandOrganizationRoleGraph(OrganizationModel organization, Set<RoleModel> roots) {
        Set<RoleModel> expanded = new LinkedHashSet<>();
        Set<String> visited = new HashSet<>();
        Deque<RoleModel> pending = new ArrayDeque<>(roots);
        while (!pending.isEmpty()) {
            RoleModel role = pending.removeFirst();
            if (role == null || !visited.add(role.getId())) {
                continue;
            }
            validateRoleRealm(organization, role);
            expanded.add(role);
            try (var composites = role.getCompositesStream()) {
                composites.filter(Objects::nonNull).forEach(child -> {
                    validateRoleRealm(organization, child);
                    OrganizationsValidation.validateOrganizationRoleComposite(role, child);
                    pending.addLast(child);
                });
            }
        }
        return expanded;
    }

    private static void validateRoleRealm(OrganizationModel organization, RoleModel role) {
        if (role.getContainer() == null || role.getContainer().getRealm() == null
                || !Objects.equals(organization.getRealm().getId(), role.getContainer().getRealm().getId())) {
            throw new ModelException("Organization role graph crosses realm boundaries");
        }
    }

    @SuppressWarnings("unchecked")
    public static void addToOrganizationClaim(Map<String, Object> organizationClaim, OrganizationRoleClaims claims) {
        if (organizationClaim == null || claims == null || claims.isEmpty()) {
            return;
        }

        if (!claims.getOrganizationRoles().isEmpty()) {
            mergeRoles(organizationClaim, ROLES, claims.getOrganizationRoles());
        }

        mergeRoles(organizationClaim, REALM_ACCESS, claims.getRealmRoles());

        if (!claims.getClientRoles().isEmpty()) {
            Map<String, Object> resourceAccess = mutableMap(organizationClaim.get(RESOURCE_ACCESS));

            claims.getClientRoles().forEach((clientId, roles) -> {
                Map<String, Object> clientAccess = mutableMap(resourceAccess.get(clientId));
                mergeRoles(clientAccess, ROLES, roles);
                resourceAccess.put(clientId, clientAccess);
            });

            organizationClaim.put(RESOURCE_ACCESS, resourceAccess);
        }
    }

    @SuppressWarnings("unchecked")
    private static void mergeRoles(Map<String, Object> parent, String claimName, Collection<String> roles) {
        if (roles == null || roles.isEmpty()) {
            return;
        }

        Object currentValue = parent.get(claimName);
        Map<String, Object> access = claimName.equals(ROLES) ? parent : mutableMap(currentValue);
        Set<String> mergedRoles = new TreeSet<>(roles);
        Object currentRoles = access.get(ROLES);

        if (currentRoles instanceof Collection<?> values) {
            values.stream().filter(Objects::nonNull).map(Object::toString).forEach(mergedRoles::add);
        } else if (currentRoles instanceof String value) {
            mergedRoles.add(value);
        }

        access.put(ROLES, List.copyOf(mergedRoles));

        if (!claimName.equals(ROLES)) {
            parent.put(claimName, access);
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> mutableMap(Object value) {
        return value instanceof Map<?, ?> ? new LinkedHashMap<>((Map<String, Object>) value) : new LinkedHashMap<>();
    }

    public static final class OrganizationRoleClaims {

        private static final OrganizationRoleClaims EMPTY = new OrganizationRoleClaims(List.of(), List.of(), Map.of());

        private final List<String> organizationRoles;
        private final List<String> realmRoles;
        private final Map<String, List<String>> clientRoles;

        private OrganizationRoleClaims(List<String> organizationRoles, List<String> realmRoles, Map<String, List<String>> clientRoles) {
            this.organizationRoles = organizationRoles;
            this.realmRoles = realmRoles;
            this.clientRoles = clientRoles;
        }

        public static OrganizationRoleClaims empty() {
            return EMPTY;
        }

        public static OrganizationRoleClaims from(OrganizationModel organization, Set<RoleModel> roles) {
            if (organization == null || roles == null || roles.isEmpty()) {
                return EMPTY;
            }

            Set<String> organizationRoles = new TreeSet<>();
            Set<String> realmRoles = new TreeSet<>();
            Map<String, Set<String>> clientRoles = new TreeMap<>();

            for (RoleModel role : roles) {
                if (role == null) {
                    continue;
                }

                if (role.isType(RoleModel.Type.ORGANIZATION)) {
                    if (Objects.equals(organization.getId(), role.getContainerId())) {
                        organizationRoles.add(role.getName());
                    }
                } else if (role.isType(RoleModel.Type.REALM)) {
                    realmRoles.add(role.getName());
                } else if (role.getContainer() instanceof ClientModel client) {
                    clientRoles.computeIfAbsent(client.getClientId(), key -> new TreeSet<>()).add(role.getName());
                }
            }

            if (organizationRoles.isEmpty() && realmRoles.isEmpty() && clientRoles.isEmpty()) {
                return EMPTY;
            }

            Map<String, List<String>> clientRoleClaims = new LinkedHashMap<>();
            clientRoles.forEach((clientId, roleNames) -> clientRoleClaims.put(clientId, List.copyOf(roleNames)));

            return new OrganizationRoleClaims(List.copyOf(organizationRoles), List.copyOf(realmRoles), clientRoleClaims);
        }

        public List<String> getOrganizationRoles() {
            return organizationRoles;
        }

        public List<String> getRealmRoles() {
            return realmRoles;
        }

        public Map<String, List<String>> getClientRoles() {
            return clientRoles;
        }

        public boolean isEmpty() {
            return organizationRoles.isEmpty() && realmRoles.isEmpty() && clientRoles.isEmpty();
        }
    }
}

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

package org.keycloak.models.jpa;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;

import org.keycloak.Config;
import org.keycloak.models.AdminRoles;
import org.keycloak.models.Constants;
import org.keycloak.models.GroupModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ModelValidationException;
import org.keycloak.models.RoleModel;
import org.keycloak.models.jpa.entities.GroupEntity;
import org.keycloak.models.jpa.entities.RealmEntity;

/**
 * Protects the local JPA role graph from paths that would let an organization role or group inherit an admin role.
 * All reads deliberately bypass the model cache and run while the realm row is write-locked.
 */
final class OrganizationRoleGraphGuard {

    static final String UNSAFE_ROLE_GRAPH = "Organization roles and groups cannot inherit admin roles";

    private final KeycloakSession session;
    private final EntityManager em;
    private final String realmId;
    private final Map<String, String> clientIds = new HashMap<>();
    private final Set<String> resolvedClientIds = new HashSet<>();
    private String realmName;
    private boolean locked;

    OrganizationRoleGraphGuard(KeycloakSession session, EntityManager em, String realmId) {
        this.session = session;
        this.em = em;
        this.realmId = realmId;
    }

    void lockRealm() {
        if (locked) {
            return;
        }

        RealmEntity realm = em.find(RealmEntity.class, realmId, LockModeType.PESSIMISTIC_WRITE);
        em.flush();
        realmName = realm.getName();
        locked = true;
    }

    void validateCompositeAddition(String parentRoleId, String childRoleId) {
        lockRealm();
        Map<String, RoleDetails> roles = getRoleDetails(Set.of(parentRoleId, childRoleId));
        RoleDetails parent = requireRole(roles, parentRoleId);
        RoleDetails child = requireRole(roles, childRoleId);
        requireSameRealm(parent);
        requireSameRealm(child);
        validateOrganizationComposite(parent, child);

        if (containsAdminRole(child.id())) {
            rejectIfOrganizationAnchored(Set.of(parent.id()));
        }
    }

    void validateOrganizationGroupGrant(String groupId, RoleModel role) {
        lockRealm();
        Map<String, RoleDetails> roles = getRoleDetails(Set.of(role.getId()));
        RoleDetails localRole = roles.get(role.getId());

        GroupDetails group = requireGroup(groupId);
        if (group.type() != GroupModel.Type.ORGANIZATION) {
            if (localRole != null && localRole.type() == RoleModel.Type.ORGANIZATION) {
                throw reject("Organization roles cannot be assigned to realm groups");
            }
            return;
        }

        OrganizationDetails organization = requireOrganization(group.organizationId());
        requireOrganizationGroupHierarchy(group, organization);

        // GROUP_ROLE_MAPPING intentionally supports IDs supplied by external role providers. Their mutable graph is
        // outside the JPA transaction; the model-level validation still checks their current state.
        if (localRole == null) {
            return;
        }

        requireSameRealm(localRole);
        if (group.id().equals(organization.groupId())) {
            if (localRole.type() != RoleModel.Type.ORGANIZATION
                    || !Objects.equals(localRole.organizationId(), organization.id())
                    || !Objects.equals(localRole.id(), organization.defaultRoleId())) {
                throw reject("The internal organization group can only be assigned its default role");
            }
        } else if (localRole.type() == RoleModel.Type.ORGANIZATION
                && (!Objects.equals(localRole.organizationId(), organization.id())
                        || Objects.equals(localRole.id(), organization.defaultRoleId()))) {
            throw reject("Organization groups can only be assigned ordinary roles from the same organization");
        }
        if (containsAdminRole(localRole.id())) {
            throw reject(UNSAFE_ROLE_GRAPH);
        }
    }

    void validateRoleNameChange(String roleId, String newName) {
        lockRealm();
        RoleDetails role = requireRole(getRoleDetails(Set.of(roleId)), roleId);
        requireSameRealm(role);

        if (!isAdminRole(role, realmName) && isAdminRole(role.withName(newName), realmName)) {
            rejectIfOrganizationAnchored(Set.of(role.id()));
        }
    }

    void validateClientIdChange(String clientEntityId, String newClientId) {
        lockRealm();
        String currentClientId = em.createQuery("select client.clientId from ClientEntity client where client.id = :clientId", String.class)
                .setParameter("clientId", clientEntityId)
                .getResultStream()
                .findFirst()
                .orElse(null);

        if (isAdminClient(realmName, currentClientId) || !isAdminClient(realmName, newClientId)) {
            return;
        }

        List<String> candidateIds = em.createQuery("select role.id from RoleEntity role where role.clientId = :clientId and role.name in :roleNames", String.class)
                .setParameter("clientId", clientEntityId)
                .setParameter("roleNames", AdminRoles.ALL_ROLES)
                .getResultList();
        rejectIfOrganizationAnchored(new LinkedHashSet<>(candidateIds));
    }

    void validateRealmNameChange(String newRealmName) {
        lockRealm();
        Map<String, RoleDetails> candidates = getRoleDetailsByRealmAndNames();
        Set<String> newlyAdministrative = new LinkedHashSet<>();

        for (RoleDetails role : candidates.values()) {
            if (!isAdminRole(role, realmName) && isAdminRole(role, newRealmName)) {
                newlyAdministrative.add(role.id());
            }
        }

        rejectIfOrganizationAnchored(newlyAdministrative);
    }

    private boolean containsAdminRole(String initialRoleId) {
        Set<String> visited = new HashSet<>();
        Set<String> currentLevel = Set.of(initialRoleId);

        while (!currentLevel.isEmpty()) {
            Map<String, RoleDetails> roles = getRoleDetails(currentLevel);
            for (String roleId : currentLevel) {
                RoleDetails role = requireRole(roles, roleId);
                requireSameRealm(role);
                if (isAdminRole(role, realmName)) {
                    return true;
                }
            }

            visited.addAll(currentLevel);
            currentLevel = em.createNamedQuery("getChildRoleIdsFromParentRoleIds", String.class)
                    .setParameter("parentRoleIds", currentLevel)
                    .getResultStream()
                    .filter(roleId -> !visited.contains(roleId))
                    .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
        }

        return false;
    }

    private void rejectIfOrganizationAnchored(Set<String> initialRoleIds) {
        Set<String> visited = new HashSet<>();
        Set<String> currentLevel = new LinkedHashSet<>(initialRoleIds);

        while (!currentLevel.isEmpty()) {
            Map<String, RoleDetails> roles = getRoleDetails(currentLevel);
            for (String roleId : currentLevel) {
                RoleDetails role = requireRole(roles, roleId);
                requireSameRealm(role);
                if (role.type() == RoleModel.Type.ORGANIZATION) {
                    throw reject(UNSAFE_ROLE_GRAPH);
                }
            }

            if (!em.createNamedQuery("getOrganizationAnchoredRoleIds", String.class)
                    .setParameter("roleIds", currentLevel)
                    .setParameter("organizationGroupType", GroupModel.Type.ORGANIZATION.intValue())
                    .setMaxResults(1)
                    .getResultList().isEmpty()) {
                throw reject(UNSAFE_ROLE_GRAPH);
            }

            visited.addAll(currentLevel);
            currentLevel = em.createNamedQuery("getParentRoleIdsFromChildRoleIds", String.class)
                    .setParameter("childRoleIds", currentLevel)
                    .getResultStream()
                    .filter(roleId -> !visited.contains(roleId))
                    .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
        }
    }

    private Map<String, RoleDetails> getRoleDetails(Collection<String> roleIds) {
        Map<String, RoleDetails> roles = new HashMap<>();
        for (Object[] row : em.createNamedQuery("getRoleDetailsByIds", Object[].class)
                .setParameter("roleIds", roleIds)
                .getResultList()) {
            RoleDetails role = toRoleDetails(row);
            roles.put(role.id(), role);
        }
        loadClientIds(roles.values());
        return roles;
    }

    private Map<String, RoleDetails> getRoleDetailsByRealmAndNames() {
        Map<String, RoleDetails> roles = new HashMap<>();
        for (Object[] row : em.createNamedQuery("getRoleDetailsByRealmAndNames", Object[].class)
                .setParameter("realmId", realmId)
                .setParameter("roleNames", AdminRoles.ALL_ROLES)
                .getResultList()) {
            RoleDetails role = toRoleDetails(row);
            roles.put(role.id(), role);
        }
        loadClientIds(roles.values());
        return roles;
    }

    private RoleDetails toRoleDetails(Object[] row) {
        return new RoleDetails((String) row[0], (String) row[1], RoleModel.Type.valueOf((String) row[2]),
                (String) row[3], (String) row[4], (String) row[5]);
    }

    private void validateOrganizationComposite(RoleDetails parent, RoleDetails child) {
        if (child.type() != RoleModel.Type.ORGANIZATION) {
            return;
        }
        if (parent.type() != RoleModel.Type.ORGANIZATION
                || !Objects.equals(parent.organizationId(), child.organizationId())) {
            throw reject("Organization roles can only be added as composites to other organization roles");
        }
        OrganizationDetails organization = requireOrganization(child.organizationId());
        if (Objects.equals(child.id(), organization.defaultRoleId())) {
            throw reject("The default organization role cannot be added as a composite role");
        }
    }

    private GroupDetails requireGroup(String groupId) {
        List<Object[]> rows = em.createQuery("select g.id, g.type, g.realm, organization.id, g.parentId "
                        + "from GroupEntity g left join g.organization organization where g.id = :groupId", Object[].class)
                .setParameter("groupId", groupId)
                .setMaxResults(1)
                .getResultList();
        if (rows.isEmpty()) throw reject("Group does not exist in local storage");
        Object[] row = rows.get(0);
        return new GroupDetails((String) row[0], GroupModel.Type.valueOf((Integer) row[1]), (String) row[2],
                (String) row[3], (String) row[4]);
    }

    private OrganizationDetails requireOrganization(String organizationId) {
        if (organizationId == null) {
            throw reject("Organization group must belong to an organization");
        }
        List<Object[]> rows = em.createQuery("select organization.id, organization.realmId, organization.groupId, organization.defaultRoleId "
                        + "from OrganizationEntity organization where organization.id = :organizationId", Object[].class)
                .setParameter("organizationId", organizationId)
                .setMaxResults(1)
                .getResultList();
        if (rows.isEmpty()) throw reject("Organization group owner does not exist");
        Object[] row = rows.get(0);
        OrganizationDetails organization = new OrganizationDetails((String) row[0], (String) row[1], (String) row[2],
                (String) row[3]);
        if (!realmId.equals(organization.realmId())) {
            throw reject("Organization group belongs to a different realm");
        }
        return organization;
    }

    private void requireOrganizationGroupHierarchy(GroupDetails group, OrganizationDetails organization) {
        Set<String> visited = new HashSet<>();
        GroupDetails current = group;
        while (visited.add(current.id())) {
            if (current.type() != GroupModel.Type.ORGANIZATION || !realmId.equals(current.realmId())
                    || !organization.id().equals(current.organizationId())) break;
            if (current.id().equals(organization.groupId())) {
                if (!GroupEntity.TOP_PARENT_ID.equals(current.parentId())) {
                    break;
                }
                return;
            }
            if (current.parentId() == null || GroupEntity.TOP_PARENT_ID.equals(current.parentId())) {
                break;
            }
            current = requireGroup(current.parentId());
        }
        throw reject("Organization group is not anchored to its internal organization group");
    }

    private void loadClientIds(Collection<RoleDetails> roles) {
        Set<String> unresolved = roles.stream()
                .filter(role -> role.type() == RoleModel.Type.CLIENT)
                .map(RoleDetails::containerId)
                .filter(Objects::nonNull)
                .filter(clientId -> !resolvedClientIds.contains(clientId))
                .collect(java.util.stream.Collectors.toSet());
        if (unresolved.isEmpty()) {
            return;
        }

        for (Object[] row : em.createQuery("select client.id, client.clientId from ClientEntity client where client.id in :clientIds", Object[].class)
                .setParameter("clientIds", unresolved)
                .getResultList()) {
            clientIds.put((String) row[0], (String) row[1]);
        }
        resolvedClientIds.addAll(unresolved);
    }

    private RoleDetails requireRole(Map<String, RoleDetails> roles, String roleId) {
        RoleDetails role = roles.get(roleId);
        if (role == null) {
            throw reject("Role does not exist in local storage");
        }
        return role;
    }

    private void requireSameRealm(RoleDetails role) {
        if (!realmId.equals(role.realmId())) {
            throw reject("Composite roles and organization group mappings must stay within the same realm");
        }
    }

    private boolean isAdminRole(RoleDetails role, String effectiveRealmName) {
        if (!AdminRoles.ALL_ROLES.contains(role.name())) {
            return false;
        }

        return switch (role.type()) {
            case REALM -> Config.getAdminRealm().equals(effectiveRealmName);
            case CLIENT -> isAdminClient(effectiveRealmName, clientIds.get(role.containerId()));
            case ORGANIZATION -> false;
        };
    }

    private boolean isAdminClient(String effectiveRealmName, String clientId) {
        return clientId != null && (Constants.REALM_MANAGEMENT_CLIENT_ID.equals(clientId)
                || Config.getAdminRealm().equals(effectiveRealmName) && clientId.endsWith(AdminRoles.APP_SUFFIX));
    }

    private ModelValidationException reject(String message) {
        if (session.getTransactionManager().isActive()) {
            session.getTransactionManager().setRollbackOnly();
        }
        return new ModelValidationException(message);
    }

    private record RoleDetails(String id, String name, RoleModel.Type type, String containerId, String realmId,
            String organizationId) {
        private RoleDetails withName(String newName) {
            return new RoleDetails(id, newName, type, containerId, realmId, organizationId);
        }
    }

    private record GroupDetails(String id, GroupModel.Type type, String realmId, String organizationId, String parentId) {
    }

    private record OrganizationDetails(String id, String realmId, String groupId, String defaultRoleId) {
    }
}

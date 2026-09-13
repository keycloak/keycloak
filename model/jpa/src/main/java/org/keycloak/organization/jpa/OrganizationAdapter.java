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

package org.keycloak.organization.jpa;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;

import org.keycloak.connections.jpa.JpaConnectionProvider;
import org.keycloak.models.GroupModel;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ModelValidationException;
import org.keycloak.models.OrganizationDomainModel;
import org.keycloak.models.OrganizationModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.jpa.JpaModel;
import org.keycloak.models.jpa.entities.GroupEntity;
import org.keycloak.models.jpa.entities.GroupRoleMappingEntity;
import org.keycloak.models.jpa.entities.IdentityProviderEntity;
import org.keycloak.models.jpa.entities.OrganizationDomainEntity;
import org.keycloak.models.jpa.entities.OrganizationEntity;
import org.keycloak.models.jpa.entities.RealmEntity;
import org.keycloak.models.jpa.entities.RoleEntity;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.organization.OrganizationProvider;
import org.keycloak.organization.utils.Organizations;
import org.keycloak.utils.StringUtil;

import static java.util.Optional.ofNullable;

/** JPA-backed organization model. */
public final class OrganizationAdapter implements OrganizationModel, JpaModel<OrganizationEntity> {

    private final KeycloakSession session;
    private final RealmModel realm;
    private OrganizationEntity entity;
    private final OrganizationProvider provider;
    private GroupModel group;
    private Map<String, List<String>> attributes;

    public OrganizationAdapter(KeycloakSession session, RealmModel realm, OrganizationEntity entity, OrganizationProvider provider) {
        this.session = session;
        this.realm = realm;
        this.entity = entity;
        this.provider = provider;
    }

    @Override
    public String getId() {
        return entity.getId();
    }

    @Override
    public RealmModel getRealm() {
        return realm;
    }

    public String getGroupId() {
        return entity.getGroupId();
    }

    void setGroupId(String id) {
        entity.setGroupId(id);
    }

    @Override
    public RoleModel getDefaultRole() {
        if (entity.getDefaultRoleId() == null) {
            return null;
        }
        return session.roles().getRoleInContainerById(this, entity.getDefaultRoleId());
    }

    @Override
    public void setDefaultRole(RoleModel role) {
        if (role == null) {
            throw new ModelValidationException("Default organization role cannot be null");
        }

        OrganizationModel current = session.getContext().getOrganization();
        session.getContext().setOrganization(this);
        try {
            replaceDefaultRole(role);
        } catch (RuntimeException cause) {
            markRollback();
            throw cause;
        } finally {
            session.getContext().setOrganization(current);
        }
    }

    void clearDefaultRoleForRemoval() {
        OrganizationModel current = session.getContext().getOrganization();
        session.getContext().setOrganization(this);
        try {
            EntityManager em = getEntityManager();
            lockRealm(em);
            OrganizationEntity locked = lockOrganization(em);
            GroupEntity root = requireOrganizationRoot(em, locked);
            List<String> mappings = getRootRoleMappings(em, root);
            String previousRoleId = locked.getDefaultRoleId();

            if (previousRoleId == null) {
                if (!mappings.isEmpty()) {
                    throw reject("Internal organization group has role mappings without a default role");
                }
                return;
            }
            if (mappings.size() != 1 || !previousRoleId.equals(mappings.get(0))) {
                throw reject("Internal organization group has role mappings other than the default role");
            }

            RoleModel previousRole = session.roles().getRoleInContainerById(this, previousRoleId);
            if (previousRole == null) {
                throw reject("Default organization role does not exist");
            }

            locked.setDefaultRoleId(null);
            em.flush();
            requireRootModel(root).deleteRoleMapping(previousRole);
        } catch (RuntimeException cause) {
            markRollback();
            throw cause;
        } finally {
            session.getContext().setOrganization(current);
        }
    }

    @Override
    public RoleModel getRole(String name) {
        return session.roles().getRole(this, name);
    }

    @Override
    public RoleModel addRole(String name) {
        return addRole(null, name);
    }

    @Override
    public RoleModel addRole(String id, String name) {
        return session.roles().addRole(this, id, name);
    }

    @Override
    public boolean removeRole(RoleModel role) {
        return session.roles().removeRole(role);
    }

    @Override
    public Stream<RoleModel> getRolesStream() {
        return session.roles().getRolesStream(this);
    }

    @Override
    public Stream<RoleModel> getRolesStream(Integer firstResult, Integer maxResults) {
        return session.roles().getRolesStream(this, firstResult, maxResults);
    }

    @Override
    public Stream<RoleModel> searchForRolesStream(String search, Integer first, Integer max) {
        return session.roles().searchForRolesStream(this, search, first, max);
    }

    @Override
    public void setName(String name) {
        entity.setName(name);
    }

    @Override
    public String getName() {
        return entity.getName();
    }

    @Override
    public String getAlias() {
        return entity.getAlias();
    }

    @Override
    public void setAlias(String alias) {
        if (StringUtil.isBlank(alias)) {
            alias = getName();
        }
        if (alias.equals(entity.getAlias())) {
            return;
        }
        if (StringUtil.isNotBlank(entity.getAlias())) {
            throw new ModelValidationException("Cannot change the alias");
        }
        entity.setAlias(alias);
    }

    @Override
    public boolean isEnabled() {
        return provider.isEnabled() && entity.isEnabled();
    }

    @Override
    public void setEnabled(boolean enabled) {
        entity.setEnabled(enabled);
    }

    @Override
    public String getDescription() {
        return entity.getDescription();
    }

    @Override
    public void setDescription(String description) {
        entity.setDescription(description);
    }

    @Override
    public String getRedirectUrl() {
        return entity.getRedirectUrl();
    }

    @Override
    public void setRedirectUrl(String redirectUrl) {
        entity.setRedirectUrl(redirectUrl);
    }

    @Override
    public void setAttributes(Map<String, List<String>> attributes) {
        if (attributes == null) {
            return;
        }

        // add organization to the session as the following code updates the underlying group
        OrganizationModel current = session.getContext().getOrganization();
        if (current == null) {
            session.getContext().setOrganization(this);
        }

        try {
            // getAttributes() can expose the group's shared cached attribute map; work off a
            // copy so we don't structurally modify its live keySet while concurrent requests
            // read it, which throws ConcurrentModificationException.
            Set<String> attrsToRemove = new HashSet<>(getAttributes().keySet());
            attrsToRemove.removeAll(attributes.keySet());
            attrsToRemove.forEach(group::removeAttribute);
            attributes.forEach(group::setAttribute);
        } finally {
            if (current == null) {
                session.getContext().setOrganization(null);
            }
        }
    }

    @Override
    public Map<String, List<String>> getAttributes() {
        if (attributes == null) {
            attributes = ofNullable(getGroup().getAttributes()).orElse(Map.of());
        }
        return attributes;
    }

    @Override
    public Stream<OrganizationDomainModel> getDomains() {
        return entity.getDomains().stream().map(this::toModel);
    }

    @Override
    public void setDomains(Set<OrganizationDomainModel> domains) {
        if (domains == null) {
            return;
        }

        jakarta.persistence.EntityManager em = session.getProvider(JpaConnectionProvider.class).getEntityManager();

        Map<String, OrganizationDomainModel> modelMap = domains.stream()
                .map(this::validateDomain)
                .collect(Collectors.toMap(OrganizationDomainModel::getName, Function.identity()));

        for (OrganizationDomainEntity domainEntity : new HashSet<>(this.entity.getDomains())) {
            if (!modelMap.containsKey(domainEntity.getName())) {
                this.entity.removeDomain(domainEntity);
                domainEntity.setIdentityProvider(null);
                em.remove(domainEntity);
            } else {
                OrganizationDomainModel updated = modelMap.remove(domainEntity.getName());
                domainEntity.setVerified(updated.isVerified());
                domainEntity.setIdentityProvider(resolveIdentityProvider(em, updated.getIdentityProviderAlias()));
                domainEntity.setAutoRedirect(updated.isAutoRedirect());
            }
        }

        // claim-or-create: for new domains in the set
        for (OrganizationDomainModel model : modelMap.values()) {
            OrganizationDomainEntity domainEntity;
            try {
                domainEntity = em.createNamedQuery("getDomainByRealmAndName", OrganizationDomainEntity.class)
                        .setParameter("realmId", realm.getId())
                        .setParameter("name", model.getName())
                        .getSingleResult();
                // domain already exists — just claim it, do not overwrite global properties
            } catch (jakarta.persistence.NoResultException e) {
                // domain doesn't exist — create it with provided properties
                domainEntity = new OrganizationDomainEntity();
                domainEntity.setId(KeycloakModelUtils.generateId());
                domainEntity.setName(model.getName());
                domainEntity.setVerified(model.isVerified());
                domainEntity.setRealmId(realm.getId());
                domainEntity.setIdentityProvider(resolveIdentityProvider(em, model.getIdentityProviderAlias()));
                domainEntity.setAutoRedirect(model.isAutoRedirect());
            }
            this.entity.addDomain(domainEntity);
        }
    }

    @Override
    public Stream<IdentityProviderModel> getIdentityProviders() {
        return provider.getIdentityProviders(this);
    }

    @Override
    public boolean isManaged(UserModel user) {
        return provider.isManagedMember(this, user);
    }

    @Override
    public boolean isMember(UserModel user) {
        return provider.isMember(this, user);
    }

    @Override
    public OrganizationEntity getEntity() {
        return entity;
    }

    @Override
    public String toString() {
        return new StringBuilder()
                .append("id=")
                .append(getId())
                .append(",")
                .append("name=")
                .append(getName())
                .append(",")
                .append("realm=")
                .append(getRealm().getName())
                .append(",")
                .append("groupId=")
                .append(getGroupId()).toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof OrganizationModel)) return false;

        OrganizationModel that = (OrganizationModel) o;
        return that.getId().equals(getId());
    }

    @Override
    public int hashCode() {
        return getId().hashCode();
    }

    private OrganizationDomainModel toModel(OrganizationDomainEntity entity) {
        IdentityProviderEntity idp = entity.getIdentityProvider();
        String alias = idp != null ? idp.getAlias() : null;
        return new OrganizationDomainModel(entity.getName(), entity.isVerified(), alias, entity.isAutoRedirect());
    }

    /**
     * Validates the domain. Specifically, the method first checks if the specified domain is valid,
     * and then checks if the domain is not already linked to a different organization.
     *
     * @param domainModel the {@link OrganizationDomainModel} representing the domain being added.
     * @throws {@link ModelValidationException} if the domain is invalid or is already linked to a different organization.
     */
    private OrganizationDomainModel validateDomain(OrganizationDomainModel domainModel) {
        String domainName = domainModel.getName();

        if (StringUtil.isBlank(domainName)) {
            throw new ModelValidationException("Domain name cannot be empty");
        }

        Organizations.validateDomain(domainName);

        // Check for conflicts with other organizations
        OrganizationModel orgModel = provider.getByDomainName(domainName);

        if (orgModel != null && !Objects.equals(getId(), orgModel.getId())
                && orgModel.getDomains().anyMatch(d -> d.getName().equalsIgnoreCase(domainName))) {
            throw new ModelValidationException("Domain " + domainName + " is already linked to organization " + orgModel.getName() + " in realm " + realm.getName());
        }

        return domainModel;
    }

    private IdentityProviderEntity resolveIdentityProvider(EntityManager em, String alias) {
        if (alias == null) {
            return null;
        }
        IdentityProviderModel idpModel = session.identityProviders().getByAlias(alias);
        if (idpModel == null) {
            throw new ModelValidationException("Identity provider with alias '" + alias + "' does not exist in realm " + realm.getName());
        }
        String internalId = idpModel.getInternalId();
        boolean linked = entity.getIdentityProviderLinks().stream()
                .anyMatch(link -> internalId.equals(link.getIdentityProviderId()));
        if (!linked) {
            throw new ModelValidationException("Identity provider '" + alias + "' is not associated with organization " + getName());
        }
        return em.getReference(IdentityProviderEntity.class, internalId);
    }

    private GroupModel getGroup() {
        if (group == null) {
            group = realm.getGroupById(getGroupId());
        }
        return group;
    }

    private void replaceDefaultRole(RoleModel candidate) {
        EntityManager em = getEntityManager();
        lockRealm(em);
        OrganizationEntity locked = lockOrganization(em);
        RoleEntity candidateEntity = requireCandidate(em, locked, candidate);
        GroupEntity root = requireOrganizationRoot(em, locked);
        List<String> mappings = getRootRoleMappings(em, root);
        String previousRoleId = locked.getDefaultRoleId();
        boolean sameRole = Objects.equals(previousRoleId, candidateEntity.getId());

        validateCurrentDefault(em, locked, previousRoleId, mappings, sameRole);
        if (hasDirectUserMapping(em, candidateEntity.getId())) {
            throw reject("A role with direct user mappings cannot become the default organization role");
        }
        if (hasGroupMappingOutsideRoot(em, candidateEntity.getId(), root.getId())) {
            throw reject("A role mapped to another group cannot become the default organization role");
        }
        if (hasIncomingComposite(em, candidateEntity.getId())) {
            throw reject("A role used as a composite child cannot become the default organization role");
        }

        if (sameRole && mappings.size() == 1) {
            return;
        }

        RoleModel authoritativeCandidate = session.roles().getRoleInContainerById(this, candidateEntity.getId());
        if (authoritativeCandidate == null) {
            throw reject("Default organization role does not exist");
        }

        RoleModel previousRole = previousRoleId == null || sameRole ? null
                : session.roles().getRoleInContainerById(this, previousRoleId);
        if (previousRoleId != null && !sameRole && previousRole == null) {
            throw reject("Current default organization role does not exist");
        }

        locked.setDefaultRoleId(candidateEntity.getId());
        em.flush();

        GroupModel rootModel = requireRootModel(root);
        if (mappings.isEmpty() || !sameRole) {
            rootModel.grantRole(authoritativeCandidate);
        }
        if (previousRole != null) {
            rootModel.deleteRoleMapping(previousRole);
        }

        List<String> result = getRootRoleMappings(em, root);
        if (result.size() != 1 || !candidateEntity.getId().equals(result.get(0))) {
            throw reject("Unable to establish the default role mapping on the internal organization group");
        }
    }

    private EntityManager getEntityManager() {
        return session.getProvider(JpaConnectionProvider.class).getEntityManager();
    }

    private void lockRealm(EntityManager em) {
        RealmEntity realmEntity = em.find(RealmEntity.class, realm.getId(), LockModeType.PESSIMISTIC_WRITE);
        if (realmEntity == null) {
            throw reject("Organization realm does not exist");
        }
        em.flush();
    }

    private OrganizationEntity lockOrganization(EntityManager em) {
        OrganizationEntity locked = em.find(OrganizationEntity.class, getId(), LockModeType.PESSIMISTIC_WRITE);
        if (locked == null || !realm.getId().equals(locked.getRealmId())) {
            throw reject("Organization does not exist in the current realm");
        }
        em.refresh(locked, LockModeType.PESSIMISTIC_WRITE);
        entity = locked;
        return locked;
    }

    private RoleEntity requireCandidate(EntityManager em, OrganizationEntity organization, RoleModel candidate) {
        RoleEntity candidateEntity = candidate == null ? null : em.find(RoleEntity.class, candidate.getId());
        if (candidateEntity == null || candidateEntity.getType() != RoleModel.Type.ORGANIZATION
                || !organization.getId().equals(candidateEntity.getOrganizationId())
                || !organization.getRealmId().equals(candidateEntity.getRealmId())) {
            throw reject("Default role must belong to the organization and realm");
        }
        return candidateEntity;
    }

    private GroupEntity requireOrganizationRoot(EntityManager em, OrganizationEntity organization) {
        GroupEntity root = organization.getGroupId() == null ? null
                : em.find(GroupEntity.class, organization.getGroupId(), LockModeType.PESSIMISTIC_WRITE);
        if (root == null || root.getType() != GroupModel.Type.ORGANIZATION.intValue()
                || !organization.getRealmId().equals(root.getRealm())
                || root.getOrganization() == null || !organization.getId().equals(root.getOrganization().getId())
                || !GroupEntity.TOP_PARENT_ID.equals(root.getParentId())) {
            throw reject("Invalid internal organization group");
        }
        return root;
    }

    private List<String> getRootRoleMappings(EntityManager em, GroupEntity root) {
        return em.createNamedQuery("groupRoleMappings", GroupRoleMappingEntity.class)
                .setParameter("group", root)
                .setLockMode(LockModeType.PESSIMISTIC_WRITE)
                .getResultStream()
                .map(GroupRoleMappingEntity::getRoleId)
                .toList();
    }

    private void validateCurrentDefault(EntityManager em, OrganizationEntity organization, String previousRoleId,
            List<String> mappings, boolean sameRole) {
        if (previousRoleId == null) {
            if (!mappings.isEmpty()) {
                throw reject("Internal organization group has role mappings without a default role");
            }
            return;
        }

        RoleEntity previousRole = em.find(RoleEntity.class, previousRoleId);
        if (previousRole == null || previousRole.getType() != RoleModel.Type.ORGANIZATION
                || !organization.getId().equals(previousRole.getOrganizationId())
                || !organization.getRealmId().equals(previousRole.getRealmId())) {
            throw reject("Current default organization role is invalid");
        }

        if (mappings.isEmpty()) {
            if (!sameRole) {
                throw reject("Current default organization role mapping is missing");
            }
            return;
        }
        if (mappings.size() != 1 || !previousRoleId.equals(mappings.get(0))) {
            throw reject("Internal organization group has role mappings other than the default role");
        }
    }

    private boolean hasDirectUserMapping(EntityManager em, String roleId) {
        boolean local = !em.createQuery("select mapping.roleId from UserRoleMappingEntity mapping where mapping.roleId = :roleId", String.class)
                .setParameter("roleId", roleId)
                .setMaxResults(1)
                .getResultList()
                .isEmpty();
        if (local) {
            return true;
        }
        return !em.createQuery("select mapping.roleId from FederatedUserRoleMappingEntity mapping where mapping.roleId = :roleId", String.class)
                .setParameter("roleId", roleId)
                .setMaxResults(1)
                .getResultList()
                .isEmpty();
    }

    private boolean hasGroupMappingOutsideRoot(EntityManager em, String roleId, String rootId) {
        return !em.createQuery("select mapping.roleId from GroupRoleMappingEntity mapping "
                        + "where mapping.roleId = :roleId and mapping.group.id <> :rootId", String.class)
                .setParameter("roleId", roleId)
                .setParameter("rootId", rootId)
                .setMaxResults(1)
                .getResultList()
                .isEmpty();
    }

    private boolean hasIncomingComposite(EntityManager em, String roleId) {
        return !em.createQuery("select composite.childRole.id from CompositeRoleEntity composite "
                        + "where composite.childRole.id = :roleId", String.class)
                .setParameter("roleId", roleId)
                .setMaxResults(1)
                .getResultList()
                .isEmpty();
    }

    private GroupModel requireRootModel(GroupEntity root) {
        GroupModel rootModel = realm.getGroupById(root.getId());
        if (rootModel == null) {
            throw reject("Internal organization group does not exist");
        }
        return rootModel;
    }

    private ModelValidationException reject(String message) {
        markRollback();
        return new ModelValidationException(message);
    }

    private void markRollback() {
        if (session.getTransactionManager().isActive()) {
            session.getTransactionManager().setRollbackOnly();
        }
    }
}

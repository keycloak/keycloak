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
import org.keycloak.connections.jpa.JpaConnectionProvider;
import org.keycloak.models.GroupModel;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ModelValidationException;
import org.keycloak.models.OrganizationDomainModel;
import org.keycloak.models.OrganizationModel;
import org.keycloak.models.OrganizationRoleModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.jpa.JpaModel;
import org.keycloak.models.jpa.entities.OrganizationDomainEntity;
import org.keycloak.models.jpa.entities.OrganizationEntity;
import org.keycloak.models.jpa.entities.OrganizationRoleEntity;
import org.keycloak.models.jpa.entities.UserEntity;
import org.keycloak.models.jpa.entities.UserOrganizationRoleMappingEntity;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.organization.OrganizationProvider;
import org.keycloak.utils.EmailValidationUtil;
import org.keycloak.utils.StringUtil;

import static java.util.Optional.ofNullable;

public final class OrganizationAdapter implements OrganizationModel, JpaModel<OrganizationEntity> {

    private final KeycloakSession session;
    private final RealmModel realm;
    private final OrganizationEntity entity;
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

    RealmModel getRealm() {
        return realm;
    }

    public String getGroupId() {
        return entity.getGroupId();
    }

    void setGroupId(String id) {
        entity.setGroupId(id);
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
            Set<String> attrsToRemove = getAttributes().keySet();
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

        Map<String, OrganizationDomainModel> modelMap = domains.stream()
                .map(this::validateDomain)
                .collect(Collectors.toMap(OrganizationDomainModel::getName, Function.identity()));

        for (OrganizationDomainEntity domainEntity : new HashSet<>(this.entity.getDomains())) {
            // update the existing domain (for now, only the verified flag can be changed).
            if (modelMap.containsKey(domainEntity.getName())) {
                domainEntity.setVerified(modelMap.get(domainEntity.getName()).isVerified());
                modelMap.remove(domainEntity.getName());
            } else {
                // remove domain that is not found in the new set.
                this.entity.removeDomain(domainEntity);
                // check if any idp is assigned to the removed domain, and unset the domain if that's the case.
                getIdentityProviders()
                        .filter(idp -> Objects.equals(domainEntity.getName(), idp.getConfig().get(ORGANIZATION_DOMAIN_ATTRIBUTE)))
                        .forEach(idp -> {
                            idp.getConfig().remove(ORGANIZATION_DOMAIN_ATTRIBUTE);
                            session.identityProviders().update(idp);
                        });
            }
        }

        // create the remaining domains.
        for (OrganizationDomainModel model : modelMap.values()) {
            OrganizationDomainEntity domainEntity = new OrganizationDomainEntity();
            domainEntity.setId(KeycloakModelUtils.generateId());
            domainEntity.setName(model.getName());
            domainEntity.setVerified(model.isVerified());
            domainEntity.setOrganization(this.entity);
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
        return new OrganizationDomainModel(entity.getName(), entity.isVerified());
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

        // we rely on the same validation util used by the EmailValidator to ensure the domain part is consistently validated.
        if (StringUtil.isBlank(domainName) || !EmailValidationUtil.isValidEmail("nouser@" + domainName)) {
            throw new ModelValidationException("The specified domain is invalid: " + domainName);
        }
        OrganizationModel orgModel = provider.getByDomainName(domainName);
        if (orgModel != null && !Objects.equals(getId(), orgModel.getId())) {
            throw new ModelValidationException("Domain " + domainName + " is already linked to another organization in realm " + realm.getName());
        }
        return domainModel;
    }

    @Override
    public OrganizationRoleModel addRole(String name) {
        return addRole(null, name);
    }

    @Override
    public OrganizationRoleModel addRole(String id, String name) {
        if (StringUtil.isBlank(name)) {
            throw new ModelValidationException("Role name cannot be null or empty");
        }

        if (getRole(name) != null) {
            throw new ModelValidationException("Role with name '" + name + "' already exists in organization");
        }

        EntityManager em = session.getProvider(JpaConnectionProvider.class).getEntityManager();
        
        OrganizationRoleEntity entity = new OrganizationRoleEntity();
        entity.setId(id != null ? id : KeycloakModelUtils.generateId());
        entity.setName(name);
        entity.setOrganization(this.entity);
        
        em.persist(entity);
        em.flush();

        return new OrganizationRoleAdapter(session, realm, this, entity);
    }

    @Override
    public OrganizationRoleModel getRole(String name) {
        if (StringUtil.isBlank(name)) {
            return null;
        }

        EntityManager em = session.getProvider(JpaConnectionProvider.class).getEntityManager();
        
        try {
            OrganizationRoleEntity entity = em.createNamedQuery("organizationRoleByName", OrganizationRoleEntity.class)
                    .setParameter("organizationId", getId())
                    .setParameter("name", name)
                    .getSingleResult();
            return new OrganizationRoleAdapter(session, realm, this, entity);
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public OrganizationRoleModel getRoleById(String id) {
        if (StringUtil.isBlank(id)) {
            return null;
        }

        EntityManager em = session.getProvider(JpaConnectionProvider.class).getEntityManager();
        
        try {
            OrganizationRoleEntity entity = em.createNamedQuery("organizationRoleById", OrganizationRoleEntity.class)
                    .setParameter("id", id)
                    .getSingleResult();
            
            // Verify the role belongs to this organization
            if (!Objects.equals(entity.getOrganization().getId(), getId())) {
                return null;
            }
            
            return new OrganizationRoleAdapter(session, realm, this, entity);
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public boolean removeRole(OrganizationRoleModel role) {
        if (role == null || !Objects.equals(role.getOrganization().getId(), getId())) {
            return false;
        }

        EntityManager em = session.getProvider(JpaConnectionProvider.class).getEntityManager();
        
        try {
            // Remove all composite role relationships
            em.createNamedQuery("deleteOrganizationRoleCompositesByComposite")
                    .setParameter("composite", role.getId())
                    .executeUpdate();
            
            em.createNamedQuery("deleteOrganizationRoleCompositesByChild")
                    .setParameter("childRole", role.getId())
                    .executeUpdate();

            // Remove all user-role mappings
            em.createNamedQuery("deleteUserOrganizationRoleMappingsByRole")
                    .setParameter("organizationRoleId", role.getId())
                    .executeUpdate();

            // Remove all role attributes
            em.createNamedQuery("deleteOrganizationRoleAttributesByRole")
                    .setParameter("organizationRoleId", role.getId())
                    .executeUpdate();

            // Remove the role itself
            OrganizationRoleEntity entity = em.find(OrganizationRoleEntity.class, role.getId());
            if (entity != null) {
                em.remove(entity);
                return true;
            }
        } catch (Exception e) {
            // Log error if needed
        }
        
        return false;
    }

    @Override
    public Stream<OrganizationRoleModel> getRolesStream() {
        return getRolesStream(null, null);
    }

    @Override
    public Stream<OrganizationRoleModel> getRolesStream(Integer firstResult, Integer maxResults) {
        EntityManager em = session.getProvider(JpaConnectionProvider.class).getEntityManager();
        
        var query = em.createNamedQuery("organizationRolesByOrganization", OrganizationRoleEntity.class)
                .setParameter("organizationId", getId());
        
        if (firstResult != null && firstResult >= 0) {
            query.setFirstResult(firstResult);
        }
        
        if (maxResults != null && maxResults >= 0) {
            query.setMaxResults(maxResults);
        }
        
        return query.getResultList()
                .stream()
                .map(entity -> new OrganizationRoleAdapter(session, realm, this, entity));
    }

    @Override
    public Stream<OrganizationRoleModel> searchForRolesStream(String search, Integer first, Integer max) {
        if (StringUtil.isBlank(search)) {
            return getRolesStream(first, max);
        }

        EntityManager em = session.getProvider(JpaConnectionProvider.class).getEntityManager();
        
        var query = em.createNamedQuery("organizationRoleByNameContaining", OrganizationRoleEntity.class)
                .setParameter("organizationId", getId())
                .setParameter("search", search);
        
        if (first != null && first >= 0) {
            query.setFirstResult(first);
        }
        
        if (max != null && max >= 0) {
            query.setMaxResults(max);
        }
        
        return query.getResultList()
                .stream()
                .map(entity -> new OrganizationRoleAdapter(session, realm, this, entity));
    }

    @Override
    public void grantRole(UserModel user, OrganizationRoleModel role) {
        if (user == null || role == null || !Objects.equals(role.getOrganization().getId(), getId())) {
            throw new ModelValidationException("User and role must be valid and role must belong to this organization");
        }

        // Check if user is a member of this organization
        if (!isMember(user)) {
            throw new ModelValidationException("User must be a member of the organization to be granted a role");
        }

        // Check if mapping already exists
        if (hasRole(user, role)) {
            return; // Already has the role
        }

        EntityManager em = session.getProvider(JpaConnectionProvider.class).getEntityManager();
        
        UserOrganizationRoleMappingEntity mapping = new UserOrganizationRoleMappingEntity();
        mapping.setId(KeycloakModelUtils.generateId());
        mapping.setUser(em.getReference(UserEntity.class, user.getId()));
        mapping.setOrganizationRole(em.getReference(OrganizationRoleEntity.class, role.getId()));
        
        em.persist(mapping);
    }

    @Override
    public void revokeRole(UserModel user, OrganizationRoleModel role) {
        if (user == null || role == null || !Objects.equals(role.getOrganization().getId(), getId())) {
            return;
        }

        EntityManager em = session.getProvider(JpaConnectionProvider.class).getEntityManager();
        
        try {
            UserOrganizationRoleMappingEntity mapping = em.createNamedQuery("userOrganizationRoleMappingByUserAndRole", UserOrganizationRoleMappingEntity.class)
                    .setParameter("userId", user.getId())
                    .setParameter("organizationRoleId", role.getId())
                    .getSingleResult();
            
            em.remove(mapping);
        } catch (Exception e) {
            // Mapping doesn't exist, nothing to remove
        }
    }

    @Override
    public Stream<OrganizationRoleModel> getUserRolesStream(UserModel user) {
        if (user == null) {
            return Stream.empty();
        }

        EntityManager em = session.getProvider(JpaConnectionProvider.class).getEntityManager();
        
        return em.createNamedQuery("userOrganizationRoleMappingsByUserAndOrganization", UserOrganizationRoleMappingEntity.class)
                .setParameter("userId", user.getId())
                .setParameter("organizationId", getId())
                .getResultList()
                .stream()
                .map(mapping -> new OrganizationRoleAdapter(session, realm, this, mapping.getOrganizationRole()));
    }

    @Override
    public boolean hasRole(UserModel user, OrganizationRoleModel role) {
        if (user == null || role == null || !Objects.equals(role.getOrganization().getId(), getId())) {
            return false;
        }

        EntityManager em = session.getProvider(JpaConnectionProvider.class).getEntityManager();
        
        try {
            em.createNamedQuery("userOrganizationRoleMappingByUserAndRole", UserOrganizationRoleMappingEntity.class)
                    .setParameter("userId", user.getId())
                    .setParameter("organizationRoleId", role.getId())
                    .getSingleResult();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public Stream<UserModel> getRoleMembersStream(OrganizationRoleModel role) {
        return getRoleMembersStream(role, null, null);
    }

    @Override
    public Stream<UserModel> getRoleMembersStream(OrganizationRoleModel role, Integer firstResult, Integer maxResults) {
        if (role == null || !Objects.equals(role.getOrganization().getId(), getId())) {
            return Stream.empty();
        }

        EntityManager em = session.getProvider(JpaConnectionProvider.class).getEntityManager();
        
        var query = em.createNamedQuery("userOrganizationRoleMappingsByRole", UserOrganizationRoleMappingEntity.class)
                .setParameter("organizationRoleId", role.getId());
        
        if (firstResult != null && firstResult >= 0) {
            query.setFirstResult(firstResult);
        }
        
        if (maxResults != null && maxResults >= 0) {
            query.setMaxResults(maxResults);
        }
        
        return query.getResultList()
                .stream()
                .map(mapping -> session.users().getUserById(realm, mapping.getUser().getId()))
                .filter(Objects::nonNull);
    }

    private GroupModel getGroup() {
        if (group == null) {
            group = realm.getGroupById(getGroupId());
        }
        return group;
    }
}

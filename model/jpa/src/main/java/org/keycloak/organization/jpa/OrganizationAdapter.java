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
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.jpa.JpaModel;
import org.keycloak.models.jpa.entities.IdentityProviderEntity;
import org.keycloak.models.jpa.entities.OrganizationDomainEntity;
import org.keycloak.models.jpa.entities.OrganizationEntity;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.organization.OrganizationProvider;
import org.keycloak.organization.utils.Organizations;
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
            if (modelMap.containsKey(domainEntity.getName())) {
                OrganizationDomainModel model = modelMap.get(domainEntity.getName());
                domainEntity.setVerified(model.isVerified());
                domainEntity.setIdentityProvider(resolveIdentityProvider(em, model.getIdentityProviderAlias()));
                domainEntity.setAutoRedirect(model.isAutoRedirect());
                modelMap.remove(domainEntity.getName());
            } else {
                this.entity.removeDomain(domainEntity);
                domainEntity.setIdentityProvider(null);
                em.remove(domainEntity);
            }
        }

        for (OrganizationDomainModel model : modelMap.values()) {
            OrganizationDomainEntity domainEntity;
            try {
                domainEntity = em.createNamedQuery("getDomainByRealmAndName", OrganizationDomainEntity.class)
                        .setParameter("realmId", realm.getId())
                        .setParameter("name", model.getName())
                        .getSingleResult();
                domainEntity.setVerified(model.isVerified());
            } catch (jakarta.persistence.NoResultException e) {
                domainEntity = new OrganizationDomainEntity();
                domainEntity.setId(KeycloakModelUtils.generateId());
                domainEntity.setName(model.getName());
                domainEntity.setVerified(model.isVerified());
                domainEntity.setRealmId(realm.getId());
            }
            domainEntity.setIdentityProvider(resolveIdentityProvider(em, model.getIdentityProviderAlias()));
            domainEntity.setAutoRedirect(model.isAutoRedirect());
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
}

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

import org.keycloak.models.GroupModel;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ModelValidationException;
import org.keycloak.models.OrganizationDomainModel;
import org.keycloak.models.OrganizationModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.jpa.JpaModel;
import org.keycloak.models.jpa.entities.OrganizationDomainEntity;
import org.keycloak.models.jpa.entities.OrganizationEntity;
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

    private GroupModel getGroup() {
        if (group == null) {
            group = realm.getGroupById(getGroupId());
        }
        return group;
    }
}

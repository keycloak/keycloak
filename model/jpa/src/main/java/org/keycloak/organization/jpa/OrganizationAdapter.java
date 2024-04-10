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

import org.keycloak.models.GroupModel;
import java.util.Collection;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.keycloak.models.OrganizationDomainModel;
import org.keycloak.models.OrganizationModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.jpa.JpaModel;
import org.keycloak.models.jpa.entities.OrganizationDomainEntity;
import org.keycloak.models.jpa.entities.OrganizationEntity;

import java.util.List;

public final class OrganizationAdapter implements OrganizationModel, JpaModel<OrganizationEntity> {

    private final RealmModel realm;
    private final OrganizationEntity entity;
    private GroupModel group;

    public OrganizationAdapter(RealmModel realm, OrganizationEntity entity) {
        this.realm = realm;
        this.entity = entity;
    }

    @Override
    public String getId() {
        return entity.getId();
    }

    RealmModel getRealm() {
        return realm;
    }

    String getGroupId() {
        return entity.getGroupId();
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
    public void setSingleAttribute(String name, String value) {
        getGroup().setSingleAttribute(name, value);
    }

    @Override
    public void setAttribute(String name, List<String> values) {
        getGroup().setAttribute(name, values);
    }

    @Override
    public void removeAttribute(String name) {
        getGroup().removeAttribute(name);
    }

    @Override
    public String getFirstAttribute(String name) {
        return getGroup().getFirstAttribute(name);
    }

    @Override
    public Stream<String> getAttributeStream(String name) {
        return getGroup().getAttributeStream(name);
    }

    @Override
    public Map<String, List<String>> getAttributes() {
        return getGroup().getAttributes();
    }

    @Override
    public Stream<OrganizationDomainModel> getDomains() {
        return entity.getDomains().stream().map(this::toModel);
    }

    @Override
    public void setDomains(Collection<OrganizationDomainModel> domains) {
        Map<String, OrganizationDomainModel> modelMap = domains.stream()
                .collect(Collectors.toMap(model -> model.getName(), Function.identity()));
        for (OrganizationDomainEntity domainEntity : this.entity.getDomains()) {
            // update the existing domain (for now, only the verified flag can be changed).
            if (modelMap.containsKey(domainEntity.getName())) {
                domainEntity.setVerified(modelMap.get(domainEntity.getName()).getVerified());
                modelMap.remove(domainEntity.getName());
            }
            // remove domain that is not found in the new set.
            else {
                this.entity.removeDomain(domainEntity);
            }
        }

        // create the remaining domains.
        for (OrganizationDomainModel model : modelMap.values()) {
            OrganizationDomainEntity domainEntity = new OrganizationDomainEntity();
            domainEntity.setName(model.getName().toLowerCase());
            domainEntity.setVerified(model.getVerified() == null ? Boolean.FALSE : model.getVerified());
            domainEntity.setOrganization(this.entity);
            this.entity.addDomain(domainEntity);
        }
    }

    @Override
    public OrganizationEntity getEntity() {
        return entity;
    }

    private GroupModel getGroup() {
        if (group == null) {
            group = realm.getGroupById(getGroupId());
        }
        return group;
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

    private OrganizationDomainModel toModel(OrganizationDomainEntity entity) {
        return new OrganizationDomainModel(entity.getName(), entity.isVerified());
    }
}

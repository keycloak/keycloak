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
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.OrganizationModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.jpa.JpaModel;
import org.keycloak.models.jpa.entities.OrganizationEntity;

public final class OrganizationAdapter implements OrganizationModel, JpaModel<OrganizationEntity> {

    private final OrganizationEntity entity;
    private final KeycloakSession session;

    public OrganizationAdapter(OrganizationEntity entity, KeycloakSession session) {
        this.entity = entity;
        this.session = session;
    }

    @Override
    public String getId() {
        return entity.getId();
    }

    String getRealm() {
        return entity.getRealmId();
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
    public boolean addMember(UserModel member) {
        RealmModel realm = session.getContext().getRealm();
        GroupModel group = session.groups().getGroupById(realm, entity.getGroupId());

        if (member.isMemberOf(group)) {
            return false;
        }

        member.joinGroup(group);

        return true;
    }

    @Override
    public OrganizationEntity getEntity() {
        return entity;
    }
}

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

import static org.keycloak.utils.StreamsUtil.closing;

import java.util.stream.Stream;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import jakarta.persistence.TypedQuery;
import org.keycloak.connections.jpa.JpaConnectionProvider;
import org.keycloak.models.GroupModel;
import org.keycloak.models.GroupProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.OrganizationModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.jpa.entities.OrganizationEntity;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.organization.OrganizationProvider;

public class JpaOrganizationProvider implements OrganizationProvider {

    private final EntityManager em;
    private final GroupProvider groupProvider;
    private final KeycloakSession session;

    public JpaOrganizationProvider(KeycloakSession session) {
        this.session = session;
        JpaConnectionProvider jpaProvider = session.getProvider(JpaConnectionProvider.class);
        this.em = jpaProvider.getEntityManager();
        groupProvider = session.groups();
    }

    @Override
    public OrganizationModel createOrganization(RealmModel realm, String name) {
        throwExceptionIfRealmIsNull(realm);

        String groupName = "org-" + name;
        GroupModel group = groupProvider.getGroupByName(realm, null, name);

        if (group != null) {
            throw new IllegalArgumentException("A group with the same already exist and it is bound to different organization");
        }

        OrganizationEntity entity = new OrganizationEntity();

        entity.setId(KeycloakModelUtils.generateId());
        entity.setRealmId(realm.getId());
        entity.setName(name);
        group = groupProvider.createGroup(realm, entity.getId(), groupName);
        entity.setGroupId(group.getId());

        em.persist(entity);

        return new OrganizationAdapter(entity, session);
    }

    @Override
    public boolean removeOrganization(RealmModel realm, OrganizationModel organization) {
        throwExceptionIfRealmIsNull(realm);
        throwExceptionIfOrganizationIsNull(organization);
        OrganizationAdapter toRemove = getAdapter(realm, organization.getId());
        throwExceptionIfOrganizationIsNull(toRemove);

        if (!toRemove.getRealm().equals(realm.getId())) {
            throw new IllegalArgumentException("Organization [" + organization.getId() + " does not belong to realm [" + realm.getId() + "]");
        }

        GroupModel group = session.groups().getGroupById(realm, toRemove.getGroupId());
        session.groups().removeGroup(realm, group);
        //TODO: delete users

        em.remove(toRemove.getEntity());

        return true;
    }

    @Override
    public void removeOrganizations(RealmModel realm) {
        throwExceptionIfRealmIsNull(realm);
        Query query = em.createNamedQuery("deleteByRealm");

        query.setParameter("realmId", realm.getId());

        query.executeUpdate();
    }

    @Override
    public OrganizationModel getOrganizationById(RealmModel realm, String id) {
        return getAdapter(realm, id);
    }

    @Override
    public Stream<OrganizationModel> getOrganizationsStream(RealmModel realm) {
        TypedQuery<String> query = em.createNamedQuery("getByRealm", String.class);

        query.setParameter("realmId", realm.getId());

        return closing(query.getResultStream().map(id -> getAdapter(realm, id)));
    }

    @Override
    public void close() {

    }

    private OrganizationAdapter getAdapter(RealmModel realm, String id) {
        OrganizationEntity entity = em.find(OrganizationEntity.class, id);

        if (entity == null) {
            return null;
        }

        if (!realm.getId().equals(entity.getRealmId())) {
            return null;
        }

        return new OrganizationAdapter(entity, session);
    }

    private void throwExceptionIfOrganizationIsNull(OrganizationModel organization) {
        if (organization == null) {
            throw new IllegalArgumentException("organization can not be null");
        }
    }

    private void throwExceptionIfRealmIsNull(RealmModel realm) {
        if (realm == null) {
            throw new IllegalArgumentException("realm can not be null");
        }
    }
}

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

import static org.keycloak.models.OrganizationModel.USER_ORGANIZATION_ATTRIBUTE;
import static org.keycloak.utils.StreamsUtil.closing;

import java.util.stream.Stream;

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import org.keycloak.connections.jpa.JpaConnectionProvider;
import org.keycloak.models.GroupModel;
import org.keycloak.models.GroupProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ModelException;
import org.keycloak.models.OrganizationModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserProvider;
import org.keycloak.models.jpa.entities.OrganizationEntity;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.organization.OrganizationProvider;

public class JpaOrganizationProvider implements OrganizationProvider {

    private final EntityManager em;
    private final GroupProvider groupProvider;
    private final UserProvider userProvider;
    private final RealmModel realm;

    public JpaOrganizationProvider(KeycloakSession session) {
        em = session.getProvider(JpaConnectionProvider.class).getEntityManager();
        groupProvider = session.groups();
        userProvider = session.users();
        realm = session.getContext().getRealm();
        if (realm == null) {
            throw new IllegalArgumentException("Session not bound to a realm");
        }
    }

    @Override
    public OrganizationModel create(String name) {
        GroupModel group = createOrganizationGroup(name);
        OrganizationEntity entity = new OrganizationEntity();

        entity.setId(KeycloakModelUtils.generateId());
        entity.setGroupId(group.getId());
        entity.setRealmId(realm.getId());
        entity.setName(name);

        em.persist(entity);

        return new OrganizationAdapter(realm, entity);
    }

    @Override
    public boolean remove(OrganizationModel organization) {
        GroupModel group = getOrganizationGroup(organization);

        //TODO: won't scale, requires a better mechanism for bulk deleting users
        userProvider.getGroupMembersStream(realm, group).forEach(userModel -> userProvider.removeUser(realm, userModel));
        groupProvider.removeGroup(realm, group);

        OrganizationAdapter adapter = getAdapter(organization.getId());

        em.remove(adapter.getEntity());

        return true;
    }

    @Override
    public void removeAll() {
        //TODO: won't scale, requires a better mechanism for bulk deleting organizations within a realm
        getAllStream().forEach(this::remove);
    }

    @Override
    public boolean addMember(OrganizationModel organization, UserModel user) {
        throwExceptionIfOrganizationIsNull(organization);
        if (user == null) {
            throw new ModelException("User can not be null");
        }
        OrganizationAdapter adapter = getAdapter(organization.getId());
        GroupModel group = groupProvider.getGroupById(realm, adapter.getGroupId());

        if (user.isMemberOf(group)) {
            return false;
        }

        if (user.getFirstAttribute(USER_ORGANIZATION_ATTRIBUTE) != null) {
            throw new ModelException("User [" + user.getId() + "] is a member of a different organization");
        }

        user.joinGroup(group);
        user.setSingleAttribute(USER_ORGANIZATION_ATTRIBUTE, adapter.getId());

        return true;
    }

    @Override
    public OrganizationModel getById(String id) {
        return getAdapter(id, false);
    }

    @Override
    public Stream<OrganizationModel> getAllStream() {
        TypedQuery<OrganizationEntity> query = em.createNamedQuery("getByRealm", OrganizationEntity.class);

        query.setParameter("realmId", realm.getId());

        return closing(query.getResultStream().map(entity -> new OrganizationAdapter(realm, entity)));
    }

    @Override
    public Stream<UserModel> getMembersStream(OrganizationModel organization) {
        throwExceptionIfOrganizationIsNull(organization);
        OrganizationAdapter adapter = getAdapter(organization.getId());
        GroupModel group = getOrganizationGroup(adapter);

        return userProvider.getGroupMembersStream(realm, group);
    }

    @Override
    public UserModel getMemberById(OrganizationModel organization, String id) {
        throwExceptionIfOrganizationIsNull(organization);
        UserModel user = userProvider.getUserById(realm, id);

        if (user == null) {
            return null;
        }

        String orgId = user.getFirstAttribute(USER_ORGANIZATION_ATTRIBUTE);

        if (organization.getId().equals(orgId)) {
            return user;
        }

        return null;
    }

    @Override
    public OrganizationModel getByMember(UserModel member) {
        if (member == null) {
            throw new ModelException("User can not be null");
        }

        String orgId = member.getFirstAttribute(USER_ORGANIZATION_ATTRIBUTE);

        if (orgId == null) {
            return null;
        }

        return getById(orgId);
    }

    @Override
    public void close() {

    }

    private OrganizationAdapter getAdapter(String id) {
        return getAdapter(id, true);
    }

    private OrganizationAdapter getAdapter(String id, boolean failIfNotFound) {
        OrganizationEntity entity = em.find(OrganizationEntity.class, id);

        if (entity == null) {
            if (failIfNotFound) {
                throw new ModelException("Organization [" + id + "] does not exist");
            }
            return null;
        }

        if (!realm.getId().equals(entity.getRealmId())) {
            throw new ModelException("Organization [" + entity.getId() + " does not belong to realm [" + realm.getId() + "]");
        }

        return new OrganizationAdapter(realm, entity);
    }

    private GroupModel createOrganizationGroup(String name) {
        if (name == null) {
            throw new ModelException("name can not be null");
        }

        String groupName = getCanonicalGroupName(name);
        GroupModel group = groupProvider.getGroupByName(realm, null, name);

        if (group != null) {
            throw new ModelException("A group with the same name already exist and it is bound to different organization");
        }

        return groupProvider.createGroup(realm, groupName);
    }

    private String getCanonicalGroupName(String name) {
        return "kc.org." + name;
    }

    private GroupModel getOrganizationGroup(OrganizationModel organization) {
        throwExceptionIfOrganizationIsNull(organization);
        OrganizationAdapter adapter = getAdapter(organization.getId());

        GroupModel group = groupProvider.getGroupById(realm, adapter.getGroupId());

        if (group == null) {
            throw new ModelException("Organization group " + adapter.getGroupId() + " not found");
        }

        return group;
    }

    private void throwExceptionIfOrganizationIsNull(OrganizationModel organization) {
        if (organization == null) {
            throw new ModelException("organization can not be null");
        }
    }
}

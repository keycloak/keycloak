/*
 * Copyright 2020 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.models.map.group;

import org.jboss.logging.Logger;
import org.keycloak.models.GroupModel;
import org.keycloak.models.GroupModel.SearchableFields;
import org.keycloak.models.GroupProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ModelDuplicateException;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.map.storage.MapKeycloakTransaction;
import org.keycloak.models.map.storage.MapStorage;

import org.keycloak.models.map.storage.ModelCriteriaBuilder.Operator;
import org.keycloak.models.map.storage.QueryParameters;

import org.keycloak.models.map.storage.criteria.DefaultModelCriteria;

import java.util.Objects;
import java.util.function.Function;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

import static org.keycloak.common.util.StackUtil.getShortStackTrace;
import static org.keycloak.models.map.common.AbstractMapProviderFactory.MapProviderObjectType.GROUP_AFTER_REMOVE;
import static org.keycloak.models.map.common.AbstractMapProviderFactory.MapProviderObjectType.GROUP_BEFORE_REMOVE;
import static org.keycloak.models.map.storage.QueryParameters.Order.ASCENDING;
import static org.keycloak.models.map.storage.QueryParameters.withCriteria;
import static org.keycloak.models.map.storage.criteria.DefaultModelCriteria.criteria;

public class MapGroupProvider implements GroupProvider {

    private static final Logger LOG = Logger.getLogger(MapGroupProvider.class);
    private final KeycloakSession session;
    final MapKeycloakTransaction<MapGroupEntity, GroupModel> tx;

    public MapGroupProvider(KeycloakSession session, MapStorage<MapGroupEntity, GroupModel> groupStore) {
        this.session = session;
        this.tx = groupStore.createTransaction(session);
        session.getTransactionManager().enlist(tx);
    }

    private Function<MapGroupEntity, GroupModel> entityToAdapterFunc(RealmModel realm) {
        // Clone entity before returning back, to avoid giving away a reference to the live object to the caller
        return origEntity -> new MapGroupAdapter(session, realm, origEntity) {
            @Override
            public Stream<GroupModel> getSubGroupsStream() {
                return getGroupsByParentId(realm, this.getId());
            }
        };
    }

    @Override
    public GroupModel getGroupById(RealmModel realm, String id) {
        if (id == null) {
            return null;
        }

        LOG.tracef("getGroupById(%s, %s)%s", realm, id, getShortStackTrace());

        MapGroupEntity entity = tx.read(id);
        String realmId = realm.getId();
        return (entity == null || ! Objects.equals(realmId, entity.getRealmId()))
                ? null
                : entityToAdapterFunc(realm).apply(entity);
    }

    @Override
    public Stream<GroupModel> getGroupsStream(RealmModel realm) {
        return getGroupsStreamInternal(realm, null, null);
    }

    private Stream<GroupModel> getGroupsStreamInternal(RealmModel realm, UnaryOperator<DefaultModelCriteria<GroupModel>> modifier, UnaryOperator<QueryParameters<GroupModel>> queryParametersModifier) {
        LOG.tracef("getGroupsStream(%s)%s", realm, getShortStackTrace());
        DefaultModelCriteria<GroupModel> mcb = criteria();
        mcb = mcb.compare(SearchableFields.REALM_ID, Operator.EQ, realm.getId());

        if (modifier != null) {
            mcb = modifier.apply(mcb);
        }

        QueryParameters<GroupModel> queryParameters = withCriteria(mcb).orderBy(SearchableFields.NAME, ASCENDING);
        if (queryParametersModifier != null) {
            queryParameters = queryParametersModifier.apply(queryParameters);
        }

        return tx.read(queryParameters)
                .map(entityToAdapterFunc(realm))
                ;
    }

    @Override
    public Stream<GroupModel> getGroupsStream(RealmModel realm, Stream<String> ids, String search, Integer first, Integer max) {
        DefaultModelCriteria<GroupModel> mcb = criteria();
        mcb = mcb.compare(SearchableFields.ID, Operator.IN, ids)
          .compare(SearchableFields.REALM_ID, Operator.EQ, realm.getId());

        if (search != null) {
            mcb = mcb.compare(SearchableFields.NAME, Operator.ILIKE, "%" + search + "%");
        }

        return tx.read(withCriteria(mcb).pagination(first, max, SearchableFields.NAME))
                .map(entityToAdapterFunc(realm));
    }

    @Override
    public Long getGroupsCount(RealmModel realm, Boolean onlyTopGroups) {
        LOG.tracef("getGroupsCount(%s, %s)%s", realm, onlyTopGroups, getShortStackTrace());
        DefaultModelCriteria<GroupModel> mcb = criteria();
        mcb = mcb.compare(SearchableFields.REALM_ID, Operator.EQ, realm.getId());

        if (Objects.equals(onlyTopGroups, Boolean.TRUE)) {
            mcb = mcb.compare(SearchableFields.PARENT_ID, Operator.NOT_EXISTS);
        }

        return tx.getCount(withCriteria(mcb));
    }

    @Override
    public Long getGroupsCountByNameContaining(RealmModel realm, String search) {
        return searchForGroupByNameStream(realm, search, null, null).count();
    }

    @Override
    public Stream<GroupModel> getGroupsByRoleStream(RealmModel realm, RoleModel role, Integer firstResult, Integer maxResults) {
        LOG.tracef("getGroupsByRole(%s, %s, %d, %d)%s", realm, role, firstResult, maxResults, getShortStackTrace());
        return getGroupsStreamInternal(realm,
          (DefaultModelCriteria<GroupModel> mcb) -> mcb.compare(SearchableFields.ASSIGNED_ROLE, Operator.EQ, role.getId()),
          qp -> qp.offset(firstResult).limit(maxResults)
        );
    }

    @Override
    public Stream<GroupModel> getTopLevelGroupsStream(RealmModel realm) {
        LOG.tracef("getTopLevelGroupsStream(%s)%s", realm, getShortStackTrace());
        return getGroupsStreamInternal(realm,
          (DefaultModelCriteria<GroupModel> mcb) -> mcb.compare(SearchableFields.PARENT_ID, Operator.NOT_EXISTS),
          null
        );
    }

    @Override
    public Stream<GroupModel> getTopLevelGroupsStream(RealmModel realm, Integer firstResult, Integer maxResults) {
        LOG.tracef("getTopLevelGroupsStream(%s, %s, %s)%s", realm, firstResult, maxResults, getShortStackTrace());
        return getGroupsStreamInternal(realm,
                (DefaultModelCriteria<GroupModel> mcb) -> mcb.compare(SearchableFields.PARENT_ID, Operator.NOT_EXISTS),
                qp -> qp.offset(firstResult).limit(maxResults)
        );
    }

    @Override
    public Stream<GroupModel> searchForGroupByNameStream(RealmModel realm, String search, Integer firstResult, Integer maxResults) {
        LOG.tracef("searchForGroupByNameStream(%s, %s, %d, %d)%s", realm, search, firstResult, maxResults, getShortStackTrace());


        DefaultModelCriteria<GroupModel> mcb = criteria();
        mcb = mcb.compare(SearchableFields.REALM_ID, Operator.EQ, realm.getId())
                .compare(SearchableFields.NAME, Operator.ILIKE, "%" + search + "%");


        return tx.read(withCriteria(mcb).pagination(firstResult, maxResults, SearchableFields.NAME))
            .map(MapGroupEntity::getId)
            .map(id -> {
                GroupModel groupById = session.groups().getGroupById(realm, id);
                while (Objects.nonNull(groupById.getParentId())) {
                    groupById = session.groups().getGroupById(realm, groupById.getParentId());
                }
                return groupById;
            }).sorted(GroupModel.COMPARE_BY_NAME).distinct();
    }

    @Override
    public GroupModel createGroup(RealmModel realm, String id, String name, GroupModel toParent) {
        LOG.tracef("createGroup(%s, %s, %s, %s)%s", realm, id, name, toParent, getShortStackTrace());
        // Check Db constraint: uniqueConstraints = { @UniqueConstraint(columnNames = {"REALM_ID", "PARENT_GROUP", "NAME"})}
        DefaultModelCriteria<GroupModel> mcb = criteria();
        mcb = mcb.compare(SearchableFields.REALM_ID, Operator.EQ, realm.getId())
          .compare(SearchableFields.NAME, Operator.EQ, name);

        mcb = toParent == null ? 
                mcb.compare(SearchableFields.PARENT_ID, Operator.NOT_EXISTS) : 
                mcb.compare(SearchableFields.PARENT_ID, Operator.EQ, toParent.getId());

        if (tx.getCount(withCriteria(mcb)) > 0) {
            throw new ModelDuplicateException("Group with name '" + name + "' in realm " + realm.getName() + " already exists for requested parent" );
        }

        MapGroupEntity entity = new MapGroupEntityImpl();
        entity.setId(id);
        entity.setRealmId(realm.getId());
        entity.setName(name);
        entity.setParentId(toParent == null ? null : toParent.getId());
        if (id != null && tx.read(id) != null) {
            throw new ModelDuplicateException("Group exists: " + id);
        }
        entity = tx.create(entity);

        return entityToAdapterFunc(realm).apply(entity);
    }

    @Override
    public boolean removeGroup(RealmModel realm, GroupModel group) {
        LOG.tracef("removeGroup(%s, %s)%s", realm, group, getShortStackTrace());
        if (group == null) return false;

        session.invalidate(GROUP_BEFORE_REMOVE, realm, group);

        tx.delete(group.getId());
        
        session.invalidate(GROUP_AFTER_REMOVE, realm, group);

        return true;
    }

    /* TODO: investigate following two methods, it seems they could be moved to model layer */

    @Override
    public void moveGroup(RealmModel realm, GroupModel group, GroupModel toParent) {
        LOG.tracef("moveGroup(%s, %s, %s)%s", realm, group, toParent, getShortStackTrace());

        if (toParent != null && group.getId().equals(toParent.getId())) {
            return;
        }
        
        DefaultModelCriteria<GroupModel> mcb = criteria();
        mcb = mcb.compare(SearchableFields.REALM_ID, Operator.EQ, realm.getId())
          .compare(SearchableFields.NAME, Operator.EQ, group.getName());

        mcb = toParent == null ? 
                mcb.compare(SearchableFields.PARENT_ID, Operator.NOT_EXISTS) : 
                mcb.compare(SearchableFields.PARENT_ID, Operator.EQ, toParent.getId());

        try (Stream<MapGroupEntity> possibleSiblings = tx.read(withCriteria(mcb))) {
            if (possibleSiblings.findAny().isPresent()) {
                throw new ModelDuplicateException("Parent already contains subgroup named '" + group.getName() + "'");
            }
        }

        if (group.getParentId() != null) {
            group.getParent().removeChild(group);
        }
        group.setParent(toParent);
        if (toParent != null) toParent.addChild(group);
    }

    @Override
    public void addTopLevelGroup(RealmModel realm, GroupModel subGroup) {
        LOG.tracef("addTopLevelGroup(%s, %s)%s", realm, subGroup, getShortStackTrace());

        DefaultModelCriteria<GroupModel> mcb = criteria();
        mcb = mcb.compare(SearchableFields.REALM_ID, Operator.EQ, realm.getId())
          .compare(SearchableFields.PARENT_ID, Operator.EQ, (Object) null)
          .compare(SearchableFields.NAME, Operator.EQ, subGroup.getName());

        try (Stream<MapGroupEntity> possibleSiblings = tx.read(withCriteria(mcb))) {
            if (possibleSiblings.findAny().isPresent()) {
                throw new ModelDuplicateException("There is already a top level group named '" + subGroup.getName() + "'");
            }
        }

        subGroup.setParent(null);
    }

    public void preRemove(RealmModel realm, RoleModel role) {
        LOG.tracef("preRemove(%s, %s)%s", realm, role, getShortStackTrace());
        DefaultModelCriteria<GroupModel> mcb = criteria();
        mcb = mcb.compare(SearchableFields.REALM_ID, Operator.EQ, realm.getId())
          .compare(SearchableFields.ASSIGNED_ROLE, Operator.EQ, role.getId());
        try (Stream<MapGroupEntity> toRemove = tx.read(withCriteria(mcb))) {
            toRemove
                .map(groupEntity -> session.groups().getGroupById(realm, groupEntity.getId()))
                .forEach(groupModel -> groupModel.deleteRoleMapping(role));
        }
    }

    public void preRemove(RealmModel realm) {
        LOG.tracef("preRemove(%s)%s", realm, getShortStackTrace());
        DefaultModelCriteria<GroupModel> mcb = criteria();
        mcb = mcb.compare(SearchableFields.REALM_ID, Operator.EQ, realm.getId());

        tx.delete(withCriteria(mcb));
    }

    @Override
    public void close() {
    }

    private Stream<GroupModel> getGroupsByParentId(RealmModel realm, String parentId) {
        LOG.tracef("getGroupsByParentId(%s)%s", parentId, getShortStackTrace());
        DefaultModelCriteria<GroupModel> mcb = criteria();
        mcb = mcb
                .compare(SearchableFields.REALM_ID, Operator.EQ, realm.getId())
                .compare(SearchableFields.PARENT_ID, Operator.EQ, parentId);

        return tx.read(withCriteria(mcb)).map(entityToAdapterFunc(realm));
    }
}

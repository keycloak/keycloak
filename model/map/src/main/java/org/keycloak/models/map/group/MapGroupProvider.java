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

import org.keycloak.models.map.storage.ModelCriteriaBuilder;
import org.keycloak.models.map.storage.ModelCriteriaBuilder.Operator;
import org.keycloak.models.map.storage.QueryParameters;

import java.util.Objects;
import java.util.function.Function;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.keycloak.common.util.StackUtil.getShortStackTrace;
import static org.keycloak.models.map.common.MapStorageUtils.registerEntityForChanges;
import static org.keycloak.models.map.storage.QueryParameters.Order.ASCENDING;
import static org.keycloak.models.map.storage.QueryParameters.withCriteria;

public class MapGroupProvider<K> implements GroupProvider {

    private static final Logger LOG = Logger.getLogger(MapGroupProvider.class);
    private final KeycloakSession session;
    final MapKeycloakTransaction<K, MapGroupEntity<K>, GroupModel> tx;
    private final MapStorage<K, MapGroupEntity<K>, GroupModel> groupStore;

    public MapGroupProvider(KeycloakSession session, MapStorage<K, MapGroupEntity<K>, GroupModel> groupStore) {
        this.session = session;
        this.groupStore = groupStore;
        this.tx = groupStore.createTransaction(session);
        session.getTransactionManager().enlist(tx);
    }

    private Function<MapGroupEntity<K>, GroupModel> entityToAdapterFunc(RealmModel realm) {
        // Clone entity before returning back, to avoid giving away a reference to the live object to the caller
        return origEntity -> new MapGroupAdapter<K>(session, realm, registerEntityForChanges(tx, origEntity)) {
            @Override
            public String getId() {
                return groupStore.getKeyConvertor().keyToString(entity.getId());
            }
        };
    }

    @Override
    public GroupModel getGroupById(RealmModel realm, String id) {
        if (id == null) {
            return null;
        }

        LOG.tracef("getGroupById(%s, %s)%s", realm, id, getShortStackTrace());

        K uid;
        try {
            uid = groupStore.getKeyConvertor().fromStringSafe(id);
        } catch (IllegalArgumentException ex) {
            return null;
        }
        
        MapGroupEntity<K> entity = tx.read(uid);
        String realmId = realm.getId();
        return (entity == null || ! Objects.equals(realmId, entity.getRealmId()))
                ? null
                : entityToAdapterFunc(realm).apply(entity);
    }

    @Override
    public Stream<GroupModel> getGroupsStream(RealmModel realm) {
        return getGroupsStreamInternal(realm, null, null);
    }

    private Stream<GroupModel> getGroupsStreamInternal(RealmModel realm, UnaryOperator<ModelCriteriaBuilder<GroupModel>> modifier, UnaryOperator<QueryParameters<GroupModel>> queryParametersModifier) {
        LOG.tracef("getGroupsStream(%s)%s", realm, getShortStackTrace());
        ModelCriteriaBuilder<GroupModel> mcb = groupStore.createCriteriaBuilder()
          .compare(SearchableFields.REALM_ID, Operator.EQ, realm.getId());

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
        ModelCriteriaBuilder<GroupModel> mcb = groupStore.createCriteriaBuilder()
          .compare(SearchableFields.ID, Operator.IN, ids.map(groupStore.getKeyConvertor()::fromString))
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
        ModelCriteriaBuilder<GroupModel> mcb = groupStore.createCriteriaBuilder()
          .compare(SearchableFields.REALM_ID, Operator.EQ, realm.getId());

        if (Objects.equals(onlyTopGroups, Boolean.TRUE)) {
            mcb = mcb.compare(SearchableFields.PARENT_ID, Operator.EQ, (Object) null);
        }

        return tx.getCount(withCriteria(mcb));
    }

    @Override
    public Long getGroupsCountByNameContaining(RealmModel realm, String search) {
        ModelCriteriaBuilder<GroupModel> mcb = groupStore.createCriteriaBuilder()
          .compare(SearchableFields.REALM_ID, Operator.EQ, realm.getId())
          .compare(SearchableFields.NAME, Operator.ILIKE, "%" + search + "%");

        return tx.getCount(withCriteria(mcb));
    }

    @Override
    public Stream<GroupModel> getGroupsByRoleStream(RealmModel realm, RoleModel role, Integer firstResult, Integer maxResults) {
        LOG.tracef("getGroupsByRole(%s, %s, %d, %d)%s", realm, role, firstResult, maxResults, getShortStackTrace());
        return getGroupsStreamInternal(realm,
          (ModelCriteriaBuilder<GroupModel> mcb) -> mcb.compare(SearchableFields.ASSIGNED_ROLE, Operator.EQ, role.getId()),
          qp -> qp.offset(firstResult).limit(maxResults)
        );
    }

    @Override
    public Stream<GroupModel> getTopLevelGroupsStream(RealmModel realm) {
        LOG.tracef("getTopLevelGroupsStream(%s)%s", realm, getShortStackTrace());
        return getGroupsStreamInternal(realm,
          (ModelCriteriaBuilder<GroupModel> mcb) -> mcb.compare(SearchableFields.PARENT_ID, Operator.NOT_EXISTS),
          null
        );
    }

    @Override
    public Stream<GroupModel> getTopLevelGroupsStream(RealmModel realm, Integer firstResult, Integer maxResults) {
        LOG.tracef("getTopLevelGroupsStream(%s, %s, %s)%s", realm, firstResult, maxResults, getShortStackTrace());
        return getGroupsStreamInternal(realm,
                (ModelCriteriaBuilder<GroupModel> mcb) -> mcb.compare(SearchableFields.PARENT_ID, Operator.NOT_EXISTS),
                qp -> qp.offset(firstResult).limit(maxResults)
        );
    }

    @Override
    public Stream<GroupModel> searchForGroupByNameStream(RealmModel realm, String search, Integer firstResult, Integer maxResults) {
        LOG.tracef("searchForGroupByNameStream(%s, %s, %d, %d)%s", realm, search, firstResult, maxResults, getShortStackTrace());


        ModelCriteriaBuilder<GroupModel> mcb = groupStore.createCriteriaBuilder()
                .compare(SearchableFields.REALM_ID, Operator.EQ, realm.getId())
                .compare(SearchableFields.NAME, Operator.ILIKE, "%" + search + "%");


        return tx.read(withCriteria(mcb).pagination(firstResult, maxResults, SearchableFields.NAME))
            .map(MapGroupEntity::getId)
            .map(groupStore.getKeyConvertor()::keyToString)
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
        final K entityId = id == null ? groupStore.getKeyConvertor().yieldNewUniqueKey() : groupStore.getKeyConvertor().fromString(id);

        // Check Db constraint: uniqueConstraints = { @UniqueConstraint(columnNames = {"REALM_ID", "PARENT_GROUP", "NAME"})}
        String parentId = toParent == null ? null : toParent.getId();
        ModelCriteriaBuilder<GroupModel> mcb = groupStore.createCriteriaBuilder()
          .compare(SearchableFields.REALM_ID, Operator.EQ, realm.getId())
          .compare(SearchableFields.PARENT_ID, Operator.EQ, parentId)
          .compare(SearchableFields.NAME, Operator.EQ, name);

        if (tx.getCount(withCriteria(mcb)) > 0) {
            throw new ModelDuplicateException("Group with name '" + name + "' in realm " + realm.getName() + " already exists for requested parent" );
        }

        MapGroupEntity<K> entity = new MapGroupEntity<K>(entityId, realm.getId());
        entity.setName(name);
        entity.setParentId(toParent == null ? null : toParent.getId());
        if (tx.read(entity.getId()) != null) {
            throw new ModelDuplicateException("Group exists: " + entityId);
        }
        tx.create(entity);

        return entityToAdapterFunc(realm).apply(entity);
    }

    @Override
    public boolean removeGroup(RealmModel realm, GroupModel group) {
        LOG.tracef("removeGroup(%s, %s)%s", realm, group, getShortStackTrace());
        if (group == null) return false;

        // TODO: Sending an event (, user group removal and realm default groups) should be extracted to store layer
        session.getKeycloakSessionFactory().publish(new GroupModel.GroupRemovedEvent() {

            @Override
            public RealmModel getRealm() {
                return realm;
            }

            @Override
            public GroupModel getGroup() {
                return group;
            }

            @Override
            public KeycloakSession getKeycloakSession() {
                return session;
            }
        });

        session.users().preRemove(realm, group);
        realm.removeDefaultGroup(group);

        group.getSubGroupsStream().collect(Collectors.toSet()).forEach(subGroup -> session.groups().removeGroup(realm, subGroup));

        // TODO: ^^^^^^^ Up to here

        tx.delete(groupStore.getKeyConvertor().fromString(group.getId()));
        
        return true;
    }

    /* TODO: investigate following two methods, it seems they could be moved to model layer */

    @Override
    public void moveGroup(RealmModel realm, GroupModel group, GroupModel toParent) {
        LOG.tracef("moveGroup(%s, %s, %s)%s", realm, group, toParent, getShortStackTrace());

        if (toParent != null && group.getId().equals(toParent.getId())) {
            return;
        }
        
        String parentId = toParent == null ? null : toParent.getId();
        ModelCriteriaBuilder<GroupModel> mcb = groupStore.createCriteriaBuilder()
          .compare(SearchableFields.REALM_ID, Operator.EQ, realm.getId())
          .compare(SearchableFields.PARENT_ID, Operator.EQ, parentId)
          .compare(SearchableFields.NAME, Operator.EQ, group.getName());

        try (Stream<MapGroupEntity<K>> possibleSiblings = tx.read(withCriteria(mcb))) {
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

        ModelCriteriaBuilder<GroupModel> mcb = groupStore.createCriteriaBuilder()
          .compare(SearchableFields.REALM_ID, Operator.EQ, realm.getId())
          .compare(SearchableFields.PARENT_ID, Operator.EQ, (Object) null)
          .compare(SearchableFields.NAME, Operator.EQ, subGroup.getName());

        try (Stream<MapGroupEntity<K>> possibleSiblings = tx.read(withCriteria(mcb))) {
            if (possibleSiblings.findAny().isPresent()) {
                throw new ModelDuplicateException("There is already a top level group named '" + subGroup.getName() + "'");
            }
        }

        subGroup.setParent(null);
    }

    public void preRemove(RealmModel realm, RoleModel role) {
        LOG.tracef("preRemove(%s, %s)%s", realm, role, getShortStackTrace());
        ModelCriteriaBuilder<GroupModel> mcb = groupStore.createCriteriaBuilder()
          .compare(SearchableFields.REALM_ID, Operator.EQ, realm.getId())
          .compare(SearchableFields.ASSIGNED_ROLE, Operator.EQ, role.getId());
        try (Stream<MapGroupEntity<K>> toRemove = tx.read(withCriteria(mcb))) {
            toRemove
                .map(groupEntity -> session.groups().getGroupById(realm, groupEntity.getId().toString()))
                .forEach(groupModel -> groupModel.deleteRoleMapping(role));
        }
    }

    @Override
    public void close() {
        
    }

}

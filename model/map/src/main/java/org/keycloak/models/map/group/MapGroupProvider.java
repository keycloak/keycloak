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
import org.keycloak.models.GroupProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ModelDuplicateException;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.map.common.Serialization;
import org.keycloak.models.map.storage.MapKeycloakTransaction;
import org.keycloak.models.map.storage.MapStorage;

import java.util.Comparator;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static org.keycloak.common.util.StackUtil.getShortStackTrace;

public class MapGroupProvider implements GroupProvider {

    private static final Logger LOG = Logger.getLogger(MapGroupProvider.class);
    private static final Predicate<MapGroupEntity> ALWAYS_FALSE = c -> { return false; };
    private final KeycloakSession session;
    final MapKeycloakTransaction<UUID, MapGroupEntity> tx;
    private final MapStorage<UUID, MapGroupEntity> groupStore;

    public MapGroupProvider(KeycloakSession session, MapStorage<UUID, MapGroupEntity> groupStore) {
        this.session = session;
        this.groupStore = groupStore;
        this.tx = new MapKeycloakTransaction<>(groupStore);
        session.getTransactionManager().enlist(tx);
    }

    private MapGroupEntity registerEntityForChanges(MapGroupEntity origEntity) {
        final MapGroupEntity res = Serialization.from(origEntity);
        tx.putIfChanged(origEntity.getId(), res, MapGroupEntity::isUpdated);
        return res;
    }

    private Function<MapGroupEntity, GroupModel> entityToAdapterFunc(RealmModel realm) {
        // Clone entity before returning back, to avoid giving away a reference to the live object to the caller
        return origEntity -> new MapGroupAdapter(session, realm, registerEntityForChanges(origEntity));
    }

    private Predicate<MapGroupEntity> entityRealmFilter(RealmModel realm) {
        if (realm == null || realm.getId() == null) {
            return MapGroupProvider.ALWAYS_FALSE;
        }
        String realmId = realm.getId();
        return entity -> Objects.equals(realmId, entity.getRealmId());
    }

    @Override
    public GroupModel getGroupById(RealmModel realm, String id) {
        if (id == null) {
            return null;
        }

        LOG.tracef("getGroupById(%s, %s)%s", realm, id, getShortStackTrace());


        UUID uid;
        try {
            uid = UUID.fromString(id);
        } catch (IllegalArgumentException ex) {
            return null;
        }
        
        MapGroupEntity entity = tx.get(uid, groupStore::get);
        return (entity == null || ! entityRealmFilter(realm).test(entity))
                ? null
                : entityToAdapterFunc(realm).apply(entity);
    }

    private Stream<MapGroupEntity> getNotRemovedUpdatedGroupsStream() {
        Stream<MapGroupEntity> updatedAndNotRemovedGroupsStream = groupStore.entrySet().stream()
                .map(tx::getUpdated)    // If the group has been removed, tx.get will return null, otherwise it will return me.getValue()
                .filter(Objects::nonNull);
        return Stream.concat(tx.createdValuesStream(groupStore.keySet()), updatedAndNotRemovedGroupsStream);
    }

    private Stream<MapGroupEntity> getUnsortedGroupEntitiesStream(RealmModel realm) {
        return getNotRemovedUpdatedGroupsStream()
                .filter(entityRealmFilter(realm));
    }

    @Override
    public Stream<GroupModel> getGroupsStream(RealmModel realm) {
        LOG.tracef("getGroupsStream(%s)%s", realm, getShortStackTrace());
        return getUnsortedGroupEntitiesStream(realm)
                .map(entityToAdapterFunc(realm))
                .sorted(GroupModel.COMPARE_BY_NAME)
                ;
    }

    @Override
    public Stream<GroupModel> getGroupsStream(RealmModel realm, Stream<String> ids, String search, Integer first, Integer max) {
        Stream<GroupModel> groupModelStream = ids.map(id -> session.groups().getGroupById(realm, id))
                .sorted(Comparator.comparing(GroupModel::getName));

        if (search != null) {
            String s = search.toLowerCase();
            groupModelStream = groupModelStream.filter(groupModel -> groupModel.getName().toLowerCase().contains(s));
        }

        if (first != null && first > 0) {
            groupModelStream = groupModelStream.skip(first);
        }

        if (max != null && max >= 0) {
            groupModelStream = groupModelStream.limit(max);
        }

        return groupModelStream;
    }

    @Override
    public Long getGroupsCount(RealmModel realm, Boolean onlyTopGroups) {
        LOG.tracef("getGroupsCount(%s, %s)%s", realm, onlyTopGroups, getShortStackTrace());
        Stream<MapGroupEntity> groupModelStream = getUnsortedGroupEntitiesStream(realm);

        if (onlyTopGroups) {
            groupModelStream = groupModelStream.filter(groupEntity -> Objects.isNull(groupEntity.getParentId()));
        }
        
        return groupModelStream.count();
    }

    @Override
    public Long getGroupsCountByNameContaining(RealmModel realm, String search) {
        return searchForGroupByNameStream(realm, search, null, null).count();
    }

    @Override
    public Stream<GroupModel> getGroupsByRoleStream(RealmModel realm, RoleModel role, Integer firstResult, Integer maxResults) {
        LOG.tracef("getGroupsByRole(%s, %s, %d, %d)%s", realm, role, firstResult, maxResults, getShortStackTrace());
        Stream<GroupModel> groupModelStream = getGroupsStream(realm).filter(groupModel -> groupModel.hasRole(role));

        if (firstResult != null && firstResult > 0) {
            groupModelStream = groupModelStream.skip(firstResult);
        }
        
        if (maxResults != null && maxResults >= 0) {
            groupModelStream = groupModelStream.limit(maxResults);
        }

        return groupModelStream;
    }

    @Override
    public Stream<GroupModel> getTopLevelGroupsStream(RealmModel realm) {
        LOG.tracef("getTopLevelGroupsStream(%s)%s", realm, getShortStackTrace());
        return getGroupsStream(realm)
                .filter(groupModel -> Objects.isNull(groupModel.getParentId()));
    }

    @Override
    public Stream<GroupModel> getTopLevelGroupsStream(RealmModel realm, Integer firstResult, Integer maxResults) {
        Stream<GroupModel> groupModelStream = getTopLevelGroupsStream(realm);
        
        if (firstResult != null && firstResult > 0) {
            groupModelStream = groupModelStream.skip(firstResult);
        }
        
        if (maxResults != null && maxResults >= 0) {
            groupModelStream = groupModelStream.limit(maxResults);
        }
        
        return groupModelStream;
        
    }

    @Override
    public Stream<GroupModel> searchForGroupByNameStream(RealmModel realm, String search, Integer firstResult, Integer maxResults) {
        LOG.tracef("searchForGroupByNameStream(%s, %s, %d, %d)%s", realm, search, firstResult, maxResults, getShortStackTrace());
        Stream<GroupModel> groupModelStream = getGroupsStream(realm)
                .filter(groupModel -> groupModel.getName().contains(search));

        if (firstResult != null && firstResult > 0) {
            groupModelStream = groupModelStream.skip(firstResult);
        }

        if (maxResults != null && maxResults >= 0) {
            groupModelStream = groupModelStream.limit(maxResults);
        }

        return groupModelStream;
    }

    @Override
    public GroupModel createGroup(RealmModel realm, String id, String name, GroupModel toParent) {
        LOG.tracef("createGroup(%s, %s, %s, %s)%s", realm, id, name, toParent, getShortStackTrace());
        final UUID entityId = id == null ? UUID.randomUUID() : UUID.fromString(id);

        // Check Db constraint: uniqueConstraints = { @UniqueConstraint(columnNames = {"REALM_ID", "PARENT_GROUP", "NAME"})}
        if (getUnsortedGroupEntitiesStream(realm)
                .anyMatch(groupEntity -> 
                        Objects.equals(groupEntity.getParentId(), toParent == null ? null : toParent.getId()) &&
                        Objects.equals(groupEntity.getName(), name))) {
            throw new ModelDuplicateException("Group with name '" + name + "' in realm " + realm.getName() + " already exists for requested parent" );
        }

        MapGroupEntity entity = new MapGroupEntity(entityId, realm.getId());
        entity.setName(name);
        entity.setParentId(toParent == null ? null : toParent.getId());
        if (tx.get(entity.getId(), groupStore::get) != null) {
            throw new ModelDuplicateException("Group exists: " + entityId);
        }
        tx.putIfAbsent(entity.getId(), entity);

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

        group.getSubGroupsStream().forEach(subGroup -> session.groups().removeGroup(realm, subGroup));

        // TODO: ^^^^^^^ Up to here

        tx.remove(UUID.fromString(group.getId()));
        
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
        Stream<MapGroupEntity> possibleSiblings = getUnsortedGroupEntitiesStream(realm)
                .filter(mapGroupEntity -> Objects.equals(mapGroupEntity.getParentId(), parentId));

        if (possibleSiblings.map(MapGroupEntity::getName).anyMatch(Predicate.isEqual(group.getName()))) {
            throw new ModelDuplicateException("Parent already contains subgroup named '" + group.getName() + "'");
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

        Stream<MapGroupEntity> possibleSiblings = getUnsortedGroupEntitiesStream(realm)
                .filter(mapGroupEntity -> mapGroupEntity.getParentId() == null);

        if (possibleSiblings.map(MapGroupEntity::getName).anyMatch(Predicate.isEqual(subGroup.getName()))) {
            throw new ModelDuplicateException("There is already a top level group named '" + subGroup.getName() + "'");
        }

        subGroup.setParent(null);
    }

    @Override
    public void preRemove(RealmModel realm, RoleModel role) {
        LOG.tracef("preRemove(%s, %s)%s", realm, role, getShortStackTrace());
        final String roleId = role.getId();
        getUnsortedGroupEntitiesStream(realm)
                .filter(groupEntity -> groupEntity.getGrantedRoles().contains(roleId))
                .map(groupEntity -> session.groups().getGroupById(realm, groupEntity.getId().toString()))
                .forEach(groupModel -> groupModel.deleteRoleMapping(role));
    }

    @Override
    public void close() {
        
    }

}

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

package org.keycloak.models.map.role;

import org.jboss.logging.Logger;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ModelDuplicateException;
import org.keycloak.models.RealmModel;

import org.keycloak.models.RoleModel;
import org.keycloak.models.map.storage.MapKeycloakTransaction;
import org.keycloak.models.map.common.Serialization;
import java.util.Comparator;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.keycloak.models.map.storage.MapStorage;
import static org.keycloak.common.util.StackUtil.getShortStackTrace;
import org.keycloak.models.RoleContainerModel;
import org.keycloak.models.RoleProvider;
import org.keycloak.models.map.common.StreamUtils;

public class MapRoleProvider implements RoleProvider {

    private static final Logger LOG = Logger.getLogger(MapRoleProvider.class);
    private static final Predicate<MapRoleEntity> ALWAYS_FALSE = role -> { return false; };
    private final KeycloakSession session;
    final MapKeycloakTransaction<UUID, MapRoleEntity> tx;
    private final MapStorage<UUID, MapRoleEntity> roleStore;

    private static final Comparator<MapRoleEntity> COMPARE_BY_NAME = new Comparator<MapRoleEntity>() {
        @Override
        public int compare(MapRoleEntity o1, MapRoleEntity o2) {
            String r1 = o1 == null ? null : o1.getName();
            String r2 = o2 == null ? null : o2.getName();
            return r1 == r2 ? 0
              : r1 == null ? -1
              : r2 == null ? 1
              : r1.compareTo(r2);

        }
    };

    public MapRoleProvider(KeycloakSession session, MapStorage<UUID, MapRoleEntity> roleStore) {
        this.session = session;
        this.roleStore = roleStore;
        this.tx = new MapKeycloakTransaction<>(roleStore);
        session.getTransactionManager().enlist(tx);
    }

    private Function<MapRoleEntity, RoleModel> entityToAdapterFunc(RealmModel realm) {
        // Clone entity before returning back, to avoid giving away a reference to the live object to the caller

        return origEntity -> new MapRoleAdapter(session, realm, registerEntityForChanges(origEntity));
    }

    private MapRoleEntity registerEntityForChanges(MapRoleEntity origEntity) {
        final MapRoleEntity res = Serialization.from(origEntity);
        tx.putIfChanged(origEntity.getId(), res, MapRoleEntity::isUpdated);
        return res;
    }

    private Predicate<MapRoleEntity> entityRealmFilter(RealmModel realm) {
        if (realm == null || realm.getId() == null) {
            return MapRoleProvider.ALWAYS_FALSE;
        }
        String realmId = realm.getId();
        return entity -> Objects.equals(realmId, entity.getRealmId());
    }

    private Predicate<MapRoleEntity> entityClientFilter(ClientModel client) {
        if (client == null || client.getId() == null) {
            return MapRoleProvider.ALWAYS_FALSE;
        }
        String clientId = client.getId();
        return entity -> entity.isClientRole() && 
                Objects.equals(clientId, entity.getClientId());
    }

    private Stream<MapRoleEntity> getNotRemovedUpdatedRolesStream(RealmModel realm) {
        Stream<MapRoleEntity> updatedAndNotRemovedRolesStream = roleStore.entrySet().stream()
          .map(tx::getUpdated)    // If the role has been removed, tx.get will return null, otherwise it will return me.getValue()
          .filter(Objects::nonNull);
        return Stream.concat(tx.createdValuesStream(), updatedAndNotRemovedRolesStream)
                .filter(entityRealmFilter(realm));
    }

    @Override
    public RoleModel addRealmRole(RealmModel realm, String id, String name) {
        if (getRealmRole(realm, name) != null) {
            throw new ModelDuplicateException("Role exists: " + id);
        }

        final UUID entityId = id == null ? UUID.randomUUID() : UUID.fromString(id);

        LOG.tracef("addRealmRole(%s, %s, %s)%s", realm.getName(), id, name, getShortStackTrace());

        MapRoleEntity entity = new MapRoleEntity(entityId, realm.getId());
        entity.setName(name);
        entity.setRealmId(realm.getId());
        if (tx.get(entity.getId(), roleStore::get) != null) {
            throw new ModelDuplicateException("Role exists: " + id);
        }
        tx.putIfAbsent(entity.getId(), entity);
        return entityToAdapterFunc(realm).apply(entity);
    }

    @Override
    public Stream<RoleModel> getRealmRolesStream(RealmModel realm, Integer first, Integer max) {
        Stream<RoleModel> s = getRealmRolesStream(realm);
        if (first != null && first >= 0) {
            s = s.skip(first);
        }
        if (max != null && max >= 0) {
            s = s.limit(max);
        }
        return s;
    }

    @Override
    public Stream<RoleModel> getRealmRolesStream(RealmModel realm) {
        return getNotRemovedUpdatedRolesStream(realm)
                .filter(this::isRealmRole)
                .sorted(COMPARE_BY_NAME)
                .map(entityToAdapterFunc(realm));
    }

    private boolean isRealmRole(MapRoleEntity role) {
        return ! role.isClientRole();
    }

    @Override
    public RoleModel addClientRole(ClientModel client, String id, String name) {
        if (getClientRole(client, name) != null) {
            throw new ModelDuplicateException("Role exists: " + id);
        }

        final UUID entityId = id == null ? UUID.randomUUID() : UUID.fromString(id);

        LOG.tracef("addClientRole(%s, %s, %s)%s", client.getClientId(), id, name, getShortStackTrace());

        MapRoleEntity entity = new MapRoleEntity(entityId, client.getRealm().getId());
        entity.setName(name);
        entity.setClientRole(true);
        entity.setClientId(client.getId());
        if (tx.get(entity.getId(), roleStore::get) != null) {
            throw new ModelDuplicateException("Role exists: " + id);
        }
        tx.putIfAbsent(entity.getId(), entity);
        return entityToAdapterFunc(client.getRealm()).apply(entity);
    }

    @Override
    public Stream<RoleModel> getClientRolesStream(ClientModel client, Integer first, Integer max) {
        Stream<RoleModel> s = getClientRolesStream(client);
        if (first != null && first > 0) {
            s = s.skip(first);
        }
        if (max != null && max >= 0) {
            s = s.limit(max);
        }
        return s;
    }

    @Override
    public Stream<RoleModel> getClientRolesStream(ClientModel client) {
        return getNotRemovedUpdatedRolesStream(client.getRealm())
                .filter(entityClientFilter(client))
                .sorted(COMPARE_BY_NAME)
                .map(entityToAdapterFunc(client.getRealm()));
    }
    @Override
    public boolean removeRole(RoleModel role) {
        LOG.tracef("removeRole(%s(%s))%s", role.getName(), role.getId(), getShortStackTrace());

        RealmModel realm = role.isClientRole() ? ((ClientModel)role.getContainer()).getRealm() : (RealmModel)role.getContainer();

        session.users().preRemove(realm, role);

        RoleContainerModel container = role.getContainer();
        if (container.getDefaultRolesStream().anyMatch(r -> Objects.equals(r, role.getName()))) {
            container.removeDefaultRoles(role.getName());
        }

        //remove role from realm-roles composites
        try (Stream<MapRoleEntity> baseStream = getNotRemovedUpdatedRolesStream(realm)
                .filter(this::isRealmRole)
                .filter(MapRoleEntity::isComposite)) {

            StreamUtils.leftInnerJoinIterable(baseStream, MapRoleEntity::getCompositeRoles)
                .filter(pair -> role.getId().equals(pair.getV()))
                .collect(Collectors.toSet())
                .forEach(pair -> {
                    MapRoleEntity origEntity = pair.getK();
                    registerEntityForChanges(origEntity);
                    origEntity.removeCompositeRole(role.getId());
                });
        }

        //remove role from client-roles composites
        session.clients().getClientsStream(realm).forEach(client -> {
            client.deleteScopeMapping(role);
            try (Stream<MapRoleEntity> baseStream = getNotRemovedUpdatedRolesStream(client.getRealm())
                    .filter(entityClientFilter(client))
                    .filter(MapRoleEntity::isComposite)) {
                
                StreamUtils.leftInnerJoinIterable(baseStream, MapRoleEntity::getCompositeRoles)
                    .filter(pair -> role.getId().equals(pair.getV()))
                    .collect(Collectors.toSet())
                    .forEach(pair -> {
                        MapRoleEntity origEntity = pair.getK();
                        registerEntityForChanges(origEntity);
                        origEntity.removeCompositeRole(role.getId());
                    });
            }
        });
        
        session.groups().preRemove(realm, role);

        // TODO: Sending an event should be extracted to store layer
        session.getKeycloakSessionFactory().publish(new RoleContainerModel.RoleRemovedEvent() {
            @Override
            public RoleModel getRole() {
                return role;
            }

            @Override
            public KeycloakSession getKeycloakSession() {
                return session;
            }
        });
        // TODO: ^^^^^^^ Up to here

        tx.remove(UUID.fromString(role.getId()));

        return true;
    }

    @Override
    public void removeRoles(RealmModel realm) {
        getRealmRolesStream(realm).forEach(this::removeRole);
    }

    @Override
    public void removeRoles(ClientModel client) {
        getClientRolesStream(client).forEach(this::removeRole);
    }

    @Override
    public RoleModel getRealmRole(RealmModel realm, String name) {
        if (name == null) {
            return null;
        }
        LOG.tracef("getRealmRole(%s, %s)%s", realm.getName(), name, getShortStackTrace());

        String roleNameLower = name.toLowerCase();

        String roleId = getNotRemovedUpdatedRolesStream(realm)
                .filter(entity -> entity.getName()!= null && Objects.equals(entity.getName().toLowerCase(), roleNameLower))
                .map(entityToAdapterFunc(realm))
                .map(RoleModel::getId)
                .findFirst()
                .orElse(null);
        //we need to go via session.roles() not to bypass cache
        return roleId == null ? null : session.roles().getRoleById(realm, roleId);
    }

    @Override
    public RoleModel getClientRole(ClientModel client, String name) {
        if (name == null) {
            return null;
        }
        LOG.tracef("getClientRole(%s, %s)%s", client.getClientId(), name, getShortStackTrace());

        String roleNameLower = name.toLowerCase();

        String roleId = getNotRemovedUpdatedRolesStream(client.getRealm())
                .filter(entityClientFilter(client))
                .filter(entity -> entity.getName()!= null && Objects.equals(entity.getName().toLowerCase(), roleNameLower))
                .map(entityToAdapterFunc(client.getRealm()))
                .map(RoleModel::getId)
                .findFirst()
                .orElse(null);
        //we need to go via session.roles() not to bypass cache
        return roleId == null ? null : session.roles().getRoleById(client.getRealm(), roleId);
    }

    @Override
    public RoleModel getRoleById(RealmModel realm, String id) {
        if (id == null) {
            return null;
        }

        LOG.tracef("getRoleById(%s, %s)%s", realm.getName(), id, getShortStackTrace());

        MapRoleEntity entity = tx.get(UUID.fromString(id), roleStore::get);
        return (entity == null || ! entityRealmFilter(realm).test(entity))
          ? null
          : entityToAdapterFunc(realm).apply(entity);
    }

    @Override
    public Stream<RoleModel> searchForRolesStream(RealmModel realm, String search, Integer first, Integer max) {
        if (search == null) {
            return Stream.empty();
        }
        String searchLower = search.toLowerCase();
        Stream<MapRoleEntity> s = getNotRemovedUpdatedRolesStream(realm)
            .filter(entity -> 
                (entity.getName() != null && entity.getName().toLowerCase().contains(searchLower)) || 
                (entity.getDescription() != null && entity.getDescription().toLowerCase().contains(searchLower))
            )
            .sorted(COMPARE_BY_NAME);

        if (first != null && first > 0) {
            s = s.skip(first);
        }
        if (max != null && max >= 0) {
            s = s.limit(max);
        }

        return s.map(entityToAdapterFunc(realm));
    }

    @Override
    public Stream<RoleModel> searchForClientRolesStream(ClientModel client, String search, Integer first, Integer max) {
        if (search == null) {
            return Stream.empty();
        }
        String searchLower = search.toLowerCase();
        Stream<MapRoleEntity> s = getNotRemovedUpdatedRolesStream(client.getRealm())
            .filter(entityClientFilter(client))
            .filter(entity -> 
                (entity.getName() != null && entity.getName().toLowerCase().contains(searchLower)) || 
                (entity.getDescription() != null && entity.getDescription().toLowerCase().contains(searchLower))
            )
            .sorted(COMPARE_BY_NAME);

        if (first != null && first > 0) {
            s = s.skip(first);
        }
        if (max != null && max >= 0) {
            s = s.limit(max);
        }

        return s.map(entityToAdapterFunc(client.getRealm()));
    }

    @Override
    public void close() {
    }
}

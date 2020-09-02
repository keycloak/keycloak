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

import java.util.Collection;
import org.jboss.logging.Logger;
import org.keycloak.models.ClientModel;
import org.keycloak.models.ClientProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ModelDuplicateException;
import org.keycloak.models.RealmModel;

import org.keycloak.models.RealmModel.ClientUpdatedEvent;
import org.keycloak.models.RealmProvider;
import org.keycloak.models.RoleModel;
import org.keycloak.models.map.storage.MapKeycloakTransaction;
import org.keycloak.models.map.common.Serialization;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.keycloak.models.map.storage.MapStorage;
import static org.keycloak.common.util.StackUtil.getShortStackTrace;
import org.keycloak.models.RoleContainerModel;
import org.keycloak.models.RoleProvider;
import org.keycloak.models.map.client.MapClientEntity;
import org.keycloak.models.map.client.MapClientProvider;

public class MapRoleProvider implements RoleProvider {

    private static final Logger LOG = Logger.getLogger(MapRoleProvider.class);
    private static final Predicate<MapRoleEntity> ALWAYS_FALSE = c -> { return false; };
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
        session.getTransactionManager().enlistAfterCompletion(tx);
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

    @Override
    public RoleModel addRealmRole(RealmModel realm, String id, String name) {
        final UUID entityId = id == null ? UUID.randomUUID() : UUID.fromString(id);

        MapRoleEntity entity = new MapRoleEntity(entityId, realm.getId());
        entity.setName(name);
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
        return getNotRemovedUpdatedRealmRolesStream()
                .filter(entityRealmFilter(realm))
                .sorted(COMPARE_BY_NAME)
                .map(entityToAdapterFunc(realm));
    }

    private Predicate<MapRoleEntity> entityRealmFilter(RealmModel realm) {
        if (realm == null || realm.getId() == null) {
            return MapRoleProvider.ALWAYS_FALSE;
        }
        String realmId = realm.getId();
        return entity -> Objects.equals(realmId, entity.getContainerId());
    }

    private Stream<MapClientEntity> getNotRemovedUpdatedClientsStream() {
        Stream<MapClientEntity> updatedAndNotRemovedClientsStream = clientStore.entrySet().stream()
          .map(tx::getUpdated)    // If the client has been removed, tx.get will return null, otherwise it will return me.getValue()
          .filter(Objects::nonNull);
        return Stream.concat(tx.createdValuesStream(clientStore.keySet()), updatedAndNotRemovedClientsStream);
    }

    @Override
    public boolean removeRole(RoleModel role) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void removeRoles(RealmModel realm) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public RoleModel addClientRole(ClientModel client, String name) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public RoleModel addClientRole(ClientModel client, String id, String name) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Stream<RoleModel> getClientRolesStream(ClientModel client, Integer first, Integer max) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void removeRoles(ClientModel client) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public RoleModel getRealmRole(RealmModel realm, String name) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public RoleModel getRoleById(RealmModel realm, String id) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Stream<RoleModel> searchForRolesStream(RealmModel realm, String search, Integer first, Integer max) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public RoleModel getClientRole(ClientModel client, String name) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Stream<RoleModel> searchForClientRolesStream(ClientModel client, String search, Integer first, Integer max) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void close() {
    }
}

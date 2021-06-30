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

import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Stream;
import org.keycloak.models.map.storage.MapStorage;
import static org.keycloak.common.util.StackUtil.getShortStackTrace;
import static org.keycloak.models.map.common.MapStorageUtils.registerEntityForChanges;
import static org.keycloak.models.map.storage.QueryParameters.Order.ASCENDING;
import static org.keycloak.models.map.storage.QueryParameters.withCriteria;

import org.keycloak.models.RoleContainerModel;
import org.keycloak.models.RoleModel.SearchableFields;
import org.keycloak.models.RoleProvider;
import org.keycloak.models.map.storage.ModelCriteriaBuilder;
import org.keycloak.models.map.storage.ModelCriteriaBuilder.Operator;

public class MapRoleProvider<K> implements RoleProvider {

    private static final Logger LOG = Logger.getLogger(MapRoleProvider.class);
    private final KeycloakSession session;
    final MapKeycloakTransaction<K, MapRoleEntity<K>, RoleModel> tx;
    private final MapStorage<K, MapRoleEntity<K>, RoleModel> roleStore;

    public MapRoleProvider(KeycloakSession session, MapStorage<K, MapRoleEntity<K>, RoleModel> roleStore) {
        this.session = session;
        this.roleStore = roleStore;
        this.tx = roleStore.createTransaction(session);
        session.getTransactionManager().enlist(tx);
    }

    private Function<MapRoleEntity<K>, RoleModel> entityToAdapterFunc(RealmModel realm) {
        // Clone entity before returning back, to avoid giving away a reference to the live object to the caller
        return origEntity -> new MapRoleAdapter<K>(session, realm, registerEntityForChanges(tx, origEntity)) {
            @Override
            public String getId() {
                return roleStore.getKeyConvertor().keyToString(entity.getId());
            }
        };
    }

    @Override
    public RoleModel addRealmRole(RealmModel realm, String id, String name) {
        if (getRealmRole(realm, name) != null) {
            throw new ModelDuplicateException("Role exists: " + id);
        }

        final K entityId = id == null ? roleStore.getKeyConvertor().yieldNewUniqueKey() : roleStore.getKeyConvertor().fromString(id);

        LOG.tracef("addRealmRole(%s, %s, %s)%s", realm, id, name, getShortStackTrace());

        MapRoleEntity<K> entity = new MapRoleEntity<K>(entityId, realm.getId());
        entity.setName(name);
        entity.setRealmId(realm.getId());
        if (tx.read(entity.getId()) != null) {
            throw new ModelDuplicateException("Role exists: " + id);
        }
        tx.create(entity);
        return entityToAdapterFunc(realm).apply(entity);
    }

    @Override
    public Stream<RoleModel> getRealmRolesStream(RealmModel realm, Integer first, Integer max) {
        ModelCriteriaBuilder<RoleModel> mcb = roleStore.createCriteriaBuilder()
                .compare(SearchableFields.REALM_ID, Operator.EQ, realm.getId())
                .compare(SearchableFields.IS_CLIENT_ROLE, Operator.NE, true);

        return tx.read(withCriteria(mcb).pagination(first, max, SearchableFields.NAME))
            .map(entityToAdapterFunc(realm));
    }

    @Override
    public Stream<RoleModel> getRealmRolesStream(RealmModel realm) {
        ModelCriteriaBuilder<RoleModel> mcb = roleStore.createCriteriaBuilder()
          .compare(SearchableFields.REALM_ID, Operator.EQ, realm.getId())
          .compare(SearchableFields.IS_CLIENT_ROLE, Operator.NE, true);
        
        return tx.read(withCriteria(mcb).orderBy(SearchableFields.NAME, ASCENDING))
                .map(entityToAdapterFunc(realm));
    }

    @Override
    public RoleModel addClientRole(ClientModel client, String id, String name) {
        if (getClientRole(client, name) != null) {
            throw new ModelDuplicateException("Role exists: " + id);
        }

        final K entityId = id == null ? roleStore.getKeyConvertor().yieldNewUniqueKey() : roleStore.getKeyConvertor().fromString(id);

        LOG.tracef("addClientRole(%s, %s, %s)%s", client, id, name, getShortStackTrace());

        MapRoleEntity<K> entity = new MapRoleEntity<K>(entityId, client.getRealm().getId());
        entity.setName(name);
        entity.setClientRole(true);
        entity.setClientId(client.getId());
        if (tx.read(entity.getId()) != null) {
            throw new ModelDuplicateException("Role exists: " + id);
        }
        tx.create(entity);
        return entityToAdapterFunc(client.getRealm()).apply(entity);
    }

    @Override
    public Stream<RoleModel> getClientRolesStream(ClientModel client, Integer first, Integer max) {
        ModelCriteriaBuilder<RoleModel> mcb = roleStore.createCriteriaBuilder()
                .compare(SearchableFields.REALM_ID, Operator.EQ, client.getRealm().getId())
                .compare(SearchableFields.CLIENT_ID, Operator.EQ, client.getId());

        return tx.read(withCriteria(mcb).pagination(first, max, SearchableFields.NAME))
                .map(entityToAdapterFunc(client.getRealm()));
    }

    @Override
    public Stream<RoleModel> getClientRolesStream(ClientModel client) {
        ModelCriteriaBuilder<RoleModel> mcb = roleStore.createCriteriaBuilder()
          .compare(SearchableFields.REALM_ID, Operator.EQ, client.getRealm().getId())
          .compare(SearchableFields.CLIENT_ID, Operator.EQ, client.getId());

        return tx.read(withCriteria(mcb).orderBy(SearchableFields.NAME, ASCENDING))
                .map(entityToAdapterFunc(client.getRealm()));
    }
    @Override
    public boolean removeRole(RoleModel role) {
        LOG.tracef("removeRole(%s(%s))%s", role.getName(), role.getId(), getShortStackTrace());

        RealmModel realm = role.isClientRole() ? ((ClientModel)role.getContainer()).getRealm() : (RealmModel)role.getContainer();

        session.users().preRemove(realm, role);

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

        tx.delete(roleStore.getKeyConvertor().fromString(role.getId()));

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
        LOG.tracef("getRealmRole(%s, %s)%s", realm, name, getShortStackTrace());

        ModelCriteriaBuilder<RoleModel> mcb = roleStore.createCriteriaBuilder()
          .compare(SearchableFields.REALM_ID, Operator.EQ, realm.getId())
          .compare(SearchableFields.NAME, Operator.ILIKE, name);

        String roleId = tx.read(withCriteria(mcb))
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
        LOG.tracef("getClientRole(%s, %s)%s", client, name, getShortStackTrace());

        ModelCriteriaBuilder<RoleModel> mcb = roleStore.createCriteriaBuilder()
          .compare(SearchableFields.REALM_ID, Operator.EQ, client.getRealm().getId())
          .compare(SearchableFields.CLIENT_ID, Operator.EQ, client.getId())
          .compare(SearchableFields.NAME, Operator.ILIKE, name);

        String roleId = tx.read(withCriteria(mcb))
                .map(entityToAdapterFunc(client.getRealm()))
                .map(RoleModel::getId)
                .findFirst()
                .orElse(null);
        //we need to go via session.roles() not to bypass cache
        return roleId == null ? null : session.roles().getRoleById(client.getRealm(), roleId);
    }

    @Override
    public RoleModel getRoleById(RealmModel realm, String id) {
        if (id == null || realm == null || realm.getId() == null) {
            return null;
        }

        LOG.tracef("getRoleById(%s, %s)%s", realm, id, getShortStackTrace());

        MapRoleEntity<K> entity = tx.read(roleStore.getKeyConvertor().fromStringSafe(id));
        String realmId = realm.getId();
        return (entity == null || ! Objects.equals(realmId, entity.getRealmId()))
          ? null
          : entityToAdapterFunc(realm).apply(entity);
    }

    @Override
    public Stream<RoleModel> searchForRolesStream(RealmModel realm, String search, Integer first, Integer max) {
        if (search == null) {
            return Stream.empty();
        }
        ModelCriteriaBuilder<RoleModel> mcb = roleStore.createCriteriaBuilder()
          .compare(SearchableFields.REALM_ID, Operator.EQ, realm.getId())
          .or(
            roleStore.createCriteriaBuilder().compare(SearchableFields.NAME, Operator.ILIKE, "%" + search + "%"),
            roleStore.createCriteriaBuilder().compare(SearchableFields.DESCRIPTION, Operator.ILIKE, "%" + search + "%")
          );

        return tx.read(withCriteria(mcb).pagination(first, max, SearchableFields.NAME))
                .map(entityToAdapterFunc(realm));
    }

    @Override
    public Stream<RoleModel> searchForClientRolesStream(ClientModel client, String search, Integer first, Integer max) {
        if (search == null) {
            return Stream.empty();
        }
        ModelCriteriaBuilder<RoleModel> mcb = roleStore.createCriteriaBuilder()
          .compare(SearchableFields.REALM_ID, Operator.EQ, client.getRealm().getId())
          .compare(SearchableFields.CLIENT_ID, Operator.EQ, client.getId())
          .or(
            roleStore.createCriteriaBuilder().compare(SearchableFields.NAME, Operator.ILIKE, "%" + search + "%"),
            roleStore.createCriteriaBuilder().compare(SearchableFields.DESCRIPTION, Operator.ILIKE, "%" + search + "%")
          );
        return tx.read(withCriteria(mcb).pagination(first, max, SearchableFields.NAME))
                .map(entityToAdapterFunc(client.getRealm()));
    }

    @Override
    public void close() {
    }
}

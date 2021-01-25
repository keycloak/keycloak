/*
 * Copyright 2021 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.models.map.clientscope;

import java.util.Comparator;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.jboss.logging.Logger;
import static org.keycloak.common.util.StackUtil.getShortStackTrace;
import org.keycloak.models.ClientScopeModel.SearchableFields;
import org.keycloak.models.ClientScopeModel;
import org.keycloak.models.ClientScopeProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ModelDuplicateException;
import org.keycloak.models.ModelException;
import org.keycloak.models.RealmModel;
import org.keycloak.models.map.common.Serialization;
import org.keycloak.models.map.storage.MapKeycloakTransaction;
import org.keycloak.models.map.storage.MapStorage;
import org.keycloak.models.map.storage.ModelCriteriaBuilder;
import org.keycloak.models.map.storage.ModelCriteriaBuilder.Operator;
import org.keycloak.models.utils.KeycloakModelUtils;

public class MapClientScopeProvider implements ClientScopeProvider {

    private static final Logger LOG = Logger.getLogger(MapClientScopeProvider.class);
    private static final Predicate<MapClientScopeEntity> ALWAYS_FALSE = c -> { return false; };
    private final KeycloakSession session;
    private final MapKeycloakTransaction<UUID, MapClientScopeEntity, ClientScopeModel> tx;
    private final MapStorage<UUID, MapClientScopeEntity, ClientScopeModel> clientScopeStore;

    private static final Comparator<MapClientScopeEntity> COMPARE_BY_NAME = Comparator.comparing(MapClientScopeEntity::getName);

    public MapClientScopeProvider(KeycloakSession session, MapStorage<UUID, MapClientScopeEntity, ClientScopeModel> clientScopeStore) {
        this.session = session;
        this.clientScopeStore = clientScopeStore;
        this.tx = clientScopeStore.createTransaction();
        session.getTransactionManager().enlist(tx);
    }

    private MapClientScopeEntity registerEntityForChanges(MapClientScopeEntity origEntity) {
        final MapClientScopeEntity res = tx.read(origEntity.getId(), id -> Serialization.from(origEntity));
        tx.updateIfChanged(origEntity.getId(), res, MapClientScopeEntity::isUpdated);
        return res;
    }

    private Function<MapClientScopeEntity, ClientScopeModel> entityToAdapterFunc(RealmModel realm) {
        // Clone entity before returning back, to avoid giving away a reference to the live object to the caller

        return origEntity -> new MapClientScopeAdapter(session, realm, registerEntityForChanges(origEntity));
    }

    private Predicate<MapClientScopeEntity> entityRealmFilter(RealmModel realm) {
        if (realm == null || realm.getId() == null) {
            return MapClientScopeProvider.ALWAYS_FALSE;
        }
        String realmId = realm.getId();
        return entity -> Objects.equals(realmId, entity.getRealmId());
    }

    @Override
    public Stream<ClientScopeModel> getClientScopesStream(RealmModel realm) {
        ModelCriteriaBuilder<ClientScopeModel> mcb = clientScopeStore.createCriteriaBuilder()
            .compare(SearchableFields.REALM_ID, Operator.EQ, realm.getId());

        return tx.getUpdatedNotRemoved(mcb)
          .sorted(COMPARE_BY_NAME)
          .map(entityToAdapterFunc(realm));
    }

    @Override
    public ClientScopeModel addClientScope(RealmModel realm, String id, String name) {
        // Check Db constraint: @UniqueConstraint(columnNames = {"REALM_ID", "NAME"})
        ModelCriteriaBuilder<ClientScopeModel> mcb = clientScopeStore.createCriteriaBuilder()
            .compare(SearchableFields.REALM_ID, Operator.EQ, realm.getId())
            .compare(SearchableFields.NAME, Operator.EQ, name);

        if (tx.getCount(mcb) > 0) {
            throw new ModelDuplicateException("Client scope with name '" + name + "' in realm " + realm.getName());
        }

        final UUID entityId = id == null ? UUID.randomUUID() : UUID.fromString(id);

        LOG.tracef("addClientScope(%s, %s, %s)%s", realm, id, name, getShortStackTrace());

        MapClientScopeEntity entity = new MapClientScopeEntity(entityId, realm.getId());
        entity.setName(KeycloakModelUtils.convertClientScopeName(name));
        if (tx.read(entity.getId()) != null) {
            throw new ModelDuplicateException("Client scope exists: " + id);
        }
        tx.create(entity.getId(), entity);
        return entityToAdapterFunc(realm).apply(entity);
    }

    @Override
    public boolean removeClientScope(RealmModel realm, String id) {
        if (id == null) return false;
        ClientScopeModel clientScope = getClientScopeById(realm, id);
        if (clientScope == null) return false;

        if (KeycloakModelUtils.isClientScopeUsed(realm, clientScope)) {
            throw new ModelException("Cannot remove client scope, it is currently in use");
        }

        session.users().preRemove(clientScope);
        realm.removeDefaultClientScope(clientScope);

        tx.delete(UUID.fromString(id));
        return true;
    }

    @Override
    public void removeClientScopes(RealmModel realm) {
        LOG.tracef("removeClients(%s)%s", realm, getShortStackTrace());

        getClientScopesStream(realm)
            .map(ClientScopeModel::getId)
            .collect(Collectors.toSet())  // This is necessary to read out all the client IDs before removing the clients
            .forEach(id -> removeClientScope(realm, id));
    }

    @Override
    public ClientScopeModel getClientScopeById(RealmModel realm, String id) {
        if (id == null) {
            return null;
        }

        LOG.tracef("getClientScopeById(%s, %s)%s", realm, id, getShortStackTrace());

        UUID uuid;
        try {
            uuid = UUID.fromString(id);
        } catch (IllegalArgumentException ex) {
            return null;
        }

        MapClientScopeEntity entity = tx.read(uuid);
        return (entity == null || ! entityRealmFilter(realm).test(entity))
          ? null
          : entityToAdapterFunc(realm).apply(entity);
    }

    @Override
    public void close() {
    }
}

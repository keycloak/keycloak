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

import java.util.Objects;
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
import org.keycloak.models.RealmModel;
import org.keycloak.models.map.storage.MapKeycloakTransaction;
import org.keycloak.models.map.storage.MapStorage;
import org.keycloak.models.map.storage.ModelCriteriaBuilder;
import org.keycloak.models.map.storage.ModelCriteriaBuilder.Operator;
import org.keycloak.models.utils.KeycloakModelUtils;

import static org.keycloak.models.map.common.MapStorageUtils.registerEntityForChanges;
import static org.keycloak.models.map.storage.QueryParameters.Order.ASCENDING;
import static org.keycloak.models.map.storage.QueryParameters.withCriteria;

public class MapClientScopeProvider<K> implements ClientScopeProvider {

    private static final Logger LOG = Logger.getLogger(MapClientScopeProvider.class);
    private final KeycloakSession session;
    private final MapKeycloakTransaction<K, MapClientScopeEntity<K>, ClientScopeModel> tx;
    private final MapStorage<K, MapClientScopeEntity<K>, ClientScopeModel> clientScopeStore;

    public MapClientScopeProvider(KeycloakSession session, MapStorage<K, MapClientScopeEntity<K>, ClientScopeModel> clientScopeStore) {
        this.session = session;
        this.clientScopeStore = clientScopeStore;
        this.tx = clientScopeStore.createTransaction(session);
        session.getTransactionManager().enlist(tx);
    }

    private Function<MapClientScopeEntity<K>, ClientScopeModel> entityToAdapterFunc(RealmModel realm) {
        // Clone entity before returning back, to avoid giving away a reference to the live object to the caller

        return origEntity -> new MapClientScopeAdapter<K>(session, realm, registerEntityForChanges(tx, origEntity)) {
            @Override
            public String getId() {
                return clientScopeStore.getKeyConvertor().keyToString(entity.getId());
            }
        };
    }

    private Predicate<MapClientScopeEntity<K>> entityRealmFilter(RealmModel realm) {
        if (realm == null || realm.getId() == null) {
            return c -> false;
        }
        String realmId = realm.getId();
        return entity -> Objects.equals(realmId, entity.getRealmId());
    }

    @Override
    public Stream<ClientScopeModel> getClientScopesStream(RealmModel realm) {
        ModelCriteriaBuilder<ClientScopeModel> mcb = clientScopeStore.createCriteriaBuilder()
            .compare(SearchableFields.REALM_ID, Operator.EQ, realm.getId());

        return tx.read(withCriteria(mcb).orderBy(SearchableFields.NAME, ASCENDING))
          .map(entityToAdapterFunc(realm));
    }

    @Override
    public ClientScopeModel addClientScope(RealmModel realm, String id, String name) {
        // Check Db constraint: @UniqueConstraint(columnNames = {"REALM_ID", "NAME"})
        ModelCriteriaBuilder<ClientScopeModel> mcb = clientScopeStore.createCriteriaBuilder()
            .compare(SearchableFields.REALM_ID, Operator.EQ, realm.getId())
            .compare(SearchableFields.NAME, Operator.EQ, name);

        if (tx.getCount(withCriteria(mcb)) > 0) {
            throw new ModelDuplicateException("Client scope with name '" + name + "' in realm " + realm.getName());
        }

        final K entityId = id == null ? clientScopeStore.getKeyConvertor().yieldNewUniqueKey() : clientScopeStore.getKeyConvertor().fromString(id);

        LOG.tracef("addClientScope(%s, %s, %s)%s", realm, id, name, getShortStackTrace());

        MapClientScopeEntity<K> entity = new MapClientScopeEntity<>(entityId, realm.getId());
        entity.setName(KeycloakModelUtils.convertClientScopeName(name));
        if (tx.read(entity.getId()) != null) {
            throw new ModelDuplicateException("Client scope exists: " + id);
        }
        tx.create(entity);
        return entityToAdapterFunc(realm).apply(entity);
    }

    @Override
    public boolean removeClientScope(RealmModel realm, String id) {
        if (id == null) return false;
        ClientScopeModel clientScope = getClientScopeById(realm, id);
        if (clientScope == null) return false;

        session.users().preRemove(clientScope);
        realm.removeDefaultClientScope(clientScope);

        session.getKeycloakSessionFactory().publish(new ClientScopeModel.ClientScopeRemovedEvent() {

            @Override
            public KeycloakSession getKeycloakSession() {
                return session;
            }

            @Override
            public ClientScopeModel getClientScope() {
                return clientScope;
            }
        });

        tx.delete(clientScopeStore.getKeyConvertor().fromString(id));
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

        K uuid;
        try {
            uuid = clientScopeStore.getKeyConvertor().fromStringSafe(id);
        } catch (IllegalArgumentException ex) {
            return null;
        }

        MapClientScopeEntity<K> entity = tx.read(uuid);
        return (entity == null || ! entityRealmFilter(realm).test(entity))
          ? null
          : entityToAdapterFunc(realm).apply(entity);
    }

    @Override
    public void close() {
    }
}

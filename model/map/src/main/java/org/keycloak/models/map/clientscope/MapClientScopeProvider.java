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
import org.keycloak.models.ClientScopeModel.SearchableFields;
import org.keycloak.models.ClientScopeModel;
import org.keycloak.models.ClientScopeProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ModelDuplicateException;
import org.keycloak.models.RealmModel;
import org.keycloak.models.map.common.DeepCloner;
import org.keycloak.models.map.common.HasRealmId;
import org.keycloak.models.map.storage.MapKeycloakTransaction;
import org.keycloak.models.map.storage.MapStorage;
import org.keycloak.models.map.storage.ModelCriteriaBuilder.Operator;
import org.keycloak.models.map.storage.criteria.DefaultModelCriteria;
import org.keycloak.models.utils.KeycloakModelUtils;

import static org.keycloak.common.util.StackUtil.getShortStackTrace;
import static org.keycloak.models.map.common.AbstractMapProviderFactory.MapProviderObjectType.CLIENT_SCOPE_AFTER_REMOVE;
import static org.keycloak.models.map.common.AbstractMapProviderFactory.MapProviderObjectType.CLIENT_SCOPE_BEFORE_REMOVE;
import static org.keycloak.models.map.storage.QueryParameters.Order.ASCENDING;
import static org.keycloak.models.map.storage.QueryParameters.withCriteria;
import static org.keycloak.models.map.storage.criteria.DefaultModelCriteria.criteria;

public class MapClientScopeProvider implements ClientScopeProvider {

    private static final Logger LOG = Logger.getLogger(MapClientScopeProvider.class);
    private final KeycloakSession session;
    private final MapKeycloakTransaction<MapClientScopeEntity, ClientScopeModel> tx;
    private final boolean txHasRealmId;

    public MapClientScopeProvider(KeycloakSession session, MapStorage<MapClientScopeEntity, ClientScopeModel> clientScopeStore) {
        this.session = session;
        this.tx = clientScopeStore.createTransaction(session);
        session.getTransactionManager().enlist(tx);
        this.txHasRealmId = tx instanceof HasRealmId;
    }

    private Function<MapClientScopeEntity, ClientScopeModel> entityToAdapterFunc(RealmModel realm) {
        // Clone entity before returning back, to avoid giving away a reference to the live object to the caller

        return origEntity -> new MapClientScopeAdapter(session, realm, origEntity);
    }

    private MapKeycloakTransaction<MapClientScopeEntity, ClientScopeModel> txInRealm(RealmModel realm) {
        if (txHasRealmId) {
            ((HasRealmId) tx).setRealmId(realm == null ? null : realm.getId());
        }
        return tx;
    }

    private Predicate<MapClientScopeEntity> entityRealmFilter(RealmModel realm) {
        if (realm == null || realm.getId() == null) {
            return c -> false;
        }
        String realmId = realm.getId();
        return entity -> Objects.equals(realmId, entity.getRealmId());
    }

    @Override
    public Stream<ClientScopeModel> getClientScopesStream(RealmModel realm) {
        DefaultModelCriteria<ClientScopeModel> mcb = criteria();
        mcb = mcb.compare(SearchableFields.REALM_ID, Operator.EQ, realm.getId());

        return txInRealm(realm).read(withCriteria(mcb).orderBy(SearchableFields.NAME, ASCENDING))
          .map(entityToAdapterFunc(realm));
    }

    @Override
    public ClientScopeModel addClientScope(RealmModel realm, String id, String name) {
        DefaultModelCriteria<ClientScopeModel> mcb = criteria();
        mcb = mcb.compare(SearchableFields.REALM_ID, Operator.EQ, realm.getId())
            .compare(SearchableFields.NAME, Operator.EQ, name);

        if (txInRealm(realm).exists(withCriteria(mcb))) {
            throw new ModelDuplicateException("Client scope with name '" + name + "' in realm " + realm.getName());
        }

        if (id != null && txInRealm(realm).exists(id)) {
            throw new ModelDuplicateException("Client scope exists: " + id);
        }

        LOG.tracef("addClientScope(%s, %s, %s)%s", realm, id, name, getShortStackTrace());

        MapClientScopeEntity entity = DeepCloner.DUMB_CLONER.newInstance(MapClientScopeEntity.class);
        entity.setId(id);
        entity.setRealmId(realm.getId());
        entity.setName(KeycloakModelUtils.convertClientScopeName(name));
        
        entity = txInRealm(realm).create(entity);
        return entityToAdapterFunc(realm).apply(entity);
    }

    @Override
    public boolean removeClientScope(RealmModel realm, String id) {
        if (id == null) return false;
        ClientScopeModel clientScope = getClientScopeById(realm, id);
        if (clientScope == null) return false;

        session.invalidate(CLIENT_SCOPE_BEFORE_REMOVE, realm, clientScope);

        txInRealm(realm).delete(id);

        session.invalidate(CLIENT_SCOPE_AFTER_REMOVE, clientScope);

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

        MapClientScopeEntity entity = txInRealm(realm).read(id);
        return (entity == null || ! entityRealmFilter(realm).test(entity))
          ? null
          : entityToAdapterFunc(realm).apply(entity);
    }

    public void preRemove(RealmModel realm) {
        LOG.tracef("preRemove(%s)%s", realm, getShortStackTrace());
        DefaultModelCriteria<ClientScopeModel> mcb = criteria();
        mcb = mcb.compare(SearchableFields.REALM_ID, Operator.EQ, realm.getId());

        txInRealm(realm).delete(withCriteria(mcb));
    }

    @Override
    public void close() {
    }
}

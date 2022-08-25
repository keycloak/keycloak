/*
 * Copyright 2022 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.models.map.authorization;

import org.jboss.logging.Logger;
import org.keycloak.authorization.AuthorizationProvider;
import org.keycloak.authorization.model.ResourceServer;
import org.keycloak.authorization.model.Scope;
import org.keycloak.authorization.model.Scope.SearchableFields;
import org.keycloak.authorization.store.ScopeStore;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ModelDuplicateException;
import org.keycloak.models.RealmModel;
import org.keycloak.models.map.authorization.adapter.MapScopeAdapter;
import org.keycloak.models.map.authorization.entity.MapScopeEntity;
import org.keycloak.models.map.authorization.entity.MapScopeEntityImpl;
import org.keycloak.models.map.storage.MapKeycloakTransaction;
import org.keycloak.models.map.storage.MapStorage;
import org.keycloak.models.map.storage.ModelCriteriaBuilder.Operator;
import org.keycloak.models.map.storage.criteria.DefaultModelCriteria;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.keycloak.common.util.StackUtil.getShortStackTrace;
import static org.keycloak.models.map.storage.QueryParameters.withCriteria;
import static org.keycloak.models.map.storage.criteria.DefaultModelCriteria.criteria;

public class MapScopeStore implements ScopeStore {

    private static final Logger LOG = Logger.getLogger(MapScopeStore.class);
    private final AuthorizationProvider authorizationProvider;
    final MapKeycloakTransaction<MapScopeEntity, Scope> tx;
    private final KeycloakSession session;

    public MapScopeStore(KeycloakSession session, MapStorage<MapScopeEntity, Scope> scopeStore, AuthorizationProvider provider) {
        this.authorizationProvider = provider;
        this.tx = scopeStore.createTransaction(session);
        session.getTransactionManager().enlist(tx);
        this.session = session;
    }

    private Function<MapScopeEntity, Scope> entityToAdapterFunc(RealmModel realm, ResourceServer resourceServer) {
        return origEntity -> new MapScopeAdapter(realm, resourceServer, origEntity, authorizationProvider.getStoreFactory());
    }

    private DefaultModelCriteria<Scope> forRealmAndResourceServer(RealmModel realm, ResourceServer resourceServer) {
        DefaultModelCriteria<Scope> mcb = DefaultModelCriteria.<Scope>criteria()
                .compare(Scope.SearchableFields.REALM_ID, Operator.EQ, realm.getId());

        return resourceServer == null
                ? mcb
                : mcb.compare(SearchableFields.RESOURCE_SERVER_ID, Operator.EQ,
                resourceServer.getId());
    }

    @Override
    public Scope create(ResourceServer resourceServer, String id, String name) {
        LOG.tracef("create(%s, %s, %s)%s", id, name, resourceServer, getShortStackTrace());

        RealmModel realm = resourceServer.getRealm();
        // @UniqueConstraint(columnNames = {"NAME", "RESOURCE_SERVER_ID"})
        DefaultModelCriteria<Scope> mcb = forRealmAndResourceServer(realm, resourceServer)
                .compare(SearchableFields.NAME, Operator.EQ, name);

        if (tx.getCount(withCriteria(mcb)) > 0) {
            throw new ModelDuplicateException("Scope with name '" + name + "' for " + resourceServer.getId() + " already exists");
        }

        MapScopeEntity entity = new MapScopeEntityImpl();
        entity.setId(id);
        entity.setName(name);
        entity.setResourceServerId(resourceServer.getId());
        entity.setRealmId(resourceServer.getRealm().getId());

        entity = tx.create(entity);

        return entity == null ? null : entityToAdapterFunc(realm, resourceServer).apply(entity);
    }

    @Override
    public void delete(RealmModel realm, String id) {
        LOG.tracef("delete(%s)%s", id, getShortStackTrace());
        Scope scope = findById(realm, null, id);
        if (scope == null) return;

        tx.delete(id);
    }

    @Override
    public Scope findById(RealmModel realm, ResourceServer resourceServer, String id) {
        LOG.tracef("findById(%s, %s)%s", id, resourceServer, getShortStackTrace());

        if (id == null) return null;

        return tx.read(withCriteria(forRealmAndResourceServer(realm, resourceServer)
                .compare(SearchableFields.ID, Operator.EQ, id)))
                .findFirst()
                .map(entityToAdapterFunc(realm, resourceServer))
                .orElse(null);
    }

    @Override
    public Scope findByName(ResourceServer resourceServer, String name) {
        LOG.tracef("findByName(%s, %s)%s", name, resourceServer, getShortStackTrace());
        RealmModel realm = resourceServer.getRealm();

        return tx.read(withCriteria(forRealmAndResourceServer(realm, resourceServer).compare(SearchableFields.NAME,
                Operator.EQ, name)))
                .findFirst()
                .map(entityToAdapterFunc(realm, resourceServer))
                .orElse(null);
    }

    @Override
    public List<Scope> findByResourceServer(ResourceServer resourceServer) {
        LOG.tracef("findByResourceServer(%s)%s", resourceServer, getShortStackTrace());
        RealmModel realm = resourceServer.getRealm();
        return tx.read(withCriteria(forRealmAndResourceServer(realm, resourceServer)))
                .map(entityToAdapterFunc(realm, resourceServer))
                .collect(Collectors.toList());
    }

    @Override
    public List<Scope> findByResourceServer(ResourceServer resourceServer, Map<Scope.FilterOption, String[]> attributes, Integer firstResult, Integer maxResults) {
        RealmModel realm = resourceServer.getRealm();
        DefaultModelCriteria<Scope> mcb = forRealmAndResourceServer(realm, resourceServer);

        for (Scope.FilterOption filterOption : attributes.keySet()) {
            String[] value = attributes.get(filterOption);
            
            switch (filterOption) {
                case ID:
                    mcb = mcb.compare(Scope.SearchableFields.ID, Operator.IN, Arrays.asList(value));
                    break;
                case NAME:
                    mcb = mcb.compare(Scope.SearchableFields.NAME, Operator.ILIKE, "%" + value[0] + "%");
                    break;
                default:
                    throw new IllegalArgumentException("Unsupported filter [" + filterOption + "]");
            }
        }

        return tx.read(withCriteria(mcb).pagination(firstResult, maxResults, SearchableFields.NAME))
            .map(entityToAdapterFunc(realm, resourceServer))
            .collect(Collectors.toList());
    }

    public void preRemove(RealmModel realm) {
        LOG.tracef("preRemove(%s)%s", realm, getShortStackTrace());

        DefaultModelCriteria<Scope> mcb = criteria();
        mcb = mcb.compare(SearchableFields.REALM_ID, Operator.EQ, realm.getId());

        tx.delete(withCriteria(mcb));
    }

    public void preRemove(ResourceServer resourceServer) {
        LOG.tracef("preRemove(%s)%s", resourceServer, getShortStackTrace());

        tx.delete(withCriteria(forRealmAndResourceServer(resourceServer.getRealm(), resourceServer)));
    }
}

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

package org.keycloak.models.map.authorization;

import org.jboss.logging.Logger;
import org.keycloak.authorization.AuthorizationProvider;
import org.keycloak.authorization.model.ResourceServer;
import org.keycloak.authorization.model.Scope;
import org.keycloak.authorization.model.Scope.SearchableFields;
import org.keycloak.authorization.store.ScopeStore;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ModelDuplicateException;
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
import java.util.stream.Collectors;

import static org.keycloak.common.util.StackUtil.getShortStackTrace;
import static org.keycloak.models.map.storage.QueryParameters.withCriteria;
import static org.keycloak.models.map.storage.criteria.DefaultModelCriteria.criteria;

public class MapScopeStore implements ScopeStore {

    private static final Logger LOG = Logger.getLogger(MapScopeStore.class);
    private final AuthorizationProvider authorizationProvider;
    final MapKeycloakTransaction<MapScopeEntity, Scope> tx;

    public MapScopeStore(KeycloakSession session, MapStorage<MapScopeEntity, Scope> scopeStore, AuthorizationProvider provider) {
        this.authorizationProvider = provider;
        this.tx = scopeStore.createTransaction(session);
        session.getTransactionManager().enlist(tx);
    }

    private Scope entityToAdapter(MapScopeEntity origEntity) {
        if (origEntity == null) return null;
        // Clone entity before returning back, to avoid giving away a reference to the live object to the caller
        return new MapScopeAdapter(origEntity, authorizationProvider.getStoreFactory());
    }

    private DefaultModelCriteria<Scope> forResourceServer(ResourceServer resourceServer) {
        DefaultModelCriteria<Scope> mcb = criteria();

        return resourceServer == null
                ? mcb
                : mcb.compare(SearchableFields.RESOURCE_SERVER_ID, Operator.EQ,
                resourceServer.getId());
    }

    @Override
    public Scope create(ResourceServer resourceServer, String id, String name) {
        LOG.tracef("create(%s, %s, %s)%s", id, name, resourceServer, getShortStackTrace());


        // @UniqueConstraint(columnNames = {"NAME", "RESOURCE_SERVER_ID"})
        DefaultModelCriteria<Scope> mcb = forResourceServer(resourceServer)
                .compare(SearchableFields.NAME, Operator.EQ, name);

        if (tx.getCount(withCriteria(mcb)) > 0) {
            throw new ModelDuplicateException("Scope with name '" + name + "' for " + resourceServer.getId() + " already exists");
        }

        MapScopeEntity entity = new MapScopeEntityImpl();
        entity.setId(id);
        entity.setName(name);
        entity.setResourceServerId(resourceServer.getId());

        entity = tx.create(entity);

        return entityToAdapter(entity);
    }

    @Override
    public void delete(String id) {
        LOG.tracef("delete(%s)%s", id, getShortStackTrace());
        tx.delete(id);
    }

    @Override
    public Scope findById(ResourceServer resourceServer, String id) {
        LOG.tracef("findById(%s, %s)%s", id, resourceServer, getShortStackTrace());

        return tx.read(withCriteria(forResourceServer(resourceServer)
                .compare(SearchableFields.ID, Operator.EQ, id)))
                .findFirst()
                .map(this::entityToAdapter)
                .orElse(null);
    }

    @Override
    public Scope findByName(ResourceServer resourceServer, String name) {
        LOG.tracef("findByName(%s, %s)%s", name, resourceServer, getShortStackTrace());

        return tx.read(withCriteria(forResourceServer(resourceServer).compare(SearchableFields.NAME,
                Operator.EQ, name)))
                .findFirst()
                .map(this::entityToAdapter)
                .orElse(null);
    }

    @Override
    public List<Scope> findByResourceServer(ResourceServer resourceServer) {
        LOG.tracef("findByResourceServer(%s)%s", resourceServer, getShortStackTrace());

        return tx.read(withCriteria(forResourceServer(resourceServer)))
                .map(this::entityToAdapter)
                .collect(Collectors.toList());
    }

    @Override
    public List<Scope> findByResourceServer(ResourceServer resourceServer, Map<Scope.FilterOption, String[]> attributes, Integer firstResult, Integer maxResults) {
        DefaultModelCriteria<Scope> mcb = forResourceServer(resourceServer);

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
            .map(this::entityToAdapter)
            .collect(Collectors.toList());
    }
}

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

    private DefaultModelCriteria<Scope> forResourceServer(String resourceServerId) {
        DefaultModelCriteria<Scope> mcb = criteria();

        return resourceServerId == null
                ? mcb
                : mcb.compare(SearchableFields.RESOURCE_SERVER_ID, Operator.EQ,
                resourceServerId);
    }

    @Override
    public Scope create(String id, String name, ResourceServer resourceServer) {
        LOG.tracef("create(%s, %s, %s)%s", id, name, resourceServer, getShortStackTrace());


        // @UniqueConstraint(columnNames = {"NAME", "RESOURCE_SERVER_ID"})
        DefaultModelCriteria<Scope> mcb = forResourceServer(resourceServer.getId())
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
    public Scope findById(String id, String resourceServerId) {
        LOG.tracef("findById(%s, %s)%s", id, resourceServerId, getShortStackTrace());

        return tx.read(withCriteria(forResourceServer(resourceServerId)
                .compare(SearchableFields.ID, Operator.EQ, id)))
                .findFirst()
                .map(this::entityToAdapter)
                .orElse(null);
    }

    @Override
    public Scope findByName(String name, String resourceServerId) {
        LOG.tracef("findByName(%s, %s)%s", name, resourceServerId, getShortStackTrace());

        return tx.read(withCriteria(forResourceServer(resourceServerId).compare(SearchableFields.NAME,
                Operator.EQ, name)))
                .findFirst()
                .map(this::entityToAdapter)
                .orElse(null);
    }

    @Override
    public List<Scope> findByResourceServer(String id) {
        LOG.tracef("findByResourceServer(%s)%s", id, getShortStackTrace());

        return tx.read(withCriteria(forResourceServer(id)))
                .map(this::entityToAdapter)
                .collect(Collectors.toList());
    }

    @Override
    public List<Scope> findByResourceServer(Map<Scope.FilterOption, String[]> attributes, String resourceServerId, int firstResult, int maxResult) {
        DefaultModelCriteria<Scope> mcb = forResourceServer(resourceServerId);

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

        return tx.read(withCriteria(mcb).pagination(firstResult, maxResult, SearchableFields.NAME))
            .map(this::entityToAdapter)
            .collect(Collectors.toList());
    }
}

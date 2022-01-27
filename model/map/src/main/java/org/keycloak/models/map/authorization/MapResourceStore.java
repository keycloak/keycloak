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
import org.keycloak.authorization.model.Resource;
import org.keycloak.authorization.model.Resource.SearchableFields;
import org.keycloak.authorization.model.ResourceServer;
import org.keycloak.authorization.store.ResourceStore;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ModelDuplicateException;
import org.keycloak.models.map.authorization.adapter.MapResourceAdapter;
import org.keycloak.models.map.authorization.entity.MapResourceEntity;
import org.keycloak.models.map.authorization.entity.MapResourceEntityImpl;
import org.keycloak.models.map.storage.MapKeycloakTransaction;
import org.keycloak.models.map.storage.MapStorage;

import org.keycloak.models.map.storage.ModelCriteriaBuilder.Operator;
import org.keycloak.models.map.storage.criteria.DefaultModelCriteria;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static org.keycloak.common.util.StackUtil.getShortStackTrace;
import static org.keycloak.models.map.storage.QueryParameters.withCriteria;
import static org.keycloak.models.map.storage.criteria.DefaultModelCriteria.criteria;

public class MapResourceStore implements ResourceStore {

    private static final Logger LOG = Logger.getLogger(MapResourceStore.class);
    private final AuthorizationProvider authorizationProvider;
    final MapKeycloakTransaction<MapResourceEntity, Resource> tx;

    public MapResourceStore(KeycloakSession session, MapStorage<MapResourceEntity, Resource> resourceStore, AuthorizationProvider provider) {
        this.tx = resourceStore.createTransaction(session);
        session.getTransactionManager().enlist(tx);
        authorizationProvider = provider;
    }

    private Resource entityToAdapter(MapResourceEntity origEntity) {
        if (origEntity == null) return null;
        // Clone entity before returning back, to avoid giving away a reference to the live object to the caller
        return new MapResourceAdapter(origEntity, authorizationProvider.getStoreFactory());
    }
    
    private DefaultModelCriteria<Resource> forResourceServer(String resourceServerId) {
        DefaultModelCriteria<Resource> mcb = criteria();

        return resourceServerId == null
                ? mcb
                : mcb.compare(SearchableFields.RESOURCE_SERVER_ID, Operator.EQ,
                              resourceServerId);
    }

    @Override
    public Resource create(String id, String name, ResourceServer resourceServer, String owner) {
        LOG.tracef("create(%s, %s, %s, %s)%s", id, name, resourceServer, owner, getShortStackTrace());
        // @UniqueConstraint(columnNames = {"NAME", "RESOURCE_SERVER_ID", "OWNER"})
        DefaultModelCriteria<Resource> mcb = forResourceServer(resourceServer.getId())
                .compare(SearchableFields.NAME, Operator.EQ, name)
                .compare(SearchableFields.OWNER, Operator.EQ, owner);

        if (tx.getCount(withCriteria(mcb)) > 0) {
            throw new ModelDuplicateException("Resource with name '" + name + "' for " + resourceServer.getId() + " already exists for request owner " + owner);
        }

        MapResourceEntity entity = new MapResourceEntityImpl();
        entity.setId(id);
        entity.setName(name);
        entity.setResourceServerId(resourceServer.getId());
        entity.setOwner(owner);

        entity = tx.create(entity);

        return entityToAdapter(entity);
    }

    @Override
    public void delete(String id) {
        LOG.tracef("delete(%s)%s", id, getShortStackTrace());

        tx.delete(id);
    }

    @Override
    public Resource findById(String id, String resourceServerId) {
        LOG.tracef("findById(%s, %s)%s", id, resourceServerId, getShortStackTrace());

        return tx.read(withCriteria(forResourceServer(resourceServerId)
                .compare(SearchableFields.ID, Operator.EQ, id)))
                .findFirst()
                .map(this::entityToAdapter)
                .orElse(null);
    }

    @Override
    public void findByOwner(String ownerId, String resourceServerId, Consumer<Resource> consumer) {
        findByOwnerFilter(ownerId, resourceServerId, consumer, -1, -1);
    }

    private void findByOwnerFilter(String ownerId, String resourceServerId, Consumer<Resource> consumer, int firstResult, int maxResult) {
        LOG.tracef("findByOwnerFilter(%s, %s, %s, %d, %d)%s", ownerId, resourceServerId, consumer, firstResult, maxResult, getShortStackTrace());

        tx.read(withCriteria(forResourceServer(resourceServerId).compare(SearchableFields.OWNER, Operator.EQ, ownerId))
                .pagination(firstResult, maxResult, SearchableFields.ID)
            ).map(this::entityToAdapter)
            .forEach(consumer);
    }

    @Override
    public List<Resource> findByOwner(String ownerId, String resourceServerId, int first, int max) {
        List<Resource> resourceList = new LinkedList<>();

        findByOwnerFilter(ownerId, resourceServerId, resourceList::add, first, max);

        return resourceList;
    }

    @Override
    public List<Resource> findByUri(String uri, String resourceServerId) {
        LOG.tracef("findByUri(%s, %s)%s", uri, resourceServerId, getShortStackTrace());

        return tx.read(withCriteria(forResourceServer(resourceServerId)
                .compare(SearchableFields.URI, Operator.EQ, uri)))
                .map(this::entityToAdapter)
                .collect(Collectors.toList());
    }

    @Override
    public List<Resource> findByResourceServer(String resourceServerId) {
        LOG.tracef("findByResourceServer(%s)%s", resourceServerId, getShortStackTrace());

        return tx.read(withCriteria(forResourceServer(resourceServerId)))
                .map(this::entityToAdapter)
                .collect(Collectors.toList());
    }

    @Override
    public List<Resource> findByResourceServer(Map<Resource.FilterOption, String[]> attributes, String resourceServerId, int firstResult, int maxResult) {
        LOG.tracef("findByResourceServer(%s, %s, %d, %d)%s", attributes, resourceServerId, firstResult, maxResult, getShortStackTrace());
        DefaultModelCriteria<Resource> mcb = forResourceServer(resourceServerId).and(
                attributes.entrySet().stream()
                        .map(this::filterEntryToDefaultModelCriteria)
                        .toArray(DefaultModelCriteria[]::new)
        );

        return tx.read(withCriteria(mcb).pagination(firstResult, maxResult, SearchableFields.NAME))
                .map(this::entityToAdapter)
                .collect(Collectors.toList());
    }

    private DefaultModelCriteria<Resource> filterEntryToDefaultModelCriteria(Map.Entry<Resource.FilterOption, String[]> entry) {
        Resource.FilterOption name = entry.getKey();
        String[] value = entry.getValue();

        DefaultModelCriteria<Resource> mcb = criteria();
        switch (name) {
            case ID:
            case SCOPE_ID:
            case OWNER:
            case URI:
                return mcb.compare(name.getSearchableModelField(), Operator.IN, Arrays.asList(value));
            case URI_NOT_NULL:
                return mcb.compare(SearchableFields.URI, Operator.EXISTS);
            case OWNER_MANAGED_ACCESS:
                return mcb.compare(SearchableFields.OWNER_MANAGED_ACCESS, Operator.EQ, Boolean.valueOf(value[0]));
            case EXACT_NAME:
                return mcb.compare(SearchableFields.NAME, Operator.EQ, value[0]);
            case NAME:
                return mcb.compare(SearchableFields.NAME, Operator.ILIKE, "%" + value[0] + "%");
            default:
                throw new IllegalArgumentException("Unsupported filter [" + name + "]");

        }
    }

    @Override
    public void findByScope(List<String> scopes, String resourceServerId, Consumer<Resource> consumer) {
        LOG.tracef("findByScope(%s, %s, %s)%s", scopes, resourceServerId, consumer, getShortStackTrace());

        tx.read(withCriteria(forResourceServer(resourceServerId)
                .compare(SearchableFields.SCOPE_ID, Operator.IN, scopes)))
                .map(this::entityToAdapter)
                .forEach(consumer);
    }

    @Override
    public Resource findByName(String name, String resourceServerId) {
        return findByName(name, resourceServerId, resourceServerId);
    }

    @Override
    public Resource findByName(String name, String ownerId, String resourceServerId) {
        LOG.tracef("findByName(%s, %s, %s)%s", name, ownerId, resourceServerId, getShortStackTrace());
        return tx.read(withCriteria(forResourceServer(resourceServerId)
                .compare(SearchableFields.OWNER, Operator.EQ, ownerId)
                .compare(SearchableFields.NAME, Operator.EQ, name)))
                .findFirst()
                .map(this::entityToAdapter)
                .orElse(null);
    }

    @Override
    public void findByType(String type, String resourceServerId, Consumer<Resource> consumer) {
        LOG.tracef("findByType(%s, %s, %s)%s", type, resourceServerId, consumer, getShortStackTrace());
        tx.read(withCriteria(forResourceServer(resourceServerId)
                .compare(SearchableFields.TYPE, Operator.EQ, type)))
            .map(this::entityToAdapter)
            .forEach(consumer);
    }

    @Override
    public void findByType(String type, String owner, String resourceServerId, Consumer<Resource> consumer) {
        LOG.tracef("findByType(%s, %s, %s, %s)%s", type, owner, resourceServerId, consumer, getShortStackTrace());

        DefaultModelCriteria<Resource> mcb = forResourceServer(resourceServerId)
                .compare(SearchableFields.TYPE, Operator.EQ, type);

        if (owner != null) {
            mcb = mcb.compare(SearchableFields.OWNER, Operator.EQ, owner);
        }

        tx.read(withCriteria(mcb))
                .map(this::entityToAdapter)
                .forEach(consumer);
    }

    @Override
    public void findByTypeInstance(String type, String resourceServerId, Consumer<Resource> consumer) {
        LOG.tracef("findByTypeInstance(%s, %s, %s)%s", type, resourceServerId, consumer, getShortStackTrace());
        tx.read(withCriteria(forResourceServer(resourceServerId)
                .compare(SearchableFields.OWNER, Operator.NE, resourceServerId)
                .compare(SearchableFields.TYPE, Operator.EQ, type)))
                .map(this::entityToAdapter)
                .forEach(consumer);
    }
}

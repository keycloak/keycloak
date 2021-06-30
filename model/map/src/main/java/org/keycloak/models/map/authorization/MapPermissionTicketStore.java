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
import org.keycloak.authorization.model.PermissionTicket;
import org.keycloak.authorization.model.PermissionTicket.SearchableFields;
import org.keycloak.authorization.model.Resource;
import org.keycloak.authorization.model.ResourceServer;
import org.keycloak.authorization.store.PermissionTicketStore;
import org.keycloak.authorization.store.ResourceStore;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ModelDuplicateException;
import org.keycloak.models.map.authorization.adapter.MapPermissionTicketAdapter;
import org.keycloak.models.map.authorization.entity.MapPermissionTicketEntity;
import org.keycloak.models.map.storage.MapKeycloakTransaction;
import org.keycloak.models.map.storage.MapStorage;
import org.keycloak.models.map.storage.ModelCriteriaBuilder;
import org.keycloak.models.map.storage.ModelCriteriaBuilder.Operator;

import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.keycloak.common.util.StackUtil.getShortStackTrace;
import static org.keycloak.models.map.common.MapStorageUtils.registerEntityForChanges;
import static org.keycloak.models.map.storage.QueryParameters.Order.ASCENDING;
import static org.keycloak.models.map.storage.QueryParameters.withCriteria;
import static org.keycloak.utils.StreamsUtil.distinctByKey;
import static org.keycloak.utils.StreamsUtil.paginatedStream;

public class MapPermissionTicketStore<K extends Comparable<K>> implements PermissionTicketStore {

    private static final Logger LOG = Logger.getLogger(MapPermissionTicketStore.class);
    private final AuthorizationProvider authorizationProvider;
    final MapKeycloakTransaction<K, MapPermissionTicketEntity<K>, PermissionTicket> tx;
    private final MapStorage<K, MapPermissionTicketEntity<K>, PermissionTicket> permissionTicketStore;

    public MapPermissionTicketStore(KeycloakSession session, MapStorage<K, MapPermissionTicketEntity<K>, PermissionTicket> permissionTicketStore, AuthorizationProvider provider) {
        this.authorizationProvider = provider;
        this.permissionTicketStore = permissionTicketStore;
        this.tx = permissionTicketStore.createTransaction(session);
        session.getTransactionManager().enlist(tx);
    }

    private PermissionTicket entityToAdapter(MapPermissionTicketEntity<K> origEntity) {
        if (origEntity == null) return null;
        // Clone entity before returning back, to avoid giving away a reference to the live object to the caller
        return new MapPermissionTicketAdapter<K>(registerEntityForChanges(tx, origEntity), authorizationProvider.getStoreFactory()) {
            @Override
            public String getId() {
                return permissionTicketStore.getKeyConvertor().keyToString(entity.getId());
            }
        };
    }

    private ModelCriteriaBuilder<PermissionTicket> forResourceServer(String resourceServerId) {
        ModelCriteriaBuilder<PermissionTicket> mcb = permissionTicketStore.createCriteriaBuilder();

        return resourceServerId == null
                ? mcb
                : mcb.compare(SearchableFields.RESOURCE_SERVER_ID, Operator.EQ,
                resourceServerId);
    }
    
    @Override
    public long count(Map<PermissionTicket.FilterOption, String> attributes, String resourceServerId) {
        ModelCriteriaBuilder<PermissionTicket> mcb = forResourceServer(resourceServerId).and(
                attributes.entrySet().stream()
                        .map(this::filterEntryToModelCriteriaBuilder)
                        .toArray(ModelCriteriaBuilder[]::new)
        );

        return tx.getCount(withCriteria(mcb));
    }

    @Override
    public PermissionTicket create(String resourceId, String scopeId, String requester, ResourceServer resourceServer) {
        LOG.tracef("create(%s, %s, %s, %s)%s", resourceId, scopeId, requester, resourceServer, getShortStackTrace());

        String owner = authorizationProvider.getStoreFactory().getResourceStore().findById(resourceId, resourceServer.getId()).getOwner();

        // @UniqueConstraint(columnNames = {"OWNER", "REQUESTER", "RESOURCE_SERVER_ID", "RESOURCE_ID", "SCOPE_ID"})
        ModelCriteriaBuilder<PermissionTicket> mcb = forResourceServer(resourceServer.getId())
                .compare(SearchableFields.OWNER, Operator.EQ, owner)
                .compare(SearchableFields.RESOURCE_ID, Operator.EQ, resourceId)
                .compare(SearchableFields.REQUESTER, Operator.EQ, requester);

        if (scopeId != null) {
            mcb = mcb.compare(SearchableFields.SCOPE_ID, Operator.EQ, scopeId);
        }

        if (tx.getCount(withCriteria(mcb)) > 0) {
            throw new ModelDuplicateException("Permission ticket for resource server: '" + resourceServer.getId()
                    + ", Resource: " + resourceId + ", owner: " + owner + ", scopeId: " + scopeId + " already exists.");
        }

        final K newId = permissionTicketStore.getKeyConvertor().yieldNewUniqueKey();
        MapPermissionTicketEntity<K> entity = new MapPermissionTicketEntity<>(newId);
        entity.setResourceId(resourceId);
        entity.setRequester(requester);
        entity.setCreatedTimestamp(System.currentTimeMillis());

        if (scopeId != null) {
            entity.setScopeId(scopeId);
        }

        entity.setOwner(owner);
        entity.setResourceServerId(resourceServer.getId());

        tx.create(entity);

        return entityToAdapter(entity);
    }

    @Override
    public void delete(String id) {
        LOG.tracef("delete(%s)%s", id, getShortStackTrace());
        tx.delete(permissionTicketStore.getKeyConvertor().fromString(id));
    }

    @Override
    public PermissionTicket findById(String id, String resourceServerId) {
        LOG.tracef("findById(%s, %s)%s", id, resourceServerId, getShortStackTrace());

        return tx.read(withCriteria(forResourceServer(resourceServerId)
                .compare(SearchableFields.ID, Operator.EQ, id)))
                .findFirst()
                .map(this::entityToAdapter)
                .orElse(null);
    }

    @Override
    public List<PermissionTicket> findByResourceServer(String resourceServerId) {
        LOG.tracef("findByResourceServer(%s)%s", resourceServerId, getShortStackTrace());

        return tx.read(withCriteria(forResourceServer(resourceServerId)))
                .map(this::entityToAdapter)
                .collect(Collectors.toList());
    }

    @Override
    public List<PermissionTicket> findByOwner(String owner, String resourceServerId) {
        LOG.tracef("findByOwner(%s, %s)%s", owner, resourceServerId, getShortStackTrace());

        return tx.read(withCriteria(forResourceServer(resourceServerId)
                .compare(SearchableFields.OWNER, Operator.EQ, owner)))
                .map(this::entityToAdapter)
                .collect(Collectors.toList());
    }

    @Override
    public List<PermissionTicket> findByResource(String resourceId, String resourceServerId) {
        LOG.tracef("findByResource(%s, %s)%s", resourceId, resourceServerId, getShortStackTrace());

        return tx.read(withCriteria(forResourceServer(resourceServerId)
                .compare(SearchableFields.RESOURCE_ID, Operator.EQ, resourceId)))
                .map(this::entityToAdapter)
                .collect(Collectors.toList());
    }

    @Override
    public List<PermissionTicket> findByScope(String scopeId, String resourceServerId) {
        LOG.tracef("findByScope(%s, %s)%s", scopeId, resourceServerId, getShortStackTrace());

        return tx.read(withCriteria(forResourceServer(resourceServerId)
                .compare(SearchableFields.SCOPE_ID, Operator.EQ, scopeId)))
                .map(this::entityToAdapter)
                .collect(Collectors.toList());
    }

    @Override
    public List<PermissionTicket> find(Map<PermissionTicket.FilterOption, String> attributes, String resourceServerId, int firstResult, int maxResult) {
        ModelCriteriaBuilder<PermissionTicket> mcb = forResourceServer(resourceServerId);

        if (attributes.containsKey(PermissionTicket.FilterOption.RESOURCE_NAME)) {
            String expectedResourceName = attributes.remove(PermissionTicket.FilterOption.RESOURCE_NAME);

            Map<Resource.FilterOption, String[]> filterOptionStringMap = new EnumMap<>(Resource.FilterOption.class);

            filterOptionStringMap.put(Resource.FilterOption.EXACT_NAME, new String[]{expectedResourceName});
            
            List<Resource> r = authorizationProvider.getStoreFactory().getResourceStore().findByResourceServer(filterOptionStringMap, resourceServerId, -1, -1);
            if (r == null || r.isEmpty()) {
                return Collections.emptyList();
            }
            mcb = mcb.compare(SearchableFields.RESOURCE_ID, Operator.IN, r.stream().map(Resource::getId));
        }
        
        mcb = mcb.and(
                attributes.entrySet().stream()
                    .map(this::filterEntryToModelCriteriaBuilder)
                    .toArray(ModelCriteriaBuilder[]::new)
        );

        return tx.read(withCriteria(mcb).pagination(firstResult, maxResult, SearchableFields.ID))
                .map(this::entityToAdapter)
                .collect(Collectors.toList());
    }
    
    private ModelCriteriaBuilder<PermissionTicket> filterEntryToModelCriteriaBuilder(Map.Entry<PermissionTicket.FilterOption, String> entry) {
        PermissionTicket.FilterOption name = entry.getKey();
        String value = entry.getValue();

        switch (name) {
            case ID:
            case SCOPE_ID:
            case RESOURCE_ID:
            case OWNER:
            case REQUESTER:
            case POLICY_ID:
                return permissionTicketStore.createCriteriaBuilder()
                        .compare(name.getSearchableModelField(), Operator.EQ, value);
            case SCOPE_IS_NULL:
            case GRANTED:
            case REQUESTER_IS_NULL: {
                Operator op = Operator.NOT_EXISTS;
                if (Boolean.parseBoolean(value)) {
                    op = Operator.EXISTS;
                }
                return permissionTicketStore.createCriteriaBuilder()
                        .compare(name.getSearchableModelField(), op);
            }
            case POLICY_IS_NOT_NULL:
                return permissionTicketStore.createCriteriaBuilder()
                        .compare(SearchableFields.REQUESTER, Operator.NOT_EXISTS);
            default:
                throw new IllegalArgumentException("Unsupported filter [" + name + "]");

        }
    }

    @Override
    public List<PermissionTicket> findGranted(String userId, String resourceServerId) {
        Map<PermissionTicket.FilterOption, String> filters = new EnumMap<>(PermissionTicket.FilterOption.class);

        filters.put(PermissionTicket.FilterOption.GRANTED, Boolean.TRUE.toString());
        filters.put(PermissionTicket.FilterOption.REQUESTER, userId);

        return find(filters, resourceServerId, -1, -1);
    }

    @Override
    public List<PermissionTicket> findGranted(String resourceName, String userId, String resourceServerId) {
        Map<PermissionTicket.FilterOption, String> filters = new EnumMap<>(PermissionTicket.FilterOption.class);

        filters.put(PermissionTicket.FilterOption.RESOURCE_NAME, resourceName);
        filters.put(PermissionTicket.FilterOption.GRANTED, Boolean.TRUE.toString());
        filters.put(PermissionTicket.FilterOption.REQUESTER, userId);

        return find(filters, resourceServerId, -1, -1);
    }

    @Override
    public List<Resource> findGrantedResources(String requester, String name, int first, int max) {
        ModelCriteriaBuilder<PermissionTicket> mcb = permissionTicketStore.createCriteriaBuilder()
                .compare(SearchableFields.REQUESTER, Operator.EQ, requester)
                .compare(SearchableFields.GRANTED_TIMESTAMP, Operator.EXISTS);

        Function<MapPermissionTicketEntity<K>, Resource> ticketResourceMapper;

        ResourceStore resourceStore = authorizationProvider.getStoreFactory().getResourceStore();
        if (name != null) {
            ticketResourceMapper = ticket -> {
                Map<Resource.FilterOption, String[]> filterOptionMap = new EnumMap<>(Resource.FilterOption.class);

                filterOptionMap.put(Resource.FilterOption.ID, new String[] {ticket.getResourceId()});
                filterOptionMap.put(Resource.FilterOption.NAME, new String[] {name});

                List<Resource> resource = resourceStore.findByResourceServer(filterOptionMap, ticket.getResourceServerId(), -1, 1);
                
                return resource.isEmpty() ? null : resource.get(0);
            };
        } else {
            ticketResourceMapper = ticket -> resourceStore
                    .findById(ticket.getResourceId(), ticket.getResourceServerId());
        }

        return paginatedStream(tx.read(withCriteria(mcb).orderBy(SearchableFields.RESOURCE_ID, ASCENDING))
            .filter(distinctByKey(MapPermissionTicketEntity::getResourceId))
            .map(ticketResourceMapper)
            .filter(Objects::nonNull), first, max)
            .collect(Collectors.toList());
    }

    @Override
    public List<Resource> findGrantedOwnerResources(String owner, int first, int max) {
        ModelCriteriaBuilder<PermissionTicket> mcb = permissionTicketStore.createCriteriaBuilder()
                .compare(SearchableFields.OWNER, Operator.EQ, owner);

        return paginatedStream(tx.read(withCriteria(mcb).orderBy(SearchableFields.RESOURCE_ID, ASCENDING))
            .filter(distinctByKey(MapPermissionTicketEntity::getResourceId)), first, max)
            .map(ticket -> authorizationProvider.getStoreFactory().getResourceStore()
                    .findById(ticket.getResourceId(), ticket.getResourceServerId()))
            .collect(Collectors.toList());
    }
}

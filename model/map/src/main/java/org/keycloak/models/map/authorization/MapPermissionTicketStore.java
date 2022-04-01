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
import org.keycloak.authorization.UserManagedPermissionUtil;
import org.keycloak.authorization.model.PermissionTicket;
import org.keycloak.authorization.model.PermissionTicket.SearchableFields;
import org.keycloak.authorization.model.Resource;
import org.keycloak.authorization.model.ResourceServer;
import org.keycloak.authorization.model.Scope;
import org.keycloak.authorization.store.PermissionTicketStore;
import org.keycloak.authorization.store.ResourceServerStore;
import org.keycloak.authorization.store.ResourceStore;
import org.keycloak.common.util.Time;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ModelDuplicateException;
import org.keycloak.models.map.authorization.adapter.MapPermissionTicketAdapter;
import org.keycloak.models.map.authorization.entity.MapPermissionTicketEntity;
import org.keycloak.models.map.authorization.entity.MapPermissionTicketEntityImpl;
import org.keycloak.models.map.storage.MapKeycloakTransaction;
import org.keycloak.models.map.storage.MapStorage;

import org.keycloak.models.map.storage.ModelCriteriaBuilder.Operator;
import org.keycloak.models.map.storage.criteria.DefaultModelCriteria;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.keycloak.common.util.StackUtil.getShortStackTrace;
import static org.keycloak.models.map.storage.QueryParameters.Order.ASCENDING;
import static org.keycloak.models.map.storage.QueryParameters.withCriteria;
import static org.keycloak.models.map.storage.criteria.DefaultModelCriteria.criteria;
import static org.keycloak.utils.StreamsUtil.distinctByKey;
import static org.keycloak.utils.StreamsUtil.paginatedStream;

public class MapPermissionTicketStore implements PermissionTicketStore {

    private static final Logger LOG = Logger.getLogger(MapPermissionTicketStore.class);
    private final AuthorizationProvider authorizationProvider;
    final MapKeycloakTransaction<MapPermissionTicketEntity, PermissionTicket> tx;

    public MapPermissionTicketStore(KeycloakSession session, MapStorage<MapPermissionTicketEntity, PermissionTicket> permissionTicketStore, AuthorizationProvider provider) {
        this.authorizationProvider = provider;
        this.tx = permissionTicketStore.createTransaction(session);
        session.getTransactionManager().enlist(tx);
    }

    private PermissionTicket entityToAdapter(MapPermissionTicketEntity origEntity) {
        if (origEntity == null) return null;
        // Clone entity before returning back, to avoid giving away a reference to the live object to the caller
        return new MapPermissionTicketAdapter(origEntity, authorizationProvider.getStoreFactory());
    }

    private DefaultModelCriteria<PermissionTicket> forResourceServer(ResourceServer resourceServer) {
        DefaultModelCriteria<PermissionTicket> mcb = criteria();

        return resourceServer == null
                ? mcb
                : mcb.compare(SearchableFields.RESOURCE_SERVER_ID, Operator.EQ,
                resourceServer.getId());
    }
    
    @Override
    public long count(ResourceServer resourceServer, Map<PermissionTicket.FilterOption, String> attributes) {
        DefaultModelCriteria<PermissionTicket> mcb = forResourceServer(resourceServer).and(
                attributes.entrySet().stream()
                        .map(this::filterEntryToDefaultModelCriteria)
                        .toArray(DefaultModelCriteria[]::new)
        );

        return tx.getCount(withCriteria(mcb));
    }

    @Override
    public PermissionTicket create(ResourceServer resourceServer, Resource resource, Scope scope, String requester) {
        LOG.tracef("create(%s, %s, %s, %s)%s", resource, scope, requester, resourceServer, getShortStackTrace());

        String owner = authorizationProvider.getStoreFactory().getResourceStore().findById(resourceServer, resource.getId()).getOwner();

        // @UniqueConstraint(columnNames = {"OWNER", "REQUESTER", "RESOURCE_SERVER_ID", "RESOURCE_ID", "SCOPE_ID"})
        DefaultModelCriteria<PermissionTicket> mcb = forResourceServer(resourceServer)
                .compare(SearchableFields.OWNER, Operator.EQ, owner)
                .compare(SearchableFields.RESOURCE_ID, Operator.EQ, resource)
                .compare(SearchableFields.REQUESTER, Operator.EQ, requester);

        if (scope != null) {
            mcb = mcb.compare(SearchableFields.SCOPE_ID, Operator.EQ, scope.getId());
        }

        if (tx.getCount(withCriteria(mcb)) > 0) {
            throw new ModelDuplicateException("Permission ticket for resource server: '" + resourceServer.getId()
                    + ", Resource: " + resource + ", owner: " + owner + ", scopeId: " + scope + " already exists.");
        }

        MapPermissionTicketEntity entity = new MapPermissionTicketEntityImpl();
        entity.setResourceId(resource.getId());
        entity.setRequester(requester);
        entity.setCreatedTimestamp(Time.currentTimeMillis());

        if (scope != null) {
            entity.setScopeId(scope.getId());
        }

        entity.setOwner(owner);
        entity.setResourceServerId(resourceServer.getId());

        entity = tx.create(entity);

        return entityToAdapter(entity);
    }

    @Override
    public void delete(String id) {
        LOG.tracef("delete(%s)%s", id, getShortStackTrace());

        PermissionTicket permissionTicket = findById((ResourceServer) null, id);
        if (permissionTicket == null) return;

        tx.delete(id);
        UserManagedPermissionUtil.removePolicy(permissionTicket, authorizationProvider.getStoreFactory());
    }

    @Override
    public PermissionTicket findById(ResourceServer resourceServer, String id) {
        LOG.tracef("findById(%s, %s)%s", id, resourceServer, getShortStackTrace());

        return tx.read(withCriteria(forResourceServer(resourceServer)
                .compare(SearchableFields.ID, Operator.EQ, id)))
                .findFirst()
                .map(this::entityToAdapter)
                .orElse(null);
    }

    @Override
    public List<PermissionTicket> findByResourceServer(ResourceServer resourceServer) {
        LOG.tracef("findByResourceServer(%s)%s", resourceServer, getShortStackTrace());

        return tx.read(withCriteria(forResourceServer(resourceServer)))
                .map(this::entityToAdapter)
                .collect(Collectors.toList());
    }

    @Override
    public List<PermissionTicket> findByOwner(ResourceServer resourceServer, String owner) {
        LOG.tracef("findByOwner(%s, %s)%s", owner, resourceServer, getShortStackTrace());

        return tx.read(withCriteria(forResourceServer(resourceServer)
                .compare(SearchableFields.OWNER, Operator.EQ, owner)))
                .map(this::entityToAdapter)
                .collect(Collectors.toList());
    }

    @Override
    public List<PermissionTicket> findByResource(ResourceServer resourceServer, Resource resource) {
        LOG.tracef("findByResource(%s, %s)%s", resource, resourceServer, getShortStackTrace());

        return tx.read(withCriteria(forResourceServer(resourceServer)
                .compare(SearchableFields.RESOURCE_ID, Operator.EQ, resource.getId())))
                .map(this::entityToAdapter)
                .collect(Collectors.toList());
    }

    @Override
    public List<PermissionTicket> findByScope(ResourceServer resourceServer, Scope scope) {
        LOG.tracef("findByScope(%s, %s)%s", scope, resourceServer, getShortStackTrace());

        return tx.read(withCriteria(forResourceServer(resourceServer)
                .compare(SearchableFields.SCOPE_ID, Operator.EQ, scope.getId())))
                .map(this::entityToAdapter)
                .collect(Collectors.toList());
    }

    @Override
    public List<PermissionTicket> find(ResourceServer resourceServer, Map<PermissionTicket.FilterOption, String> attributes, Integer firstResult, Integer maxResult) {
        DefaultModelCriteria<PermissionTicket> mcb = forResourceServer(resourceServer);

        if (attributes.containsKey(PermissionTicket.FilterOption.RESOURCE_NAME)) {
            String expectedResourceName = attributes.remove(PermissionTicket.FilterOption.RESOURCE_NAME);

            Map<Resource.FilterOption, String[]> filterOptionStringMap = new EnumMap<>(Resource.FilterOption.class);

            filterOptionStringMap.put(Resource.FilterOption.EXACT_NAME, new String[]{expectedResourceName});
            
            List<Resource> r = authorizationProvider.getStoreFactory().getResourceStore().findByResourceServer(resourceServer, filterOptionStringMap, null, null);
            if (r == null || r.isEmpty()) {
                return Collections.emptyList();
            }
            mcb = mcb.compare(SearchableFields.RESOURCE_ID, Operator.IN, r.stream().map(Resource::getId));
        }
        
        mcb = mcb.and(
                attributes.entrySet().stream()
                    .map(this::filterEntryToDefaultModelCriteria)
                    .toArray(DefaultModelCriteria[]::new)
        );

        return tx.read(withCriteria(mcb).pagination(firstResult, maxResult, SearchableFields.ID))
                .map(this::entityToAdapter)
                .collect(Collectors.toList());
    }
    
    private DefaultModelCriteria<PermissionTicket> filterEntryToDefaultModelCriteria(Map.Entry<PermissionTicket.FilterOption, String> entry) {
        PermissionTicket.FilterOption name = entry.getKey();
        String value = entry.getValue();

        DefaultModelCriteria<PermissionTicket> mcb = criteria();
        switch (name) {
            case ID:
            case SCOPE_ID:
            case RESOURCE_ID:
            case OWNER:
            case REQUESTER:
            case POLICY_ID:
                return mcb.compare(name.getSearchableModelField(), Operator.EQ, value);
            case SCOPE_IS_NULL:
            case GRANTED:
            case REQUESTER_IS_NULL: {
                Operator op = Operator.NOT_EXISTS;
                if (Boolean.parseBoolean(value)) {
                    op = Operator.EXISTS;
                }
                return mcb.compare(name.getSearchableModelField(), op);
            }
            case POLICY_IS_NOT_NULL:
                return mcb.compare(SearchableFields.REQUESTER, Operator.NOT_EXISTS);
            default:
                throw new IllegalArgumentException("Unsupported filter [" + name + "]");

        }
    }

    @Override
    public List<PermissionTicket> findGranted(ResourceServer resourceServer, String userId) {
        Map<PermissionTicket.FilterOption, String> filters = new EnumMap<>(PermissionTicket.FilterOption.class);

        filters.put(PermissionTicket.FilterOption.GRANTED, Boolean.TRUE.toString());
        filters.put(PermissionTicket.FilterOption.REQUESTER, userId);

        return find(resourceServer, filters, null, null);
    }

    @Override
    public List<PermissionTicket> findGranted(ResourceServer resourceServer, String resourceName, String userId) {
        Map<PermissionTicket.FilterOption, String> filters = new EnumMap<>(PermissionTicket.FilterOption.class);

        filters.put(PermissionTicket.FilterOption.RESOURCE_NAME, resourceName);
        filters.put(PermissionTicket.FilterOption.GRANTED, Boolean.TRUE.toString());
        filters.put(PermissionTicket.FilterOption.REQUESTER, userId);

        return find(resourceServer, filters, null, null);
    }

    @Override
    public List<Resource> findGrantedResources(String requester, String name, Integer first, Integer max) {
        DefaultModelCriteria<PermissionTicket> mcb = criteria();
        mcb = mcb.compare(SearchableFields.REQUESTER, Operator.EQ, requester)
                .compare(SearchableFields.GRANTED_TIMESTAMP, Operator.EXISTS);

        Function<MapPermissionTicketEntity, Resource> ticketResourceMapper;

        ResourceStore resourceStore = authorizationProvider.getStoreFactory().getResourceStore();
        ResourceServerStore resourceServerStore = authorizationProvider.getStoreFactory().getResourceServerStore();
        if (name != null) {
            ticketResourceMapper = ticket -> {
                Map<Resource.FilterOption, String[]> filterOptionMap = new EnumMap<>(Resource.FilterOption.class);

                filterOptionMap.put(Resource.FilterOption.ID, new String[] {ticket.getResourceId()});
                filterOptionMap.put(Resource.FilterOption.NAME, new String[] {name});

                List<Resource> resource = resourceStore.findByResourceServer(resourceServerStore.findById(ticket.getResourceServerId()), filterOptionMap, -1, 1);
                
                return resource.isEmpty() ? null : resource.get(0);
            };
        } else {
            ticketResourceMapper = ticket -> resourceStore
                    .findById(resourceServerStore.findById(ticket.getResourceServerId()), ticket.getResourceId());
        }

        return paginatedStream(tx.read(withCriteria(mcb).orderBy(SearchableFields.RESOURCE_ID, ASCENDING))
            .filter(distinctByKey(MapPermissionTicketEntity::getResourceId))
            .map(ticketResourceMapper)
            .filter(Objects::nonNull), first, max)
            .collect(Collectors.toList());
    }

    @Override
    public List<Resource> findGrantedOwnerResources(String owner, Integer firstResult, Integer maxResults) {
        DefaultModelCriteria<PermissionTicket> mcb = criteria();
        mcb = mcb.compare(SearchableFields.OWNER, Operator.EQ, owner);

        ResourceStore resourceStore = authorizationProvider.getStoreFactory().getResourceStore();
        ResourceServerStore resourceServerStore = authorizationProvider.getStoreFactory().getResourceServerStore();

        return paginatedStream(tx.read(withCriteria(mcb).orderBy(SearchableFields.RESOURCE_ID, ASCENDING))
            .filter(distinctByKey(MapPermissionTicketEntity::getResourceId)), firstResult, maxResults)
            .map(ticket -> resourceStore.findById(resourceServerStore.findById(ticket.getResourceServerId()), ticket.getResourceId()))
            .collect(Collectors.toList());
    }
}

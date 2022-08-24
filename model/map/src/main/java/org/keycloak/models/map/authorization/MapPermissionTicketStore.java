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
import org.keycloak.models.RealmModel;
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

    private Function<MapPermissionTicketEntity, PermissionTicket> entityToAdapterFunc(RealmModel realm, ResourceServer resourceServer) {
        return origEntity -> new MapPermissionTicketAdapter(realm, resourceServer, origEntity, authorizationProvider.getStoreFactory());
    }

    private DefaultModelCriteria<PermissionTicket> forRealmAndResourceServer(RealmModel realm, ResourceServer resourceServer) {
        final DefaultModelCriteria<PermissionTicket> mcb =  DefaultModelCriteria.<PermissionTicket>criteria()
                .compare(PermissionTicket.SearchableFields.REALM_ID, Operator.EQ, realm.getId());

        return resourceServer == null
                ? mcb
                : mcb.compare(SearchableFields.RESOURCE_SERVER_ID, Operator.EQ,
                resourceServer.getId());
    }
    
    @Override
    public long count(ResourceServer resourceServer, Map<PermissionTicket.FilterOption, String> attributes) {
        DefaultModelCriteria<PermissionTicket> mcb = forRealmAndResourceServer(resourceServer.getRealm(), resourceServer).and(
                attributes.entrySet().stream()
                        .map(this::filterEntryToDefaultModelCriteria)
                        .toArray(DefaultModelCriteria[]::new)
        );

        return tx.getCount(withCriteria(mcb));
    }

    @Override
    public PermissionTicket create(ResourceServer resourceServer, Resource resource, Scope scope, String requester) {
        LOG.tracef("create(%s, %s, %s, %s)%s", resource, scope, requester, resourceServer, getShortStackTrace());
        RealmModel realm = resourceServer.getRealm();

        String owner = authorizationProvider.getStoreFactory().getResourceStore().findById(realm, resourceServer, resource.getId()).getOwner();


        // @UniqueConstraint(columnNames = {"OWNER", "REQUESTER", "RESOURCE_SERVER_ID", "RESOURCE_ID", "SCOPE_ID"})
        DefaultModelCriteria<PermissionTicket> mcb = forRealmAndResourceServer(realm, resourceServer)
                .compare(SearchableFields.OWNER, Operator.EQ, owner)
                .compare(SearchableFields.RESOURCE_ID, Operator.EQ, resource.getId())
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
        entity.setRealmId(realm.getId());

        entity = tx.create(entity);

        return entity == null ? null : entityToAdapterFunc(realm, resourceServer).apply(entity);
    }

    @Override
    public void delete(RealmModel realm, String id) {
        LOG.tracef("delete(%s)%s", id, getShortStackTrace());

        PermissionTicket permissionTicket = findById(realm, null, id);
        if (permissionTicket == null) return;

        tx.delete(id);
        UserManagedPermissionUtil.removePolicy(permissionTicket, authorizationProvider.getStoreFactory());
    }

    @Override
    public PermissionTicket findById(RealmModel realm, ResourceServer resourceServer, String id) {
        LOG.tracef("findById(%s, %s)%s", id, resourceServer, getShortStackTrace());

        if (id == null) return null;

        return tx.read(withCriteria(forRealmAndResourceServer(realm, resourceServer)
                .compare(SearchableFields.ID, Operator.EQ, id)))
                .findFirst()
                .map(entityToAdapterFunc(realm, resourceServer))
                .orElse(null);
    }

    @Override
    public List<PermissionTicket> findByResource(ResourceServer resourceServer, Resource resource) {
        LOG.tracef("findByResource(%s, %s)%s", resource, resourceServer, getShortStackTrace());

        RealmModel realm = resourceServer.getRealm();

        return tx.read(withCriteria(forRealmAndResourceServer(realm, resourceServer)
                .compare(SearchableFields.RESOURCE_ID, Operator.EQ, resource.getId())))
                .map(entityToAdapterFunc(realm, resourceServer))
                .collect(Collectors.toList());
    }

    @Override
    public List<PermissionTicket> findByScope(ResourceServer resourceServer, Scope scope) {
        LOG.tracef("findByScope(%s, %s)%s", scope, resourceServer, getShortStackTrace());

        RealmModel realm = resourceServer.getRealm();

        return tx.read(withCriteria(forRealmAndResourceServer(realm, resourceServer)
                .compare(SearchableFields.SCOPE_ID, Operator.EQ, scope.getId())))
                .map(entityToAdapterFunc(realm, resourceServer))
                .collect(Collectors.toList());
    }

    @Override
    public List<PermissionTicket> find(RealmModel realm, ResourceServer resourceServer, Map<PermissionTicket.FilterOption, String> attributes, Integer firstResult, Integer maxResult) {
        DefaultModelCriteria<PermissionTicket> mcb = forRealmAndResourceServer(realm, resourceServer);

        if (attributes.containsKey(PermissionTicket.FilterOption.RESOURCE_NAME)) {
            String expectedResourceName = attributes.remove(PermissionTicket.FilterOption.RESOURCE_NAME);

            Map<Resource.FilterOption, String[]> filterOptionStringMap = new EnumMap<>(Resource.FilterOption.class);

            filterOptionStringMap.put(Resource.FilterOption.EXACT_NAME, new String[]{expectedResourceName});
            
            List<Resource> r = authorizationProvider.getStoreFactory().getResourceStore().find(realm, resourceServer, filterOptionStringMap, null, null);
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
                .map(entityToAdapterFunc(realm, resourceServer))
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

        return find(resourceServer.getRealm(), resourceServer, filters, null, null);
    }

    @Override
    public List<PermissionTicket> findGranted(ResourceServer resourceServer, String resourceName, String userId) {
        Map<PermissionTicket.FilterOption, String> filters = new EnumMap<>(PermissionTicket.FilterOption.class);

        filters.put(PermissionTicket.FilterOption.RESOURCE_NAME, resourceName);
        filters.put(PermissionTicket.FilterOption.GRANTED, Boolean.TRUE.toString());
        filters.put(PermissionTicket.FilterOption.REQUESTER, userId);

        return find(resourceServer.getRealm(), resourceServer, filters, null, null);
    }

    @Override
    public List<Resource> findGrantedResources(RealmModel realm, String requester, String name, Integer first, Integer max) {
        DefaultModelCriteria<PermissionTicket> mcb = criteria();
        mcb = mcb.compare(SearchableFields.REQUESTER, Operator.EQ, requester)
                .compare(SearchableFields.GRANTED_TIMESTAMP, Operator.EXISTS)
                .compare(SearchableFields.REALM_ID, Operator.EQ, realm.getId());

        Function<MapPermissionTicketEntity, Resource> ticketResourceMapper;

        ResourceStore resourceStore = authorizationProvider.getStoreFactory().getResourceStore();
        ResourceServerStore resourceServerStore = authorizationProvider.getStoreFactory().getResourceServerStore();
        if (name != null) {
            ticketResourceMapper = ticket -> {
                Map<Resource.FilterOption, String[]> filterOptionMap = new EnumMap<>(Resource.FilterOption.class);

                filterOptionMap.put(Resource.FilterOption.ID, new String[] {ticket.getResourceId()});
                filterOptionMap.put(Resource.FilterOption.NAME, new String[] {name});

                List<Resource> resource = resourceStore.find(realm, resourceServerStore.findById(realm, ticket.getResourceServerId()), filterOptionMap, -1, 1);
                
                return resource.isEmpty() ? null : resource.get(0);
            };
        } else {
            ticketResourceMapper = ticket -> resourceStore
                    .findById(realm, resourceServerStore.findById(realm, ticket.getResourceServerId()), ticket.getResourceId());
        }

        return paginatedStream(tx.read(withCriteria(mcb).orderBy(SearchableFields.RESOURCE_ID, ASCENDING))
            .filter(distinctByKey(MapPermissionTicketEntity::getResourceId))
            .map(ticketResourceMapper)
            .filter(Objects::nonNull), first, max)
            .collect(Collectors.toList());
    }

    @Override
    public List<Resource> findGrantedOwnerResources(RealmModel realm, String owner, Integer firstResult, Integer maxResults) {
        DefaultModelCriteria<PermissionTicket> mcb = criteria();
        mcb = mcb.compare(SearchableFields.OWNER, Operator.EQ, owner)
                .compare(SearchableFields.REALM_ID, Operator.EQ, realm.getId());

        ResourceStore resourceStore = authorizationProvider.getStoreFactory().getResourceStore();
        ResourceServerStore resourceServerStore = authorizationProvider.getStoreFactory().getResourceServerStore();

        return paginatedStream(tx.read(withCriteria(mcb).orderBy(SearchableFields.RESOURCE_ID, ASCENDING))
            .filter(distinctByKey(MapPermissionTicketEntity::getResourceId)), firstResult, maxResults)
            .map(ticket -> resourceStore.findById(realm, resourceServerStore.findById(realm, ticket.getResourceServerId()), ticket.getResourceId()))
            .collect(Collectors.toList());
    }

    public void preRemove(RealmModel realm) {
        LOG.tracef("preRemove(%s)%s", realm, getShortStackTrace());

        DefaultModelCriteria<PermissionTicket> mcb = criteria();
        mcb = mcb.compare(SearchableFields.REALM_ID, Operator.EQ, realm.getId());

        tx.delete(withCriteria(mcb));
    }

    public void preRemove(ResourceServer resourceServer) {
        LOG.tracef("preRemove(%s)%s", resourceServer, getShortStackTrace());

        tx.delete(withCriteria(forRealmAndResourceServer(resourceServer.getRealm(), resourceServer)));
    }
}

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
package org.keycloak.authorization.jpa.store;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import jakarta.persistence.EntityManager;
import jakarta.persistence.FlushModeType;
import jakarta.persistence.LockModeType;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

import org.keycloak.authorization.AuthorizationProvider;
import org.keycloak.authorization.jpa.entities.PermissionTicketEntity;
import org.keycloak.authorization.model.PermissionTicket;
import org.keycloak.authorization.model.Resource;
import org.keycloak.authorization.model.ResourceServer;
import org.keycloak.authorization.model.Scope;
import org.keycloak.authorization.store.PermissionTicketStore;
import org.keycloak.authorization.store.ResourceStore;
import org.keycloak.common.util.Time;
import org.keycloak.models.utils.KeycloakModelUtils;

import static org.keycloak.models.jpa.PaginationUtils.paginateQuery;
import static org.keycloak.utils.StreamsUtil.closing;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public class JPAPermissionTicketStore implements PermissionTicketStore {

    private final EntityManager entityManager;
    private final AuthorizationProvider provider;

    public JPAPermissionTicketStore(EntityManager entityManager, AuthorizationProvider provider) {
        this.entityManager = entityManager;
        this.provider = provider;
    }

    @Override
    public long count(ResourceServer resourceServer, Map<PermissionTicket.FilterOption, String> attributes) {
        CriteriaBuilder builder = entityManager.getCriteriaBuilder();
        CriteriaQuery<String> querybuilder = builder.createQuery(String.class);
        Root<PermissionTicketEntity> root = querybuilder.from(PermissionTicketEntity.class);

        querybuilder.select(root.get("id"));

        List<Predicate> predicates = getPredicates(builder, root, resourceServer, attributes);

        querybuilder.where(predicates.toArray(new Predicate[0])).orderBy(builder.asc(root.get("id")));

        TypedQuery<String> query = entityManager.createQuery(querybuilder);

        return closing(query.getResultStream()).count();
    }

    private List<Predicate> getPredicates(CriteriaBuilder builder,
                                          Root<PermissionTicketEntity> root,
                                          ResourceServer resourceServer,
                                          Map<PermissionTicket.FilterOption, String> attributes) {
        List<Predicate> predicates = new ArrayList<>();

        if (resourceServer != null) {
            predicates.add(builder.equal(root.get("resourceServer").get("id"), resourceServer.getId()));
        }

        attributes.forEach((filterOption, value) -> {
            switch (filterOption) {
                case ID:
                case OWNER:
                case REQUESTER:
                    predicates.add(builder.equal(root.get(filterOption.getName()), value));
                    break;
                case SCOPE_ID:
                case RESOURCE_ID:
                case RESOURCE_NAME:
                case POLICY_ID:
                    String[] predicateValues = filterOption.getName().split("\\.");
                    predicates.add(root.join(predicateValues[0]).get(predicateValues[1]).in(value));
                    break;
                case SCOPE_IS_NULL:
                    if (Boolean.parseBoolean(value)) {
                        predicates.add(builder.isNull(root.get("scope")));
                    } else {
                        predicates.add(builder.isNotNull(root.get("scope")));
                    }
                    break;
                case GRANTED:
                    if (Boolean.parseBoolean(value)) {
                        predicates.add(builder.isNotNull(root.get("grantedTimestamp")));
                    } else {
                        predicates.add(builder.isNull(root.get("grantedTimestamp")));
                    }
                    break;
                case REQUESTER_IS_NULL:
                    predicates.add(builder.isNull(root.get("requester")));
                    break;
                case POLICY_IS_NOT_NULL:
                    predicates.add(builder.isNotNull(root.get("policy")));
                    break;
                default:
                    throw new IllegalArgumentException("Unsupported filter [" + filterOption + "]");
            }
        });
        return predicates;
    }

    @Override
    public PermissionTicket create(ResourceServer resourceServer, Resource resource, Scope scope, String requester) {
        PermissionTicketEntity entity = new PermissionTicketEntity();

        entity.setId(KeycloakModelUtils.generateId());
        entity.setResource(ResourceAdapter.toEntity(entityManager, resource));
        entity.setRequester(requester);
        entity.setCreatedTimestamp(Time.currentTimeMillis());

        if (scope != null) {
            entity.setScope(ScopeAdapter.toEntity(entityManager, scope));
        }

        entity.setOwner(entity.getResource().getOwner());
        entity.setResourceServer(ResourceServerAdapter.toEntity(entityManager, resourceServer));

        this.entityManager.persist(entity);
        this.entityManager.flush();
        PermissionTicket model = new PermissionTicketAdapter(entity, entityManager, provider.getStoreFactory());
        return model;
    }

    @Override
    public void delete(String id) {
        PermissionTicketEntity policy = entityManager.find(PermissionTicketEntity.class, id, LockModeType.PESSIMISTIC_WRITE);
        if (policy != null) {
            this.entityManager.remove(policy);
        }
    }


    @Override
    public PermissionTicket findById(ResourceServer resourceServer, String id) {
        if (id == null) {
            return null;
        }

        PermissionTicketEntity entity = entityManager.find(PermissionTicketEntity.class, id);
        if (entity == null) return null;

        return new PermissionTicketAdapter(entity, entityManager, provider.getStoreFactory());
    }

    @Override
    public List<PermissionTicket> findByResource(ResourceServer resourceServer, final Resource resource) {
        TypedQuery<String> query = entityManager.createNamedQuery("findPermissionIdByResource", String.class);

        query.setFlushMode(FlushModeType.COMMIT);
        query.setParameter("resourceId", resource.getId());
        query.setParameter("serverId", resourceServer == null ? null : resourceServer.getId());

        List<String> result = query.getResultList();
        List<PermissionTicket> list = new LinkedList<>();
        PermissionTicketStore ticketStore = provider.getStoreFactory().getPermissionTicketStore();

        for (String id : result) {
            PermissionTicket ticket = ticketStore.findById(resourceServer, id);
            if (Objects.nonNull(ticket)) {
                list.add(ticket);
            }
        }

        return list;
    }

    @Override
    public List<PermissionTicket> findByScope(ResourceServer resourceServer, Scope scope) {
        if (scope == null) {
            return Collections.emptyList();
        }

        // Use separate subquery to handle DB2 and MSSSQL
        TypedQuery<String> query = entityManager.createNamedQuery("findPermissionIdByScope", String.class);

        query.setFlushMode(FlushModeType.COMMIT);
        query.setParameter("scopeId", scope.getId());
        query.setParameter("serverId", resourceServer == null ? null : resourceServer.getId());

        List<String> result = query.getResultList();
        List<PermissionTicket> list = new LinkedList<>();
        PermissionTicketStore ticketStore = provider.getStoreFactory().getPermissionTicketStore();

        for (String id : result) {
            PermissionTicket ticket = ticketStore.findById(resourceServer, id);
            if (Objects.nonNull(ticket)) {
                list.add(ticket);
            }
        }

        return list;
    }

    @Override
    public List<PermissionTicket> find(ResourceServer resourceServer, Map<PermissionTicket.FilterOption, String> attributes, Integer firstResult, Integer maxResult) {
        CriteriaBuilder builder = entityManager.getCriteriaBuilder();
        CriteriaQuery<String> querybuilder = builder.createQuery(String.class);
        Root<PermissionTicketEntity> root = querybuilder.from(PermissionTicketEntity.class);

        querybuilder.select(root.get("id"));

        List<Predicate> predicates = getPredicates(builder, root, resourceServer, attributes);

        querybuilder.where(predicates.toArray(new Predicate[predicates.size()])).orderBy(builder.asc(root.get("id")));

        TypedQuery query = entityManager.createQuery(querybuilder);

        List<String> result = paginateQuery(query, firstResult, maxResult).getResultList();
        List<PermissionTicket> list = new LinkedList<>();
        PermissionTicketStore ticketStore = provider.getStoreFactory().getPermissionTicketStore();

        for (String id : result) {
            PermissionTicket ticket = ticketStore.findById(resourceServer, id);
            if (Objects.nonNull(ticket)) {
                list.add(ticket);
            }
        }

        return list;
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
        TypedQuery<String> query = name == null ? 
                entityManager.createNamedQuery("findGrantedResources", String.class) :
                entityManager.createNamedQuery("findGrantedResourcesByName", String.class);

        query.setFlushMode(FlushModeType.COMMIT);
        query.setParameter("requester", requester);
        
        if (name != null) {
            query.setParameter("resourceName", "%" + name.toLowerCase() + "%");
        }

        List<String> result = paginateQuery(query, first, max).getResultList();
        List<Resource> list = new LinkedList<>();
        ResourceStore resourceStore = provider.getStoreFactory().getResourceStore();

        for (String id : result) {
            Resource resource = resourceStore.findById(null, id);

            if (Objects.nonNull(resource)) {
                list.add(resource);
            }
        }
        
        return list;
    }

    @Override
    public List<Resource> findGrantedOwnerResources(String owner, Integer firstResult, Integer maxResults) {
        TypedQuery<String> query = entityManager.createNamedQuery("findGrantedOwnerResources", String.class);

        query.setFlushMode(FlushModeType.COMMIT);
        query.setParameter("owner", owner);

        List<String> result = paginateQuery(query, firstResult, maxResults).getResultList();
        List<Resource> list = new LinkedList<>();
        ResourceStore resourceStore = provider.getStoreFactory().getResourceStore();

        for (String id : result) {
            Resource resource = resourceStore.findById(null, id);

            if (Objects.nonNull(resource)) {
                list.add(resource);
            }
        }

        return list;
    }

}

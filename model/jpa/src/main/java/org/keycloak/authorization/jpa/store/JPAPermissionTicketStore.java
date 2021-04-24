/*
 * Copyright 2017 Red Hat, Inc. and/or its affiliates
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

import javax.persistence.EntityManager;
import javax.persistence.FlushModeType;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import org.keycloak.authorization.AuthorizationProvider;
import org.keycloak.authorization.jpa.entities.PermissionTicketEntity;
import org.keycloak.authorization.model.PermissionTicket;
import org.keycloak.authorization.model.Resource;
import org.keycloak.authorization.model.ResourceServer;
import org.keycloak.authorization.store.PermissionTicketStore;
import org.keycloak.authorization.store.ResourceStore;
import org.keycloak.models.utils.KeycloakModelUtils;
import javax.persistence.LockModeType;

import static org.keycloak.models.jpa.PaginationUtils.paginateQuery;

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
    public long count(Map<PermissionTicket.FilterOption, String> attributes, String resourceServerId) {
        CriteriaBuilder builder = entityManager.getCriteriaBuilder();
        CriteriaQuery<Long> querybuilder = builder.createQuery(Long.class);
        Root<PermissionTicketEntity> root = querybuilder.from(PermissionTicketEntity.class);

        querybuilder.select(root.get("id"));

        List<Predicate> predicates = getPredicates(builder, root, resourceServerId, attributes);

        querybuilder.where(predicates.toArray(new Predicate[predicates.size()])).orderBy(builder.asc(root.get("id")));

        TypedQuery query = entityManager.createQuery(querybuilder);

        return query.getResultStream().count();
    }

    private List<Predicate> getPredicates(CriteriaBuilder builder,
                                          Root<PermissionTicketEntity> root,
                                          String resourceServerId,
                                          Map<PermissionTicket.FilterOption, String> attributes) {
        List<Predicate> predicates = new ArrayList<>();

        if (resourceServerId != null) {
            predicates.add(builder.equal(root.get("resourceServer").get("id"), resourceServerId));
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
    public PermissionTicket create(String resourceId, String scopeId, String requester, ResourceServer resourceServer) {
        PermissionTicketEntity entity = new PermissionTicketEntity();

        entity.setId(KeycloakModelUtils.generateId());
        entity.setResource(ResourceAdapter.toEntity(entityManager, provider.getStoreFactory().getResourceStore().findById(resourceId, resourceServer.getId())));
        entity.setRequester(requester);
        entity.setCreatedTimestamp(System.currentTimeMillis());

        if (scopeId != null) {
            entity.setScope(ScopeAdapter.toEntity(entityManager, provider.getStoreFactory().getScopeStore().findById(scopeId, resourceServer.getId())));
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
    public PermissionTicket findById(String id, String resourceServerId) {
        if (id == null) {
            return null;
        }

        PermissionTicketEntity entity = entityManager.find(PermissionTicketEntity.class, id);
        if (entity == null) return null;

        return new PermissionTicketAdapter(entity, entityManager, provider.getStoreFactory());
    }

    @Override
    public List<PermissionTicket> findByResourceServer(final String resourceServerId) {
        TypedQuery<String> query = entityManager.createNamedQuery("findPolicyIdByServerId", String.class);

        query.setParameter("serverId", resourceServerId);

        List<String> result = query.getResultList();
        List<PermissionTicket> list = new LinkedList<>();
        PermissionTicketStore ticketStore = provider.getStoreFactory().getPermissionTicketStore();

        for (String id : result) {
            PermissionTicket ticket = ticketStore.findById(id, resourceServerId);
            if (Objects.nonNull(ticket)) {
                list.add(ticket);
            }
        }

        return list;
    }

    @Override
    public List<PermissionTicket> findByResource(final String resourceId, String resourceServerId) {
        TypedQuery<String> query = entityManager.createNamedQuery("findPermissionIdByResource", String.class);

        query.setFlushMode(FlushModeType.COMMIT);
        query.setParameter("resourceId", resourceId);
        query.setParameter("serverId", resourceServerId);

        List<String> result = query.getResultList();
        List<PermissionTicket> list = new LinkedList<>();
        PermissionTicketStore ticketStore = provider.getStoreFactory().getPermissionTicketStore();

        for (String id : result) {
            PermissionTicket ticket = ticketStore.findById(id, resourceServerId);
            if (Objects.nonNull(ticket)) {
                list.add(ticket);
            }
        }

        return list;
    }

    @Override
    public List<PermissionTicket> findByScope(String scopeId, String resourceServerId) {
        if (scopeId==null) {
            return Collections.emptyList();
        }

        // Use separate subquery to handle DB2 and MSSSQL
        TypedQuery<String> query = entityManager.createNamedQuery("findPermissionIdByScope", String.class);

        query.setFlushMode(FlushModeType.COMMIT);
        query.setParameter("scopeId", scopeId);
        query.setParameter("serverId", resourceServerId);

        List<String> result = query.getResultList();
        List<PermissionTicket> list = new LinkedList<>();
        PermissionTicketStore ticketStore = provider.getStoreFactory().getPermissionTicketStore();

        for (String id : result) {
            PermissionTicket ticket = ticketStore.findById(id, resourceServerId);
            if (Objects.nonNull(ticket)) {
                list.add(ticket);
            }
        }

        return list;
    }

    @Override
    public List<PermissionTicket> find(Map<PermissionTicket.FilterOption, String> attributes, String resourceServerId, int firstResult, int maxResult) {
        CriteriaBuilder builder = entityManager.getCriteriaBuilder();
        CriteriaQuery<PermissionTicketEntity> querybuilder = builder.createQuery(PermissionTicketEntity.class);
        Root<PermissionTicketEntity> root = querybuilder.from(PermissionTicketEntity.class);

        querybuilder.select(root.get("id"));

        List<Predicate> predicates = getPredicates(builder, root, resourceServerId, attributes);

        querybuilder.where(predicates.toArray(new Predicate[predicates.size()])).orderBy(builder.asc(root.get("id")));

        TypedQuery query = entityManager.createQuery(querybuilder);

        List<String> result = paginateQuery(query, firstResult, maxResult).getResultList();
        List<PermissionTicket> list = new LinkedList<>();
        PermissionTicketStore ticketStore = provider.getStoreFactory().getPermissionTicketStore();

        for (String id : result) {
            PermissionTicket ticket = ticketStore.findById(id, resourceServerId);
            if (Objects.nonNull(ticket)) {
                list.add(ticket);
            }
        }

        return list;
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
            Resource resource = resourceStore.findById(id, null);

            if (Objects.nonNull(resource)) {
                list.add(resource);
            }
        }
        
        return list;
    }

    @Override
    public List<Resource> findGrantedOwnerResources(String owner, int first, int max) {
        TypedQuery<String> query = entityManager.createNamedQuery("findGrantedOwnerResources", String.class);

        query.setFlushMode(FlushModeType.COMMIT);
        query.setParameter("owner", owner);

        List<String> result = paginateQuery(query, first, max).getResultList();
        List<Resource> list = new LinkedList<>();
        ResourceStore resourceStore = provider.getStoreFactory().getResourceStore();

        for (String id : result) {
            Resource resource = resourceStore.findById(id, null);

            if (Objects.nonNull(resource)) {
                list.add(resource);
            }
        }

        return list;
    }

    @Override
    public List<PermissionTicket> findByOwner(String owner, String resourceServerId) {
        TypedQuery<String> query = entityManager.createNamedQuery("findPolicyIdByType", String.class);

        query.setFlushMode(FlushModeType.COMMIT);
        query.setParameter("serverId", resourceServerId);
        query.setParameter("owner", owner);

        List<String> result = query.getResultList();
        List<PermissionTicket> list = new LinkedList<>();
        PermissionTicketStore ticketStore = provider.getStoreFactory().getPermissionTicketStore();

        for (String id : result) {
            PermissionTicket ticket = ticketStore.findById(id, resourceServerId);
            if (Objects.nonNull(ticket)) {
                list.add(ticket);
            }
        }

        return list;
    }
}

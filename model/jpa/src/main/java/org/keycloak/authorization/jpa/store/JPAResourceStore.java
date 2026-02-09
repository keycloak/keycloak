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
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import jakarta.persistence.EntityManager;
import jakarta.persistence.FlushModeType;
import jakarta.persistence.NoResultException;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

import org.keycloak.authorization.AuthorizationProvider;
import org.keycloak.authorization.jpa.entities.ResourceEntity;
import org.keycloak.authorization.model.Resource;
import org.keycloak.authorization.model.ResourceServer;
import org.keycloak.authorization.model.Scope;
import org.keycloak.authorization.store.ResourceStore;
import org.keycloak.authorization.store.StoreFactory;
import org.keycloak.models.utils.KeycloakModelUtils;

import static org.keycloak.models.jpa.PaginationUtils.paginateQuery;
import static org.keycloak.utils.StreamsUtil.closing;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public class JPAResourceStore implements ResourceStore {

    private final EntityManager entityManager;
    private final AuthorizationProvider provider;

    public JPAResourceStore(EntityManager entityManager, AuthorizationProvider provider) {
        this.entityManager = entityManager;
        this.provider = provider;
    }

    @Override
    public Resource create(ResourceServer resourceServer, String id, String name, String owner) {
        ResourceEntity entity = new ResourceEntity();

        if (id == null) {
            entity.setId(KeycloakModelUtils.generateId());
        } else {
            entity.setId(id);
        }

        entity.setName(name);
        entity.setResourceServer(ResourceServerAdapter.toEntity(entityManager, resourceServer).getId());
        entity.setOwner(owner);

        this.entityManager.persist(entity);
        this.entityManager.flush();

        return new ResourceAdapter(entity, entityManager, provider.getStoreFactory());
    }

    @Override
    public void delete(String id) {
        ResourceEntity resource = entityManager.getReference(ResourceEntity.class, id);
        if (resource == null) return;

        resource.getScopes().clear();
        this.entityManager.remove(resource);
    }

    @Override
    public Resource findById(ResourceServer resourceServer, String id) {
        if (id == null) {
            return null;
        }

        ResourceEntity entity = entityManager.find(ResourceEntity.class, id);
        if (entity == null) return null;
        return new ResourceAdapter(entity, entityManager, provider.getStoreFactory());
    }

    @Override
    public void findByOwner(ResourceServer resourceServer, String ownerId, Consumer<Resource> consumer) {
        findByOwnerFilter(ownerId, resourceServer, consumer, -1, -1);
    }

    private void findByOwnerFilter(String ownerId, ResourceServer resourceServer, Consumer<Resource> consumer, int firstResult, int maxResult) {
        boolean pagination = firstResult > -1 && maxResult > -1;
        String queryName = pagination ? "findResourceIdByOwnerOrdered" : "findResourceIdByOwner";

        if (resourceServer == null) {
            queryName = pagination ? "findAnyResourceIdByOwnerOrdered" : "findAnyResourceIdByOwner";
        }

        TypedQuery<ResourceEntity> query = entityManager.createNamedQuery(queryName, ResourceEntity.class);

        query.setFlushMode(FlushModeType.COMMIT);
        query.setParameter("owner", ownerId);

        if (resourceServer != null) {
            query.setParameter("serverId", resourceServer.getId());
        }

        if (pagination) {
            query.setFirstResult(firstResult);
            query.setMaxResults(maxResult);
        }

        ResourceStore resourceStore = provider.getStoreFactory().getResourceStore();
        closing(query.getResultStream().map(id -> resourceStore.findById(resourceServer, id.getId()))).forEach(consumer);
    }

    @Override
    public List<Resource> findByResourceServer(ResourceServer resourceServer) {
        TypedQuery<String> query = entityManager.createNamedQuery("findResourceIdByServerId", String.class);

        query.setParameter("serverId", resourceServer == null ? null : resourceServer.getId());

        List<String> result = query.getResultList();
        List<Resource> list = new LinkedList<>();
        ResourceStore resourceStore = provider.getStoreFactory().getResourceStore();

        for (String id : result) {
            Resource resource = resourceStore.findById(resourceServer, id);

            if (resource != null) {
                list.add(resource);
            }
        }

        return list;
    }

    @Override
    public List<Resource> find(ResourceServer resourceServer, Map<Resource.FilterOption, String[]> attributes, Integer firstResult, Integer maxResults) {
        CriteriaBuilder builder = entityManager.getCriteriaBuilder();
        CriteriaQuery<String> querybuilder = builder.createQuery(String.class);
        Root<ResourceEntity> root = querybuilder.from(ResourceEntity.class);
        querybuilder.select(root.get("id"));
        List<Predicate> predicates = new ArrayList<>();

        if (resourceServer != null) {
            predicates.add(builder.equal(root.get("resourceServer"), resourceServer.getId()));
        }

        attributes.forEach((filterOption, value) -> {
            switch (filterOption) {
                case ID:
                case OWNER:
                    predicates.add(root.get(filterOption.getName()).in(value));
                    break;
                case SCOPE_ID:
                    predicates.add(root.join("scopes").get("id").in(value));
                    break;
                case OWNER_MANAGED_ACCESS:
                    predicates.add(builder.equal(root.get(filterOption.getName()), Boolean.valueOf(value[0])));
                    break;
                case URI:
                    predicates.add(builder.lower(root.join("uris")).in(value[0].toLowerCase()));
                    break;
                case URI_NOT_NULL:
                    // predicates.add(builder.isNotEmpty(root.get("uris"))); looks like there is a bug in hibernate and this line doesn't work: https://hibernate.atlassian.net/browse/HHH-6686
                    // Workaround
                    Expression<Integer> urisSize = builder.size(root.get("uris"));
                    predicates.add(builder.notEqual(urisSize, 0));
                    break;
                case NAME:
                case TYPE:
                    predicates.add(builder.like(builder.lower(root.get(filterOption.getName())), "%" + value[0].toLowerCase() + "%"));
                    break;
                case EXACT_NAME:
                    predicates.add(builder.equal(builder.lower(root.get(filterOption.getName())), value[0].toLowerCase()));
                    break;
                default:
                    throw new IllegalArgumentException("Unsupported filter [" + filterOption + "]");
            }
        });

        querybuilder.where(predicates.toArray(new Predicate[0])).orderBy(builder.asc(root.get("name")));

        TypedQuery<String> query = entityManager.createQuery(querybuilder);

        List<String> result = paginateQuery(query, firstResult, maxResults).getResultList();
        List<Resource> list = new LinkedList<>();
        ResourceStore resourceStore = provider.getStoreFactory().getResourceStore();

        for (String id : result) {
            Resource resource = resourceStore.findById(resourceServer, id);

            if (resource != null) {
                list.add(resource);
            }
        }

        return list;
    }

    @Override
    public void findByScopes(ResourceServer resourceServer, Set<Scope> scopes, Consumer<Resource> consumer) {
        TypedQuery<ResourceEntity> query = entityManager.createNamedQuery("findResourceIdByScope", ResourceEntity.class);

        query.setFlushMode(FlushModeType.COMMIT);
        query.setParameter("scopeIds", scopes.stream().map(Scope::getId).collect(Collectors.toSet()));
        query.setParameter("serverId", resourceServer == null ? null : resourceServer.getId());

        StoreFactory storeFactory = provider.getStoreFactory();

        query.getResultList().stream()
                .map(id -> new ResourceAdapter(id, entityManager, storeFactory))
                .forEach(consumer);
    }

    @Override
    public Resource findByName(ResourceServer resourceServer, String name, String ownerId) {
        TypedQuery<ResourceEntity> query = entityManager.createNamedQuery("findResourceIdByName", ResourceEntity.class);

        query.setParameter("serverId", resourceServer == null ? null : resourceServer.getId());
        query.setParameter("name", name);
        query.setParameter("ownerId", ownerId);

        try {
            return new ResourceAdapter(query.getSingleResult(), entityManager, provider.getStoreFactory());
        } catch (NoResultException ex) {
            return null;
        }
    }

    @Override
    public void findByType(ResourceServer resourceServer, String type, Consumer<Resource> consumer) {
        findByType(resourceServer, type, resourceServer == null ? null : resourceServer.getId(), consumer);
    }

    @Override
    public void findByType(ResourceServer resourceServer, String type, String owner, Consumer<Resource> consumer) {
        TypedQuery<ResourceEntity> query;

        if (owner != null) {
            query = entityManager.createNamedQuery("findResourceIdByType", ResourceEntity.class);
        } else {
            query = entityManager.createNamedQuery("findResourceIdByTypeNoOwner", ResourceEntity.class);
        }

        query.setFlushMode(FlushModeType.COMMIT);
        query.setParameter("type", type);

        if (owner != null) {
            query.setParameter("ownerId", owner);
        }

        query.setParameter("serverId", resourceServer == null ? null : resourceServer.getId());

        StoreFactory storeFactory = provider.getStoreFactory();

        query.getResultList().stream()
                .map(entity -> new ResourceAdapter(entity, entityManager, storeFactory))
                .forEach(consumer);
    }

    @Override
    public void findByTypeInstance(ResourceServer resourceServer, String type, Consumer<Resource> consumer) {
        TypedQuery<ResourceEntity> query = entityManager.createNamedQuery("findResourceIdByTypeInstance", ResourceEntity.class);

        query.setFlushMode(FlushModeType.COMMIT);
        query.setParameter("type", type);
        query.setParameter("serverId", resourceServer == null ? null : resourceServer.getId());

        StoreFactory storeFactory = provider.getStoreFactory();

        query.getResultList().stream()
                .map(entity -> new ResourceAdapter(entity, entityManager, storeFactory))
                .forEach(consumer);
    }
}

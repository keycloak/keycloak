/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2016 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.keycloak.authorization.jpa.store;

import org.keycloak.authorization.AuthorizationProvider;
import org.keycloak.authorization.jpa.entities.ResourceEntity;
import org.keycloak.authorization.model.Resource;
import org.keycloak.authorization.model.ResourceServer;
import org.keycloak.authorization.store.ResourceStore;
import org.keycloak.authorization.store.StoreFactory;
import org.keycloak.models.utils.KeycloakModelUtils;

import javax.persistence.EntityManager;
import javax.persistence.FlushModeType;
import javax.persistence.NoResultException;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

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
    public Resource create(String name, ResourceServer resourceServer, String owner) {
        return create(null, name, resourceServer, owner);
    }

    @Override
    public Resource create(String id, String name, ResourceServer resourceServer, String owner) {
        ResourceEntity entity = new ResourceEntity();

        if (id == null) {
            entity.setId(KeycloakModelUtils.generateId());
        } else {
            entity.setId(id);
        }

        entity.setName(name);
        entity.setResourceServer(ResourceServerAdapter.toEntity(entityManager, resourceServer));
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
    public Resource findById(String id, String resourceServerId) {
        if (id == null) {
            return null;
        }

        ResourceEntity entity = entityManager.find(ResourceEntity.class, id);
        if (entity == null) return null;
        return new ResourceAdapter(entity, entityManager, provider.getStoreFactory());
    }

    @Override
    public List<Resource> findByOwner(String ownerId, String resourceServerId) {
        List<Resource> list = new LinkedList<>();

        findByOwner(ownerId, resourceServerId, list::add);

        return list;
    }

    @Override
    public void findByOwner(String ownerId, String resourceServerId, Consumer<Resource> consumer) {
        findByOwnerFilter(ownerId, resourceServerId, consumer, -1, -1);
    }

    @Override
    public List<Resource> findByOwner(String ownerId, String resourceServerId, int first, int max) {
        List<Resource> list = new LinkedList<>();

        findByOwnerFilter(ownerId, resourceServerId, list::add, first, max);

        return list;
    }

    private void findByOwnerFilter(String ownerId, String resourceServerId, Consumer<Resource> consumer, int firstResult, int maxResult) {
        boolean pagination = firstResult > -1 && maxResult > -1;
        String queryName = pagination ? "findResourceIdByOwnerOrdered" : "findResourceIdByOwner";

        if (resourceServerId == null) {
            queryName = pagination ? "findAnyResourceIdByOwnerOrdered" : "findAnyResourceIdByOwner";
        }

        TypedQuery<ResourceEntity> query = entityManager.createNamedQuery(queryName, ResourceEntity.class);

        query.setFlushMode(FlushModeType.COMMIT);
        query.setParameter("owner", ownerId);

        if (resourceServerId != null) {
            query.setParameter("serverId", resourceServerId);
        }

        if (pagination) {
            query.setFirstResult(firstResult);
            query.setMaxResults(maxResult);
        }

        ResourceStore resourceStore = provider.getStoreFactory().getResourceStore();
        List<ResourceEntity> result = query.getResultList();

        for (ResourceEntity entity : result) {
            Resource cached = resourceStore.findById(entity.getId(), resourceServerId);
            
            if (cached != null) {
                consumer.accept(cached);
            }
        }
    }

    @Override
    public List<Resource> findByUri(String uri, String resourceServerId) {
        TypedQuery<String> query = entityManager.createNamedQuery("findResourceIdByUri", String.class);

        query.setFlushMode(FlushModeType.COMMIT);
        query.setParameter("uri", uri);
        query.setParameter("serverId", resourceServerId);

        List<String> result = query.getResultList();
        List<Resource> list = new LinkedList<>();
        ResourceStore resourceStore = provider.getStoreFactory().getResourceStore();

        for (String id : result) {
            Resource resource = resourceStore.findById(id, resourceServerId);

            if (resource != null) {
                list.add(resource);
            }
        }

        return list;
    }

    @Override
    public List<Resource> findByResourceServer(String resourceServerId) {
        TypedQuery<String> query = entityManager.createNamedQuery("findResourceIdByServerId", String.class);

        query.setParameter("serverId", resourceServerId);

        List<String> result = query.getResultList();
        List<Resource> list = new LinkedList<>();
        ResourceStore resourceStore = provider.getStoreFactory().getResourceStore();

        for (String id : result) {
            Resource resource = resourceStore.findById(id, resourceServerId);

            if (resource != null) {
                list.add(resource);
            }
        }

        return list;
    }

    @Override
    public List<Resource> findByResourceServer(Map<String, String[]> attributes, String resourceServerId, int firstResult, int maxResult) {
        CriteriaBuilder builder = entityManager.getCriteriaBuilder();
        CriteriaQuery<ResourceEntity> querybuilder = builder.createQuery(ResourceEntity.class);
        Root<ResourceEntity> root = querybuilder.from(ResourceEntity.class);
        querybuilder.select(root.get("id"));
        List<Predicate> predicates = new ArrayList();

        if (resourceServerId != null) {
            predicates.add(builder.equal(root.get("resourceServer").get("id"), resourceServerId));
        }

        attributes.forEach((name, value) -> {
            if ("id".equals(name)) {
                predicates.add(root.get(name).in(value));
            } else if ("scope".equals(name)) {
                predicates.add(root.join("scopes").get("id").in(value));
            } else if ("ownerManagedAccess".equals(name) && value.length > 0) {
                predicates.add(builder.equal(root.get(name), Boolean.valueOf(value[0])));
            } else if ("uri".equals(name) && value.length > 0 && value[0] != null) {
                predicates.add(builder.lower(root.join("uris")).in(value[0].toLowerCase()));
            } else if ("uri_not_null".equals(name)) {
                // predicates.add(builder.isNotEmpty(root.get("uris"))); looks like there is a bug in hibernate and this line doesn't work: https://hibernate.atlassian.net/browse/HHH-6686
                // Workaround
                Expression<Integer> urisSize = builder.size(root.get("uris"));
                predicates.add(builder.notEqual(urisSize, 0));
            } else if ("owner".equals(name)) {
                predicates.add(root.get(name).in(value));
            } else if (!Resource.EXACT_NAME.equals(name)) {
                if ("name".equals(name) && attributes.containsKey(Resource.EXACT_NAME) && Boolean.valueOf(attributes.get(Resource.EXACT_NAME)[0]) 
                        && value.length > 0 && value[0] != null) {
                    predicates.add(builder.equal(builder.lower(root.get(name)), value[0].toLowerCase()));
                } else if (value.length > 0 &&  value[0] != null) {
                    predicates.add(builder.like(builder.lower(root.get(name)), "%" + value[0].toLowerCase() + "%"));
                }
            }
        });

        querybuilder.where(predicates.toArray(new Predicate[predicates.size()])).orderBy(builder.asc(root.get("name")));

        Query query = entityManager.createQuery(querybuilder);

        if (firstResult != -1) {
            query.setFirstResult(firstResult);
        }
        if (maxResult != -1) {
            query.setMaxResults(maxResult);
        }

        List<String> result = query.getResultList();
        List<Resource> list = new LinkedList<>();
        ResourceStore resourceStore = provider.getStoreFactory().getResourceStore();

        for (String id : result) {
            Resource resource = resourceStore.findById(id, resourceServerId);

            if (resource != null) {
                list.add(resource);
            }
        }

        return list;
    }

    @Override
    public List<Resource> findByScope(List<String> scopes, String resourceServerId) {
        List<Resource> result = new ArrayList<>();

        findByScope(scopes, resourceServerId, result::add);

        return result;
    }

    @Override
    public void findByScope(List<String> scopes, String resourceServerId, Consumer<Resource> consumer) {
        TypedQuery<ResourceEntity> query = entityManager.createNamedQuery("findResourceIdByScope", ResourceEntity.class);

        query.setFlushMode(FlushModeType.COMMIT);
        query.setParameter("scopeIds", scopes);
        query.setParameter("serverId", resourceServerId);

        StoreFactory storeFactory = provider.getStoreFactory();

        query.getResultList().stream()
                .map(id -> new ResourceAdapter(id, entityManager, storeFactory))
                .forEach(consumer);
    }

    @Override
    public Resource findByName(String name, String resourceServerId) {
        return findByName(name, resourceServerId, resourceServerId);
    }

    @Override
    public Resource findByName(String name, String ownerId, String resourceServerId) {
        TypedQuery<ResourceEntity> query = entityManager.createNamedQuery("findResourceIdByName", ResourceEntity.class);

        query.setParameter("serverId", resourceServerId);
        query.setParameter("name", name);
        query.setParameter("ownerId", ownerId);

        try {
            return new ResourceAdapter(query.getSingleResult(), entityManager, provider.getStoreFactory());
        } catch (NoResultException ex) {
            return null;
        }
    }

    @Override
    public List<Resource> findByType(String type, String resourceServerId) {
        List<Resource> list = new LinkedList<>();

        findByType(type, resourceServerId, list::add);

        return list;
    }

    @Override
    public List<Resource> findByType(String type, String owner, String resourceServerId) {
        List<Resource> list = new LinkedList<>();

        findByType(type, owner, resourceServerId, list::add);

        return list;
    }

    @Override
    public void findByType(String type, String resourceServerId, Consumer<Resource> consumer) {
        findByType(type, resourceServerId, resourceServerId, consumer);
    }

    @Override
    public void findByType(String type, String owner, String resourceServerId, Consumer<Resource> consumer) {
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

        query.setParameter("serverId", resourceServerId);

        StoreFactory storeFactory = provider.getStoreFactory();

        query.getResultList().stream()
                .map(entity -> new ResourceAdapter(entity, entityManager, storeFactory))
                .forEach(consumer);
    }

    @Override
    public List<Resource> findByTypeInstance(String type, String resourceServerId) {
        List<Resource> list = new LinkedList<>();

        findByTypeInstance(type, resourceServerId, list::add);

        return list;
    }

    @Override
    public void findByTypeInstance(String type, String resourceServerId, Consumer<Resource> consumer) {
        TypedQuery<ResourceEntity> query = entityManager.createNamedQuery("findResourceIdByTypeInstance", ResourceEntity.class);

        query.setFlushMode(FlushModeType.COMMIT);
        query.setParameter("type", type);
        query.setParameter("serverId", resourceServerId);

        StoreFactory storeFactory = provider.getStoreFactory();

        query.getResultList().stream()
                .map(entity -> new ResourceAdapter(entity, entityManager, storeFactory))
                .forEach(consumer);
    }
}

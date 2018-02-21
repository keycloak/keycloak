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
import org.keycloak.models.utils.KeycloakModelUtils;

import javax.persistence.EntityManager;
import javax.persistence.FlushModeType;
import javax.persistence.NoResultException;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

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
        ResourceEntity entity = new ResourceEntity();

        entity.setId(KeycloakModelUtils.generateId());
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
        TypedQuery<String> query = entityManager.createNamedQuery("findResourceIdByOwner", String.class);

        query.setFlushMode(FlushModeType.COMMIT);
        query.setParameter("owner", ownerId);
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

        predicates.add(builder.equal(root.get("resourceServer").get("id"), resourceServerId));

        attributes.forEach((name, value) -> {
            if ("id".equals(name)) {
                predicates.add(root.get(name).in(value));
            } else if ("scope".equals(name)) {
                predicates.add(root.join("scopes").get("id").in(value));
            } else {
                predicates.add(builder.like(builder.lower(root.get(name)), "%" + value[0].toLowerCase() + "%"));
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
        TypedQuery<String> query = entityManager.createNamedQuery("findResourceIdByScope", String.class);

        query.setFlushMode(FlushModeType.COMMIT);
        query.setParameter("scopeIds", scopes);
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
    public Resource findByName(String name, String resourceServerId) {
        TypedQuery<String> query = entityManager.createNamedQuery("findResourceIdByName", String.class);

        query.setFlushMode(FlushModeType.COMMIT);
        query.setParameter("serverId", resourceServerId);
        query.setParameter("name", name);

        try {
            String id = query.getSingleResult();
            return provider.getStoreFactory().getResourceStore().findById(id, resourceServerId);
        } catch (NoResultException ex) {
            return null;
        }
    }

    @Override
    public List<Resource> findByType(String type, String resourceServerId) {
        TypedQuery<String> query = entityManager.createNamedQuery("findResourceIdByType", String.class);

        query.setFlushMode(FlushModeType.COMMIT);
        query.setParameter("type", type);
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
}

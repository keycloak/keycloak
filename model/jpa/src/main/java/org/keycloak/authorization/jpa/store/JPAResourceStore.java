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

import org.keycloak.authorization.jpa.entities.ResourceEntity;
import org.keycloak.authorization.jpa.entities.ResourceServerEntity;
import org.keycloak.authorization.model.Resource;
import org.keycloak.authorization.model.ResourceServer;
import org.keycloak.authorization.store.ResourceStore;
import org.keycloak.models.utils.KeycloakModelUtils;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public class JPAResourceStore implements ResourceStore {

    private final EntityManager entityManager;

    public JPAResourceStore(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public Resource create(String name, ResourceServer resourceServer, String owner) {
        if (!(resourceServer instanceof ResourceServerEntity)) {
            throw new RuntimeException("Unexpected type [" + resourceServer.getClass() + "].");
        }

        ResourceEntity entity = new ResourceEntity();

        entity.setId(KeycloakModelUtils.generateId());
        entity.setName(name);
        entity.setResourceServer((ResourceServerEntity) resourceServer);
        entity.setOwner(owner);

        this.entityManager.persist(entity);

        return entity;
    }

    @Override
    public void delete(String id) {
        Resource resource = findById(id);

        resource.getScopes().clear();

        if (resource != null) {
            this.entityManager.remove(resource);
        }
    }

    @Override
    public Resource findById(String id) {
        if (id == null) {
            return null;
        }

        return entityManager.find(ResourceEntity.class, id);
    }

    @Override
    public List<Resource> findByOwner(String ownerId) {
        Query query = entityManager.createQuery("from ResourceEntity where owner = :ownerId");

        query.setParameter("ownerId", ownerId);

        return query.getResultList();
    }

    @Override
    public List findByResourceServer(String resourceServerId) {
        Query query = entityManager.createQuery("from ResourceEntity where resourceServer.id = :serverId");

        query.setParameter("serverId", resourceServerId);

        return query.getResultList();
    }

    @Override
    public List findByResourceServer(Map<String, String[]> attributes, String resourceServerId, int firstResult, int maxResult) {
        CriteriaBuilder builder = entityManager.getCriteriaBuilder();
        CriteriaQuery<ResourceEntity> querybuilder = builder.createQuery(ResourceEntity.class);
        Root<ResourceEntity> root = querybuilder.from(ResourceEntity.class);
        List<Predicate> predicates = new ArrayList();

        predicates.add(builder.equal(root.get("resourceServer").get("id"), resourceServerId));

        attributes.forEach((name, value) -> {
            if ("scope".equals(name)) {
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

        return query.getResultList();
    }

    @Override
    public List<Resource> findByScope(String... id) {
        Query query = entityManager.createQuery("select r from ResourceEntity r inner join r.scopes s where s.id in (:scopeIds)");

        query.setParameter("scopeIds", Arrays.asList(id));

        return query.getResultList();
    }

    @Override
    public Resource findByName(String name, String resourceServerId) {
        Query query = entityManager.createQuery("from ResourceEntity where resourceServer.id = :serverId and name = :name");

        query.setParameter("serverId", resourceServerId);
        query.setParameter("name", name);

        List<Resource> result = query.getResultList();

        if (!result.isEmpty()) {
            return result.get(0);
        }

        return null;
    }

    @Override
    public List<Resource> findByType(String type) {
        Query query = entityManager.createQuery("from ResourceEntity where type = :type");

        query.setParameter("type", type);

        return query.getResultList();
    }
}

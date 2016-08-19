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

import org.keycloak.authorization.jpa.entities.ResourceServerEntity;
import org.keycloak.authorization.jpa.entities.ScopeEntity;
import org.keycloak.authorization.model.ResourceServer;
import org.keycloak.authorization.model.Scope;
import org.keycloak.authorization.store.ScopeStore;
import org.keycloak.models.utils.KeycloakModelUtils;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.Query;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public class JPAScopeStore implements ScopeStore {

    private final EntityManager entityManager;

    public JPAScopeStore(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public Scope create(final String name, final ResourceServer resourceServer) {
        ScopeEntity entity = new ScopeEntity();

        entity.setId(KeycloakModelUtils.generateId());
        entity.setName(name);
        entity.setResourceServer((ResourceServerEntity) resourceServer);

        this.entityManager.persist(entity);

        return entity;
    }

    @Override
    public void delete(String id) {
        this.entityManager.remove(findById(id));
    }

    @Override
    public Scope findById(String id) {
        return entityManager.find(ScopeEntity.class, id);
    }

    @Override
    public Scope findByName(String name, String resourceServerId) {
        try {
            Query query = entityManager.createQuery("select s from ScopeEntity s inner join s.resourceServer rs where rs.id = :resourceServerId and name = :name");

            query.setParameter("name", name);
            query.setParameter("resourceServerId", resourceServerId);

            return (Scope) query.getSingleResult();
        } catch (NoResultException nre) {
            return null;
        }
    }

    @Override
    public List<Scope> findByResourceServer(final String serverId) {
        Query query = entityManager.createQuery("from ScopeEntity where resourceServer.id = :serverId");

        query.setParameter("serverId", serverId);

        return query.getResultList();
    }

    @Override
    public List<Scope> findByResourceServer(Map<String, String[]> attributes, String resourceServerId, int firstResult, int maxResult) {
        CriteriaBuilder builder = entityManager.getCriteriaBuilder();
        CriteriaQuery<ScopeEntity> querybuilder = builder.createQuery(ScopeEntity.class);
        Root<ScopeEntity> root = querybuilder.from(ScopeEntity.class);
        List<Predicate> predicates = new ArrayList();

        predicates.add(builder.equal(root.get("resourceServer").get("id"), resourceServerId));

        attributes.forEach((name, value) -> {
            predicates.add(builder.like(builder.lower(root.get(name)), "%" + value[0].toLowerCase() + "%"));
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
}

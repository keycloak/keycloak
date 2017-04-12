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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.Query;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import org.keycloak.authorization.jpa.entities.PolicyEntity;
import org.keycloak.authorization.jpa.entities.ResourceServerEntity;
import org.keycloak.authorization.model.Policy;
import org.keycloak.authorization.model.ResourceServer;
import org.keycloak.authorization.store.PolicyStore;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.representations.idm.authorization.AbstractPolicyRepresentation;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public class JPAPolicyStore implements PolicyStore {

    private final EntityManager entityManager;

    public JPAPolicyStore(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public Policy create(AbstractPolicyRepresentation representation, ResourceServer resourceServer) {
        PolicyEntity entity = new PolicyEntity();

        entity.setId(KeycloakModelUtils.generateId());
        entity.setType(representation.getType());
        entity.setName(representation.getName());
        entity.setResourceServer((ResourceServerEntity) resourceServer);

        this.entityManager.persist(entity);
        this.entityManager.flush();
        return entity;
    }

    @Override
    public void delete(String id) {
        Policy policy = entityManager.find(PolicyEntity.class, id);

        if (policy != null) {
            this.entityManager.remove(policy);
        }
    }


    @Override
    public Policy findById(String id, String resourceServerId) {
        if (id == null) {
            return null;
        }

        if (resourceServerId == null) {
            return entityManager.find(PolicyEntity.class, id);
        }

        Query query = entityManager.createQuery("from PolicyEntity where resourceServer.id = :serverId and id = :id");

        query.setParameter("serverId", resourceServerId);
        query.setParameter("id", id);

        return entityManager.find(PolicyEntity.class, id);
    }

    @Override
    public Policy findByName(String name, String resourceServerId) {
        try {
            Query query = entityManager.createQuery("from PolicyEntity where name = :name and resourceServer.id = :serverId");

            query.setParameter("name", name);
            query.setParameter("serverId", resourceServerId);

            return (Policy) query.getSingleResult();
        } catch (NoResultException nre) {
            return null;
        }
    }

    @Override
    public List<Policy> findByResourceServer(final String resourceServerId) {
        Query query = entityManager.createQuery("from PolicyEntity where resourceServer.id = :serverId");

        query.setParameter("serverId", resourceServerId);

        return query.getResultList();
    }

    @Override
    public List<Policy> findByResourceServer(Map<String, String[]> attributes, String resourceServerId, int firstResult, int maxResult) {
        CriteriaBuilder builder = entityManager.getCriteriaBuilder();
        CriteriaQuery<PolicyEntity> querybuilder = builder.createQuery(PolicyEntity.class);
        Root<PolicyEntity> root = querybuilder.from(PolicyEntity.class);
        List<Predicate> predicates = new ArrayList();

        predicates.add(builder.equal(root.get("resourceServer").get("id"), resourceServerId));

        attributes.forEach((name, value) -> {
            if ("permission".equals(name)) {
                if (Boolean.valueOf(value[0])) {
                    predicates.add(root.get("type").in("resource", "scope"));
                } else {
                    predicates.add(builder.not(root.get("type").in("resource", "scope")));
                }
            } else if ("id".equals(name)) {
                predicates.add(root.get(name).in(value));
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
    public List<Policy> findByResource(final String resourceId, String resourceServerId) {
        Query query = entityManager.createQuery("select p from PolicyEntity p inner join p.resources r where p.resourceServer.id = :serverId and (r.resourceServer.id = :serverId and r.id = :resourceId)");

        query.setParameter("resourceId", resourceId);
        query.setParameter("serverId", resourceServerId);

        return query.getResultList();
    }

    @Override
    public List<Policy> findByResourceType(final String resourceType, String resourceServerId) {
        Query query = entityManager.createQuery("select p from PolicyEntity p inner join p.config c where p.resourceServer.id = :serverId and KEY(c) = 'defaultResourceType' and c like :type");

        query.setParameter("serverId", resourceServerId);
        query.setParameter("type", resourceType);

        return query.getResultList();
    }

    @Override
    public List<Policy> findByScopeIds(List<String> scopeIds, String resourceServerId) {
        if (scopeIds==null || scopeIds.isEmpty()) {
            return Collections.emptyList();
        }

        // Use separate subquery to handle DB2 and MSSSQL
        Query query = entityManager.createQuery("select pe from PolicyEntity pe where pe.resourceServer.id = :serverId and pe.id IN (select p.id from ScopeEntity s inner join s.policies p where s.resourceServer.id = :serverId and (p.resourceServer.id = :serverId and p.type = 'scope' and s.id in (:scopeIds)))");

        query.setParameter("serverId", resourceServerId);
        query.setParameter("scopeIds", scopeIds);

        return query.getResultList();
    }

    @Override
    public List<Policy> findByType(String type, String resourceServerId) {
        Query query = entityManager.createQuery("select p from PolicyEntity p where p.resourceServer.id = :serverId and p.type = :type");

        query.setParameter("serverId", resourceServerId);
        query.setParameter("type", type);

        return query.getResultList();
    }

    @Override
    public List<Policy> findDependentPolicies(String policyId, String resourceServerId) {
        Query query = entityManager.createQuery("select p from PolicyEntity p inner join p.associatedPolicies ap where p.resourceServer.id = :serverId and (ap.resourceServer.id = :serverId and ap.id = :policyId)");

        query.setParameter("serverId", resourceServerId);
        query.setParameter("policyId", policyId);

        return query.getResultList();
    }
}

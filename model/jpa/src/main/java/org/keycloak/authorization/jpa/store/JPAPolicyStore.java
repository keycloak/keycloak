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
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.FlushModeType;
import javax.persistence.NoResultException;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import org.keycloak.authorization.AuthorizationProvider;
import org.keycloak.authorization.jpa.entities.PolicyEntity;
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
    private final AuthorizationProvider provider;
    public JPAPolicyStore(EntityManager entityManager, AuthorizationProvider provider) {
        this.entityManager = entityManager;
        this.provider = provider;
    }

    @Override
    public Policy create(AbstractPolicyRepresentation representation, ResourceServer resourceServer) {
        PolicyEntity entity = new PolicyEntity();

        entity.setId(KeycloakModelUtils.generateId());
        entity.setType(representation.getType());
        entity.setName(representation.getName());
        entity.setResourceServer(ResourceServerAdapter.toEntity(entityManager, resourceServer));

        this.entityManager.persist(entity);
        this.entityManager.flush();
        Policy model = new PolicyAdapter(entity, entityManager, provider.getStoreFactory());
        return model;
    }

    @Override
    public void delete(String id) {
        PolicyEntity policy = entityManager.find(PolicyEntity.class, id);
        if (policy != null) {
            this.entityManager.remove(policy);
        }
    }


    @Override
    public Policy findById(String id, String resourceServerId) {
        if (id == null) {
            return null;
        }

        PolicyEntity entity = entityManager.find(PolicyEntity.class, id);
        if (entity == null) return null;

        return new PolicyAdapter(entity, entityManager, provider.getStoreFactory());
    }

    @Override
    public Policy findByName(String name, String resourceServerId) {
        TypedQuery<String> query = entityManager.createNamedQuery("findPolicyIdByName", String.class);

        query.setFlushMode(FlushModeType.COMMIT);
        query.setParameter("serverId", resourceServerId);
        query.setParameter("name", name);

        try {
            String id = query.getSingleResult();
            return provider.getStoreFactory().getPolicyStore().findById(id, resourceServerId);
        } catch (NoResultException ex) {
            return null;
        }
    }

    @Override
    public List<Policy> findByResourceServer(final String resourceServerId) {
        TypedQuery<String> query = entityManager.createNamedQuery("findPolicyIdByServerId", String.class);

        query.setParameter("serverId", resourceServerId);

        List<String> result = query.getResultList();
        List<Policy> list = new LinkedList<>();
        for (String id : result) {
            list.add(provider.getStoreFactory().getPolicyStore().findById(id, resourceServerId));
        }
        return list;
    }

    @Override
    public List<Policy> findByResourceServer(Map<String, String[]> attributes, String resourceServerId, int firstResult, int maxResult) {
        CriteriaBuilder builder = entityManager.getCriteriaBuilder();
        CriteriaQuery<PolicyEntity> querybuilder = builder.createQuery(PolicyEntity.class);
        Root<PolicyEntity> root = querybuilder.from(PolicyEntity.class);
        List<Predicate> predicates = new ArrayList();
        querybuilder.select(root.get("id"));

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

        List<String> result = query.getResultList();
        List<Policy> list = new LinkedList<>();
        for (String id : result) {
            list.add(provider.getStoreFactory().getPolicyStore().findById(id, resourceServerId));
        }
        return list;
    }

    @Override
    public List<Policy> findByResource(final String resourceId, String resourceServerId) {
        TypedQuery<String> query = entityManager.createNamedQuery("findPolicyIdByResource", String.class);

        query.setFlushMode(FlushModeType.COMMIT);
        query.setParameter("resourceId", resourceId);
        query.setParameter("serverId", resourceServerId);

        List<String> result = query.getResultList();
        List<Policy> list = new LinkedList<>();
        for (String id : result) {
            list.add(provider.getStoreFactory().getPolicyStore().findById(id, resourceServerId));
        }
        return list;
    }

    @Override
    public List<Policy> findByResourceType(final String resourceType, String resourceServerId) {
        TypedQuery<String> query = entityManager.createNamedQuery("findPolicyIdByResourceType", String.class);

        query.setFlushMode(FlushModeType.COMMIT);
        query.setParameter("type", resourceType);
        query.setParameter("serverId", resourceServerId);

        List<String> result = query.getResultList();
        List<Policy> list = new LinkedList<>();
        for (String id : result) {
            list.add(provider.getStoreFactory().getPolicyStore().findById(id, resourceServerId));
        }
        return list;
    }

    @Override
    public List<Policy> findByScopeIds(List<String> scopeIds, String resourceServerId) {
        if (scopeIds==null || scopeIds.isEmpty()) {
            return Collections.emptyList();
        }

        // Use separate subquery to handle DB2 and MSSSQL
        TypedQuery<String> query = entityManager.createNamedQuery("findPolicyIdByScope", String.class);

        query.setFlushMode(FlushModeType.COMMIT);
        query.setParameter("scopeIds", scopeIds);
        query.setParameter("serverId", resourceServerId);

        List<String> result = query.getResultList();
        List<Policy> list = new LinkedList<>();
        for (String id : result) {
            list.add(provider.getStoreFactory().getPolicyStore().findById(id, resourceServerId));
        }
        return list;
    }

    @Override
    public List<Policy> findByType(String type, String resourceServerId) {
        TypedQuery<String> query = entityManager.createNamedQuery("findPolicyIdByType", String.class);

        query.setFlushMode(FlushModeType.COMMIT);
        query.setParameter("serverId", resourceServerId);
        query.setParameter("type", type);

        List<String> result = query.getResultList();
        List<Policy> list = new LinkedList<>();
        for (String id : result) {
            list.add(provider.getStoreFactory().getPolicyStore().findById(id, resourceServerId));
        }
        return list;
    }

    @Override
    public List<Policy> findDependentPolicies(String policyId, String resourceServerId) {

        TypedQuery<String> query = entityManager.createNamedQuery("findPolicyIdByDependentPolices", String.class);

        query.setFlushMode(FlushModeType.COMMIT);
        query.setParameter("serverId", resourceServerId);
        query.setParameter("policyId", policyId);

        List<String> result = query.getResultList();
        List<Policy> list = new LinkedList<>();
        for (String id : result) {
            list.add(provider.getStoreFactory().getPolicyStore().findById(id, resourceServerId));
        }
        return list;
    }
}

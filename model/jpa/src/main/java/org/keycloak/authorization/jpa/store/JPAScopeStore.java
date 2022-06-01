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

import javax.persistence.EntityManager;
import javax.persistence.FlushModeType;
import javax.persistence.NoResultException;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import org.keycloak.authorization.AuthorizationProvider;
import org.keycloak.authorization.jpa.entities.ScopeEntity;
import org.keycloak.authorization.model.ResourceServer;
import org.keycloak.authorization.model.Scope;
import org.keycloak.authorization.store.PermissionTicketStore;
import org.keycloak.authorization.store.ScopeStore;
import org.keycloak.models.RealmModel;
import org.keycloak.models.utils.KeycloakModelUtils;
import javax.persistence.LockModeType;

import static org.keycloak.models.jpa.PaginationUtils.paginateQuery;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public class JPAScopeStore implements ScopeStore {

    private final EntityManager entityManager;
    private final AuthorizationProvider provider;

    public JPAScopeStore(EntityManager entityManager, AuthorizationProvider provider) {
        this.entityManager = entityManager;
        this.provider = provider;
    }

    @Override
    public Scope create(final ResourceServer resourceServer, final String name) {
        return create(resourceServer, null, name);
    }

    @Override
    public Scope create(final ResourceServer resourceServer, String id, final String name) {
        ScopeEntity entity = new ScopeEntity();

        if (id == null) {
            entity.setId(KeycloakModelUtils.generateId());
        } else {
            entity.setId(id);
        }

        entity.setName(name);
        entity.setResourceServer(ResourceServerAdapter.toEntity(entityManager, resourceServer));

        this.entityManager.persist(entity);
        this.entityManager.flush();

        return new ScopeAdapter(entity, entityManager, provider.getStoreFactory());
    }

    @Override
    public void delete(RealmModel realm, String id) {
        ScopeEntity scope = entityManager.find(ScopeEntity.class, id, LockModeType.PESSIMISTIC_WRITE);

        if (scope != null) {
            this.entityManager.remove(scope);
        }
    }

    @Override
    public Scope findById(RealmModel realm, ResourceServer resourceServer, String id) {
        if (id == null) {
            return null;
        }

        ScopeEntity entity = entityManager.find(ScopeEntity.class, id);
        if (entity == null) return null;

        return new ScopeAdapter(entity, entityManager, provider.getStoreFactory());
    }

    @Override
    public Scope findByName(ResourceServer resourceServer, String name) {
        try {
            TypedQuery<String> query = entityManager.createNamedQuery("findScopeIdByName", String.class);

            query.setFlushMode(FlushModeType.COMMIT);
            query.setParameter("serverId", resourceServer.getId());
            query.setParameter("name", name);

            String id = query.getSingleResult();
            return provider.getStoreFactory().getScopeStore().findById(JPAAuthorizationStoreFactory.NULL_REALM, resourceServer, id);
        } catch (NoResultException nre) {
            return null;
        }
    }

    @Override
    public List<Scope> findByResourceServer(final ResourceServer resourceServer) {
        TypedQuery<String> query = entityManager.createNamedQuery("findScopeIdByResourceServer", String.class);

        query.setFlushMode(FlushModeType.COMMIT);
        query.setParameter("serverId", resourceServer.getId());

        List<String> result = query.getResultList();
        List<Scope> list = new LinkedList<>();
        for (String id : result) {
            list.add(provider.getStoreFactory().getScopeStore().findById(JPAAuthorizationStoreFactory.NULL_REALM, resourceServer, id));
        }
        return list;
    }

    @Override
    public List<Scope> findByResourceServer(ResourceServer resourceServer, Map<Scope.FilterOption, String[]> attributes, Integer firstResult, Integer maxResults) {
        CriteriaBuilder builder = entityManager.getCriteriaBuilder();
        CriteriaQuery<ScopeEntity> querybuilder = builder.createQuery(ScopeEntity.class);
        Root<ScopeEntity> root = querybuilder.from(ScopeEntity.class);
        querybuilder.select(root.get("id"));
        List<Predicate> predicates = new ArrayList();

        predicates.add(builder.equal(root.get("resourceServer").get("id"), resourceServer.getId()));

        attributes.forEach((filterOption, value) -> {
            switch (filterOption) {
                case ID:
                    predicates.add(root.get(filterOption.getName()).in(value));
                    break;
                case NAME:
                    predicates.add(builder.like(builder.lower(root.get(filterOption.getName())), "%" + value[0].toLowerCase() + "%"));
                    break;
                default:
                    throw new IllegalArgumentException("Unsupported filter [" + filterOption + "]");
            }
        });

        querybuilder.where(predicates.toArray(new Predicate[predicates.size()])).orderBy(builder.asc(root.get("name")));

        TypedQuery query = entityManager.createQuery(querybuilder);

        List result = paginateQuery(query, firstResult, maxResults).getResultList();
        List<Scope> list = new LinkedList<>();
        for (Object id : result) {
            list.add(provider.getStoreFactory().getScopeStore().findById(JPAAuthorizationStoreFactory.NULL_REALM, resourceServer, (String)id));
        }
        return list;

    }
}

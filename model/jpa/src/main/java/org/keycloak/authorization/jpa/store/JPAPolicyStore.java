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
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jakarta.persistence.EntityManager;
import jakarta.persistence.FlushModeType;
import jakarta.persistence.LockModeType;
import jakarta.persistence.NoResultException;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.MapJoin;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

import org.keycloak.authorization.AuthorizationProvider;
import org.keycloak.authorization.fgap.AdminPermissionsSchema;
import org.keycloak.authorization.jpa.entities.PolicyEntity;
import org.keycloak.authorization.model.Policy;
import org.keycloak.authorization.model.Resource;
import org.keycloak.authorization.model.ResourceServer;
import org.keycloak.authorization.model.Scope;
import org.keycloak.authorization.store.PolicyStore;
import org.keycloak.authorization.store.StoreFactory;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.representations.idm.authorization.AbstractPolicyRepresentation;

import org.hibernate.Session;

import static org.keycloak.models.jpa.PaginationUtils.paginateQuery;
import static org.keycloak.utils.StreamsUtil.closing;

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
    public Policy create(ResourceServer resourceServer, AbstractPolicyRepresentation representation) {
        PolicyEntity entity = new PolicyEntity();

        if (representation.getId() == null) {
            entity.setId(KeycloakModelUtils.generateId());
        } else {
            entity.setId(representation.getId());
        }

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
        PolicyEntity policy = entityManager.find(PolicyEntity.class, id, LockModeType.PESSIMISTIC_WRITE);
        if (policy != null) {
            this.entityManager.remove(policy);
        }
    }


    @Override
    public Policy findById(ResourceServer resourceServer, String id) {
        if (id == null) {
            return null;
        }

        PolicyEntity policyEntity = entityManager.find(PolicyEntity.class, id);

        if (policyEntity == null) {
            return null;
        }

        return new PolicyAdapter(policyEntity, entityManager, provider.getStoreFactory());
    }

    @Override
    public Policy findByName(ResourceServer resourceServer, String name) {
        TypedQuery<PolicyEntity> query = entityManager.createNamedQuery("findPolicyIdByName", PolicyEntity.class);

        query.setFlushMode(FlushModeType.COMMIT);
        query.setParameter("serverId", resourceServer.getId());
        query.setParameter("name", name);

        try {
            PolicyEntity policy = query.getSingleResult();
            return provider.getStoreFactory().getPolicyStore().findById(resourceServer, policy.getId());
        } catch (NoResultException ex) {
            return null;
        }
    }

    @Override
    public List<Policy> findByResourceServer(final ResourceServer resourceServer) {
        TypedQuery<String> query = entityManager.createNamedQuery("findPolicyIdByServerId", String.class);

        query.setParameter("serverId", resourceServer.getId());

        List<String> result = query.getResultList();
        List<Policy> list = new LinkedList<>();
        for (String id : result) {
            Policy policy = provider.getStoreFactory().getPolicyStore().findById(resourceServer, id);
            if (Objects.nonNull(policy)) {
                list.add(policy);
            }
        }
        return list;
    }

    @Override
    public List<Policy> find(ResourceServer resourceServer, Map<Policy.FilterOption, String[]> attributes, Integer firstResult, Integer maxResults) {
        CriteriaBuilder builder = entityManager.getCriteriaBuilder();
        CriteriaQuery<String> querybuilder = builder.createQuery(String.class);
        Root<PolicyEntity> root = querybuilder.from(PolicyEntity.class);
        List<Predicate> predicates = new ArrayList();
        querybuilder.select(root.get("id"));

        if (resourceServer != null) {
            predicates.add(builder.equal(root.get("resourceServer").get("id"), resourceServer.getId()));
        }

        attributes.forEach((filterOption, value) -> {
            switch (filterOption) {
                case ID:
                case OWNER:
                    predicates.add(root.get(filterOption.getName()).in(value));
                    break;
                case SCOPE_ID:
                case RESOURCE_ID:
                    String[] predicateValues = filterOption.getName().split("\\.");
                    predicates.add(root.join(predicateValues[0]).get(predicateValues[1]).in(value));
                    break;
                case PERMISSION: {
                    if (Boolean.parseBoolean(value[0])) {
                        predicates.add(root.get("type").in("resource", "scope", "uma"));
                    } else {
                        predicates.add(builder.not(root.get("type").in("resource", "scope", "uma")));
                    }
                }
                    break;
                case ANY_OWNER:
                    break;
                case CONFIG:
                    if (value.length != 2) {
                        throw new IllegalArgumentException("Config filter option requires value with two items: [config_name, expected_config_value]");
                    }

                    predicates.add(root.joinMap("config").key().in(value[0]));
                    predicates.add(builder.like(root.joinMap("config").value().as(String.class), "%" + value[1] + "%"));
                    break;
                case TYPE:
                case NAME:
                    predicates.add(builder.like(builder.lower(root.get(filterOption.getName())), "%" + value[0].toLowerCase() + "%"));
                    break;
                default:
                    throw new IllegalArgumentException("Unsupported filter [" + filterOption + "]");
            }
        });

        if (!attributes.containsKey(Policy.FilterOption.OWNER) && !attributes.containsKey(Policy.FilterOption.ANY_OWNER)) {
            predicates.add(builder.isNull(root.get("owner")));
        }

        querybuilder.where(predicates.toArray(new Predicate[predicates.size()])).orderBy(builder.asc(root.get("name")));

        TypedQuery query = entityManager.createQuery(querybuilder);

        List<String> result = paginateQuery(query, firstResult, maxResults).getResultList();
        List<Policy> list = new LinkedList<>();
        PolicyStore policyStore = provider.getStoreFactory().getPolicyStore();
        for (String id : result) {
            Policy policy = policyStore.findById(resourceServer, id);
            if (Objects.nonNull(policy)) {
                list.add(policy);
            }
        }
        return list;
    }

    @Override
    public void findByResource(ResourceServer resourceServer, Resource resource, Consumer<Policy> consumer) {
        TypedQuery<PolicyEntity> query = entityManager.createNamedQuery("findPolicyIdByResource", PolicyEntity.class);

        query.setFlushMode(FlushModeType.COMMIT);
        query.setParameter("resourceId", resource.getId());
        query.setParameter("serverId", resourceServer.getId());

        PolicyStore storeFactory = provider.getStoreFactory().getPolicyStore();

        closing(query.getResultStream()
                .map(entity -> storeFactory.findById(resourceServer, entity.getId()))
                .filter(Objects::nonNull))
                .forEach(consumer::accept);
    }

    @Override
    public void findByResourceType(ResourceServer resourceServer, String resourceType, Consumer<Policy> consumer) {
        TypedQuery<PolicyEntity> query = entityManager.createNamedQuery("findPolicyIdByResourceType", PolicyEntity.class);

        query.setFlushMode(FlushModeType.COMMIT);
        query.setParameter("type", resourceType);
        query.setParameter("serverId", resourceServer.getId());

        closing(query.getResultStream()
                .map(id -> new PolicyAdapter(id, entityManager, provider.getStoreFactory()))
                .filter(Objects::nonNull))
                .forEach(consumer::accept);
    }

    @Override
    public List<Policy> findByScopes(ResourceServer resourceServer, List<Scope> scopes) {
        if (scopes==null || scopes.isEmpty()) {
            return Collections.emptyList();
        }

        // Use separate subquery to handle DB2 and MSSSQL
        TypedQuery<PolicyEntity> query = entityManager.createNamedQuery("findPolicyIdByScope", PolicyEntity.class);

        query.setFlushMode(FlushModeType.COMMIT);
        query.setParameter("scopeIds", scopes.stream().map(Scope::getId).collect(Collectors.toSet()));
        query.setParameter("serverId", resourceServer.getId());

        List<Policy> list = new LinkedList<>();
        PolicyStore storeFactory = provider.getStoreFactory().getPolicyStore();

        for (PolicyEntity entity : query.getResultList()) {
            list.add(storeFactory.findById(resourceServer, entity.getId()));
        }

        return list;
    }

    @Override
    public void findByScopes(ResourceServer resourceServer, Resource resource, List<Scope> scopes, Consumer<Policy> consumer) {
        // Use separate subquery to handle DB2 and MSSSQL
        TypedQuery<PolicyEntity> query;

        if (resource == null) {
            query = entityManager.createNamedQuery("findPolicyIdByNullResourceScope", PolicyEntity.class);
        } else {
            query = entityManager.createNamedQuery("findPolicyIdByResourceScope", PolicyEntity.class);
            query.setParameter("resourceId", resource.getId());
        }

        query.setFlushMode(FlushModeType.COMMIT);
        query.setParameter("scopeIds", scopes.stream().map(Scope::getId).collect(Collectors.toSet()));
        query.setParameter("serverId", resourceServer.getId());

        StoreFactory storeFactory = provider.getStoreFactory();

        closing(query.getResultStream()
                .map(id -> new PolicyAdapter(id, entityManager, storeFactory))
                .filter(Objects::nonNull))
                .forEach(consumer::accept);
    }

    @Override
    public List<Policy> findByType(ResourceServer resourceServer, String type) {
        TypedQuery<String> query = entityManager.createNamedQuery("findPolicyIdByType", String.class);

        query.setFlushMode(FlushModeType.COMMIT);
        query.setParameter("serverId", resourceServer.getId());
        query.setParameter("type", type);

        List<String> result = query.getResultList();
        List<Policy> list = new LinkedList<>();
        for (String id : result) {
            Policy policy = provider.getStoreFactory().getPolicyStore().findById(resourceServer, id);
            if (Objects.nonNull(policy)) {
                list.add(policy);
            }
        }
        return list;
    }

    @Override
    public List<Policy> findDependentPolicies(ResourceServer resourceServer, String policyId) {
        TypedQuery<String> query = entityManager.createNamedQuery("findPolicyIdByDependentPolices", String.class);

        query.setFlushMode(FlushModeType.COMMIT);
        query.setParameter("serverId", resourceServer.getId());
        query.setParameter("policyId", policyId);

        List<String> result = query.getResultList();
        List<Policy> list = new LinkedList<>();
        for (String id : result) {
            Policy policy = provider.getStoreFactory().getPolicyStore().findById(resourceServer, id);
            if (Objects.nonNull(policy)) {
                list.add(policy);
            }
        }
        return list;
    }

    @Override
    public Stream<Policy> findDependentPolicies(ResourceServer resourceServer, String resourceType, String associatedPolicyType, String configKey, String configValue) {
        return findDependentPolicies(resourceServer, resourceType, associatedPolicyType, configKey, List.of(configValue));
    }

    @Override
    public Stream<Policy> findDependentPolicies(ResourceServer resourceServer, String resourceType, String associatedPolicyType, String configKey, List<String> configValues) {
        String dbProductName = entityManager.unwrap(Session.class).doReturningWork(connection -> connection.getMetaData().getDatabaseProductName());

        if (dbProductName.equals("Oracle")) {
            Stream<Policy> result = Stream.empty();

            for (String value : configValues) {
                TypedQuery<String> query = entityManager.createNamedQuery("findDependentPolicyByResourceTypeAndConfig", String.class);

                query.setParameter("serverId", resourceServer.getId());
                query.setParameter("resourceType", resourceType);
                query.setParameter("associatedPolicyType", associatedPolicyType);
                query.setParameter("configKey", configKey);
                query.setParameter("configValue", "%" + value + "%");

                PolicyStore policyStore = provider.getStoreFactory().getPolicyStore();

                result = Stream.concat(result, query.getResultStream().map((id) -> policyStore.findById(resourceServer, id)).filter(Objects::nonNull));
            }

            return result;
        }

        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<String> query = cb.createQuery(String.class);
        Root<PolicyEntity> from = query.from(PolicyEntity.class);

        query.select(from.get("id"));

        Join<Object, Object> scope = from.join("scopes");
        MapJoin<Object, Object, Object> config = from.joinMap("config");
        Join<Object, Object> associatedPolicy = from.join("associatedPolicies");
        MapJoin<Object, Object, Object> associatedPolicyConfig = associatedPolicy.joinMap("config");

        List<Predicate> predicates = new LinkedList<>();

        predicates.add(cb.equal(from.get("resourceServer").get("id"), resourceServer.getId()));
        predicates.add(scope.get("name").in(AdminPermissionsSchema.VIEW, AdminPermissionsSchema.VIEW_MEMBERS));
        predicates.add(cb.equal(associatedPolicy.get("type"), associatedPolicyType));
        predicates.add(cb.equal(config.key(), "defaultResourceType"));
        predicates.add(cb.equal(config.value(), resourceType));

        List<Predicate> configValuePredicates = new LinkedList<>();

        predicates.add(cb.equal(associatedPolicyConfig.key(), configKey));

        for (String value : configValues) {
            configValuePredicates.add(cb.like(associatedPolicyConfig.value().as(String.class), "%" + value + "%"));
        }

        predicates.add(cb.or(configValuePredicates.toArray(new Predicate[0])));

        query.where(predicates.toArray(new Predicate[0]));

        PolicyStore policyStore = provider.getStoreFactory().getPolicyStore();

        return entityManager.createQuery(query).getResultStream().map((id) -> policyStore.findById(resourceServer, id)).filter(Objects::nonNull);
    }
}

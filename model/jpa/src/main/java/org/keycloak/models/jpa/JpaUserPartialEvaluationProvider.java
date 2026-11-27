/*
 * Copyright 2025 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.models.jpa;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;

import org.keycloak.authorization.fgap.AdminPermissionsSchema;
import org.keycloak.authorization.fgap.evaluation.partial.PartialEvaluationContext;
import org.keycloak.authorization.fgap.evaluation.partial.PartialEvaluationStorageProvider;
import org.keycloak.authorization.jpa.entities.ResourceEntity;
import org.keycloak.common.Profile;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.jpa.entities.UserGroupMembershipEntity;

import static org.keycloak.authorization.fgap.AdminPermissionsSchema.GROUPS_RESOURCE_TYPE;
import static org.keycloak.authorization.fgap.AdminPermissionsSchema.USERS_RESOURCE_TYPE;

/**
 * A {@link PartialEvaluationStorageProvider} that provides support for partial evaluation when querying {@link UserModel}.
 */
public interface JpaUserPartialEvaluationProvider extends PartialEvaluationStorageProvider {

    KeycloakSession getSession();
    EntityManager getEntityManager();

    @Override
    default List<Predicate> getFilters(PartialEvaluationContext context) {
        KeycloakSession session = getSession();

        if (Profile.isFeatureEnabled(Profile.Feature.ADMIN_FINE_GRAINED_AUTHZ)) {
            // support for FGAP v1, remove once v1 is removed
            Set<String> userGroups = (Set<String>) session.getAttribute(UserModel.GROUPS);


            if (userGroups != null) {
                return List.of(getFilterByGroupMembership(session, context, userGroups));
            }

            return List.of();
        }

        return Optional.ofNullable(getAllowedGroupFilters(context)).map(List::of).orElse(List.of());
    }

    @Override
    default List<Predicate> getNegateFilters(PartialEvaluationContext context) {
        return Optional.ofNullable(getDeniedGroupsFilters(context)).map(List::of).orElse(List.of());
    }

    private Predicate getAllowedGroupFilters(PartialEvaluationContext context) {
        Set<String> allowedGroups = context.getAllowedGroups();

        if (allowedGroups.isEmpty()) {
            return null;
        }

        if (context.deniedResources().contains(USERS_RESOURCE_TYPE)) {
            return null;
        }

        if (context.isResourceTypeAllowed()) {
            return null;
        }

        return hasGrantingMembership(context);
    }

    private Predicate hasGrantingMembership(PartialEvaluationContext context) {
        CriteriaBuilder cb = context.getCriteriaBuilder();
        Set<String> allowed = context.getAllowedGroups();
        Set<String> denied = new HashSet<>(context.getUngrantedGroups());
        denied.addAll(context.getDeniedGroupIds());
        denied.remove(GROUPS_RESOURCE_TYPE);
        Set<String> excluded = expandGroupsToDescendants(denied);
        excluded.removeAll(allowed);

        if (allowed.contains(GROUPS_RESOURCE_TYPE)) {
            if (excluded.isEmpty()) {
                return cb.exists(createUserMembershipSubquery(context));
            }
            return cb.exists(createUserMembershipSubquery(context, root -> cb.not(context.inPredicate(root.get("groupId"), excluded))));
        }

        Set<String> eligible = expandGroupsToDescendants(allowed);
        eligible.removeAll(excluded);
        return cb.exists(createUserMembershipSubquery(context, root -> context.inPredicate(root.get("groupId"), eligible)));
    }

    private Predicate getDeniedGroupsFilters(PartialEvaluationContext context) {
        Set<String> denied = context.getDeniedGroups();
        if (denied.isEmpty()) {
            return null;
        }

        CriteriaBuilder cb = context.getCriteriaBuilder();
        Set<String> allowed = context.getAllowedGroups();
        Set<String> blocked = expandGroupsToDescendants(context.getDeniedGroupIds());
        // Only a successful direct grant carves a child out of an explicitly denied subtree.
        blocked.removeAll(allowed);
        List<Predicate> restrictions = new ArrayList<>();
        if (!blocked.isEmpty()) {
            restrictions.add(cb.not(cb.exists(createUserMembershipSubquery(context, root -> context.inPredicate(root.get("groupId"), blocked)))));
        }
        if (denied.contains(GROUPS_RESOURCE_TYPE) && context.isResourceTypeAllowed()) {
            List<Predicate> exceptions = new ArrayList<>();
            exceptions.add(cb.not(cb.exists(createUserMembershipSubquery(context))));
            if (!allowed.isEmpty()) {
                exceptions.add(hasGrantingMembership(context));
            }
            if (!context.getAllowedResourceIds().isEmpty()) {
                exceptions.add(context.inPredicate(context.getPath().get("id"), context.getAllowedResourceIds()));
            }
            restrictions.add(cb.or(exceptions.toArray(Predicate[]::new)));
        }
        return restrictions.isEmpty() ? null : cb.and(restrictions.toArray(Predicate[]::new));
    }

    private Subquery<?> createUserMembershipSubquery(PartialEvaluationContext context) {
        return createUserMembershipSubquery(context, null);
    }

    private Subquery<?> createUserMembershipSubquery(PartialEvaluationContext context, Function<Root<?>, Predicate> predicate) {
        EntityManager em = getEntityManager();
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<?> query = context.criteriaQuery();
        Subquery<Integer> subquery = query.subquery(Integer.class);
        Root<?> from = subquery.from(UserGroupMembershipEntity.class);

        subquery.select(cb.literal(1));

        Path<?> root = context.getPath();
        List<Predicate> finalPredicates = new ArrayList<>();

        if (predicate != null) {
            finalPredicates.add(predicate.apply(from));
        }

        finalPredicates.add(cb.equal(from.get("user").get("id"), root.get("id")));

        subquery.where(finalPredicates.toArray(Predicate[]::new));

        return subquery;
    }

//    One query per level of group nesting (N + 1 queries). For typical Keycloak deployments (2-4 levels deep) this is fine, but worth noting.
//    A recursive CTE would be a single query, though it isn't used elsewhere and portability across DB vendors might be a concern. The iterative
//    approach is pragmatic here.
    private Set<String> expandGroupsToDescendants(Set<String> groupIds) {
        if (groupIds.isEmpty()) {
            return new HashSet<>();
        }

        EntityManager em = getEntityManager();
        String realmId = getSession().getContext().getRealm().getId();
        Set<String> expanded = new HashSet<>(groupIds);
        Set<String> currentLevel = new HashSet<>(groupIds);

        while (!currentLevel.isEmpty()) {
            List<String> children = em.createNamedQuery("getChildGroupIdsByParentIds", String.class)
                    .setParameter("realm", realmId)
                    .setParameter("parentIds", currentLevel)
                    .getResultList();

            currentLevel = new HashSet<>();
            for (String childId : children) {
                if (expanded.add(childId)) {
                    currentLevel.add(childId);
                }
            }
        }

        return expanded;
    }

    /**
     * @deprecated remove once FGAP v1 is removed
     */
    @Deprecated
    private Predicate getFilterByGroupMembership(KeycloakSession session, PartialEvaluationContext context, Set<String> groupIds) {
        EntityManager em = getEntityManager();
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<?> query = context.criteriaQuery();
        Subquery<Integer> subquery = query.subquery(Integer.class);
        Root<?> from = subquery.from(UserGroupMembershipEntity.class);

        subquery.select(cb.literal(1));

        List<Predicate> subPredicates = new ArrayList<>();

        subPredicates.add(context.inPredicate(from.get("groupId"), groupIds));

        Path<?> root = context.getPath();

        subPredicates.add(cb.equal(from.get("user").get("id"), root.get("id")));

        Subquery<Integer> subquery1 = query.subquery(Integer.class);

        subquery1.select(cb.literal(1));

        Root<ResourceEntity> from1 = subquery1.from(ResourceEntity.class);

        List<Predicate> subs = new ArrayList<>();

        Expression<String> groupId = from.get("groupId");

        RealmModel realm = session.getContext().getRealm();

        if (AdminPermissionsSchema.SCHEMA.isAdminPermissionsEnabled(realm)) {
            subs.add(cb.like(from1.get("name"), groupId));
        } else {
            subs.add(cb.like(from1.get("name"), cb.concat("group.resource.", groupId)));
        }

        subquery1.where(subs.toArray(Predicate[]::new));

        subPredicates.add(cb.exists(subquery1));

        subquery.where(subPredicates.toArray(Predicate[]::new));

        return cb.exists(subquery);
    }
}

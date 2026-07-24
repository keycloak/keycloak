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

        EntityManager em = getEntityManager();
        CriteriaBuilder cb = em.getCriteriaBuilder();

        if (allowedGroups.contains(GROUPS_RESOURCE_TYPE)) {
            return cb.exists(createUserMembershipSubquery(context));
        }

        return cb.exists(createUserMembershipSubquery(context, root -> root.get("groupId").in(allowedGroups)));
    }

    private Predicate getDeniedGroupsFilters(PartialEvaluationContext context) {
        CriteriaBuilder cb = context.getCriteriaBuilder();
        Set<String> allowedGroups = context.getAllowedGroups();
        Set<String> deniedGroups = context.getDeniedGroups();

        if (deniedGroups.contains(GROUPS_RESOURCE_TYPE)) {
            // no access granted to group resources
            Predicate notMembers = cb.not(cb.exists(createUserMembershipSubquery(context)));

            // access denied for the group resource type
            if (context.isResourceTypeAllowed()) {
                // access granted to all resources
                if (allowedGroups.isEmpty()) {
                    if (context.getDeniedGroupIds().isEmpty()) {
                        // filter group members but allow
                        return cb.and(cb.or(notMembers, context.getPath().get("id").in(context.getAllowedResourceIds())));
                    }

                    return notMembers;
                }

                Predicate onlySpecificGroups = cb.exists(createUserMembershipSubquery(context, root -> root.get("groupId").in(allowedGroups)));
                return cb.and(cb.or(notMembers, onlySpecificGroups));
            }

            return cb.not(cb.exists(createUserMembershipSubquery(context, root -> root.get("groupId").in(context.getDeniedGroupIds()))));
        }

        if (context.getAllowedResources().isEmpty() && (allowedGroups.isEmpty() || context.deniedResources().contains(USERS_RESOURCE_TYPE))) {
            return null;
        }

        if (deniedGroups.isEmpty()) {
            return null;
        }

        return cb.not(cb.exists(createUserMembershipSubquery(context, root -> root.get("groupId").in(deniedGroups))));
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

        subPredicates.add(from.get("groupId").in(groupIds));

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

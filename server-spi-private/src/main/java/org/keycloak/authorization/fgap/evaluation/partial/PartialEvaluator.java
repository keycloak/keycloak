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

package org.keycloak.authorization.fgap.evaluation.partial;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;

import org.keycloak.Config;
import org.keycloak.authorization.fgap.AdminPermissionsSchema;
import org.keycloak.authorization.model.Policy;
import org.keycloak.authorization.policy.provider.PolicyProvider;
import org.keycloak.common.Profile;
import org.keycloak.models.AdminRoles;
import org.keycloak.models.ClientModel;
import org.keycloak.models.Constants;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserModel;
import org.keycloak.representations.idm.authorization.Logic;
import org.keycloak.representations.idm.authorization.ResourceType;

import static org.keycloak.authorization.fgap.AdminPermissionsSchema.isSkipEvaluation;

public final class PartialEvaluator {

    private static final String NO_ID = "none";
    private static final String ID_FIELD = "id";
    private static final String PARTIAL_EVALUATION_CONTEXT_CACHE = "kc.authz.fgap.partial.evaluation.cache";

    public List<Predicate> getPredicates(KeycloakSession session, ResourceType resourceType, PartialEvaluationStorageProvider storage, RealmModel realm, CriteriaBuilder builder, CriteriaQuery<?> queryBuilder, Path<?> path) {
        if (Profile.isFeatureEnabled(Profile.Feature.ADMIN_FINE_GRAINED_AUTHZ)) {
            // feature not enabled, if a storage evaluator is provided try to resolve any filter from there
            return storage == null ? List.of() : storage.getFilters(new PartialEvaluationContext(storage, builder, queryBuilder, path));
        }

        UserModel adminUser = session.getContext().getUser();

        if (shouldSkipPartialEvaluation(session, adminUser, resourceType)) {
            // only run partial evaluation if the admin user does not have view-* or manage-* role for specified resourceType or has any query-* role
            return List.of();
        }

        // collect the result from the partial evaluation so that the filters can be applied
        PartialEvaluationContext context = runEvaluation(session, adminUser, resourceType, storage, builder, queryBuilder, path);

        return buildPredicates(context);
    }

    private PartialEvaluationContext runEvaluation(KeycloakSession session, UserModel adminUser, ResourceType resourceType, PartialEvaluationStorageProvider storage, CriteriaBuilder builder, CriteriaQuery<?> queryBuilder, Path<?> path) {
        Map<String, Map<String, PartialEvaluationContext>> cache = session.getAttributeOrDefault(PARTIAL_EVALUATION_CONTEXT_CACHE, Map.of());

        if (cache.getOrDefault(adminUser.getId(), Map.of()).containsKey(resourceType.getType())) {
            PartialEvaluationContext evaluationContext = cache.get(adminUser.getId()).get(resourceType.getType());
            evaluationContext.setStorage(storage);
            evaluationContext.setCriteriaBuilder(builder);
            evaluationContext.setCriteriaQuery(queryBuilder);
            evaluationContext.setPath(path);
            return evaluationContext;
        }

        Set<String> allowedResources = new HashSet<>();
        Set<String> deniedResources = new HashSet<>();
        List<PartialEvaluationPolicyProvider> policyProviders = getPartialEvaluationPolicyProviders(session);

        for (PartialEvaluationPolicyProvider policyProvider : policyProviders) {
            policyProvider.getPermissions(session, resourceType, adminUser).forEach(permission -> {
                Set<String> ids = permission.getResourceNames();
                Set<Policy> policies = permission.getAssociatedPolicies();

                for (Policy policy : policies) {
                    PartialEvaluationPolicyProvider provider = getPartialEvaluationPolicyProvider(policy, policyProviders);

                    if (provider == null) {
                        continue;
                    }

                    boolean granted = provider.evaluate(session, policy, adminUser);

                    if (Logic.NEGATIVE.equals(policy.getLogic())) {
                        granted = !granted;
                    }

                    if (granted) {
                        allowedResources.addAll(ids);
                    } else {
                        deniedResources.addAll(ids);
                    }
                }
            });
        }

        allowedResources.removeAll(deniedResources);

        PartialEvaluationContext context = createEvaluationContext(session, resourceType, allowedResources, deniedResources, storage, builder, queryBuilder, path, adminUser);

        if (cache.isEmpty()) {
            cache = new HashMap<>();
        }

        cache.computeIfAbsent(adminUser.getId(), s -> new HashMap<>()).computeIfAbsent(resourceType.getType(), s -> context);

        if (session.getAttribute(PARTIAL_EVALUATION_CONTEXT_CACHE) == null) {
            session.setAttribute(PARTIAL_EVALUATION_CONTEXT_CACHE, cache);
        }

        return cache.get(adminUser.getId()).get(resourceType.getType());
    }

    private List<Predicate> buildPredicates(PartialEvaluationContext context) {
        List<Predicate> storageFilters = getStorageFilters(context);
        CriteriaBuilder builder = context.getCriteriaBuilder();
        Path<?> path = context.getPath();

        if (isDenied(storageFilters, context)) {
            // do not return any result because there is no filter from the evaluator, and access is denied for the resource type
            return List.of(builder.equal(path.get(ID_FIELD), NO_ID));
        }

        Set<String> deniedIds = context.getDeniedResources();
        ResourceType resourceType = context.getResourceType();

        if (deniedIds.contains(resourceType.getType())) {
            // do not filter by id if access is granted to the resource type
            deniedIds = Set.of();
        }

        List<Predicate> predicates = new ArrayList<>();

        if (!deniedIds.isEmpty()) {
            // add filters to remove denied resources from the result set
            predicates.add(builder.not(path.get(ID_FIELD).in(deniedIds)));
        }

        List<Predicate> storageNegateFilters = getStorageNegateFilters(context);

        // add filters from the storage that deny access to resources
        predicates.addAll(storageNegateFilters);

        Set<String> allowedResourceIds = context.getAllowedResources();

        if (allowedResourceIds.contains(resourceType.getType())) {
            // do not filter by id if access is granted to the resource type
            allowedResourceIds = Set.of();
        }

        if (allowedResourceIds.isEmpty()) {
            // no resources granted, return any predicates created until now
            predicates.addAll(storageFilters);
            return predicates;
        }

        if (storageFilters.isEmpty()) {
            // no filter from the evaluator, filter based on the resources that were granted
            predicates.add(builder.and(path.get(ID_FIELD).in(allowedResourceIds)));
        } else {
            // there are filters from the evaluator, the resources granted will be a returned using a or condition
            List<Predicate> orPredicates = new ArrayList<>(storageFilters);
            orPredicates.add(path.get(ID_FIELD).in(allowedResourceIds));
            predicates.add(builder.or(orPredicates.toArray(new Predicate[0])));
        }

        return predicates;
    }

    private PartialEvaluationContext createEvaluationContext(KeycloakSession session, ResourceType resourceType, Set<String> allowedResources, Set<String> deniedResources, PartialEvaluationStorageProvider storage, CriteriaBuilder builder, CriteriaQuery<?> queryBuilder, Path<?> path, UserModel adminUser) {
        PartialEvaluationContext context = new PartialEvaluationContext(resourceType, allowedResources, deniedResources, storage, builder, queryBuilder, path);
        String groupType = resourceType.getGroupType();

        if (groupType != null) {
            // if the resource type has support for groups, evaluate permissions for the group
            ResourceType groupResourceType = AdminPermissionsSchema.SCHEMA.getResourceTypes().get(groupType);

            if (groupResourceType == null) {
                return context;
            }

            PartialEvaluationContext evaluateGroups = runEvaluation(session, adminUser, groupResourceType, storage, builder, queryBuilder, path);
            context.setAllowedGroups(evaluateGroups.getAllowedResources());
            context.setDeniedGroups(evaluateGroups.getDeniedResources());
        }

        return context;
    }

    private List<Predicate> getStorageFilters(PartialEvaluationContext context) {
        PartialEvaluationStorageProvider storage = context.getStorage();
        return storage == null ? List.of() : storage.getFilters(context);
    }

    private List<Predicate> getStorageNegateFilters(PartialEvaluationContext context) {
        PartialEvaluationStorageProvider storage = context.getStorage();
        return storage == null ? List.of() : storage.getNegateFilters(context);
    }

    private boolean isDenied(List<Predicate> storageFilters, PartialEvaluationContext context) {
        return context.getAllowedResources().isEmpty() && storageFilters.isEmpty();
    }

    private List<PartialEvaluationPolicyProvider> getPartialEvaluationPolicyProviders(KeycloakSession session) {
        return session.getAllProviders(PolicyProvider.class).stream()
                .filter(provider -> provider instanceof PartialEvaluationPolicyProvider)
                .map(PartialEvaluationPolicyProvider.class::cast)
                .toList();
    }

    private PartialEvaluationPolicyProvider getPartialEvaluationPolicyProvider(Policy policy, List<PartialEvaluationPolicyProvider> policyProviders) {
        return policyProviders.stream()
                .filter((p) -> p.supports(policy))
                .findAny()
                .orElse(null);
    }

    private boolean shouldSkipPartialEvaluation(KeycloakSession session, UserModel user, ResourceType resourceType) {
        if (user == null) {
            return true;
        }

        if (isSkipEvaluation(session)) {
            return true;
        }

        ClientModel client = getRealmManagementClient(session);

        if (client == null) {
            return true;
        }

        if (resourceType.equals(AdminPermissionsSchema.USERS) || resourceType.equals(AdminPermissionsSchema.GROUPS)) {
            return user.hasRole(client.getRole(AdminRoles.VIEW_USERS)) || user.hasRole(client.getRole(AdminRoles.MANAGE_USERS)) || !hasAnyQueryAdminRole(client, user);
        } else if (resourceType.equals(AdminPermissionsSchema.CLIENTS)) {
            return user.hasRole(client.getRole(AdminRoles.VIEW_CLIENTS)) || user.hasRole(client.getRole(AdminRoles.MANAGE_CLIENTS)) || !hasAnyQueryAdminRole(client, user);
        }

        return false;
    }

    private ClientModel getRealmManagementClient(KeycloakSession session) {
        RealmModel realm = session.getContext().getRealm();

        if (realm == null) {
            return null;
        }

        if (realm.getName().equals(Config.getAdminRealm())) {
            return session.clients().getClientByClientId(realm, realm.getMasterAdminClient().getClientId());
        }

        return realm.getClientByClientId(Constants.REALM_MANAGEMENT_CLIENT_ID);
    }

    private boolean hasAnyQueryAdminRole(ClientModel client, UserModel user) {
        boolean result = false;
        for (String adminRole : List.of(AdminRoles.QUERY_CLIENTS, AdminRoles.QUERY_GROUPS, AdminRoles.QUERY_USERS)) {
            RoleModel role = client.getRole(adminRole);

            if (role == null) {
                continue;
            }

            if (user.hasRole(role)) {
                result = true;
                break;
            }
        }

        return result;
    }
}

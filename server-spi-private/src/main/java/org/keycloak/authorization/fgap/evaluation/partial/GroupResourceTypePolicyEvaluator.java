/*
 * Copyright 2026 Red Hat, Inc. and/or its affiliates
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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.keycloak.authorization.AuthorizationProvider;
import org.keycloak.authorization.fgap.AdminPermissionsSchema;
import org.keycloak.authorization.model.Policy;
import org.keycloak.authorization.model.Resource;
import org.keycloak.authorization.model.ResourceServer;
import org.keycloak.authorization.model.Scope;
import org.keycloak.authorization.permission.ResourcePermission;
import org.keycloak.authorization.store.PolicyStore;
import org.keycloak.authorization.store.ResourceStore;
import org.keycloak.authorization.store.StoreFactory;
import org.keycloak.models.GroupModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;

import static org.keycloak.authorization.fgap.AdminPermissionsSchema.GROUPS_RESOURCE_TYPE;

/**
 * Evaluates the permissions of the ancestors of a group that are marked with {@link AdminPermissionsSchema#EXTEND_CHILDREN},
 * so that a permission granted or denied on a group also applies to its descendants.
 *
 * <p>For each scope, permissions associated with the group itself take precedence over those inherited from its
 * ancestors. For the group scopes (e.g. {@code view}), the closest ancestor with an extended permission decides. For
 * the member scopes (e.g. {@code view-members}), every extended ancestor is evaluated, so that a deny from any of them
 * wins, consistently with {@link UserResourceTypePolicyEvaluator}. Once a member scope is inherited from an extended
 * ancestor, the non-extended permissions of the ancestors for that scope are also evaluated, as they already are when
 * evaluating the members of the group.
 *
 * <p>An inherited permission is only evaluated for the scopes it is allowed to decide, see {@link ScopeRestrictedPolicy}.
 */
public final class GroupResourceTypePolicyEvaluator implements ResourceTypePolicyEvaluator {

    private static final Set<String> MEMBER_SCOPES = AdminPermissionsSchema.USERS.getScopeAliases().values().stream()
            .flatMap(Set::stream)
            .collect(Collectors.toUnmodifiableSet());

    @Override
    public void evaluate(ResourcePermission permission, AuthorizationProvider authorization, Consumer<Policy> policyConsumer) {
        ResourceServer resourceServer = permission.getResourceServer();

        if (!AdminPermissionsSchema.SCHEMA.hasExtendChildrenPermissions(authorization.getKeycloakSession(), resourceServer)) {
            return;
        }

        GroupModel group = resolveGroup(permission, authorization);

        if (group == null) {
            return;
        }

        PolicyStore policyStore = authorization.getStoreFactory().getPolicyStore();

        // scopes decided by a more specific group, starting with the group itself, whose own permissions were
        // already evaluated by the default evaluator
        Set<Scope> claimedScopes = new HashSet<>();
        policyStore.findByResource(resourceServer, permission.getResource(), policy -> claimedScopes.addAll(policy.getScopes()));

        // scopes decided by the permissions of the group itself
        Set<Scope> ownScopes = new HashSet<>(claimedScopes);
        // member scopes inherited from at least one extended ancestor
        Set<Scope> inheritedMemberScopes = new HashSet<>();
        // non-extended permissions of the ancestors
        List<Policy> nonExtendedPolicies = new ArrayList<>();
        Set<String> visited = new HashSet<>();
        visited.add(group.getId());
        GroupModel ancestor = group.getParent();

        while (ancestor != null && visited.add(ancestor.getId())) {
            Set<Scope> claimedByAncestor = new HashSet<>();

            evaluateAncestorPolicies(authorization, resourceServer, ancestor, policy -> {
                if (!AdminPermissionsSchema.SCHEMA.isExtendChildren(policy)) {
                    nonExtendedPolicies.add(policy);
                    return;
                }

                Set<Scope> unclaimed = new HashSet<>(policy.getScopes());
                unclaimed.removeAll(claimedScopes);

                if (unclaimed.isEmpty()) {
                    return;
                }

                policyConsumer.accept(unclaimed.size() == policy.getScopes().size() ? policy : new ScopeRestrictedPolicy(policy, unclaimed));

                for (Scope scope : unclaimed) {
                    if (isMemberScope(scope)) {
                        inheritedMemberScopes.add(scope);
                    } else {
                        claimedByAncestor.add(scope);
                    }
                }
            });

            // claimed after the whole level is evaluated, so that all the permissions of the same group are evaluated together
            claimedScopes.addAll(claimedByAncestor);
            ancestor = ancestor.getParent();
        }

        inheritedMemberScopes.removeAll(ownScopes);

        if (inheritedMemberScopes.isEmpty()) {
            return;
        }

        // the member scopes of any ancestor permission, extended or not, already protect the members of the group when
        // evaluating the users (see UserResourceTypePolicyEvaluator). They are also evaluated here for the member scopes
        // inherited from an extended ancestor, so that a deny from any ancestor wins in both cases. They are not evaluated
        // otherwise, so that a permission is only inherited by a group if it is explicitly extended.
        for (Policy policy : nonExtendedPolicies) {
            Set<Scope> scopes = new HashSet<>(policy.getScopes());
            scopes.retainAll(inheritedMemberScopes);

            if (!scopes.isEmpty()) {
                policyConsumer.accept(new ScopeRestrictedPolicy(policy, scopes));
            }
        }
    }

    private boolean isMemberScope(Scope scope) {
        return MEMBER_SCOPES.contains(scope.getName());
    }
    private GroupModel resolveGroup(ResourcePermission permission, AuthorizationProvider authorization) {
        Resource resource = permission.getResource();

        if (resource == null || !GROUPS_RESOURCE_TYPE.equals(permission.getResourceType())) {
            return null;
        }

        String groupId = resource.getName();

        if (GROUPS_RESOURCE_TYPE.equals(groupId)) {
            // permission for all groups, there is no hierarchy to walk
            return null;
        }

        KeycloakSession session = authorization.getKeycloakSession();
        RealmModel realm = authorization.getRealm();

        return session.groups().getGroupById(realm, groupId);
    }

    private void evaluateAncestorPolicies(AuthorizationProvider authorization, ResourceServer resourceServer, GroupModel group, Consumer<Policy> consumer) {
        StoreFactory storeFactory = authorization.getStoreFactory();
        ResourceStore resourceStore = storeFactory.getResourceStore();
        Resource resource = resourceStore.findByName(resourceServer, group.getId());

        if (resource == null) {
            return;
        }

        storeFactory.getPolicyStore().findByResource(resourceServer, resource, consumer);
    }
}

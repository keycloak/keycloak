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

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import org.keycloak.authorization.AuthorizationProvider;
import org.keycloak.authorization.fgap.AdminPermissionsSchema;
import org.keycloak.authorization.model.Policy;
import org.keycloak.authorization.model.ResourceServer;
import org.keycloak.authorization.model.Scope;
import org.keycloak.models.ClientModel;
import org.keycloak.models.GroupModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;

import static org.keycloak.authorization.fgap.AdminPermissionsSchema.GROUPS_RESOURCE_TYPE;

/**
 * Resolves, for partial evaluation, the descendants of groups that are allowed or denied by permissions marked with
 * {@link AdminPermissionsSchema#EXTEND_CHILDREN}, following the same rules as {@link GroupResourceTypePolicyEvaluator}
 * and {@link UserResourceTypePolicyEvaluator}:
 *
 * <ul>
 *     <li>a group with any permission of its own for the scope is decided by those permissions only;</li>
 *     <li>for the group scopes (e.g. {@code view}), the closest ancestor with an extended permission decides;</li>
 *     <li>for the member scopes (e.g. {@code view-members}), a deny from any extended ancestor wins over an allow, and
 *     a grant is not inherited by the descendants of a group whose own permission denies, extended or not, nor
 *     propagated at all if the extended group or any of its ancestors has a permission that denies.</li>
 * </ul>
 */
final class ExtendChildrenPartialEvaluator {

    record Result(Set<String> allowed, Set<String> denied) {
        static final Result EMPTY = new Result(Set.of(), Set.of());
    }

    /**
     * @param session the session
     * @param scope the scope being evaluated
     * @param memberScope {@code true} if the scope protects the members of a group rather than the group itself
     * @param outcome the outcome of a permission for the current admin: {@code true} if it grants access, {@code false}
     *                if it denies access, or {@code null} if it can not be decided during partial evaluation (e.g.: its
     *                policies can not be partially evaluated), in which case it neither grants nor denies access to the
     *                descendants, as for permissions that are not extended
     */
    Result evaluate(KeycloakSession session, String scope, boolean memberScope, Function<Policy, Boolean> outcome) {
        RealmModel realm = session.getContext().getRealm();

        if (realm == null) {
            return Result.EMPTY;
        }

        ClientModel client = realm.getAdminPermissionsClient();

        if (client == null) {
            return Result.EMPTY;
        }

        AuthorizationProvider authorization = session.getProvider(AuthorizationProvider.class);
        ResourceServer resourceServer = authorization.getStoreFactory().getResourceServerStore().findByClient(client);

        if (resourceServer == null || !AdminPermissionsSchema.SCHEMA.hasExtendChildrenPermissions(session, resourceServer)) {
            return Result.EMPTY;
        }

        // groups with a permission of their own for the scope, regardless of the admin the permission targets
        Set<String> claimedGroups = new HashSet<>();
        // groups with a permission of their own for the scope that denies access to the admin
        Set<String> deniedGroups = new HashSet<>();
        // outcome of the extended permissions for the scope, by group
        Map<String, List<Boolean>> extendedByGroup = new HashMap<>();
        Set<String> visitedPermissions = new HashSet<>();

        authorization.getStoreFactory().getPolicyStore().findByResourceType(resourceServer, GROUPS_RESOURCE_TYPE, permission -> {
            if (!visitedPermissions.add(permission.getId()) || !hasScope(permission, scope)) {
                return;
            }

            boolean extended = AdminPermissionsSchema.SCHEMA.isExtendChildren(permission);
            Boolean grants = outcome.apply(permission);

            for (String groupId : permission.getResourceNames()) {
                if (GROUPS_RESOURCE_TYPE.equals(groupId)) {
                    continue;
                }

                claimedGroups.add(groupId);

                if (Boolean.FALSE.equals(grants)) {
                    deniedGroups.add(groupId);
                }

                if (extended) {
                    extendedByGroup.computeIfAbsent(groupId, k -> new ArrayList<>()).add(grants);
                }
            }
        });

        if (extendedByGroup.isEmpty()) {
            return Result.EMPTY;
        }

        Set<String> allowed = new HashSet<>();
        Set<String> denied = new HashSet<>();

        for (Map.Entry<String, List<Boolean>> entry : extendedByGroup.entrySet()) {
            List<Boolean> outcomes = entry.getValue();

            if (!outcomes.contains(Boolean.FALSE) && outcomes.contains(null)) {
                // a permission that can not be decided might deny access during direct evaluation, so the descendants
                // are neither allowed nor denied. For group scopes, the walk of other ancestors still stops at this group
                continue;
            }

            GroupModel group = session.groups().getGroupById(realm, entry.getKey());

            if (group == null) {
                continue;
            }

            // all extended permissions on the group must grant, as with any other set of permissions for a resource
            boolean granted = !outcomes.contains(Boolean.FALSE);

            if (memberScope && granted && isDeniedOrHasDeniedAncestor(group, deniedGroups)) {
                // for member scopes, a deny from the group itself or from any of its ancestors, extended or not, also
                // protects the members of the descendants, so the grant is not propagated
                continue;
            }

            // for member scopes, a deny from any ancestor, extended or not, protects the members of the descendants, so
            // the walk stops at denied groups to not grant access to their descendants
            Set<String> stopAt = memberScope ? (granted ? deniedGroups : Set.of()) : extendedByGroup.keySet();

            for (String descendant : getInheritingDescendants(group, claimedGroups, stopAt)) {
                if (granted) {
                    allowed.add(descendant);
                } else {
                    denied.add(descendant);
                }
            }
        }

        // a deny from any ancestor wins over an allow from another one (only possible for member scopes)
        allowed.removeAll(denied);

        return new Result(allowed, denied);
    }

    private boolean isDeniedOrHasDeniedAncestor(GroupModel group, Set<String> deniedGroups) {
        Set<String> visited = new HashSet<>();

        for (GroupModel current = group; current != null && visited.add(current.getId()); current = current.getParent()) {
            if (deniedGroups.contains(current.getId())) {
                return true;
            }
        }

        return false;
    }

    /**
     * Returns the descendants of {@code group} that inherit its extended permissions. Descendants in {@code claimedGroups}
     * are skipped because their own permissions decide, and the walk stops at descendants in {@code stopAt}, whose
     * own extended permissions take over for their subtree.
     */
    private Set<String> getInheritingDescendants(GroupModel group, Set<String> claimedGroups, Set<String> stopAt) {
        Set<String> result = new HashSet<>();
        Set<String> visited = new HashSet<>();
        Deque<GroupModel> queue = new ArrayDeque<>();

        visited.add(group.getId());
        group.getSubGroupsStream().forEach(queue::add);

        while (!queue.isEmpty()) {
            GroupModel current = queue.poll();

            if (!visited.add(current.getId())) {
                continue;
            }

            if (stopAt.contains(current.getId())) {
                continue;
            }

            if (!claimedGroups.contains(current.getId())) {
                result.add(current.getId());
            }

            List<GroupModel> children = current.getSubGroupsStream().toList();
            queue.addAll(children);
        }

        return result;
    }

    private boolean hasScope(Policy permission, String scope) {
        for (Scope s : permission.getScopes()) {
            if (s.getName().equals(scope)) {
                return true;
            }
        }
        return false;
    }
}

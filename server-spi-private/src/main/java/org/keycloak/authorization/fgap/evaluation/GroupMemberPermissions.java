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

package org.keycloak.authorization.fgap.evaluation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.keycloak.authorization.fgap.AdminPermissionsSchema;
import org.keycloak.authorization.model.Policy;
import org.keycloak.authorization.policy.evaluation.Evaluation;

import static org.keycloak.authorization.fgap.evaluation.GroupMemberDecision.GRANT;
import static org.keycloak.authorization.fgap.evaluation.GroupMemberDecision.NO_GRANT;

/** Per-request votes for user operations inherited from membership in a group. */
final class GroupMemberPermissions {
    private final List<List<String>> memberships = new ArrayList<>();
    private final Map<String, Map<String, GroupMemberDecision>> scopes = new HashMap<>();
    private final Map<String, GroupMemberDecision> resolved = new HashMap<>();

    void addMembership(List<String> path) {
        memberships.add(path);
    }

    void onDecision(Evaluation evaluation) {
        Policy permission = evaluation.getParentPolicy();
        if (permission == null || permission.equals(evaluation.getPolicy())
                || !AdminPermissionsSchema.USERS_RESOURCE_TYPE.equals(evaluation.getPermission().getResourceType())
                || !AdminPermissionsSchema.GROUPS_RESOURCE_TYPE.equals(permission.getResourceType())) {
            return;
        }
        GroupMemberDecision vote = GroupMemberDecision.of(evaluation.getPermission(), evaluation.getPolicy(), evaluation.getEffect());

        record(permission, vote);
    }

    // Votes are combined with AND: admin permissions only support the UNANIMOUS decision strategy
    // (enforced by AdminPermissionsSchema#throwExceptionIfResourceTypeOrScopesNotProvided).
    void record(Policy permission, GroupMemberDecision vote) {
        for (var scope : permission.getScopes()) {
            var groups = scopes.computeIfAbsent(scope.getName(), ignored -> new HashMap<>());
            permission.getResources().forEach(group -> groups.merge(group.getName(), vote, GroupMemberDecision::and));
        }
    }

    GroupMemberDecision resolve(String scope) {
        return resolved.computeIfAbsent(scope, this::resolveMemberships);
    }

    GroupMemberDecision typeDecision(String scope) {
        return scopes.getOrDefault(scope, Map.of()).get(AdminPermissionsSchema.GROUPS_RESOURCE_TYPE);
    }

    private GroupMemberDecision resolveMemberships(String scope) {
        Map<String, GroupMemberDecision> groups = scopes.getOrDefault(scope, Map.of());
        GroupMemberDecision result = NO_GRANT;
        for (List<String> path : memberships) {
            GroupMemberDecision effective = resolveMembership(groups, path);
            if (effective.blocksOtherGrants()) {
                return effective;
            }
            if (effective == GRANT) {
                result = GRANT;
            }
        }
        return result;
    }

    private GroupMemberDecision resolveMembership(Map<String, GroupMemberDecision> groups, List<String> path) {
        GroupMemberDecision direct = groups.get(path.get(0));
        if (direct == GRANT || direct != null && direct.blocksOtherGrants()) {
            return direct;
        }

        GroupMemberDecision effective = direct;
        for (int i = 1; i < path.size(); i++) {
            GroupMemberDecision ancestor = groups.get(path.get(i));
            if (ancestor != null) {
                effective = effective == null ? ancestor : effective.and(ancestor);
            }
        }
        if (effective != null) {
            return effective;
        }
        // An all-groups denial can be overridden by a more specific grant and is resolved separately.
        return groups.get(AdminPermissionsSchema.GROUPS_RESOURCE_TYPE) == GRANT ? GRANT : NO_GRANT;
    }
}

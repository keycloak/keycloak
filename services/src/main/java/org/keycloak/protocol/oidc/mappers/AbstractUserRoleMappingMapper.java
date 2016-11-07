/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.protocol.oidc.mappers;

import org.keycloak.models.ClientModel;
import org.keycloak.models.GroupModel;
import org.keycloak.models.RoleModel;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Deque;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Base class for mapping of user role mappings to an ID and Access Token claim.
 *
 * @author <a href="mailto:thomas.darimont@gmail.com">Thomas Darimont</a>
 */
abstract class AbstractUserRoleMappingMapper extends AbstractOIDCProtocolMapper implements OIDCAccessTokenMapper, OIDCIDTokenMapper, UserInfoTokenMapper {

    /**
     * Returns the role names extracted from the given {@code roleModels} while recursively traversing "Composite Roles".
     * Note that this method enumerates roles directly from the given role models, not accounting for roles coming
     * from group membership.
     * <p>
     * Optionally prefixes each role name with the given {@code prefix}.
     * </p>
     *
     * @param roleModels
     * @param prefix     the prefix to apply, may be {@literal null}
     * @return
     */
    protected Set<String> flattenRoleModelToRoleNames(Set<RoleModel> roleModels, String prefix) {
        Set<String> roleNames = new LinkedHashSet<>();
        String realPrefix = prefix == null ? "" : prefix.trim();

        addRoleNames(roleModels, realPrefix, roleNames);

        return roleNames;
    }

    /**
     * Returns the realm role names extracted from the given {@code group}
     * and its parent groups while recursively traversing "Composite Roles".
     * <p>
     * Optionally prefixes each role name with the given {@code prefix}.
     * </p>
     *
     * @param roleModels
     * @param prefix     the prefix to apply, may be {@literal null}
     * @return
     */
    protected Set<String> flattenRealmRoleModelToRoleNames(GroupModel group, String prefix) {
        Set<String> roleNames = new LinkedHashSet<>();
        String realPrefix = prefix == null ? "" : prefix.trim();

        while (group != null) {
            addRoleNames(group.getRealmRoleMappings(), realPrefix, roleNames);
            group = group.getParent();
        }

        return roleNames;
    }

    /**
     * Returns the client role names defined for given client roles
     * extracted from the given {@code group} and its parent
     * groups while recursively traversing "Composite Roles".
     * <p>
     * Optionally prefixes each role name with the given {@code prefix}.
     * </p>
     *
     * @param roleModels
     * @param prefix     the prefix to apply, may be {@literal null}
     * @return
     */
    protected Set<String> flattenClientRoleModelToRoleNames(GroupModel group, ClientModel app, String prefix) {
        Set<String> roleNames = new LinkedHashSet<>();
        String realPrefix = prefix == null ? "" : prefix.trim();

        while (group != null) {
            addRoleNames(group.getClientRoleMappings(app), realPrefix, roleNames);
            group = group.getParent();
        }

        return roleNames;
    }

    private void addRoleNames(Collection<RoleModel> roleModels, String prefix, Set<String> targetRoleNames) {
        if (roleModels == null) {
            return;
        }

        Deque<RoleModel> stack = new ArrayDeque<>(roleModels);

        while (!stack.isEmpty()) {

            RoleModel current = stack.pop();

            if (current.isComposite()) {
                for (RoleModel compositeRoleModel : current.getComposites()) {
                    stack.push(compositeRoleModel);
                }
            }

            String roleName = current.getName();
            roleName = prefix + roleName;
            targetRoleNames.add(roleName);
        }
    }
}

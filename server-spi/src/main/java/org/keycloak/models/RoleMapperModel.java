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

package org.keycloak.models;

import java.util.Set;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public interface RoleMapperModel {
    /**
     * Returns set of realm roles that are directly set to this object.
     * @return see description
     */
    Set<RoleModel> getRealmRoleMappings();

    /**
     * Returns set of client roles that are directly set to this object for the given client.
     * @param app Client to get the roles for
     * @return see description
     */
    Set<RoleModel> getClientRoleMappings(ClientModel app);

    /**
     * Returns {@code true} if this object is directly or indirectly assigned the given role, {@code false} otherwise.
     * <p>
     * For example, {@code true} is returned for hasRole(R) if:
     * <ul>
     *  <li>R is directly assigned to this object</li>
     *  <li>R is not assigned to this object but this object belongs to a group G which is assigned the role R</li>
     *  <li>R is not assigned to this object but this object belongs to a group G, and G belongs to group H which is assigned the role R</li>
     * </ul>
     * @param role
     * @return see description
     */
    boolean hasRole(RoleModel role);

    /**
     * Grants the given role to this object.
     * @param role
     */
    void grantRole(RoleModel role);

    /**
     * Returns set of all role (both realm all client) that are directly set to this object.
     * @return
     */
    Set<RoleModel> getRoleMappings();

    /**
     * Removes the given role mapping from this object.
     * @param role Role to remove
     */
    void deleteRoleMapping(RoleModel role);
}

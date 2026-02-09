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
package org.keycloak.storage.federated;

import java.util.stream.Stream;

import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public interface UserRoleMappingsFederatedStorage {

    void grantRole(RealmModel realm, String userId, RoleModel role);

    /**
     * Obtains the roles associated with the federated user identified by {@code userId}.
     *
     * @param realm a reference to the realm.
     * @param userId the user identifier.
     * @return a non-null {@code Stream} of roles.
     */
    Stream<RoleModel> getRoleMappingsStream(RealmModel realm, String userId);

    void deleteRoleMapping(RealmModel realm, String userId, RoleModel role);

   /**
    * Obtains the federated users that are members of the given {@code role} in the specified {@code realm}.
    *
    * @param realm a reference to the realm.
    * @param role a reference to the role whose federated members are being searched.
    * @param firstResult first result to return. Ignored if negative or {@code null}.
    * @param max maximum number of results to return. Ignored if negative or {@code null}.
    * @return a non-null {@code Stream} of federated user ids that are members of the role in the realm.
    */
	Stream<String> getRoleMembersStream(RealmModel realm, RoleModel role, Integer firstResult, Integer max);
    
    /**
     * @deprecated This interface is no longer necessary; collection-based methods were removed from the parent interface
     * and therefore the parent interface can be used directly
     */
    @Deprecated
    interface Streams extends UserRoleMappingsFederatedStorage {
    }
}

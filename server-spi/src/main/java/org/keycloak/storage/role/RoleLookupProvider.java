/*
 * Copyright 2020 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.storage.role;

import java.util.stream.Stream;

import org.keycloak.models.ClientModel;
import org.keycloak.models.OrganizationModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleContainerModel;
import org.keycloak.models.RoleModel;

/**
 * Abstraction interface for lookup of realm, client and organization roles by id, name and description.
 */
public interface RoleLookupProvider {

    /**
     * Exact search for a role by given name.
     * @param realm Realm.
     * @param name String name of the role.
     * @return Model of the role, or {@code null} if no role is found.
     *
     * @deprecated Use {@link #getRole(RoleContainerModel, String)} instead. This method is kept for backward compatibility and will be removed in future versions.
     */
    RoleModel getRealmRole(RealmModel realm, String name);

    /**
     * Exact search for a role by its internal ID within the realm.
     * This lookup is not limited to realm roles and may return realm, client or organization roles.
     * @param realm Realm.
     * @param id Internal ID of the role.
     * @return Model of the role.
     */
    RoleModel getRoleById(RealmModel realm, String id);

    /**
     * Case-insensitive search for roles that contain the given string in their name or description.
     * @param realm Realm.
     * @param search Searched substring of the role's name or description.
     * @param first First result to return. Ignored if negative or {@code null}.
     * @param max Maximum number of results to return. Ignored if negative or {@code null}.
     * @return Stream of the realm roles their name or description contains given search string.
     * Never returns {@code null}.
     *
     * @deprecated Use {@link #searchForRolesStream(RoleContainerModel, String, Integer, Integer)} instead. This method is kept for backward compatibility and will be removed in future versions.
     */
    Stream<RoleModel> searchForRolesStream(RealmModel realm, String search, Integer first, Integer max);

    /**
     * Exact search for a client role by given name.
     * @param client Client.
     * @param name String name of the role.
     * @return Model of the role, or {@code null} if no role is found.
     *
     * @deprecated Use {@link #getRole(RoleContainerModel, String)} instead. This method is kept for backward compatibility and will be removed in future versions.
     */
    RoleModel getClientRole(ClientModel client, String name);

    /**
     * Case-insensitive search for client roles that contain the given string in their name or description.
     * @param client Client.
     * @param search String to search by role's name or description.
     * @param first First result to return. Ignored if negative or {@code null}.
     * @param max Maximum number of results to return. Ignored if negative or {@code null}.
     * @return Stream of the client roles their name or description contains given search string.
     * Never returns {@code null}.
     * @deprecated Use {@link #searchForRolesStream(RoleContainerModel, String, Integer, Integer)} instead. This method is kept for backward compatibility and will be removed in future versions.
     */
    Stream<RoleModel> searchForClientRolesStream(ClientModel client, String search, Integer first, Integer max);

    /**
     * Case-insensitive search for client roles that contain the given string in its name or their client's public identifier (clientId - ({@code client_id} in OIDC or {@code entityID} in SAML)).
     * @param realm Realm.
     * @param ids Stream of ids to include in search. Ignored when {@code null}. Returns empty {@code Stream} when empty.
     * @param search String to search by role's name or client's public identifier.
     * @param first First result to return. Ignored if negative or {@code null}.
     * @param max Maximum number of results to return. Ignored if negative or {@code null}.
     * @return Stream of the client roles where role name or client public identifier contains given search string.
     * Never returns {@code null}.
     */
    Stream<RoleModel> searchForClientRolesStream(RealmModel realm, Stream<String> ids, String search, Integer first, Integer max);

    /**
     * Case-insensitive search for client roles that contain the given string in their name or their client's public identifier (clientId - ({@code client_id} in OIDC or {@code entityID} in SAML)).
     *
     * @param realm       Realm.
     * @param search      String to search by role's name or client's public identifier.
     * @param excludedIds Stream of ids to exclude. Ignored if empty or {@code null}.
     * @param first       First result to return. Ignored if negative or {@code null}.
     * @param max         Maximum number of results to return. Ignored if negative or {@code null}.
     * @return Stream of the client roles where role name or client's public identifier contains given search string.
     * Never returns {@code null}.
     */
    Stream<RoleModel> searchForClientRolesStream(RealmModel realm, String search, Stream<String> excludedIds, Integer first, Integer max);

    /**
     * Returns an organization role by name within the given {@code container}.
     *
     * @param container the container that owns the role.
     * @param name Role name.
     * @return Model of the role, or {@code null} if no role is found.
     */
    RoleModel getRole(RoleContainerModel container, String name);

    /**
     * Returns role by internal ID within the given {@code container}.
     * This lookup only returns organization roles owned by the given {@code container}.
     *
     * @param container the container that owns the role.
     * @param id Internal role ID.
     * @return Model of the role, or {@code null} if no role is found in the container.
     */
    default RoleModel getRoleInContainerById(RoleContainerModel container, String id) {
        RealmModel realm = container.getRealm();
        RoleModel role = getRoleById(realm, id);
        if (role == null) {
            return null;
        }

        boolean typeMatches = switch (role.getType()) {
            case REALM -> container instanceof RealmModel;
            case CLIENT -> container instanceof ClientModel;
            case ORGANIZATION -> container instanceof OrganizationModel;
        };

        if (typeMatches) {
            return role.getContainerId().equals(container.getId()) ? role : null;
        }

        return null;
    }

    /**
     * Searches roles by name or description within the given {@code container}.
     *
     * @param container The container that owns the roles.
     * @param search Case-insensitive substring to search for. Ignored if {@code null}.
     * @param first Index of the first result. Ignored if negative or {@code null}.
     * @param max Maximum number of results. Ignored if negative or {@code null}.
     * @return Stream of matching roles. Never returns {@code null}.
     */
    Stream<RoleModel> searchForRolesStream(RoleContainerModel container, String search, Integer first, Integer max);
}

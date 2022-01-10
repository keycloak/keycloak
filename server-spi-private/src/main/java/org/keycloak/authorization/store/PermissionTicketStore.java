/*
 * Copyright 2017 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.authorization.store;


import java.util.List;
import java.util.Map;

import org.keycloak.authorization.model.PermissionTicket;
import org.keycloak.authorization.model.Resource;
import org.keycloak.authorization.model.ResourceServer;

/**
 * A {@link PermissionTicketStore} is responsible to manage the persistence of {@link org.keycloak.authorization.model.PermissionTicket} instances.
 *
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public interface PermissionTicketStore {

    /**
     * Returns count of {@link PermissionTicket}, filtered by the given attributes.
     *
     * @param attributes permission tickets that do not match the attributes are not included with the count; possible filter options are given by {@link PermissionTicket.FilterOption}
     * @param resourceServerId the resource server id
     * @return an integer indicating the amount of permission tickets
     * @throws IllegalArgumentException when there is an unknown attribute in the {@code attributes} map
     */
    long count(Map<PermissionTicket.FilterOption, String> attributes, String resourceServerId);

    /**
     * Creates a new {@link PermissionTicket} instance.
     *
     * @param permission the policy representation
     * @param resourceServer the resource server to which this policy belongs
     * @return a new instance of {@link PermissionTicket}
     */
    PermissionTicket create(String resourceId, String scopeId, String requester, ResourceServer resourceServer);

    /**
     * Deletes a permission from the underlying persistence mechanism.
     *
     * @param id the id of the policy to delete
     */
    void delete(String id);

    /**
     * Returns a {@link PermissionTicket} with the given <code>id</code>
     *
     * @param id the identifier of the permission
     * @param resourceServerId the resource server id
     * @return a permission with the given identifier.
     */
    PermissionTicket findById(String id, String resourceServerId);

    /**
     * Returns a list of {@link PermissionTicket} associated with a {@link ResourceServer} with the given <code>resourceServerId</code>.
     *
     * @param resourceServerId the identifier of a resource server
     * @return a list of permissions belonging to the given resource server
     */
    List<PermissionTicket> findByResourceServer(String resourceServerId);

    /**
     * Returns a list of {@link PermissionTicket} associated with the given <code>owner</code>.
     *
     * @param owner the identifier of a resource server
     * @return a list of permissions belonging to the given owner
     */
    List<PermissionTicket> findByOwner(String owner, String resourceServerId);

    /**
     * Returns a list of {@link PermissionTicket} associated with a {@link org.keycloak.authorization.core.model.Resource} with the given <code>resourceId</code>.
     *
     * @param resourceId the identifier of a resource
     * @param resourceServerId the resource server id
     * @return a list of permissions associated with the given resource
     */
    List<PermissionTicket> findByResource(String resourceId, String resourceServerId);

    /**
     * Returns a list of {@link PermissionTicket} associated with a {@link org.keycloak.authorization.core.model.Scope} with the given <code>scopeId</code>.
     *
     * @param scopeId the id of the scopes
     * @param resourceServerId the resource server id
     * @return a list of permissions associated with the given scopes
     */
    List<PermissionTicket> findByScope(String scopeId, String resourceServerId);

    /**
     * Returns a list of {@link PermissionTicket}, filtered by the given attributes.
     *
     * @param attributes a map of keys and values to filter on; possible filter options are given by {@link PermissionTicket.FilterOption}
     * @param resourceServerId an id of resource server that resulting tickets should belong to. Ignored if {@code null}
     * @param firstResult first result to return; Ignored if negative or zero
     * @param maxResult maximum number of results to return; Ignored if negative
     * @return a list of filtered and paginated permissions
     *
     * @throws IllegalArgumentException when there is an unknown attribute in the {@code attributes} map
     *
     */
    List<PermissionTicket> find(Map<PermissionTicket.FilterOption, String> attributes, String resourceServerId, int firstResult, int maxResult);

    /**
     * Returns a list of {@link PermissionTicket} granted to the given {@code userId}.
     *
     * @param userId the user id
     * @param resourceServerId the resource server id
     * @return a list of permissions granted for a particular user
     */
    List<PermissionTicket> findGranted(String userId, String resourceServerId);

    /**
     * Returns a list of {@link PermissionTicket} with name equal to {@code resourceName} granted to the given {@code userId}.
     *
     * @param resourceName the name of a resource
     * @param userId the user id
     * @param resourceServerId the resource server id
     * @return a list of permissions granted for a particular user
     */
    List<PermissionTicket> findGranted(String resourceName, String userId, String resourceServerId);

    /**
     * Returns a list of {@link Resource} granted to the given {@code requester}
     *
     * @param requester the requester
     * @param name the keyword to query resources by name or null if any resource
     * @param first first  result
     * @param max max result
     * @return a list of {@link Resource} granted to the given {@code requester}
     */
    List<Resource> findGrantedResources(String requester, String name, int first, int max);

    /**
     * Returns a list of {@link Resource} granted by the owner to other users
     *
     * @param owner the owner
     * @param first first  result
     * @param max max result
     * @return a list of {@link Resource} granted by the owner
     */
    List<Resource> findGrantedOwnerResources(String owner, int first, int max);
}

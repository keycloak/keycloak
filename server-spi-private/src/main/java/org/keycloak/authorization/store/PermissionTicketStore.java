/*
 * Copyright 2022 Red Hat, Inc. and/or its affiliates
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
import org.keycloak.authorization.model.Scope;
import org.keycloak.models.RealmModel;

/**
 * A {@link PermissionTicketStore} is responsible to manage the persistence of {@link org.keycloak.authorization.model.PermissionTicket} instances.
 *
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public interface PermissionTicketStore {

    /**
     * Returns count of {@link PermissionTicket}, filtered by the given attributes.
     *
     *
     * @param resourceServer the resource server. Cannot be {@code null}.
     * @param attributes permission tickets that do not match the attributes are not included with the count; possible filter options are given by {@link PermissionTicket.FilterOption}
     * @return an integer indicating the amount of permission tickets
     * @throws IllegalArgumentException when there is an unknown attribute in the {@code attributes} map
     */
    long count(ResourceServer resourceServer, Map<PermissionTicket.FilterOption, String> attributes);

    /**
     * Creates a new {@link PermissionTicket} instance.
     *
     * @param resourceServer the resource server to which this permission ticket belongs. Cannot be {@code null}.
     * @param resource resource. Cannot be {@code null}.
     * @param scope scope. Cannot be {@code null}
     * @param requester requester of the permission
     * @return a new instance of {@link PermissionTicket}
     */
    PermissionTicket create(ResourceServer resourceServer, Resource resource, Scope scope, String requester);

    /**
     * Deletes a permission from the underlying persistence mechanism.
     *
     * @param realm realm. Cannot be {@code null}.
     * @param id the id of the policy to delete
     */
    void delete(RealmModel realm, String id);

    /**
     * Returns a {@link PermissionTicket} with the given <code>id</code>
     *
     *
     *
     * @param realm the realm. Cannot be {@code null}.
     * @param resourceServer the resource server. Ignored if {@code null}.
     * @param id the identifier of the permission
     * @return a permission with the given identifier.
     */
    PermissionTicket findById(RealmModel realm, ResourceServer resourceServer, String id);

    /**
     * Returns a list of {@link PermissionTicket} associated with the {@link org.keycloak.authorization.model.Resource resource}.
     *
     * @param resourceServer the resource server. Cannot be {@code null}.
     * @param resource the resource. Cannot be {@code null}
     * @return a list of permissions associated with the given resource
     */
    List<PermissionTicket> findByResource(ResourceServer resourceServer, Resource resource);

    /**
     * Returns a list of {@link PermissionTicket} associated with the {@link org.keycloak.authorization.model.Scope scope}.
     *
     *
     * @param resourceServer the resource server. Cannot be {@code null}.
     * @param scope the scope. Cannot be {@code null}.
     * @return a list of permissions associated with the given scopes
     */
    List<PermissionTicket> findByScope(ResourceServer resourceServer, Scope scope);

    /**
     * Returns a list of {@link PermissionTicket}, filtered by the given attributes.
     *
     *
     * @param realm the realm. Cannot be {@code null}.
     * @param resourceServer a resource server that resulting tickets should belong to. Ignored if {@code null}.
     * @param attributes a map of keys and values to filter on; possible filter options are given by {@link PermissionTicket.FilterOption}
     * @param firstResult first result to return. Ignored if negative or {@code null}.
     * @param maxResults maximum number of results to return. Ignored if negative or {@code null}.
     * @return a list of filtered and paginated permissions
     *
     * @throws IllegalArgumentException when there is an unknown attribute in the {@code attributes} map
     *
     */
    List<PermissionTicket> find(RealmModel realm, ResourceServer resourceServer, Map<PermissionTicket.FilterOption, String> attributes, Integer firstResult, Integer maxResults);

    /**
     * Returns a list of {@link PermissionTicket} granted to the given {@code userId}.
     *
     * @param resourceServer the resource server. Cannot be {@code null}
     * @param userId the user id
     * @return a list of permissions granted for a particular user
     */
    List<PermissionTicket> findGranted(ResourceServer resourceServer, String userId);

    /**
     * Returns a list of {@link PermissionTicket} with name equal to {@code resourceName} granted to the given {@code userId}.
     *
     * @param resourceServer the resource server. Cannot be {@code null}.
     * @param resourceName the name of a resource
     * @param userId the user id
     * @return a list of permissions granted for a particular user
     *
     * TODO: investigate a way how to replace resourceName with Resource class
     */
    List<PermissionTicket> findGranted(ResourceServer resourceServer, String resourceName, String userId);

    /**
     * Returns a list of {@link Resource} granted to the given {@code requester}
     *
     *
     * @param realm realm that is searched. Cannot be {@code null}
     * @param requester the requester
     * @param name the keyword to query resources by name or null if any resource
     * @param firstResult first result to return. Ignored if negative or {@code null}.
     * @param maxResults maximum number of results to return. Ignored if negative or {@code null}.
     * @return a list of {@link Resource} granted to the given {@code requester}
     */
    List<Resource> findGrantedResources(RealmModel realm, String requester, String name, Integer firstResult, Integer maxResults);

    /**
     * Returns a list of {@link Resource} granted by the owner to other users
     *
     *
     * @param realm
     * @param owner the owner
     * @param firstResult first result to return. Ignored if negative or {@code null}.
     * @param maxResults maximum number of results to return. Ignored if negative or {@code null}.
     * @return a list of {@link Resource} granted by the owner
     */
    List<Resource> findGrantedOwnerResources(RealmModel realm, String owner, Integer firstResult, Integer maxResults);
}

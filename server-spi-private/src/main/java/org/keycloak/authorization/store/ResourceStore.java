/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2016 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.keycloak.authorization.store;

import org.keycloak.authorization.model.Resource;
import org.keycloak.authorization.model.ResourceServer;
import org.keycloak.authorization.model.Scope;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

/**
 * A {@link ResourceStore} is responsible to manage the persistence of {@link Resource} instances.
 *
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public interface ResourceStore {

    /**
     * <p>Creates a {@link Resource} instance backed by this persistent storage implementation.
     *
     * @param resourceServer the resource server to where the given resource belongs to
     * @param name the name of this resource. It must be unique.
     * @param owner the owner of this resource or null if the resource server is the owner
     * @return an instance backed by the underlying storage implementation
     */
    default Resource create(ResourceServer resourceServer, String name, String owner) {
        return create(resourceServer, null, name, owner);
    }

    /**
     * <p>Creates a {@link Resource} instance backed by this persistent storage implementation.
     *
     * @param resourceServer the resource server to where the given resource belongs to
     * @param id the id of this resource. It must be unique. Will be randomly generated if null.
     * @param name the name of this resource. It must be unique.
     * @param owner the owner of this resource or null if the resource server is the owner
     * @return an instance backed by the underlying storage implementation
     */
    Resource create(ResourceServer resourceServer, String id, String name, String owner);

    /**
     * Removes a {@link Resource} instance, with the given {@code id} from the persistent storage.
     *
     * @param id the identifier of an existing resource instance
     */
    void delete(String id);

    /**
     * Returns a {@link Resource} instance based on its identifier.
     *
     * @param resourceServer the resource server
     * @param id the identifier of an existing resource instance
     * @return the resource instance with the given identifier or null if no instance was found
     */
    Resource findById(ResourceServer resourceServer, String id);

    /**
     * Finds all {@link Resource} instances with the given {@code ownerId}.
     *
     *
     * @param resourceServer
     * @param ownerId the identifier of the owner
     * @return a list with all resource instances owned by the given owner
     */
    default List<Resource> findByOwner(ResourceServer resourceServer, String ownerId) {
        List<Resource> list = new LinkedList<>();

        findByOwner(resourceServer, ownerId, list::add);

        return list;
    }

    void findByOwner(ResourceServer resourceServer, String ownerId, Consumer<Resource> consumer);

    List<Resource> findByOwner(ResourceServer resourceServer, String ownerId, Integer firstResult, Integer maxResults);

    /**
     * Finds all {@link Resource} instances with the given uri.
     *
     *
     * @param resourceServer
     * @param uri the identifier of the uri
     * @return a list with all resource instances owned by the given owner
     */
    List<Resource> findByUri(ResourceServer resourceServer, String uri);

    /**
     * Finds all {@link Resource} instances associated with a given resource server.
     *
     * @param resourceServer the identifier of the resource server
     * @return a list with all resources associated with the given resource server
     */
    List<Resource> findByResourceServer(ResourceServer resourceServer);

    /**
     * Finds all {@link Resource} instances associated with a given resource server.
     *
     * @param resourceServer the identifier of the resource server
     * @param attributes a map holding the attributes that will be used as a filter; possible filter options are given by {@link Resource.FilterOption}
     * @param firstResult first result to return. Ignored if negative or {@code null}.
     * @param maxResults maximum number of results to return. Ignored if negative or {@code null}.
     * @return a list with all resources associated with the given resource server
     *
     * @throws IllegalArgumentException when there is an unknown attribute in the {@code attributes} map
     */
    List<Resource> findByResourceServer(ResourceServer resourceServer, Map<Resource.FilterOption, String[]> attributes, Integer firstResult, Integer maxResults);

    /**
     * Finds all {@link Resource} associated with a given scope.
     *
     *
     * @param resourceServer
     * @param scopes one or more scope identifiers
     * @return a list of resources associated with the given scope(s)
     */
    default List<Resource> findByScopes(ResourceServer resourceServer, Set<Scope> scopes) {
        List<Resource> result = new ArrayList<>();

        findByScopes(resourceServer, scopes, result::add);

        return result;
    }

    void findByScopes(ResourceServer resourceServer, Set<Scope> scopes, Consumer<Resource> consumer);

    /**
     * Find a {@link Resource} by its name where the owner is the resource server itself.
     *
     * @param resourceServer the resource server
     * @param name the name of the resource
     * @return a resource with the given name
     */
    default Resource findByName(ResourceServer resourceServer, String name) {
        return findByName(resourceServer, name, resourceServer.getClientId());
    }

    /**
     * Find a {@link Resource} by its name where the owner is the given <code>ownerId</code>.
     *
     * @param resourceServer the identifier of the resource server
     * @param name the name of the resource
     * @param ownerId the owner id
     * @return a resource with the given name
     */
    Resource findByName(ResourceServer resourceServer, String name, String ownerId);

    /**
     * Finds all {@link Resource} with the given type.
     *
     *
     * @param resourceServer
     * @param type the type of the resource
     * @return a list of resources with the given type
     */
    default List<Resource> findByType(ResourceServer resourceServer, String type) {
        List<Resource> list = new LinkedList<>();

        findByType(resourceServer, type, list::add);

        return list;
    }

    /**
     * Finds all {@link Resource} with the given type.
     *
     *
     * @param resourceServer
     * @param type the type of the resource
     * @param owner the resource owner or null for any resource with a given type
     * @return a list of resources with the given type
     */
    default List<Resource> findByType(ResourceServer resourceServer, String type, String owner) {
        List<Resource> list = new LinkedList<>();

        findByType(resourceServer, type, owner, list::add);

        return list;
    }

    /**
     * Finds all {@link Resource} with the given type.
     *
     * @param resourceServer the resource server id
     * @param type the type of the resource
     * @param consumer the result consumer
     * @return a list of resources with the given type
     */
    void findByType(ResourceServer resourceServer, String type, Consumer<Resource> consumer);

    /**
     * Finds all {@link Resource} with the given type.
     *
     * @param resourceServer the resource server id
     * @param type the type of the resource
     * @param owner the resource owner or null for any resource with a given type
     * @param consumer the result consumer
     * @return a list of resources with the given type
     */
    void findByType(ResourceServer resourceServer, String type, String owner, Consumer<Resource> consumer);

    default List<Resource> findByTypeInstance(ResourceServer resourceServer, String type) {
        List<Resource> list = new LinkedList<>();

        findByTypeInstance(resourceServer, type, list::add);

        return list;
    }

    void findByTypeInstance(ResourceServer resourceServerId, String type, Consumer<Resource> consumer);
}

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

import org.keycloak.authorization.model.ResourceServer;
import org.keycloak.authorization.model.Scope;

/**
 * A {@link ScopeStore} is responsible to manage the persistence of {@link Scope} instances.
 *
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public interface ScopeStore {

    /**
     * Creates a new {@link Scope} instance. The new instance is not necessarily persisted though, which may require
     * a call to the {#save} method to actually make it persistent.
     *
     * @param resourceServer the resource server to which this scope belongs. Cannot be {@code null}.
     * @param name the name of the scope
     * @return a new instance of {@link Scope}
     */
    default Scope create(ResourceServer resourceServer, String name) {
        return create(resourceServer, null, name);
    }

    /**
     * Creates a new {@link Scope} instance. The new instance is not necessarily persisted though, which may require
     * a call to the {#save} method to actually make it persistent.
     *
     * @param resourceServer the resource server to which this scope belongs. Cannot be {@code null}.
     * @param id the id of the scope. Is generated randomly when null
     * @param name the name of the scope
     * @return a new instance of {@link Scope}
     */
    Scope create(ResourceServer resourceServer, String id, String name);

    /**
     * Deletes a scope from the underlying persistence mechanism.
     *
     * @param id the id of the scope to delete
     */
    void delete(String id);

    /**
     * Returns a {@link Scope} with the given <code>id</code>
     *
     * @param resourceServer the resource server id. Ignored if {@code null}.
     * @param id             the identifier of the scope
     * @return a scope with the given identifier.
     */
    Scope findById(ResourceServer resourceServer, String id);

    /**
     * Returns a {@link Scope} with the given <code>name</code>
     *
     * @param resourceServer the resource server. Cannot be {@code null}.
     * @param name the name of the scope
     *
     * @return a scope with the given name.
     */
    Scope findByName(ResourceServer resourceServer, String name);

    /**
     * Returns a list of {@link Scope} associated with the {@link ResourceServer}.
     *
     * @param resourceServer the resource server. Cannot be {@code null}.
     *
     * @return a list of scopes that belong to the given resource server
     */
    List<Scope> findByResourceServer(ResourceServer resourceServer);

    /**
     * Returns a list of {@link Scope} associated with a {@link ResourceServer} with the given <code>resourceServerId</code>.
     *
     * @param resourceServer the resource server. Cannot be {@code null}.
     * @param attributes a map holding the attributes that will be used as a filter; possible filter options are given by {@link Scope.FilterOption}
     * @param firstResult first result to return. Ignored if negative or {@code null}.
     * @param maxResults maximum number of results to return. Ignored if negative or {@code null}.
     * @return a list of scopes that belong to the given resource server
     * 
     * @throws IllegalArgumentException when there is an unknown attribute in the {@code attributes} map
     * 
     */
    List<Scope> findByResourceServer(ResourceServer resourceServer, Map<Scope.FilterOption, String[]> attributes, Integer firstResult, Integer maxResults);
}

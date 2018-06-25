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


import org.keycloak.authorization.model.ResourceServer;
import org.keycloak.authorization.model.Scope;

import java.util.List;
import java.util.Map;

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
     * @param name the name of the scope
     * @param resourceServer the resource server to which this scope belongs
     *
     * @return a new instance of {@link Scope}
     */
    Scope create(String name, ResourceServer resourceServer);

    /**
     * Creates a new {@link Scope} instance. The new instance is not necessarily persisted though, which may require
     * a call to the {#save} method to actually make it persistent.
     *
     * @param id the id of the scope
     * @param name the name of the scope
     * @param resourceServer the resource server to which this scope belongs
     *
     * @return a new instance of {@link Scope}
     */
    Scope create(String id, String name, ResourceServer resourceServer);

    /**
     * Deletes a scope from the underlying persistence mechanism.
     *
     * @param id the id of the scope to delete
     */
    void delete(String id);

    /**
     * Returns a {@link Scope} with the given <code>id</code>
     *
     * @param id the identifier of the scope
     * @param resourceServerId the resource server id
     * @return a scope with the given identifier.
     */
    Scope findById(String id, String resourceServerId);

    /**
     * Returns a {@link Scope} with the given <code>name</code>
     *
     * @param name the name of the scope
     *
     * @param resourceServerId the resource server id
     * @return a scope with the given name.
     */
    Scope findByName(String name, String resourceServerId);

    /**
     * Returns a list of {@link Scope} associated with a {@link ResourceServer} with the given <code>resourceServerId</code>.
     *
     * @param resourceServerId the identifier of a resource server
     *
     * @return a list of scopes that belong to the given resource server
     */
    List<Scope> findByResourceServer(String id);

    /**
     * Returns a list of {@link Scope} associated with a {@link ResourceServer} with the given <code>resourceServerId</code>.
     *
     * @param attributes a map holding the attributes that will be used as a filter
     * @param resourceServerId the identifier of a resource server
     *
     * @return a list of scopes that belong to the given resource server
     */
    List<Scope> findByResourceServer(Map<String, String[]> attributes, String resourceServerId, int firstResult, int maxResult);
}
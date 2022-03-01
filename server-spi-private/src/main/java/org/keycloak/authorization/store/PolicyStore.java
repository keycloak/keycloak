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


import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import org.keycloak.authorization.model.Policy;
import org.keycloak.authorization.model.ResourceServer;
import org.keycloak.representations.idm.authorization.AbstractPolicyRepresentation;

/**
 * A {@link PolicyStore} is responsible to manage the persistence of {@link Policy} instances.
 *
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public interface PolicyStore {

    /**
     * Creates a new {@link Policy} instance. The new instance is not necessarily persisted though, which may require
     * a call to the {#save} method to actually make it persistent.
     *
     * @param resourceServer the resource server to which this policy belongs
     * @param representation the policy representation
     * @return a new instance of {@link Policy}
     */
    Policy create(ResourceServer resourceServer, AbstractPolicyRepresentation representation);

    /**
     * Deletes a policy from the underlying persistence mechanism.
     *
     * @param id the id of the policy to delete
     */
    void delete(String id);

    /**
     * Returns a {@link Policy} with the given <code>id</code>
     *
     * @param resourceServerId the resource server id
     * @param id the identifier of the policy
     * @return a policy with the given identifier.
     */
    Policy findById(String resourceServerId, String id);

    /**
     * Returns a {@link Policy} with the given <code>name</code>
     *
     * @param resourceServerId the resource server id
     * @param name             the name of the policy
     * @return a policy with the given name.
     */
    Policy findByName(String resourceServerId, String name);

    /**
     * Returns a list of {@link Policy} associated with a {@link ResourceServer} with the given <code>resourceServerId</code>.
     *
     * @param resourceServerId the identifier of a resource server
     * @return a list of policies that belong to the given resource server
     */
    List<Policy> findByResourceServer(String resourceServerId);

    /**
     * Returns a list of {@link Policy} associated with a {@link ResourceServer} with the given <code>resourceServerId</code>.
     *
     * @param resourceServerId the identifier of a resource server
     * @param attributes a map holding the attributes that will be used as a filter; possible filter options are given by {@link Policy.FilterOption}
     * @return a list of policies that belong to the given resource server
     *
     * @throws IllegalArgumentException when there is an unknown attribute in the {@code attributes} map
     */
    List<Policy> findByResourceServer(String resourceServerId, Map<Policy.FilterOption, String[]> attributes, int firstResult, int maxResult);

    /**
     * Returns a list of {@link Policy} associated with a {@link org.keycloak.authorization.core.model.Resource} with the given <code>resourceId</code>.
     *
     * @param resourceServerId the resource server id
     * @param resourceId the identifier of a resource
     * @return a list of policies associated with the given resource
     */
    default List<Policy> findByResource(String resourceServerId, String resourceId) {
        List<Policy> result = new LinkedList<>();

        findByResource(resourceServerId, resourceId, result::add);

        return result;
    }

    void findByResource(String resourceServerId, String resourceId, Consumer<Policy> consumer);

    /**
     * Returns a list of {@link Policy} associated with a {@link org.keycloak.authorization.core.model.Resource} with the given <code>type</code>.
     *
     * @param resourceServerId the resource server id
     * @param resourceType     the type of a resource
     * @return a list of policies associated with the given resource type
     */
    default List<Policy> findByResourceType(String resourceServerId, String resourceType) {
        List<Policy> result = new LinkedList<>();

        findByResourceType(resourceServerId, resourceType, result::add);

        return result;
    }

    /**
     * Returns a list of {@link Policy} associated with a {@link org.keycloak.authorization.core.model.Scope} with the given <code>scopeIds</code>.
     *
     * @param resourceServerId the resource server id
     * @param scopeIds the id of the scopes
     * @return a list of policies associated with the given scopes
     */
    List<Policy> findByScopeIds(String resourceServerId, List<String> scopeIds);

    /**
     * Returns a list of {@link Policy} associated with a {@link org.keycloak.authorization.core.model.Scope} with the given <code>resourceId</code> and <code>scopeIds</code>.
     *
     * @param resourceServerId the resource server id
     * @param resourceId the id of the resource. Ignored if {@code null}.
     * @param scopeIds the id of the scopes
     * @return a list of policies associated with the given scopes
     */
    default List<Policy> findByScopeIds(String resourceServerId, String resourceId, List<String> scopeIds) {
        List<Policy> result = new LinkedList<>();

        findByScopeIds(resourceServerId, resourceId, scopeIds, result::add);

        return result;
    }

    /**
     * Effectively the same method as {@link #findByScopeIds(String, String, List)}, however in the end
     * the {@code consumer} is fed with the result.
     *
     */
    void findByScopeIds(String resourceServerId, String resourceId, List<String> scopeIds, Consumer<Policy> consumer);

    /**
     * Returns a list of {@link Policy} with the given <code>type</code>.
     *
     * @param resourceServerId the resource server id
     * @param type the type of the policy
     * @return a list of policies with the given type
     */
    List<Policy> findByType(String resourceServerId, String type);

    /**
     * Returns a list of {@link Policy} that depends on another policy with the given <code>id</code>.
     *
     * @param resourceServerId the resource server id
     * @param id the id of the policy to query its dependents
     * @return a list of policies that depends on the a policy with the given identifier
     */
    List<Policy> findDependentPolicies(String resourceServerId, String id);

    void findByResourceType(String resourceServerId, String type, Consumer<Policy> policyConsumer);
}

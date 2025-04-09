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


import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Stream;

import org.keycloak.authorization.model.Policy;
import org.keycloak.authorization.model.Resource;
import org.keycloak.authorization.model.ResourceServer;
import org.keycloak.authorization.model.Scope;
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
     * @param resourceServer the resource server to which this policy belongs. Cannot be {@code null}.
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
     * @param resourceServer the resource server. Ignored if {@code null}.
     * @param id             the identifier of the policy
     * @return a policy with the given identifier.
     */
    Policy findById(ResourceServer resourceServer, String id);

    /**
     * Returns a {@link Policy} with the given <code>name</code>
     *
     * @param resourceServer the resource server. Cannot be {@code null}
     * @param name the name of the policy
     * @return a policy with the given name or {@code null} if no such policy exists.
     */
    Policy findByName(ResourceServer resourceServer, String name);

    /**
     * Returns a list of {@link Policy} associated with the {@link ResourceServer}
     *
     * @param resourceServer the resource server. Cannot be {@code null}.
     * @return a list of policies that belong to the given resource server
     */
    List<Policy> findByResourceServer(ResourceServer resourceServer);

    /**
     * Returns a list of {@link Policy} associated with a {@link ResourceServer} with the given <code>resourceServerId</code>.
     *
     * @param resourceServer the identifier of a resource server. Ignored if {@code null}.
     * @param attributes     a map holding the attributes that will be used as a filter; possible filter options are given by {@link Policy.FilterOption}
     * @param firstResult    first result to return. Ignored if negative or {@code null}.
     * @param maxResults     maximum number of results to return. Ignored if negative or {@code null}.
     * @return a list of policies that belong to the given resource server
     * @throws IllegalArgumentException when there is an unknown attribute in the {@code attributes} map
     */
    List<Policy> find(ResourceServer resourceServer, Map<Policy.FilterOption, String[]> attributes, Integer firstResult, Integer maxResults);

    /**
     * Returns a list of {@link Policy} associated with a {@link org.keycloak.authorization.model.Resource}
     *
     * @param resourceServer the resource server. Cannot be {@code null}.
     * @param resource the resource. Cannot be {@code null}.
     * @return a list of policies associated with the given resource
     */
    default List<Policy> findByResource(ResourceServer resourceServer, Resource resource) {
        List<Policy> result = new LinkedList<>();

        findByResource(resourceServer, resource, result::add);

        return result;
    }

    /**
     * Searches for all policies associated with the {@link org.keycloak.authorization.model.Resource} and passes the result to the {@code consumer}
     *
     * @param resourceServer the resourceServer. Cannot be {@code null}.
     * @param resource the resource. Cannot be {@code null}.
     * @param consumer consumer of policies resulted from the search
     */
    void findByResource(ResourceServer resourceServer, Resource resource, Consumer<Policy> consumer);

    /**
     * Returns a list of {@link Policy} associated with a {@link org.keycloak.authorization.model.ResourceServer} with the given <code>type</code>.
     *
     * @param resourceServer the resource server id. Cannot be {@code null}.
     * @param resourceType the type of a resource
     * @return a list of policies associated with the given resource type
     */
    default List<Policy> findByResourceType(ResourceServer resourceServer, String resourceType) {
        List<Policy> result = new LinkedList<>();

        findByResourceType(resourceServer, resourceType, result::add);

        return result;
    }

    /**
     * Searches for policies associated with a {@link org.keycloak.authorization.model.ResourceServer} and passes the result to the consumer
     *
     * @param resourceServer the resourceServer. Cannot be {@code null}.
     * @param type the type of a resource
     * @param policyConsumer consumer of policies resulted from the search
     */
    void findByResourceType(ResourceServer resourceServer, String type, Consumer<Policy> policyConsumer);

    /**
     * Returns a list of {@link Policy} associated with a {@link org.keycloak.authorization.model.Scope} within the given <code>scope</code>.
     *
     * @param resourceServer the resource server. Cannot be {@code null}.
     * @param scopes the scopes
     * @return a list of policies associated with the given scopes
     */
    List<Policy> findByScopes(ResourceServer resourceServer, List<Scope> scopes);

    /**
     * Returns a list of {@link Policy} associated with a {@link org.keycloak.authorization.model.Scope} with the given <code>resource</code> and <code>scopes</code>.
     *
     * @param resourceServer the resource server. Cannot be {@code null}.
     * @param resource the resource. Ignored if {@code null}.
     * @param scopes the scopes
     * @return a list of policies associated with the given scopes
     */
    default List<Policy> findByScopes(ResourceServer resourceServer, Resource resource, List<Scope> scopes) {
        List<Policy> result = new LinkedList<>();

        findByScopes(resourceServer, resource, scopes, result::add);

        return result;
    }

    /**
     * Effectively the same method as {@link #findByScopes(ResourceServer, Resource, List)}, however in the end
     * the {@code consumer} is fed with the result.
     *
     */
    void findByScopes(ResourceServer resourceServer, Resource resource, List<Scope> scopes, Consumer<Policy> consumer);

    /**
     * Returns a list of {@link Policy} with the given <code>type</code>.
     *
     * @param resourceServer the resource server id. Cannot be {@code null}.
     * @param type the type of the policy
     * @return a list of policies with the given type
     */
    List<Policy> findByType(ResourceServer resourceServer, String type);

    /**
     * Returns a list of {@link Policy} that depends on another policy with the given <code>id</code>.
     *
     * @param resourceServer the resource server. Cannot be {@code null}.
     * @param id the id of the policy to query its dependents
     * @return a list of policies that depends on the a policy with the given identifier
     */
    List<Policy> findDependentPolicies(ResourceServer resourceServer, String id);

    Stream<Policy> findDependentPolicies(ResourceServer resourceServer, String resourceType, String associatedPolicyType, String configKey, String configValue);

    Stream<Policy> findDependentPolicies(ResourceServer resourceServer, String resourceType, String associatedPolicyType, String configKey, List<String> configValues);
}

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
     * @param representation the policy representation
     * @param resourceServer the resource server to which this policy belongs
     * @return a new instance of {@link Policy}
     */
    Policy create(AbstractPolicyRepresentation representation, ResourceServer resourceServer);

    /**
     * Deletes a policy from the underlying persistence mechanism.
     *
     * @param id the id of the policy to delete
     */
    void delete(String id);

    /**
     * Returns a {@link Policy} with the given <code>id</code>
     *
     * @param id the identifier of the policy
     * @param resourceServerId the resource server id
     * @return a policy with the given identifier.
     */
    Policy findById(String id, String resourceServerId);

    /**
     * Returns a {@link Policy} with the given <code>name</code>
     *
     * @param name             the name of the policy
     * @param resourceServerId the resource server id
     * @return a policy with the given name.
     */
    Policy findByName(String name, String resourceServerId);

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
     * @param attributes a map holding the attributes that will be used as a filter
     * @param resourceServerId the identifier of a resource server
     * @return a list of policies that belong to the given resource server
     */
    List<Policy> findByResourceServer(Map<String, String[]> attributes, String resourceServerId, int firstResult, int maxResult);

    /**
     * Returns a list of {@link Policy} associated with a {@link org.keycloak.authorization.core.model.Resource} with the given <code>resourceId</code>.
     *
     * @param resourceId the identifier of a resource
     * @param resourceServerId the resource server id
     * @return a list of policies associated with the given resource
     */
    List<Policy> findByResource(String resourceId, String resourceServerId);

    void findByResource(String resourceId, String resourceServerId, Consumer<Policy> consumer);

    /**
     * Returns a list of {@link Policy} associated with a {@link org.keycloak.authorization.core.model.Resource} with the given <code>type</code>.
     *
     * @param resourceType     the type of a resource
     * @param resourceServerId the resource server id
     * @return a list of policies associated with the given resource type
     */
    List<Policy> findByResourceType(String resourceType, String resourceServerId);

    /**
     * Returns a list of {@link Policy} associated with a {@link org.keycloak.authorization.core.model.Scope} with the given <code>scopeIds</code>.
     *
     * @param scopeIds the id of the scopes
     * @param resourceServerId the resource server id
     * @return a list of policies associated with the given scopes
     */
    List<Policy> findByScopeIds(List<String> scopeIds, String resourceServerId);

    /**
     * Returns a list of {@link Policy} associated with a {@link org.keycloak.authorization.core.model.Scope} with the given <code>resourceId</code> and <code>scopeIds</code>.
     *
     * @param scopeIds the id of the scopes
     * @param resourceId the id of the resource
     * @param resourceServerId the resource server id
     * @return a list of policies associated with the given scopes
     */
    List<Policy> findByScopeIds(List<String> scopeIds, String resourceId, String resourceServerId);

    void findByScopeIds(List<String> scopeIds, String resourceId, String resourceServerId, Consumer<Policy> consumer);

    /**
     * Returns a list of {@link Policy} with the given <code>type</code>.
     *
     * @param type the type of the policy
     * @param resourceServerId the resource server id
     * @return a list of policies with the given type
     */
    List<Policy> findByType(String type, String resourceServerId);

    /**
     * Returns a list of {@link Policy} that depends on another policy with the given <code>id</code>.
     *
     * @param id the id of the policy to query its dependents
     * @param resourceServerId the resource server id
     * @return a list of policies that depends on the a policy with the given identifier
     */
    List<Policy> findDependentPolicies(String id, String resourceServerId);

    void findByResourceType(String type, String id, Consumer<Policy> policyConsumer);
}

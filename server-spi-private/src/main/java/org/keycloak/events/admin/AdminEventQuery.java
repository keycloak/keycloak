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

package org.keycloak.events.admin;

import java.util.Date;
import java.util.List;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public interface AdminEventQuery {
    
    /**
     * Search by resource realm
     *
     * @param realmId realm id
     * @return Associated <code>AdminEventQuery</code> for method chaining
     */
    AdminEventQuery realm(String realmId);
    
    /**
     * Search by authentication realm
     *
     * @param realmId realm name
     * @return Associated <code>AdminEventQuery</code> for method chaining
     */
    AdminEventQuery authRealm(String realmId);
    
    /**
     * Search by authenticated client
     *
     * @param clientId client uuid
     * @return Associated <code>AdminEventQuery</code> for method chaining
     */
    AdminEventQuery authClient(String clientId);

    /**
     * Search by authenticated user
     *
     * @param userId user uuid
     * @return Associated <code>AdminEventQuery</code> for method chaining
     */
    AdminEventQuery authUser(String userId);

    /**
     * Search by request ip address
     *
     * @param ipAddress
     * @return Associated <code>AdminEventQuery</code> for method chaining
     */
    AdminEventQuery authIpAddress(String ipAddress);

    /**
     * Search by operation type
     *
     * @param operations
     * @return <code>this</code> for method chaining
     */
    AdminEventQuery operation(OperationType... operations);

    /**
     * Search by {@link ResourceType}.
     * @param resourceTypes
     * @return <code>this</code> for method chaining
     */
    AdminEventQuery resourceType(ResourceType ... resourceTypes);

    /**
     * Search by resource path. Supports wildcard <code>*</code>. For example:
     * <ul>
     * <li><b>*&#47;master</b> - matches 'realms/master'</li>
     * <li><b>realms/master&#47;*&#47;00d4b16f</b> - matches 'realms/master/clients/00d4b16f'</li>
     * <li><b>realms&#47;master&#47;*</b> - matches anything under 'realms/master'</li>
     * </ul>
     *
     * @param resourcePath
     * @return <code>this</code> for method chaining
     */
    AdminEventQuery resourcePath(String resourcePath);

    /**
     * Search by events after the specified time
     * 
     * @param fromTime from date
     * @return <code>this</code> for method chaining
     */
    AdminEventQuery fromTime(Date fromTime);

    /**
     * Search by events before the specified time
     * 
     * @param toTime to date
     * @return <code>this</code> for method chaining
     */
    AdminEventQuery toTime(Date toTime);

    /**
     * Used for pagination
     * 
     * @param first first result to return
     * @return <code>this</code> for method chaining
     */
    AdminEventQuery firstResult(int first);

    /**
     * Use for pagination
     * 
     * @param max the maximum results to return
     * @return <code>this</code> for method chaining
     */
    AdminEventQuery maxResults(int max);

    /**
     * Executes the query and returns the results
     * 
     * @return
     */
    List<AdminEvent> getResultList();
}

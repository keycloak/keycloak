/*
 * Copyright 2023 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.health;

import org.keycloak.provider.Provider;

/**
 * This interface is used for controlling load balancer. If one of the implementations reports that it is down,
 * the load balancer endpoint will return the {@code DOWN} status.
 *
 */
@FunctionalInterface
public interface LoadBalancerCheckProvider extends Provider {

    /**
     * Check if a component represented by this check is down/unhealthy.
     * <p />
     * The implementation must be non-blocking as it is executed in the event loop.
     * It is necessary to run this in the event loop as blocking requests are queued and then the check
     * would time out on the loadbalancer side when there is an overload situation in Keycloak.
     * An automatic failover to the secondary site due to an overloaded primary site is desired as this could
     * lead to a ping-pong between the sites where the primary site becomes available again once the switchover
     * is complete.
     *
     * @return true if the component is down/unhealthy, false otherwise
     */
    boolean isDown();

    @Override
    default void close() {
        //no-op by default
    }
}

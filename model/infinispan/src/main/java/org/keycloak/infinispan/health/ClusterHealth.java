/*
 * Copyright 2025 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.infinispan.health;

/**
 * Infinispan cluster health checks, to detect network partitions.
 */
public interface ClusterHealth {

    /**
     * It checks the cluster health returning {@code true} if this node can continue processing requests.
     * <p>
     * If the network and cluster are stable, this method must return {@code true}.
     * <p>
     * If a network partition is detected, the return value depends on whether this node belongs to the winning
     * partition. It must return {@code true} if it belongs to the winning partition or {@code false} if it does not.
     * Deciding the winning partition is at the implementation discretion.
     *
     * @return {@code true} if the cluster is healthy and this node can continue processing requests, {@code false}
     * otherwise.
     */
    boolean isHealthy();

    /**
     * Triggers a cluster health check.
     * <p>
     * This method should only trigger the health check logic without blocking or waiting for its outcome.
     */
    void triggerClusterHealthCheck();

    /**
     * Determine if the cluster health check is supported.
     * @return false if the current transport setup doesn't provide enough information.
     */
    boolean isSupported();

}

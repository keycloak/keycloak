/*
 * Copyright 2026 Red Hat, Inc. and/or its affiliates
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

import java.util.List;
import java.util.Objects;

/**
 * Aggregates multiple {@link ClusterHealth} implementations into a single health check. The composite is healthy only
 * if all delegates are healthy.
 */
public class CompositeClusterHealth implements ClusterHealth {

    private final List<ClusterHealth> clusterHealth;

    public CompositeClusterHealth(List<ClusterHealth> clusterHealth) {
        this.clusterHealth = Objects.requireNonNullElse(clusterHealth, List.<ClusterHealth>of())
                .stream()
                .filter(Objects::nonNull)
                .toList();
    }

    @Override
    public boolean isHealthy() {
        return clusterHealth.stream().allMatch(ClusterHealth::isHealthy);
    }

    @Override
    public boolean isSiteActive() {
        return clusterHealth.stream().allMatch(ClusterHealth::isSiteActive);
    }

    @Override
    public void triggerClusterHealthCheck() {
        clusterHealth.forEach(ClusterHealth::triggerClusterHealthCheck);
    }

    @Override
    public boolean isSupported() {
        return clusterHealth.stream().anyMatch(ClusterHealth::isSupported);
    }

    @Override
    public void close() {
        clusterHealth.forEach(ClusterHealth::close);
    }
}

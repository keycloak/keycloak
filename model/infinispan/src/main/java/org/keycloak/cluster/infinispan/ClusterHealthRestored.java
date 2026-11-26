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

package org.keycloak.cluster.infinispan;

import org.keycloak.provider.ProviderEvent;

/**
 * Published when {@code KEYCLOAK_JDBC_PING2}'s periodic health check detects a transition
 * from any non-healthy state back to {@code HEALTHY}. This covers:
 *
 * <ul>
 *     <li>Database reconnect — the health check could not reach the database ({@code ERROR})
 *         and connectivity is restored.</li>
 *     <li>Split-brain recovery — this node was in the losing partition ({@code UNHEALTHY})
 *         and has rejoined the correct cluster view.</li>
 *     <li>Coordinator election — no coordinator was present in the database table
 *         ({@code NO_COORDINATOR}) and one has since been elected.</li>
 * </ul>
 *
 * <p>Only available when the JDBC-PING cache stack is in use. Listened to by
 * {@link DatabaseAwareClusterProviderFactory} in stateless mode, which broadcasts a
 * {@code CLEAR_ALL_LOCAL_CACHES_EVENT} to all cluster nodes so that stale cache entries
 * from the outage period are discarded.
 */
public class ClusterHealthRestored implements ProviderEvent {
}

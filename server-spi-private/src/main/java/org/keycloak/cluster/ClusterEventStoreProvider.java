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

package org.keycloak.cluster;

import java.util.Collection;
import java.util.List;

import org.keycloak.provider.Provider;

public interface ClusterEventStoreProvider extends Provider {

    /**
     * Persist an event for all other active clusters.
     * Queries JGROUPS_PING for distinct cluster names and inserts one row per target cluster.
     */
    void persist(String senderCluster, byte[] eventData);

    /**
     * Read events addressed to the given cluster, ordered by creation time.
     */
    List<StoredClusterEvent> readEvents(String targetCluster, int maxResults);

    /**
     * Delete consumed events by their IDs.
     */
    void deleteEvents(Collection<String> ids);

    /**
     * Delete events older than the given timestamp (milliseconds). Backstop for stale rows.
     */
    void deleteEventsOlderThan(long timestampMillis);
}

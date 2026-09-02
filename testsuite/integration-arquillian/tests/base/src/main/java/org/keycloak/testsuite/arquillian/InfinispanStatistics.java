/*
 * Copyright 2017 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.testsuite.arquillian;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.keycloak.connections.infinispan.InfinispanConnectionProvider;

/**
 *
 * @author hmlnarik
 */
public interface InfinispanStatistics {

    public static class Constants {
        public static final String DOMAIN_INFINISPAN_DATAGRID = InfinispanConnectionProvider.JMX_DOMAIN;

        public static final String TYPE_CHANNEL = "channel";
        public static final String TYPE_CACHE = "Cache";
        public static final String TYPE_CACHE_MANAGER = "CacheManager";

        public static final String COMPONENT_STATISTICS = "Statistics";

        /** Cache statistics */
        public static final String STAT_CACHE_AVERAGE_READ_TIME = "averageReadTime";
        public static final String STAT_CACHE_AVERAGE_WRITE_TIME = "averageWriteTime";
        public static final String STAT_CACHE_ELAPSED_TIME = "elapsedTime";
        public static final String STAT_CACHE_EVICTIONS = "evictions";
        public static final String STAT_CACHE_HITS = "hits";
        public static final String STAT_CACHE_HIT_RATIO = "hitRatio";
        public static final String STAT_CACHE_MISSES = "misses";
        public static final String STAT_CACHE_NUMBER_OF_ENTRIES = "numberOfEntries";
        public static final String STAT_CACHE_NUMBER_OF_ENTRIES_IN_MEMORY = "numberOfEntriesInMemory";
        public static final String STAT_CACHE_READ_WRITE_RATIO = "readWriteRatio";
        public static final String STAT_CACHE_REMOVE_HITS = "removeHits";
        public static final String STAT_CACHE_REMOVE_MISSES = "removeMisses";
        public static final String STAT_CACHE_STORES = "stores";
        public static final String STAT_CACHE_TIME_SINCE_RESET = "timeSinceReset";

        /** JGroups channel statistics */
        public static final String STAT_CHANNEL_ADDRESS = "address";
        public static final String STAT_CHANNEL_ADDRESS_UUID = "address_uuid";
        public static final String STAT_CHANNEL_CLOSED = "closed";
        public static final String STAT_CHANNEL_CLUSTER_NAME = "cluster_name";
        public static final String STAT_CHANNEL_CONNECTED = "connected";
        public static final String STAT_CHANNEL_CONNECTING = "connecting";
        public static final String STAT_CHANNEL_DISCARD_OWN_MESSAGES = "discard_own_messages";
        public static final String STAT_CHANNEL_OPEN = "open";
        public static final String STAT_CHANNEL_RECEIVED_BYTES = "received_bytes";
        public static final String STAT_CHANNEL_RECEIVED_MESSAGES = "received_messages";
        public static final String STAT_CHANNEL_SENT_BYTES = "sent_bytes";
        public static final String STAT_CHANNEL_SENT_MESSAGES = "sent_messages";
        public static final String STAT_CHANNEL_STATE = "state";
        public static final String STAT_CHANNEL_STATS = "stats";
        public static final String STAT_CHANNEL_VIEW = "view";

    }

    Map<String, Object> getStatistics();

    Comparable getSingleStatistics(String statisticsName);

    void waitToBecomeAvailable(int time, TimeUnit unit);

    /**
     * Resets the statistics counters.
     */
    void reset();

    /**
     * Returns {@code true} iff the statistics represented by this object can be retrieved from the server.
     */
    boolean exists();
}

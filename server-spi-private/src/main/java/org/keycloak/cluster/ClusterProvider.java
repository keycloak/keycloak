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

package org.keycloak.cluster;


import java.util.Collection;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

import org.keycloak.provider.Provider;

/**
 * Various utils related to clustering and concurrent tasks on cluster nodes
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public interface ClusterProvider extends Provider {

    /**
     * Same value for all cluster nodes. It will use startup time of this server in non-cluster environment.
     */
    int getClusterStartupTime();


    /**
     * Execute given task just if it's not already in progress (either on this or any other cluster node).
     *
     * @param taskKey
     * @param taskTimeoutInSeconds timeout for given task. If there is existing task in progress for longer time, it's considered outdated so we will start our task.
     * @param task
     * @param <T>
     * @return result with "executed" flag specifying if execution was executed or ignored.
     */
    <T> ExecutionResult<T> executeIfNotExecuted(String taskKey, int taskTimeoutInSeconds, Callable<T> task);


    /**
     * Execute given task just if it's not already in progress (either on this or any other cluster node). It will return corresponding future to every caller and this future is fulfilled if:
     * - The task is successfully finished. In that case Future will be true
     * - The task wasn't successfully finished. For example because cluster node failover. In that case Future will be false
     *
     * @param taskKey
     * @param taskTimeoutInSeconds timeout for given task. If there is existing task in progress for longer time, it's considered outdated so we will start our task.
     * @param task
     * @return Future, which will be completed once the running task is finished. Returns true if task was successfully finished. Otherwise (for example if cluster node when task was running leaved cluster) returns false
     */
    Future<Boolean> executeIfNotExecutedAsync(String taskKey, int taskTimeoutInSeconds, Callable task);


    /**
     * Register task (listener) under given key. When this key will be put to the cache on any cluster node, the task will be executed.
     *
     * @param taskKey
     * @param task
     */
    void registerListener(String taskKey, ClusterListener task);


    /**
     * Notify registered listeners on all cluster nodes in all datacenters. It will notify listeners registered under given taskKey
     *
     * @param taskKey
     * @param event
     * @param ignoreSender if true, then sender node itself won't receive the notification
     * @param dcNotify Specify which DCs to notify. See {@link DCNotify} enum values for more info
     * @deprecated use {@link ClusterProvider#notify(String, ClusterEvent, boolean)} instead. {@link DCNotify} is deprecated. For removal in Keycloak 27.
     */
    @Deprecated(since = "26.3", forRemoval = true)
    void notify(String taskKey, ClusterEvent event, boolean ignoreSender, DCNotify dcNotify);

    default void notify(String taskKey, ClusterEvent event, boolean ignoreSender) {
        notify(taskKey, event, ignoreSender, DCNotify.ALL_DCS);
    }

    /**
     * An alternative to {@link #notify(String, ClusterEvent, boolean, DCNotify)} that sends multiple events in a single
     * network call.
     * <p>
     * Notifies registered listeners on all cluster nodes in all datacenters. It will notify listeners registered under
     * given {@code taskKey}
     *
     * @see #notify(String, ClusterEvent, boolean, DCNotify)
     * @deprecated use {@link ClusterProvider#notify(String, Collection, boolean)} instead. {@link DCNotify} is deprecated. For removal in Keycloak 27.
     */
    @Deprecated(since = "26.3", forRemoval = true)
    default void notify(String taskKey, Collection<? extends ClusterEvent> events, boolean ignoreSender, DCNotify dcNotify) {
        events.forEach(event -> notify(taskKey, event, ignoreSender, dcNotify));
    }

    default void notify(String taskKey, Collection<? extends ClusterEvent> events, boolean ignoreSender) {
        events.forEach(event -> notify(taskKey, event, ignoreSender, DCNotify.ALL_DCS));
    }

    /**
     * This is now used only in deprecated methods.
     * All methods that are using this enum have a Javadoc suggesting alternative.
     * @deprecated For removal in Keycloak 27.
     */
    @Deprecated(since = "26.3", forRemoval = true)
    enum DCNotify {
        /** Send message to all cluster nodes in all DCs **/
        ALL_DCS,

        /** Send message to all cluster nodes on THIS datacenter only **/
        LOCAL_DC_ONLY,

        /** Send message to all cluster nodes in all datacenters, but NOT to this datacenter. Option "ignoreSender" of method {@link #notify} will be ignored as sender is ignored anyway due it is in this datacenter **/
         ALL_BUT_LOCAL_DC
    }

}

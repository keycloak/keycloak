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

package org.keycloak.connections.infinispan;

import java.util.Arrays;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Predicate;
import java.util.stream.Stream;

import org.keycloak.common.util.MultiSiteUtils;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.provider.Provider;

import org.infinispan.Cache;
import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.util.concurrent.BlockingManager;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public interface InfinispanConnectionProvider extends Provider {

    String REALM_CACHE_NAME = "realms";
    String REALM_REVISIONS_CACHE_NAME = "realmRevisions";
    int REALM_REVISIONS_CACHE_DEFAULT_MAX = 20000;

    String USER_CACHE_NAME = "users";
    String USER_REVISIONS_CACHE_NAME = "userRevisions";
    int USER_REVISIONS_CACHE_DEFAULT_MAX = 100000;

    String USER_SESSION_CACHE_NAME = "sessions";
    String CLIENT_SESSION_CACHE_NAME = "clientSessions";
    String OFFLINE_USER_SESSION_CACHE_NAME = "offlineSessions";
    String OFFLINE_CLIENT_SESSION_CACHE_NAME = "offlineClientSessions";
    String LOGIN_FAILURE_CACHE_NAME = "loginFailures";
    String AUTHENTICATION_SESSIONS_CACHE_NAME = "authenticationSessions";
    String WORK_CACHE_NAME = "work";
    String AUTHORIZATION_CACHE_NAME = "authorization";
    String AUTHORIZATION_REVISIONS_CACHE_NAME = "authorizationRevisions";
    int AUTHORIZATION_REVISIONS_CACHE_DEFAULT_MAX = 20000;
    int SESSIONS_CACHE_DEFAULT_MAX = 10000;

    String ACTION_TOKEN_CACHE = "actionTokens";
    int ACTION_TOKEN_CACHE_DEFAULT_MAX = -1;
    int ACTION_TOKEN_MAX_IDLE_SECONDS = -1;
    long ACTION_TOKEN_WAKE_UP_INTERVAL_SECONDS = 5 * 60 * 1000L;

    String KEYS_CACHE_NAME = "keys";
    int KEYS_CACHE_DEFAULT_MAX = 1000;
    int KEYS_CACHE_MAX_IDLE_SECONDS = 3600;

    String CRL_CACHE_NAME = "crl";
    int CRL_CACHE_DEFAULT_MAX = 1000;

    // System property used on Wildfly to identify distributedCache address and sticky session route
    String JBOSS_NODE_NAME = "jboss.node.name";
    String JGROUPS_UDP_MCAST_ADDR = "jgroups.mcast_addr";
    String JGROUPS_BIND_ADDR = "jgroups.bind.address";

    // TODO This property is not in Wildfly. Check if corresponding property in Wildfly exists
    String JBOSS_SITE_NAME = "jboss.site.name";

    String JMX_DOMAIN = "jboss.datagrid-infinispan";

    // Constant used as the prefix of the current node if "jboss.node.name" is not configured
    String NODE_PREFIX = "node_";

    // list of cache name for local caches (not replicated)
    String[] LOCAL_CACHE_NAMES = {
            REALM_CACHE_NAME,
            REALM_REVISIONS_CACHE_NAME,
            USER_CACHE_NAME,
            USER_REVISIONS_CACHE_NAME,
            AUTHORIZATION_CACHE_NAME,
            AUTHORIZATION_REVISIONS_CACHE_NAME,
            KEYS_CACHE_NAME,
            CRL_CACHE_NAME,
    };

    // list of cache name for user and client session caches, both offline and online
    String[] USER_AND_CLIENT_SESSION_CACHES = {
            USER_SESSION_CACHE_NAME,
            CLIENT_SESSION_CACHE_NAME,
            OFFLINE_USER_SESSION_CACHE_NAME,
            OFFLINE_CLIENT_SESSION_CACHE_NAME,
    };

    // list of cache name which could be defined as distributed or replicated
    String[] CLUSTERED_CACHE_NAMES = {
            USER_SESSION_CACHE_NAME,
            CLIENT_SESSION_CACHE_NAME,
            OFFLINE_USER_SESSION_CACHE_NAME,
            OFFLINE_CLIENT_SESSION_CACHE_NAME,
            LOGIN_FAILURE_CACHE_NAME,
            AUTHENTICATION_SESSIONS_CACHE_NAME,
            ACTION_TOKEN_CACHE,
            WORK_CACHE_NAME
    };

    String[] ALL_CACHES_NAME = Stream.concat(Arrays.stream(LOCAL_CACHE_NAMES), Arrays.stream(CLUSTERED_CACHE_NAMES)).toArray(String[]::new);

    String[] LOCAL_MAX_COUNT_CACHES = new String[]{
            AUTHORIZATION_CACHE_NAME,
            CRL_CACHE_NAME,
            KEYS_CACHE_NAME,
            REALM_CACHE_NAME,
            USER_CACHE_NAME
    };

    String[] CLUSTERED_MAX_COUNT_CACHES = new String[]{
            CLIENT_SESSION_CACHE_NAME,
            OFFLINE_USER_SESSION_CACHE_NAME,
            OFFLINE_CLIENT_SESSION_CACHE_NAME,
            USER_SESSION_CACHE_NAME
    };

    // caches that allow numOwner attribute to be configurable using options.
    String[] CLUSTERED_CACHE_NUM_OWNERS = new String[]{
            USER_SESSION_CACHE_NAME,
            CLIENT_SESSION_CACHE_NAME,
            LOGIN_FAILURE_CACHE_NAME,
            AUTHENTICATION_SESSIONS_CACHE_NAME,
            ACTION_TOKEN_CACHE,
    };

    /**
     *
     * Effectively the same as {@link InfinispanConnectionProvider#getCache(String, boolean)} with createIfAbsent set to {@code true}
     *
     */
    default <K, V> Cache<K, V> getCache(String name) {
        return getCache(name, true);
    }

    /**
     * Provides an instance if Infinispan cache by name
     *
     * @param name name of the requested cache
     * @param createIfAbsent if true the connection provider will create the requested cache on method call if it does not exist
     * @return return a cache instance
     * @param <K> key type
     * @param <V> value type
     */
    <K, V> Cache<K, V> getCache(String name, boolean createIfAbsent);

    /**
     * Get remote cache of given name. Could just retrieve the remote cache from the remoteStore configured in given infinispan cache and/or
     * alternatively return the secured remoteCache (remoteCache corresponding to secured hotrod endpoint)
     */
    <K, V> RemoteCache<K, V> getRemoteCache(String name);

    /**
     * @return Information about cluster topology
     * @deprecated The logic in {@link TopologyInfo} is not used anymore in Keycloak. To get the node or site name, use {@link #getNodeInfo()}.
     */
    @Deprecated(since = "26.5", forRemoval = true)
    TopologyInfo getTopologyInfo();

    /**
     * @return The information about the local node.
     */
    NodeInfo getNodeInfo();

    /**
     * Migrates the JBoss Marshalling encoding to Infinispan ProtoStream
     *
     * @return A {@link CompletionStage} to signal when the operator is completed.
     */
    CompletionStage<Void> migrateToProtoStream();

    /**
     * Returns an executor that will run the given tasks on a blocking thread as required.
     * <p>
     * The Infinispan block {@link Executor} is used to execute blocking operation, like I/O.
     * If Virtual Threads are enabled, this will be an executor with Virtual Threads.
     *
     * @param name The name for trace logging purpose.
     * @return The Infinispan blocking {@link Executor}.
     */
    default Executor getExecutor(String name) {
        return getBlockingManager().asExecutor(name);
    }

    /**
     * @return The Infinispan {@link ScheduledExecutorService}. Long or blocking operations must not be executed directly.
     */
    ScheduledExecutorService getScheduledExecutor();

    /**
     * Syntactic sugar to get a {@link RemoteCache}.
     *
     * @see InfinispanConnectionProvider#getRemoteCache(String)
     */
    static <K, V> RemoteCache<K, V> getRemoteCache(KeycloakSessionFactory factory, String cacheName) {
        try (var session = factory.create()) {
            return session.getProvider(InfinispanConnectionProvider.class).getRemoteCache(cacheName);
        }
    }

    /**
     * Returns the Infinispan {@link BlockingManager}.
     * <p>
     * The {@link BlockingManager} should be used to execute blocking operation like disk I/O. It offloads the task to
     * the Infinispan blocking thread pool.
     *
     * @return The Infinispan {@link BlockingManager}.
     */
    BlockingManager getBlockingManager();

    static Stream<String> skipSessionsCacheIfRequired(Stream<String> caches) {
        if (!MultiSiteUtils.isPersistentSessionsEnabled()) {
            return caches;
        }
        // persistent-user-sessions enabled, we do not need the sessions caches from external Infinispan.
        return caches
                .filter(Predicate.isEqual(USER_SESSION_CACHE_NAME).negate())
                .filter(Predicate.isEqual(OFFLINE_USER_SESSION_CACHE_NAME).negate())
                .filter(Predicate.isEqual(CLIENT_SESSION_CACHE_NAME).negate())
                .filter(Predicate.isEqual(OFFLINE_CLIENT_SESSION_CACHE_NAME).negate());
    }
}

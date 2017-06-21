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

import org.infinispan.Cache;
import org.keycloak.provider.Provider;

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

    String SESSION_CACHE_NAME = "sessions";
    String OFFLINE_SESSION_CACHE_NAME = "offlineSessions";
    String LOGIN_FAILURE_CACHE_NAME = "loginFailures";
    String AUTHENTICATION_SESSIONS_CACHE_NAME = "authenticationSessions";
    String WORK_CACHE_NAME = "work";
    String AUTHORIZATION_CACHE_NAME = "authorization";
    String AUTHORIZATION_REVISIONS_CACHE_NAME = "authorizationRevisions";
    int AUTHORIZATION_REVISIONS_CACHE_DEFAULT_MAX = 20000;

    String ACTION_TOKEN_CACHE = "actionTokens";
    int ACTION_TOKEN_CACHE_DEFAULT_MAX = -1;
    int ACTION_TOKEN_MAX_IDLE_SECONDS = -1;
    long ACTION_TOKEN_WAKE_UP_INTERVAL_SECONDS = 5 * 60 * 1000l;

    String KEYS_CACHE_NAME = "keys";
    int KEYS_CACHE_DEFAULT_MAX = 1000;
    int KEYS_CACHE_MAX_IDLE_SECONDS = 3600;

    // System property used on Wildfly to identify distributedCache address and sticky session route
    String JBOSS_NODE_NAME = "jboss.node.name";
    String JGROUPS_UDP_MCAST_ADDR = "jgroups.udp.mcast_addr";

    String JMX_DOMAIN = "jboss.datagrid-infinispan";

    <K, V> Cache<K, V> getCache(String name);

}

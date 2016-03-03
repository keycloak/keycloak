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

    public static final String VERSION_CACHE_NAME = "realmVersions";
    static final String REALM_CACHE_NAME = "realms";
    static final String USER_CACHE_NAME = "users";
    static final String SESSION_CACHE_NAME = "sessions";
    static final String OFFLINE_SESSION_CACHE_NAME = "offlineSessions";
    static final String LOGIN_FAILURE_CACHE_NAME = "loginFailures";
    static final String WORK_CACHE_NAME = "work";

    <K, V> Cache<K, V> getCache(String name);

}

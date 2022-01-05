/*
 * Copyright 2021 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.models.map.storage.hotRod.connections;

import org.infinispan.client.hotrod.RemoteCache;
import org.keycloak.provider.Provider;

/**
 * @author <a href="mailto:mkanis@redhat.com">Martin Kanis</a>
 */
public interface HotRodConnectionProvider extends Provider {

    /**
     * Returns a remote Infinispan cache specified by the given name.
     * @param name String Name of the remote cache.
     * @param <K> key
     * @param <V> value
     * @return A remote Infinispan cache specified by name.
     */
    <K, V> RemoteCache<K, V> getRemoteCache(String name);
}

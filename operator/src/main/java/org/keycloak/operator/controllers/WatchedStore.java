/*
 * Copyright 2023 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.operator.controllers;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.client.informers.cache.BasicItemStore;
import io.fabric8.kubernetes.client.informers.cache.Cache;
import io.vertx.core.impl.ConcurrentHashSet;

import java.util.Set;

public class WatchedStore<T extends HasMetadata> extends BasicItemStore<T> {

    private final Set<String> watched = new ConcurrentHashSet<>();

    public WatchedStore() {
        super(Cache::metaNamespaceKeyFunc);
    }

    @Override
    public T put(String key, T obj) {
        if (watched.contains(key)) {
            return super.put(key, obj);
        }
        super.remove(key);
        return null; // will always be seen as an add
    };

    public void addWatched(String name, String namespace) {
        watched.add(Cache.namespaceKeyFunc(namespace, name));
    }

    public void removeWatched(String name, String namespace) {
        String key = Cache.namespaceKeyFunc(namespace, name);
        watched.remove(key);
        this.remove(key);
    }

}

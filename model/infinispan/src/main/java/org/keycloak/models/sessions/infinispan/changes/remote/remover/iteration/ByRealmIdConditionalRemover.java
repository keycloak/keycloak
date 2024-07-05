/*
 * Copyright 2024 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.models.sessions.infinispan.changes.remote.remover.iteration;

import java.util.ArrayList;
import java.util.List;

import org.infinispan.client.hotrod.RemoteCache;
import org.keycloak.models.sessions.infinispan.changes.remote.remover.ConditionalRemover;
import org.keycloak.models.sessions.infinispan.entities.SessionEntity;

/**
 * A {@link ConditionalRemover} implementation to remove {@link SessionEntity} from a {@link RemoteCache} based on
 * {@link SessionEntity#getRealmId()} value.
 *
 * @param <K> The key's type stored in the {@link RemoteCache}.
 * @param <V> The value's type stored in the {@link RemoteCache}.
 */
public class ByRealmIdConditionalRemover<K, V extends SessionEntity> extends IterationBasedConditionalRemover<K, V> {

    private final List<String> realms;

    public ByRealmIdConditionalRemover() {
        realms = new ArrayList<>();
    }

    public void removeByRealmId(String realmId) {
        realms.add(realmId);
    }

    @Override
    boolean isEmpty() {
        return realms.isEmpty();
    }

    @Override
    public boolean willRemove(K key, V value) {
        return realms.contains(value.getRealmId());
    }

}

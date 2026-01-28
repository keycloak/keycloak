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

package org.keycloak.models.sessions.infinispan.changes.remote.remover.query;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.keycloak.models.sessions.infinispan.changes.remote.remover.ConditionalRemover;
import org.keycloak.models.sessions.infinispan.entities.SessionEntity;

import org.infinispan.client.hotrod.RemoteCache;

/**
 * A {@link ConditionalRemover} implementation to delete {@link SessionEntity} based on the {@code realmId} value.
 * <p>
 * This implementation uses Infinispan Ickle Queries to delete all entries belonging to the realm.
 *
 * @param <K> The key's type stored in the {@link RemoteCache}.
 * @param <V> The value's type stored in the {@link RemoteCache}.
 */
public class ByRealmIdQueryConditionalRemover<K, V extends SessionEntity> extends QueryBasedConditionalRemover<K, V> {

    private static final String CONDITION_FMT = "realmId IN (%s)";

    private final String entity;
    private final List<String> realms;

    public ByRealmIdQueryConditionalRemover(String entity) {
        this.entity = entity;
        this.realms = new ArrayList<>();
    }

    private static String parameter(int index) {
        return "p" + index;
    }

    public void removeByRealmId(String realmId) {
        realms.add(realmId);
    }

    @Override
    String getEntity() {
        return entity;
    }

    @Override
    String getQueryConditions() {
        assert !isEmpty();
        var condition = IntStream.range(0, realms.size())
                .mapToObj(value -> ":" + parameter(value))
                .collect(Collectors.joining(", "));
        return CONDITION_FMT.formatted(condition);
    }

    @Override
    Map<String, Object> getQueryParameters() {
        assert !isEmpty();
        Map<String, Object> params = new HashMap<>();
        int paramIdx = 0;
        for (var realmId : realms) {
            params.put(parameter(paramIdx++), realmId);
        }
        return params;
    }

    @Override
    boolean isEmpty() {
        return realms.isEmpty();
    }

    @Override
    public boolean willRemove(K key, V value) {
        return value != null && realms.contains(value.getRealmId());
    }
}

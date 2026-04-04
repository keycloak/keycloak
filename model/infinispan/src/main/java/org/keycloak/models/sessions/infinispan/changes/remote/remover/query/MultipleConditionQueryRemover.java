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

import org.infinispan.client.hotrod.RemoteCache;

/**
 * Base class implementing {@link QueryBasedConditionalRemover} and supports multiple remove conditions.
 * <p>
 * The remove condition can be added dynamically and, when the query is executed, they are joined together with an "or"
 * operator.
 *
 * @param <K> The key's type stored in the {@link RemoteCache}.
 * @param <V> The value's type stored in the {@link RemoteCache}.
 */
abstract class MultipleConditionQueryRemover<K, V> extends QueryBasedConditionalRemover<K, V> {

    private final List<RemoveCondition<K, V>> removes;
    private int parameterIndex;

    MultipleConditionQueryRemover() {
        removes = new ArrayList<>();
    }

    @Override
    String getQueryConditions() {
        return removes.stream()
                .map(RemoveCondition::getConditionalClause)
                .collect(Collectors.joining(" || "));
    }

    @Override
    Map<String, Object> getQueryParameters() {
        Map<String, Object> parameters = new HashMap<>();
        removes.forEach(removeCondition -> removeCondition.addParameters(parameters));
        return parameters;
    }

    @Override
    boolean isEmpty() {
        return removes.isEmpty();
    }

    @Override
    public boolean willRemove(K key, V value) {
        return !isEmpty() && removes.stream().anyMatch(c -> c.willRemove(key, value));
    }

    /**
     * If the query has parameters, use this method to generate a new unique parameter.
     */
    String nextParameter() {
        return "p" + parameterIndex++;
    }

    void add(RemoveCondition<K, V> condition) {
        removes.add(condition);
    }

    /**
     * A single remove condition.
     */
    interface RemoveCondition<K, V> {
        /**
         * @return The where clause with parameters.
         */
        String getConditionalClause();

        /**
         * Stores this condition parameters value
         */
        void addParameters(Map<String, Object> parameters);

        /**
         * @return {@code true} if the entry wil be removed by the query.
         */
        boolean willRemove(K key, V value);
    }
}

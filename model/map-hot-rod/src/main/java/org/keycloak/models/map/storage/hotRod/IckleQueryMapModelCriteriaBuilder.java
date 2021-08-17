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

package org.keycloak.models.map.storage.hotRod;

import org.keycloak.models.ClientModel;
import org.keycloak.models.map.common.AbstractEntity;
import org.keycloak.models.map.storage.ModelCriteriaBuilder;
import org.keycloak.storage.SearchableModelField;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.keycloak.models.map.storage.hotRod.IckleQueryOperators.C;
import static org.keycloak.models.map.storage.hotRod.IckleQueryOperators.findAvailableNamedParam;

public class IckleQueryMapModelCriteriaBuilder<K, V extends AbstractEntity, M> implements ModelCriteriaBuilder<M, IckleQueryMapModelCriteriaBuilder<K, V, M>> {

    private static final int INITIAL_BUILDER_CAPACITY = 250;
    private final StringBuilder whereClauseBuilder = new StringBuilder(INITIAL_BUILDER_CAPACITY);
    private final Map<String, Object> parameters;
    public static final Map<SearchableModelField<?>, String> INFINISPAN_NAME_OVERRIDES = new HashMap<>();

    static {
        INFINISPAN_NAME_OVERRIDES.put(ClientModel.SearchableFields.SCOPE_MAPPING_ROLE, "scopeMappings");
        INFINISPAN_NAME_OVERRIDES.put(ClientModel.SearchableFields.ATTRIBUTE, "attributes");
    }

    public IckleQueryMapModelCriteriaBuilder(StringBuilder whereClauseBuilder, Map<String, Object> parameters) {
        this.whereClauseBuilder.append(whereClauseBuilder);
        this.parameters = parameters;
    }

    public IckleQueryMapModelCriteriaBuilder() {
        this.parameters = new HashMap<>();
    }

    public static String getFieldName(SearchableModelField<?> modelField) {
        return INFINISPAN_NAME_OVERRIDES.getOrDefault(modelField, modelField.getName());
    }

    private static boolean notEmpty(StringBuilder builder) {
        return builder.length() != 0;
    }

    @Override
    public IckleQueryMapModelCriteriaBuilder<K, V, M> compare(SearchableModelField<? super M> modelField, Operator op, Object... value) {
        StringBuilder newBuilder = new StringBuilder(INITIAL_BUILDER_CAPACITY);
        newBuilder.append("(");

        if (notEmpty(whereClauseBuilder)) {
            newBuilder.append(whereClauseBuilder).append(" AND (");
        }

        Map<String, Object> newParameters = new HashMap<>(parameters);
        newBuilder.append(IckleQueryWhereClauses.produceWhereClause(modelField, op, value, newParameters));

        if (notEmpty(whereClauseBuilder)) {
            newBuilder.append(")");
        }

        return new IckleQueryMapModelCriteriaBuilder<>(newBuilder.append(")"), newParameters);
    }

    private StringBuilder joinBuilders(IckleQueryMapModelCriteriaBuilder<K, V, M>[] builders, String delimiter) {
        return new StringBuilder(INITIAL_BUILDER_CAPACITY).append("(").append(Arrays.stream(builders)
                .map(IckleQueryMapModelCriteriaBuilder::getWhereClauseBuilder)
                .filter(IckleQueryMapModelCriteriaBuilder::notEmpty)
                .collect(Collectors.joining(delimiter))).append(")");
    }

    private Map<String, Object> joinParameters(IckleQueryMapModelCriteriaBuilder<K, V, M>[] builders) {
        return Arrays.stream(builders)
                .map(IckleQueryMapModelCriteriaBuilder::getParameters)
                .map(Map::entrySet)
                .flatMap(Collection::stream)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    @SuppressWarnings("unchecked")
    private IckleQueryMapModelCriteriaBuilder<K, V, M>[] resolveNamedQueryConflicts(IckleQueryMapModelCriteriaBuilder<K, V, M>[] builders) {
        final Set<String> existingKeys = new HashSet<>();

        return Arrays.stream(builders).map(builder -> {
           Map<String, Object> oldParameters = builder.getParameters();

           if (oldParameters.keySet().stream().noneMatch(existingKeys::contains)) {
               existingKeys.addAll(oldParameters.keySet());
               return builder;
           }

           String newWhereClause = builder.getWhereClauseBuilder().toString();
           Map<String, Object> newParameters = new HashMap<>();
           for (String key : oldParameters.keySet()) {
               if (existingKeys.contains(key)) {
                   // resolve conflict
                   String newNamedParameter = findAvailableNamedParam(existingKeys, key + "n");
                   newParameters.put(newNamedParameter, oldParameters.get(key));
                   newWhereClause = newWhereClause.replace(key, newNamedParameter);
                   existingKeys.add(newNamedParameter);
               } else {
                   newParameters.put(key, oldParameters.get(key));
                   existingKeys.add(key);
               }
           }

           return new IckleQueryMapModelCriteriaBuilder<>(new StringBuilder(newWhereClause), newParameters);
        }).toArray(IckleQueryMapModelCriteriaBuilder[]::new);
    }

    @Override
    public IckleQueryMapModelCriteriaBuilder<K, V, M> and(IckleQueryMapModelCriteriaBuilder<K, V, M>... builders) {
        if (builders.length == 0) {
            return new IckleQueryMapModelCriteriaBuilder<>();
        }

        builders = resolveNamedQueryConflicts(builders);

        return new IckleQueryMapModelCriteriaBuilder<>(joinBuilders(builders, " AND "),
                joinParameters(builders));
    }

    @Override
    public IckleQueryMapModelCriteriaBuilder<K, V, M> or(IckleQueryMapModelCriteriaBuilder<K, V, M>... builders) {
        if (builders.length == 0) {
            return new IckleQueryMapModelCriteriaBuilder<>();
        }

        builders = resolveNamedQueryConflicts(builders);

        return new IckleQueryMapModelCriteriaBuilder<>(joinBuilders(builders, " OR "),
                joinParameters(builders));
    }

    @Override
    public IckleQueryMapModelCriteriaBuilder<K, V, M> not(IckleQueryMapModelCriteriaBuilder<K, V, M> builder) {
        StringBuilder newBuilder = new StringBuilder(INITIAL_BUILDER_CAPACITY);
        StringBuilder originalBuilder = builder.getWhereClauseBuilder();

        if (originalBuilder.length() != 0) {
            newBuilder.append("not").append(originalBuilder);
        }

        return new IckleQueryMapModelCriteriaBuilder<>(newBuilder, builder.getParameters());
    }

    private StringBuilder getWhereClauseBuilder() {
        return whereClauseBuilder;
    }

    /**
     *
     * @return Ickle query that represents this QueryBuilder
     */
    public String getIckleQuery() {
        return "FROM org.keycloak.models.map.storage.hotrod.HotRodClientEntity " + C + ((whereClauseBuilder.length() != 0) ? " WHERE " + whereClauseBuilder : "");
    }

    /**
     * Ickle queries are created using named parameters to avoid query injections; this method provides mapping
     * between parameter names and corresponding values
     *
     * @return Mapping from name of the parameter to value
     */
    public Map<String, Object> getParameters() {
        return parameters;
    }
}

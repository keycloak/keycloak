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
import org.keycloak.models.map.storage.CriterionNotSupportedException;
import org.keycloak.models.map.storage.ModelCriteriaBuilder;
import org.keycloak.storage.SearchableModelField;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static org.keycloak.models.map.storage.hotRod.IckleQueryMapModelCriteriaBuilder.sanitizeAnalyzed;
import static org.keycloak.models.map.storage.hotRod.IckleQueryOperators.C;

/**
 * This class provides knowledge on how to build Ickle query where clauses for specified {@link SearchableModelField}.
 *
 * For example,
 * <p/>
 * for {@link ClientModel.SearchableFields.CLIENT_ID} we use {@link IckleQueryOperators.ExpressionCombinator} for 
 * obtained {@link ModelCriteriaBuilder.Operator} and use it with field name corresponding to {@link ClientModel.SearchableFields.CLIENT_ID}
 * <p/>
 * however, for {@link ClientModel.SearchableFields.ATTRIBUTE} we need to compare attribute name and attribute value
 * so we create where clause similar to the following:
 * {@code (attributes.name = :attributeName) AND ( attributes.value = :attributeValue )}
 * 
 *
 */
public class IckleQueryWhereClauses {
    private static final Map<SearchableModelField<?>, WhereClauseProducer> WHERE_CLAUSE_PRODUCER_OVERRIDES = new HashMap<>();

    static {
        WHERE_CLAUSE_PRODUCER_OVERRIDES.put(ClientModel.SearchableFields.ATTRIBUTE, IckleQueryWhereClauses::whereClauseForClientsAttributes);
    }

    @FunctionalInterface
    private interface WhereClauseProducer {
        String produceWhereClause(String modelFieldName, ModelCriteriaBuilder.Operator op, Object[] values, Map<String, Object> parameters);
    }

    private static String produceWhereClause(String modelFieldName, ModelCriteriaBuilder.Operator op, Object[] values, Map<String, Object> parameters) {
        return IckleQueryOperators.combineExpressions(op, modelFieldName, values, parameters);
    }

    private static WhereClauseProducer whereClauseProducerForModelField(SearchableModelField<?> modelField) {
        return WHERE_CLAUSE_PRODUCER_OVERRIDES.getOrDefault(modelField, IckleQueryWhereClauses::produceWhereClause);
    }

    /**
     * Produces where clause for given {@link SearchableModelField}, {@link ModelCriteriaBuilder.Operator} and values
     *
     * @param modelField model field
     * @param op operator
     * @param values searched values
     * @param parameters mapping between named parameters and corresponding values
     * @return resulting where clause
     */
    public static String produceWhereClause(SearchableModelField<?> modelField, ModelCriteriaBuilder.Operator op,
                                            Object[] values, Map<String, Object> parameters) {
        String fieldName = IckleQueryMapModelCriteriaBuilder.getFieldName(modelField);

        if (IckleQueryMapModelCriteriaBuilder.isAnalyzedModelField(modelField) &&
                (op.equals(ModelCriteriaBuilder.Operator.ILIKE) || op.equals(ModelCriteriaBuilder.Operator.EQ) || op.equals(ModelCriteriaBuilder.Operator.NE))) {

            String clause = C + "." + fieldName + " : '" + sanitizeAnalyzed(values[0]) + "'";
            if (op.equals(ModelCriteriaBuilder.Operator.NE)) {
                return "not(" + clause + ")";
            }

            return clause;
        }

        return whereClauseProducerForModelField(modelField).produceWhereClause(fieldName, op, values, parameters);
    }

    private static String whereClauseForClientsAttributes(String modelFieldName, ModelCriteriaBuilder.Operator op, Object[] values, Map<String, Object> parameters) {
        if (values == null || values.length != 2) {
            throw new CriterionNotSupportedException(ClientModel.SearchableFields.ATTRIBUTE, op, "Invalid arguments, expected attribute_name-value pair, got: " + Arrays.toString(values));
        }

        final Object attrName = values[0];
        if (! (attrName instanceof String)) {
            throw new CriterionNotSupportedException(ClientModel.SearchableFields.ATTRIBUTE, op, "Invalid arguments, expected (String attribute_name), got: " + Arrays.toString(values));
        }

        String attrNameS = (String) attrName;
        Object[] realValues = new Object[values.length - 1];
        System.arraycopy(values, 1, realValues, 0, values.length - 1);

        // Clause for searching attribute name
        String nameClause = IckleQueryOperators.combineExpressions(ModelCriteriaBuilder.Operator.EQ, modelFieldName + ".name", new Object[]{attrNameS}, parameters);
        // Clause for searching attribute value
        String valueClause = IckleQueryOperators.combineExpressions(op, modelFieldName + ".values", realValues, parameters);

        return "(" + nameClause + ")" + " AND " + "(" + valueClause + ")";
    }
}

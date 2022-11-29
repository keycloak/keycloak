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

import org.keycloak.models.map.storage.ModelCriteriaBuilder;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * This class provides knowledge on how to build Ickle query where clauses for specified {@link ModelCriteriaBuilder.Operator}.
 * <p/>
 * For example,
 * <p/>
 * for operator {@link ModelCriteriaBuilder.Operator#EQ} we concatenate left operand and right operand with equal sign:
 * {@code fieldName = :parameterName}
 * <p/>
 * however, for operator {@link ModelCriteriaBuilder.Operator#EXISTS} we add following:
 * <p/>
 * {@code fieldName IS NOT NULL AND fieldName IS NOT EMPTY"}.
 *
 * For right side operands we use named parameters to avoid injection attacks. Mapping between named parameter and 
 * corresponding value is then saved into {@code Map<String, Object>} that is passed to each {@link ExpressionCombinator}.
 */
public class IckleQueryOperators {
    private static final Pattern UNWANTED_CHARACTERS_REGEX = Pattern.compile("[^a-zA-Z\\d]");
    public static final String C = "c";
    private static final Map<ModelCriteriaBuilder.Operator, String> OPERATOR_TO_STRING = new HashMap<>();
    private static final Map<ModelCriteriaBuilder.Operator, ExpressionCombinator> OPERATOR_TO_EXPRESSION_COMBINATORS = new HashMap<>();

    static {
        OPERATOR_TO_EXPRESSION_COMBINATORS.put(ModelCriteriaBuilder.Operator.IN, IckleQueryOperators::in);
        OPERATOR_TO_EXPRESSION_COMBINATORS.put(ModelCriteriaBuilder.Operator.EXISTS, IckleQueryOperators::exists);
        OPERATOR_TO_EXPRESSION_COMBINATORS.put(ModelCriteriaBuilder.Operator.NOT_EXISTS, IckleQueryOperators::notExists);
        OPERATOR_TO_EXPRESSION_COMBINATORS.put(ModelCriteriaBuilder.Operator.ILIKE, IckleQueryOperators::iLike);
        OPERATOR_TO_EXPRESSION_COMBINATORS.put(ModelCriteriaBuilder.Operator.LIKE, IckleQueryOperators::like);

        OPERATOR_TO_STRING.put(ModelCriteriaBuilder.Operator.EQ, "=");
        OPERATOR_TO_STRING.put(ModelCriteriaBuilder.Operator.NE, "!=");
        OPERATOR_TO_STRING.put(ModelCriteriaBuilder.Operator.LT, "<");
        OPERATOR_TO_STRING.put(ModelCriteriaBuilder.Operator.LE, "<=");
        OPERATOR_TO_STRING.put(ModelCriteriaBuilder.Operator.GT, ">");
        OPERATOR_TO_STRING.put(ModelCriteriaBuilder.Operator.GE, ">=");
        OPERATOR_TO_STRING.put(ModelCriteriaBuilder.Operator.LIKE, "LIKE");
        OPERATOR_TO_STRING.put(ModelCriteriaBuilder.Operator.ILIKE, "LIKE");
        OPERATOR_TO_STRING.put(ModelCriteriaBuilder.Operator.IN, "IN");
    }

    @FunctionalInterface
    private interface ExpressionCombinator {

        /**
         * Produces an Ickle query where clause for obtained parameters
         *
         * @param fieldName left side operand
         * @param values right side operands
         * @param parameters mapping between named parameters and actual parameter values
         * @return resulting string that will be part of resulting
         */
        String combine(String fieldName, Object[] values, Map<String, Object> parameters);
    }

    private static String exists(String modelFieldName, Object[] values, Map<String, Object> parameters) {
        String field = C + "." + modelFieldName;
        return field + " IS NOT NULL AND " + field + " IS NOT EMPTY";
    }

    private static String notExists(String modelFieldName, Object[] values, Map<String, Object> parameters) {
        String field = C + "." + modelFieldName;
        return field + " IS NULL OR " + field + " IS EMPTY";
    }

    private static String iLike(String modelFieldName, Object[] values, Map<String, Object> parameters) {
        String sanitizedValue = (String) IckleQueryMapModelCriteriaBuilder.sanitizeNonAnalyzed(values[0]);
        return singleValueOperator(ModelCriteriaBuilder.Operator.ILIKE)
                .combine(modelFieldName + "Lowercase", new String[] {sanitizedValue.toLowerCase()}, parameters);
    }

    private static String like(String modelFieldName, Object[] values, Map<String, Object> parameters) {
        String sanitizedValue = (String) IckleQueryMapModelCriteriaBuilder.sanitizeNonAnalyzed(values[0]);
        return singleValueOperator(ModelCriteriaBuilder.Operator.LIKE)
                .combine(modelFieldName, new String[] {sanitizedValue}, parameters);
    }

    private static String in(String modelFieldName, Object[] values, Map<String, Object> parameters) {
        if (values == null || values.length == 0) {
            return "false";
        }

        final Collection<?> operands;
        if (values.length == 1) {
            final Object value0 = values[0];
            if (value0 instanceof Collection) {
                operands = (Collection) value0;
            } else if (value0 instanceof Stream) {
                try (Stream valueS = (Stream) value0) {
                    operands = (Set) valueS.collect(Collectors.toSet());
                }
            } else {
                operands = Collections.singleton(value0);
            }
        } else {
            operands = new HashSet<>(Arrays.asList(values));
        }

        return operands.isEmpty() ? "false" : C + "." + modelFieldName + " IN (" + operands.stream()
                .map(operand -> {
                    String namedParam = findAvailableNamedParam(parameters.keySet(), modelFieldName);
                    parameters.put(namedParam, operand);
                    return ":" + namedParam;
                })
                .collect(Collectors.joining(", ")) +
                ")";
    }

    private static String removeForbiddenCharactersFromNamedParameter(String name) {
        return UNWANTED_CHARACTERS_REGEX.matcher(name).replaceAll( "");
    }

    /**
     * Maps {@code namePrefix} to next available parameter name. For example, if {@code namePrefix == "id"}
     * and {@code existingNames} set already contains {@code id0} and {@code id1} it returns {@code id2}.
     * Any character that is not an alphanumeric will be stripped, so that it works for prefixes like
     * {@code "attributes.name"} as well.
     *
     * This method is used for computing available names for name query parameters.
     * Instead of creating generic named parameters that would be hard to debug and read by humans, it creates readable
     * named parameters from the prefix.
     *
     * @param existingNames set of parameter names that are already used in this Ickle query
     * @param namePrefix name of the parameter
     * @return next available parameter name
     */
    public static String findAvailableNamedParam(Set<String> existingNames, String namePrefix) {
        String namePrefixCleared = removeForbiddenCharactersFromNamedParameter(namePrefix);
        return IntStream.iterate(0, i -> i + 1)
                .boxed()
                .map(num -> namePrefixCleared + num)
                .filter(name -> !existingNames.contains(name))
                .findFirst().orElseThrow(() -> new IllegalArgumentException("Cannot create Parameter name for " + namePrefix));
    }

    private static ExpressionCombinator singleValueOperator(ModelCriteriaBuilder.Operator op) {
        return (modelFieldName, values, parameters) -> {
            if (values.length != 1) throw new RuntimeException("Invalid arguments, expected (" + modelFieldName + "), got: " + Arrays.toString(values));

            if (values[0] == null && op.equals(ModelCriteriaBuilder.Operator.EQ)) {
                return C + "." + modelFieldName + " IS NULL";
            }

            String namedParameter = findAvailableNamedParam(parameters.keySet(), modelFieldName);
            parameters.put(namedParameter, values[0]);

            return C + "." + modelFieldName + " " + IckleQueryOperators.operatorToString(op) + " :" + namedParameter;
        };
    }

    private static String operatorToString(ModelCriteriaBuilder.Operator op) {
        return OPERATOR_TO_STRING.get(op);
    }

    private static ExpressionCombinator operatorToExpressionCombinator(ModelCriteriaBuilder.Operator op) {
        return OPERATOR_TO_EXPRESSION_COMBINATORS.getOrDefault(op, singleValueOperator(op));
    }

    /**
     * Provides a string containing where clause for given operator, field name and values
     *
     * @param op operator
     * @param modelFieldName field name
     * @param values values
     * @param parameters mapping between named parameters and their values
     * @return where clause
     */
    public static String combineExpressions(ModelCriteriaBuilder.Operator op, String modelFieldName, Object[] values, Map<String, Object> parameters) {
        return operatorToExpressionCombinator(op).combine(modelFieldName, values, parameters);
    }

}

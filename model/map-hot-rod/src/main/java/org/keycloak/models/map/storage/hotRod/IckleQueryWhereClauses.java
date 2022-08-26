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

import org.keycloak.authorization.model.Policy;
import org.keycloak.events.Event;
import org.keycloak.events.admin.AdminEvent;
import org.keycloak.models.ClientModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.models.map.storage.CriterionNotSupportedException;
import org.keycloak.models.map.storage.ModelCriteriaBuilder;
import org.keycloak.storage.SearchableModelField;
import org.keycloak.storage.StorageId;
import org.keycloak.util.EnumWithStableIndex;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.keycloak.models.map.storage.hotRod.IckleQueryMapModelCriteriaBuilder.getFieldName;
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
        WHERE_CLAUSE_PRODUCER_OVERRIDES.put(ClientModel.SearchableFields.ATTRIBUTE, IckleQueryWhereClauses::whereClauseForAttributes);
        WHERE_CLAUSE_PRODUCER_OVERRIDES.put(UserModel.SearchableFields.ATTRIBUTE, IckleQueryWhereClauses::whereClauseForAttributes);
        WHERE_CLAUSE_PRODUCER_OVERRIDES.put(UserModel.SearchableFields.IDP_AND_USER, IckleQueryWhereClauses::whereClauseForUserIdpAlias);
        WHERE_CLAUSE_PRODUCER_OVERRIDES.put(UserModel.SearchableFields.CONSENT_CLIENT_FEDERATION_LINK, IckleQueryWhereClauses::whereClauseForConsentClientFederationLink);
        WHERE_CLAUSE_PRODUCER_OVERRIDES.put(UserSessionModel.SearchableFields.CORRESPONDING_SESSION_ID, IckleQueryWhereClauses::whereClauseForCorrespondingSessionId);
        WHERE_CLAUSE_PRODUCER_OVERRIDES.put(Policy.SearchableFields.CONFIG, IckleQueryWhereClauses::whereClauseForPolicyConfig);
        WHERE_CLAUSE_PRODUCER_OVERRIDES.put(Event.SearchableFields.EVENT_TYPE, IckleQueryWhereClauses::whereClauseForEnumWithStableIndex);
        WHERE_CLAUSE_PRODUCER_OVERRIDES.put(AdminEvent.SearchableFields.OPERATION_TYPE, IckleQueryWhereClauses::whereClauseForEnumWithStableIndex);
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

            String clause = C + "." + fieldName + " : '" + sanitizeAnalyzed(((String)values[0]).toLowerCase()) + "'";
            if (op.equals(ModelCriteriaBuilder.Operator.NE)) {
                return "not(" + clause + ")";
            }

            return clause;
        }

        return whereClauseProducerForModelField(modelField).produceWhereClause(fieldName, op, values, parameters);
    }

    private static String whereClauseForAttributes(String modelFieldName, ModelCriteriaBuilder.Operator op, Object[] values, Map<String, Object> parameters) {
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

    private static String whereClauseForUserIdpAlias(String modelFieldName, ModelCriteriaBuilder.Operator op, Object[] values, Map<String, Object> parameters) {
        if (op != ModelCriteriaBuilder.Operator.EQ) {
            throw new CriterionNotSupportedException(UserModel.SearchableFields.IDP_AND_USER, op);
        }
        if (values == null || values.length == 0 || values.length > 2) {
            throw new CriterionNotSupportedException(UserModel.SearchableFields.IDP_AND_USER, op, "Invalid arguments, expected (idp_alias) or (idp_alias, idp_user), got: " + Arrays.toString(values));
        }

        final Object idpAlias = values[0];
        if (values.length == 1) {
            return IckleQueryOperators.combineExpressions(op, modelFieldName + ".identityProvider", values, parameters);
        } else if (idpAlias == null) {
            final Object idpUserId = values[1];
            return IckleQueryOperators.combineExpressions(op, modelFieldName + ".userId", new Object[] { idpUserId }, parameters);
        } else {
            final Object idpUserId = values[1];
            // Clause for searching federated identity id
            String idClause = IckleQueryOperators.combineExpressions(op, modelFieldName + ".identityProvider", new Object[]{ idpAlias }, parameters);
            // Clause for searching federated identity userId
            String userIdClause = IckleQueryOperators.combineExpressions(op, modelFieldName + ".userId", new Object[] { idpUserId }, parameters);

            return "(" + idClause + ")" + " AND " + "(" + userIdClause + ")";
        }
    }

    private static String whereClauseForConsentClientFederationLink(String modelFieldName, ModelCriteriaBuilder.Operator op, Object[] values, Map<String, Object> parameters) {
        if (op != ModelCriteriaBuilder.Operator.EQ) {
            throw new CriterionNotSupportedException(UserModel.SearchableFields.CONSENT_CLIENT_FEDERATION_LINK, op);
        }
        if (values == null || values.length != 1) {
            throw new CriterionNotSupportedException(UserModel.SearchableFields.CONSENT_CLIENT_FEDERATION_LINK, op, "Invalid arguments, expected (federation_provider_id), got: " + Arrays.toString(values));
        }

        String providerId = new StorageId((String) values[0], "").getId();
        return IckleQueryOperators.combineExpressions(ModelCriteriaBuilder.Operator.LIKE, getFieldName(UserModel.SearchableFields.CONSENT_FOR_CLIENT), new String[] {providerId + "%"}, parameters);
    }

    private static String whereClauseForCorrespondingSessionId(String modelFieldName, ModelCriteriaBuilder.Operator op, Object[] values, Map<String, Object> parameters) {
        if (op != ModelCriteriaBuilder.Operator.EQ) {
            throw new CriterionNotSupportedException(UserSessionModel.SearchableFields.CORRESPONDING_SESSION_ID, op);
        }
        if (values == null || values.length != 1) {
            throw new CriterionNotSupportedException(UserSessionModel.SearchableFields.CORRESPONDING_SESSION_ID, op, "Invalid arguments, expected (corresponding_session:id), got: " + Arrays.toString(values));
        }

        // Clause for searching key
        String nameClause = IckleQueryOperators.combineExpressions(op, "notes.key", new String[]{UserSessionModel.CORRESPONDING_SESSION_ID}, parameters);
        // Clause for searching value
        String valueClause = IckleQueryOperators.combineExpressions(op, "notes.value", values, parameters);

        return "(" + nameClause + ")" + " AND " + "(" + valueClause + ")";
    }

    private static String whereClauseForPolicyConfig(String modelFieldName, ModelCriteriaBuilder.Operator op, Object[] values, Map<String, Object> parameters) {
        if (values == null || values.length == 0) {
            throw new CriterionNotSupportedException(Policy.SearchableFields.CONFIG, op, "Invalid arguments, expected (config_name, config_value_operator_arguments), got: " + Arrays.toString(values));
        }

        final Object attrName = values[0];
        if (!(attrName instanceof String)) {
            throw new CriterionNotSupportedException(Policy.SearchableFields.CONFIG, op, "Invalid arguments, expected (String config_name), got: " + Arrays.toString(values));
        }

        String attrNameS = (String) attrName;
        Object[] realValues = new Object[values.length - 1];
        System.arraycopy(values, 1, realValues, 0, values.length - 1);

        boolean isNotExists = op.equals(ModelCriteriaBuilder.Operator.NOT_EXISTS);
        if (isNotExists || op.equals(ModelCriteriaBuilder.Operator.EXISTS)) {
            ModelCriteriaBuilder.Operator o = isNotExists ? ModelCriteriaBuilder.Operator.NE : ModelCriteriaBuilder.Operator.EQ;
            return IckleQueryOperators.combineExpressions(o, modelFieldName + ".key", new String[] { attrNameS }, parameters);
        }

        String nameClause = IckleQueryOperators.combineExpressions(ModelCriteriaBuilder.Operator.EQ, modelFieldName + ".key", new String[] { attrNameS }, parameters);

        if (realValues.length == 0) {
            return nameClause;
        }

        String valueClause = IckleQueryOperators.combineExpressions(op, modelFieldName + ".value", realValues, parameters);
        return "(" + nameClause + ")" + " AND " + "(" + valueClause + ")";
    }

    private static String whereClauseForEnumWithStableIndex(String modelFieldName, ModelCriteriaBuilder.Operator op, Object[] values, Map<String, Object> parameters) {
        if (values != null && values.length == 1) {
            if (values[0] instanceof EnumWithStableIndex) {
                values[0] = ((EnumWithStableIndex) values[0]).getStableIndex();
            } else if (values[0] instanceof Collection) {
                values[0] = ((Collection<EnumWithStableIndex>) values[0]).stream().map(EnumWithStableIndex::getStableIndex).collect(Collectors.toSet());
            } else if (values[0] instanceof Stream) {
                values[0] = ((Stream<EnumWithStableIndex>) values[0]).map(EnumWithStableIndex::getStableIndex);
            }
        }

        return produceWhereClause(modelFieldName, op, values, parameters);
    }
}

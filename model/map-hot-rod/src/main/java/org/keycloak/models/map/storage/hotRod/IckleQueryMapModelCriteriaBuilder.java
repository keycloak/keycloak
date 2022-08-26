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
import org.keycloak.authorization.model.Resource;
import org.keycloak.events.Event;
import org.keycloak.models.AuthenticatedClientSessionModel;
import org.keycloak.models.ClientModel;
import org.keycloak.models.GroupModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.models.map.storage.ModelCriteriaBuilder;
import org.keycloak.models.map.storage.hotRod.common.AbstractHotRodEntity;
import org.keycloak.storage.SearchableModelField;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.UnaryOperator;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.keycloak.models.map.storage.hotRod.IckleQueryOperators.C;
import static org.keycloak.models.map.storage.hotRod.IckleQueryOperators.findAvailableNamedParam;
import static org.keycloak.models.map.storage.hotRod.common.CommonPrimitivesProtoSchemaInitializer.HOT_ROD_ENTITY_PACKAGE;

public class IckleQueryMapModelCriteriaBuilder<E extends AbstractHotRodEntity, M> implements ModelCriteriaBuilder<M, IckleQueryMapModelCriteriaBuilder<E, M>> {

    private static final int INITIAL_BUILDER_CAPACITY = 250;
    private final Class<E> hotRodEntityClass;
    private final StringBuilder whereClauseBuilder = new StringBuilder(INITIAL_BUILDER_CAPACITY);
    private final Map<String, Object> parameters;
    private static final Pattern LIKE_PATTERN_DELIMITER = Pattern.compile("%+");
    private static final Pattern NON_ANALYZED_FIELD_REGEX = Pattern.compile("[%_\\\\]");
    // private static final Pattern ANALYZED_FIELD_REGEX = Pattern.compile("[+!^\"~*?:\\\\]"); // TODO reevaluate once https://github.com/keycloak/keycloak/issues/9295 is fixed
    private static final Pattern ANALYZED_FIELD_REGEX = Pattern.compile("\\\\"); // escape "\" with extra "\"
    public static final Map<SearchableModelField<?>, String> INFINISPAN_NAME_OVERRIDES = new HashMap<>();
    public static final Set<SearchableModelField<?>> ANALYZED_MODEL_FIELDS = new HashSet<>();


    static {
        INFINISPAN_NAME_OVERRIDES.put(ClientModel.SearchableFields.SCOPE_MAPPING_ROLE, "scopeMappings");
        INFINISPAN_NAME_OVERRIDES.put(ClientModel.SearchableFields.ATTRIBUTE, "attributes");

        INFINISPAN_NAME_OVERRIDES.put(GroupModel.SearchableFields.PARENT_ID, "parentId");
        INFINISPAN_NAME_OVERRIDES.put(GroupModel.SearchableFields.ASSIGNED_ROLE, "grantedRoles");

        INFINISPAN_NAME_OVERRIDES.put(RoleModel.SearchableFields.IS_CLIENT_ROLE, "clientRole");

        INFINISPAN_NAME_OVERRIDES.put(UserModel.SearchableFields.SERVICE_ACCOUNT_CLIENT, "serviceAccountClientLink");
        INFINISPAN_NAME_OVERRIDES.put(UserModel.SearchableFields.CONSENT_FOR_CLIENT, "userConsents.clientId");
        INFINISPAN_NAME_OVERRIDES.put(UserModel.SearchableFields.CONSENT_WITH_CLIENT_SCOPE, "userConsents.grantedClientScopesIds");
        INFINISPAN_NAME_OVERRIDES.put(UserModel.SearchableFields.ASSIGNED_ROLE, "rolesMembership");
        INFINISPAN_NAME_OVERRIDES.put(UserModel.SearchableFields.ASSIGNED_GROUP, "groupsMembership");
        INFINISPAN_NAME_OVERRIDES.put(UserModel.SearchableFields.ATTRIBUTE, "attributes");
        INFINISPAN_NAME_OVERRIDES.put(UserModel.SearchableFields.IDP_AND_USER, "federatedIdentities");

        INFINISPAN_NAME_OVERRIDES.put(RealmModel.SearchableFields.CLIENT_INITIAL_ACCESS, "clientInitialAccesses");
        INFINISPAN_NAME_OVERRIDES.put(RealmModel.SearchableFields.COMPONENT_PROVIDER_TYPE, "components.providerType");

        INFINISPAN_NAME_OVERRIDES.put(UserSessionModel.SearchableFields.IS_OFFLINE, "offline");
        INFINISPAN_NAME_OVERRIDES.put(UserSessionModel.SearchableFields.CLIENT_ID, "authenticatedClientSessions.clientId");

        INFINISPAN_NAME_OVERRIDES.put(Resource.SearchableFields.SCOPE_ID, "scopeIds");

        INFINISPAN_NAME_OVERRIDES.put(Policy.SearchableFields.RESOURCE_ID, "resourceIds");
        INFINISPAN_NAME_OVERRIDES.put(Policy.SearchableFields.SCOPE_ID, "scopeIds");
        INFINISPAN_NAME_OVERRIDES.put(Policy.SearchableFields.ASSOCIATED_POLICY_ID, "associatedPolicyIds");
        INFINISPAN_NAME_OVERRIDES.put(Policy.SearchableFields.CONFIG, "configs");

        INFINISPAN_NAME_OVERRIDES.put(Event.SearchableFields.EVENT_TYPE, "type");
    }

    static {
        // the "filename" analyzer in Infinispan works correctly for case-insensitive search with whitespaces
        ANALYZED_MODEL_FIELDS.add(RoleModel.SearchableFields.DESCRIPTION);
        ANALYZED_MODEL_FIELDS.add(UserModel.SearchableFields.FIRST_NAME);
        ANALYZED_MODEL_FIELDS.add(UserModel.SearchableFields.LAST_NAME);
        ANALYZED_MODEL_FIELDS.add(UserModel.SearchableFields.EMAIL);
        ANALYZED_MODEL_FIELDS.add(Policy.SearchableFields.TYPE);
        ANALYZED_MODEL_FIELDS.add(Resource.SearchableFields.TYPE);
    }

    public IckleQueryMapModelCriteriaBuilder(Class<E> hotRodEntityClass, StringBuilder whereClauseBuilder, Map<String, Object> parameters) {
        this.hotRodEntityClass = hotRodEntityClass;
        this.whereClauseBuilder.append(whereClauseBuilder);
        this.parameters = parameters;
    }

    public IckleQueryMapModelCriteriaBuilder(Class<E> hotRodEntityClass) {
        this.hotRodEntityClass = hotRodEntityClass;
        this.parameters = new HashMap<>();
    }

    public static String getFieldName(SearchableModelField<?> modelField) {
        return INFINISPAN_NAME_OVERRIDES.getOrDefault(modelField, modelField.getName());
    }

    private static boolean notEmpty(StringBuilder builder) {
        return builder.length() != 0;
    }

    @Override
    public IckleQueryMapModelCriteriaBuilder<E, M> compare(SearchableModelField<? super M> modelField, Operator op, Object... value) {
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

        return new IckleQueryMapModelCriteriaBuilder<>(hotRodEntityClass, newBuilder.append(")"), newParameters);
    }

    private StringBuilder joinBuilders(IckleQueryMapModelCriteriaBuilder<E, M>[] builders, String delimiter) {
        return new StringBuilder(INITIAL_BUILDER_CAPACITY).append("(").append(Arrays.stream(builders)
                .map(IckleQueryMapModelCriteriaBuilder::getWhereClauseBuilder)
                .filter(IckleQueryMapModelCriteriaBuilder::notEmpty)
                .collect(Collectors.joining(delimiter))).append(")");
    }

    private Map<String, Object> joinParameters(IckleQueryMapModelCriteriaBuilder<E, M>[] builders) {
        return Arrays.stream(builders)
                .map(IckleQueryMapModelCriteriaBuilder::getParameters)
                .map(Map::entrySet)
                .flatMap(Collection::stream)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    @SuppressWarnings("unchecked")
    private IckleQueryMapModelCriteriaBuilder<E, M>[] resolveNamedQueryConflicts(IckleQueryMapModelCriteriaBuilder<E, M>[] builders) {
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

           return new IckleQueryMapModelCriteriaBuilder<>(hotRodEntityClass, new StringBuilder(newWhereClause), newParameters);
        }).toArray(IckleQueryMapModelCriteriaBuilder[]::new);
    }

    @Override
    public IckleQueryMapModelCriteriaBuilder<E, M> and(IckleQueryMapModelCriteriaBuilder<E, M>... builders) {
        if (builders.length == 0) {
            return new IckleQueryMapModelCriteriaBuilder<>(hotRodEntityClass);
        }

        builders = resolveNamedQueryConflicts(builders);

        return new IckleQueryMapModelCriteriaBuilder<>(hotRodEntityClass, joinBuilders(builders, " AND "),
                joinParameters(builders));
    }

    @Override
    public IckleQueryMapModelCriteriaBuilder<E, M> or(IckleQueryMapModelCriteriaBuilder<E, M>... builders) {
        if (builders.length == 0) {
            return new IckleQueryMapModelCriteriaBuilder<>(hotRodEntityClass);
        }

        builders = resolveNamedQueryConflicts(builders);

        return new IckleQueryMapModelCriteriaBuilder<>(hotRodEntityClass, joinBuilders(builders, " OR "),
                joinParameters(builders));
    }

    @Override
    public IckleQueryMapModelCriteriaBuilder<E, M> not(IckleQueryMapModelCriteriaBuilder<E, M> builder) {
        StringBuilder newBuilder = new StringBuilder(INITIAL_BUILDER_CAPACITY);
        StringBuilder originalBuilder = builder.getWhereClauseBuilder();

        if (originalBuilder.length() != 0) {
            newBuilder.append("not").append(originalBuilder);
        }

        return new IckleQueryMapModelCriteriaBuilder<>(hotRodEntityClass, newBuilder, builder.getParameters());
    }

    private StringBuilder getWhereClauseBuilder() {
        return whereClauseBuilder;
    }

    public static Object sanitizeNonAnalyzed(Object value) {
        if (value instanceof String) {
            return sanitizeEachUnitAndReplaceDelimiter((String) value, IckleQueryMapModelCriteriaBuilder::sanitizeSingleUnitNonAnalyzed, "%");
        }

        return value;
    }

    public static Object sanitizeAnalyzed(Object value) {
        if (value instanceof String) {
            return sanitizeEachUnitAndReplaceDelimiter((String) value, IckleQueryMapModelCriteriaBuilder::sanitizeSingleUnitAnalyzed, "*");
        }

        return value;
    }

    private static String sanitizeEachUnitAndReplaceDelimiter(String value, UnaryOperator<String> sanitizeSingleUnit, String replacement) {
        return LIKE_PATTERN_DELIMITER.splitAsStream(value)
                .map(sanitizeSingleUnit)
                .collect(Collectors.joining(replacement))
                + (value.endsWith("%") ? replacement : "");
    }

    private static String sanitizeSingleUnitNonAnalyzed(String value) {
        return NON_ANALYZED_FIELD_REGEX.matcher(value).replaceAll("\\\\\\\\" + "$0");
    }

    private static String sanitizeSingleUnitAnalyzed(String value) {
        return ANALYZED_FIELD_REGEX.matcher(value).replaceAll("\\\\\\\\"); // escape "\" with extra "\"
        //      .replaceAll("\\\\\\\\" + "$0"); skipped for now because Infinispan is not able to escape
        //      special characters for analyzed fields
        //      TODO reevaluate once https://github.com/keycloak/keycloak/issues/9295 is fixed
    }


    public static boolean isAnalyzedModelField(SearchableModelField<?> modelField) {
        return ANALYZED_MODEL_FIELDS.contains(modelField);
    }

    /**
     *
     * @return Ickle query that represents this QueryBuilder
     */
    public String getIckleQuery() {
        return "FROM " + HOT_ROD_ENTITY_PACKAGE + "." + hotRodEntityClass.getSimpleName() + " " + C + ((whereClauseBuilder.length() != 0) ? " WHERE " + whereClauseBuilder : "");
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

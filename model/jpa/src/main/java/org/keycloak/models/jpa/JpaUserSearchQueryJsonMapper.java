/*
 * Copyright 2020 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.models.jpa;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaBuilder.In;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.JoinType;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import org.keycloak.models.jpa.entities.UserEntity;
import org.keycloak.models.search.SearchQueryJson;
import org.keycloak.models.search.SearchQueryJsonAnd;
import org.keycloak.models.search.SearchQueryJsonEquals;
import org.keycloak.models.search.SearchQueryJsonGt;
import org.keycloak.models.search.SearchQueryJsonGte;
import org.keycloak.models.search.SearchQueryJsonIn;
import org.keycloak.models.search.SearchQueryJsonLike;
import org.keycloak.models.search.SearchQueryJsonLt;
import org.keycloak.models.search.SearchQueryJsonLte;
import org.keycloak.models.search.SearchQueryJsonNot;
import org.keycloak.models.search.SearchQueryJsonOr;
import org.keycloak.models.ModelException;
import org.keycloak.models.UserModel;


/**
 * @author <a href="mailto:niannarilli@efalia.com">Nicolas iannarilli</a>
 * @version $Revision: 1 $
 */
public class JpaUserSearchQueryJsonMapper {
    private static final String EMAIL = "email";
    private static final String EMAIL_VERIFIED = "emailVerified";
    private static final String USERNAME = "username";
    private static final String FIRST_NAME = "firstName";
    private static final String LAST_NAME = "lastName";

    private JpaUserSearchQueryJsonMapper() {}
    
    public static Predicate build (Root<UserEntity> root, CriteriaBuilder builder, SearchQueryJson query) {
        Map<String, Join<Object, Object>> joinByKey = new HashMap<>();
        
        return buildCondition(root, builder, joinByKey, query);
    }

    /** Build predicate from search json query. */
    private static Predicate buildCondition(Root<UserEntity> root, CriteriaBuilder builder, Map<String, Join<Object, Object>> joinByKey, SearchQueryJson query){
        switch (query.getOperator()) {
            case AND:
                SearchQueryJsonAnd queryAnd = (SearchQueryJsonAnd) query;
                return builder.and(queryAnd.getValues().stream().map(c -> JpaUserSearchQueryJsonMapper.buildCondition(root, builder, joinByKey, c)).toArray(Predicate[]::new));
            case OR:
                SearchQueryJsonOr queryOr = (SearchQueryJsonOr) query;
                return builder.or(queryOr.getValues().stream().map(c -> JpaUserSearchQueryJsonMapper.buildCondition(root, builder, joinByKey, c)).toArray(Predicate[]::new));
            case IN:
                SearchQueryJsonIn queryIn = (SearchQueryJsonIn) query;
                return JpaUserSearchQueryJsonMapper.<List<String>>buildConditionAttribute(builder, root, joinByKey, queryIn.getProperty(), (k, v) -> {
                        In<String> inClause = builder.in(builder.lower(root.get(queryIn.getProperty())));
                        v.stream().map(String::toLowerCase).forEach(inClause::value);
                        return inClause;
                    })
                    .apply(queryIn.getProperty(), queryIn.getValues());
            case NOT:
                SearchQueryJsonNot queryNot = (SearchQueryJsonNot) query;
                return builder.not(buildCondition(root, builder, joinByKey, queryNot));
            case EQUALS:
                SearchQueryJsonEquals queryEquals = (SearchQueryJsonEquals) query;
                return JpaUserSearchQueryJsonMapper.<String>buildConditionAttribute(builder, root, joinByKey, queryEquals.getProperty(), (k, v) -> builder.equal(builder.lower(k), v.toLowerCase()))
                    .apply(queryEquals.getProperty(), queryEquals.getValue());
            case LIKE:
                SearchQueryJsonLike queryLike = (SearchQueryJsonLike) query;
                return JpaUserSearchQueryJsonMapper.<String>buildConditionAttribute(builder, root, joinByKey, queryLike.getProperty(), (k, v) -> builder.like(builder.lower(k), v.toLowerCase()))
                    .apply(queryLike.getProperty(), queryLike.getValue().replaceAll("*", "%"));
            case GT:
                SearchQueryJsonGt queryGt = (SearchQueryJsonGt) query;
                return JpaUserSearchQueryJsonMapper.<String>buildConditionAttribute(builder, root, joinByKey, queryGt.getProperty(), (k, v) -> builder.greaterThan(builder.lower(k), v))
                    .apply(queryGt.getProperty(), queryGt.getValue());
            case GTE:
                SearchQueryJsonGte queryGte = (SearchQueryJsonGte) query;
                return JpaUserSearchQueryJsonMapper.<String>buildConditionAttribute(builder, root, joinByKey, queryGte.getProperty(), (k, v) -> builder.greaterThanOrEqualTo(builder.lower(k), v))
                    .apply(queryGte.getProperty(), queryGte.getValue());
            case LT:
                SearchQueryJsonLt queryLt = (SearchQueryJsonLt) query;
                return JpaUserSearchQueryJsonMapper.<String>buildConditionAttribute(builder, root, joinByKey, queryLt.getProperty(), (k, v) -> builder.lessThan(builder.lower(k), v))
                    .apply(queryLt.getProperty(), queryLt.getValue());
            case LTE:
                SearchQueryJsonLte queryLte = (SearchQueryJsonLte) query;
                return JpaUserSearchQueryJsonMapper.<String>buildConditionAttribute(builder, root, joinByKey, queryLte.getProperty(), (k, v) -> builder.lessThanOrEqualTo(builder.lower(k), v))
                    .apply(queryLte.getProperty(), queryLte.getValue());
            default:
                throw new ModelException("No implementation to build condition available for " + query.getOperator().name());
        }
    }

    /** Build condition with join if needed. */
    private static <T> BiFunction<String, T, Predicate> buildConditionAttribute(CriteriaBuilder builder, Root<UserEntity> root, Map<String, Join<Object, Object>> joinByKey, String property, BiFunction<Expression<String>, T, Predicate> condition) {
        switch (property) {
            case FIRST_NAME:
            case LAST_NAME:
            case USERNAME:
            case EMAIL:
                return (k, v) -> condition.apply(root.get(k), v);
            case EMAIL_VERIFIED:
            case UserModel.ENABLED:
                return (k, v) -> builder.equal(root.get(k), Boolean.parseBoolean(v.toString()));
            case UserModel.IDP_ALIAS:
                return (k, v) -> builder.equal(joinByKey.computeIfAbsent("federatedIdentities", m -> root.join("federatedIdentities"))
                    .get("identityProvider"), v);
            case UserModel.IDP_USER_ID:
                return (k, v) -> builder.equal(joinByKey.computeIfAbsent("federatedIdentities", m -> root.join("federatedIdentities"))
                    .get("userId"), v);
            default:
                return (k, v) -> (builder.and(
                    builder.equal(builder.lower(joinByKey.computeIfAbsent("attributes", m -> root.join("attributes", JoinType.LEFT)).get("name")), k.toLowerCase()),
                    condition.apply(builder.lower(joinByKey.get("attributes").get("value")), v)));
        }
    }
}
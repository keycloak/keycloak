/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2025 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.keycloak.authorization.policy.provider;

import java.util.List;
import java.util.Set;

import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import org.keycloak.representations.idm.authorization.ResourceType;

/**
 * If a realm has the {@link org.keycloak.common.Profile.Feature#ADMIN_FINE_GRAINED_AUTHZ} feature enabled,
 * realm resources storage providers can implement this interface if they want to apply custom predicates to queries
 * to filter their results based on the permissions available from the realm.
 */
public interface PartialEvaluationStorageProvider {

    /**
     * A callback method that will be called when building queries for realm resources to grant access to resources. It returns a list of
     * {@link Predicate} instances representing the filters that should be applied to queries
     * when querying realm resources.
     *
     * @param evaluationContext the evaluation context.
     * @return the list of predicates
     */
    List<Predicate> getFilters(EvaluationContext evaluationContext);

    /**
     * A callback method that will be called when building queries for realm resources to deny access to resources. It returns a list of
     * {@link Predicate} instances representing the filters that should be applied to queries
     * when querying realm resources.
     *
     * @param evaluationContext the evaluation context.
     * @return the list of predicates
     */
    List<Predicate> getNegateFilters(EvaluationContext evaluationContext);

    /**
     * An {@link EvaluationContext} instance provides access to contextual information when building a query for realm
     * resources of a given {@link ResourceType}.
     *
     * @param resourceType the type of the resource to query
     * @param criteriaQuery the query to rely on when building predicates
     * @param path the path for the root entity
     */
    record EvaluationContext(ResourceType resourceType, CriteriaQuery<?> criteriaQuery, Path<?> path, Set<String> allowedGroupIds, Set<String> deniedGroupIds) {
    }
}

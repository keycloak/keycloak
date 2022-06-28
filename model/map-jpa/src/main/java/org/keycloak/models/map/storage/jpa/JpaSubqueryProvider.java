/*
 * Copyright 2022 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.models.map.storage.jpa;

import javax.persistence.criteria.Subquery;

/**
 * This is handed down to a {@link JpaModelCriteriaBuilder} to be able to create subqueries.
 * Depending on the caller this will delegate of an instance of a {@link javax.persistence.criteria.CriteriaDelete}
 * or a {@link javax.persistence.criteria.CriteriaQuery} as necessary.
 *
 * @author Alexander Schwartz
 */
@FunctionalInterface
public interface JpaSubqueryProvider {
    <U> Subquery<U> subquery(Class<U> type);
}

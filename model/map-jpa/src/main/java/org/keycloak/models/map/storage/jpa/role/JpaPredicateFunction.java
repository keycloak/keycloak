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

package org.keycloak.models.map.storage.jpa.role;

import org.keycloak.models.map.storage.jpa.JpaSubqueryProvider;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.util.Objects;
import java.util.function.Function;

/**
 * @author Alexander Schwartz
 */
@FunctionalInterface
public interface JpaPredicateFunction<RE> {
    Predicate apply(CriteriaBuilder t, JpaSubqueryProvider u, Root<RE> v);

    default JpaPredicateFunction<RE> andThen(Function<? super Predicate, Predicate> after) {
        Objects.requireNonNull(after);
        return (CriteriaBuilder t, JpaSubqueryProvider u, Root<RE> v) -> after.apply(apply(t, u, v));
    }
}

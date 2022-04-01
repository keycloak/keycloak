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

import com.fasterxml.jackson.core.JsonProcessingException;
import java.util.Arrays;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Stream;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import org.keycloak.models.map.storage.CriterionNotSupportedException;
import org.keycloak.models.map.storage.ModelCriteriaBuilder;
import org.keycloak.models.map.storage.jpa.hibernate.jsonb.JsonbType;
import org.keycloak.storage.SearchableModelField;

/**
 * Abstract class containing methods common to all Jpa*ModelCriteriaBuilder implementations
 * 
 * @param <E> Entity
 * @param <M> Model
 * @param <Self> specific implementation of this class
 */
public abstract class JpaModelCriteriaBuilder<E, M, Self extends JpaModelCriteriaBuilder<E, M, Self>> implements ModelCriteriaBuilder<M, Self> {

    private final Function<BiFunction<CriteriaBuilder, Root<E>, Predicate>, Self> instantiator;
    private BiFunction<CriteriaBuilder, Root<E>, Predicate> predicateFunc = null;

    public JpaModelCriteriaBuilder(Function<BiFunction<CriteriaBuilder, Root<E>, Predicate>, Self> instantiator) {
        this.instantiator = instantiator;
    }

    public JpaModelCriteriaBuilder(Function<BiFunction<CriteriaBuilder, Root<E>, Predicate>, Self> instantiator,
            BiFunction<CriteriaBuilder, Root<E>, Predicate> predicateFunc) {
        this.instantiator = instantiator;
        this.predicateFunc = predicateFunc;
    }

    protected void validateValue(Object[] value, SearchableModelField<? super M> field, ModelCriteriaBuilder.Operator op, Class<?>... expectedTypes) {
        if (value == null || expectedTypes == null || value.length != expectedTypes.length) {
            throw new CriterionNotSupportedException(field, op, "Invalid argument: " + Arrays.toString(value));
        }
        for (int i = 0; i < expectedTypes.length; i++) {
            if (! expectedTypes[i].isInstance(value[i])) {
                throw new CriterionNotSupportedException(field, op, "Expected types: " + Arrays.toString(expectedTypes) +
                        " but got: " + Arrays.toString(value));
            }
        }
    }

    protected String convertToJson(Object input) {
        try {
            return JsonbType.MAPPER.writeValueAsString(input);
        } catch (JsonProcessingException ex) {
            throw new RuntimeException("Unable to write value as String.", ex);
        }
    }

    @SafeVarargs
    @Override
    public final Self and(Self... builders) {
        return instantiator.apply((cb, root) -> cb.and(Stream.of(builders).map((Self b) -> b.getPredicateFunc().apply(cb, root)).toArray(Predicate[]::new)));
    }

    @SafeVarargs
    @Override
    public final Self or(Self... builders) {
        return instantiator.apply((cb, root) -> cb.or(Stream.of(builders).map((Self b) -> (b).getPredicateFunc().apply(cb, root)).toArray(Predicate[]::new)));
    }

    @Override
    public Self not(Self builder) {
        return instantiator.apply((cb, root) -> cb.not(builder.getPredicateFunc().apply(cb, root)));
    }

    public BiFunction<CriteriaBuilder, Root<E>, Predicate> getPredicateFunc() {
        return predicateFunc;
    }
}

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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.persistence.criteria.Predicate;

import org.keycloak.models.map.common.StringKeyConverter;
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

    private final Function<JpaPredicateFunction<E>, Self> instantiator;
    private JpaPredicateFunction<E> predicateFunc = null;
    private boolean isDistinct = false;

    public JpaModelCriteriaBuilder(Function<JpaPredicateFunction<E>, Self> instantiator) {
        this.instantiator = instantiator;
    }

    public JpaModelCriteriaBuilder(Function<JpaPredicateFunction<E>, Self> instantiator,
                                                                      JpaPredicateFunction<E> predicateFunc) {
        this.instantiator = instantiator;
        this.predicateFunc = predicateFunc;
    }

    public JpaModelCriteriaBuilder(Function<JpaPredicateFunction<E>, Self> instantiator,
                                   JpaPredicateFunction<E> predicateFunc,
                                   boolean isDistinct) {
        this.instantiator = instantiator;
        this.predicateFunc = predicateFunc;
        this.isDistinct = isDistinct;
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
        return instantiator.apply((cb, query, root) -> cb.and(Stream.of(builders).map((Self b) -> b.getPredicateFunc().apply(cb, query, root)).toArray(Predicate[]::new)));
    }

    @SafeVarargs
    @Override
    public final Self or(Self... builders) {
        return instantiator.apply((cb, query, root) -> cb.or(Stream.of(builders).map((Self b) -> (b).getPredicateFunc().apply(cb, query, root)).toArray(Predicate[]::new)));
    }

    @Override
    public Self not(Self builder) {
        return instantiator.apply((cb, query, root) -> cb.not(builder.getPredicateFunc().apply(cb, query, root)));
    }

    public JpaPredicateFunction<E> getPredicateFunc() {
        return predicateFunc;
    }

    public boolean isDistinct() {
        return this.isDistinct;
    }

    @SuppressWarnings("unchecked")
    protected Collection<?> getValuesForInOperator(Object[] values, SearchableModelField<?> modelField) {
        if (values == null || values.length == 0) throw new CriterionNotSupportedException(modelField, Operator.IN);

        Collection<?> collectionValues;
        if (values.length == 1) {

            if (values[0] instanceof Object[]) {
                collectionValues = Arrays.asList(values[0]);
            } else if (values[0] instanceof Collection) {
                collectionValues = (Collection) values[0];
            } else if (values[0] instanceof Stream) {
                try (Stream<?> str = ((Stream) values[0])) {
                    collectionValues = str.collect(Collectors.toCollection(ArrayList::new));
                }
            } else {
                collectionValues = Collections.singleton(values[0]);
            }

        } else  {
            collectionValues = new HashSet(Arrays.asList(values));
        }
        return collectionValues;
    }

    protected Set<UUID> getUuidsForInOperator(Object[] values, SearchableModelField<?> modelField) {
        return getValuesForInOperator(values, modelField).stream()
                    .map(val -> StringKeyConverter.UUIDKey.INSTANCE.fromStringSafe(Objects.toString(val, null)))
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());
    }
}

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
package org.keycloak.models.map.storage.chm;

import org.keycloak.models.map.common.AbstractEntity;
import org.keycloak.storage.SearchableModelField;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;
import org.keycloak.models.map.storage.ModelCriteriaBuilder;

/**
 *
 * @author hmlnarik
 */
public class MapModelCriteriaBuilder<K, V extends AbstractEntity<K>, M> implements ModelCriteriaBuilder<M> {

    @FunctionalInterface
    public static interface UpdatePredicatesFunc<K, V extends AbstractEntity<K>, M> {
        MapModelCriteriaBuilder<K, V, M> apply(MapModelCriteriaBuilder<K, V, M> builder, Operator op, Object[] params);
    }

    private static final Predicate<Object> ALWAYS_TRUE = (e) -> true;
    private static final Predicate<Object> ALWAYS_FALSE = (e) -> false;
    private final Predicate<? super K> keyFilter;
    private final Predicate<? super V> entityFilter;
    private final Map<SearchableModelField<M>, UpdatePredicatesFunc<K, V, M>> fieldPredicates;

    public MapModelCriteriaBuilder(Map<SearchableModelField<M>, UpdatePredicatesFunc<K, V, M>> fieldPredicates) {
        this(fieldPredicates, ALWAYS_TRUE, ALWAYS_TRUE);
    }

    private MapModelCriteriaBuilder(Map<SearchableModelField<M>, UpdatePredicatesFunc<K, V, M>> fieldPredicates, Predicate<? super K> indexReadFilter, Predicate<? super V> sequentialReadFilter) {
        this.fieldPredicates = fieldPredicates;
        this.keyFilter = indexReadFilter;
        this.entityFilter = sequentialReadFilter;
    }

    @Override
    public MapModelCriteriaBuilder<K, V, M> compare(SearchableModelField<M> modelField, Operator op, Object... values) {
        UpdatePredicatesFunc<K, V, M> method = fieldPredicates.get(modelField);
        if (method == null) {
            throw new IllegalArgumentException("Filter not implemented for field " + modelField);
        }

        return method.apply(this, op, values);
    }

    @SafeVarargs
    @SuppressWarnings("unchecked")
    @Override
    public final MapModelCriteriaBuilder<K, V, M> and(ModelCriteriaBuilder<M>... builders) {
        Predicate<? super K> resIndexFilter = Stream.of(builders).map(MapModelCriteriaBuilder.class::cast).map(MapModelCriteriaBuilder::getKeyFilter).reduce(keyFilter, Predicate::and);
        Predicate<V> resEntityFilter = Stream.of(builders).map(MapModelCriteriaBuilder.class::cast).map(MapModelCriteriaBuilder::getEntityFilter).reduce(entityFilter, Predicate::and);
        return new MapModelCriteriaBuilder<>(fieldPredicates, resIndexFilter, resEntityFilter);
    }

    @SafeVarargs
    @SuppressWarnings("unchecked")
    @Override
    public final MapModelCriteriaBuilder<K, V, M> or(ModelCriteriaBuilder<M>... builders) {
        Predicate<? super K> resIndexFilter = Stream.of(builders).map(MapModelCriteriaBuilder.class::cast).map(MapModelCriteriaBuilder::getKeyFilter).reduce(ALWAYS_FALSE, Predicate::or);
        Predicate<V> resEntityFilter = Stream.of(builders).map(MapModelCriteriaBuilder.class::cast).map(MapModelCriteriaBuilder::getEntityFilter).reduce(ALWAYS_FALSE, Predicate::or);
        return new MapModelCriteriaBuilder<>(
          fieldPredicates,
          v -> keyFilter.test(v) && resIndexFilter.test(v),
          v -> entityFilter.test(v) && resEntityFilter.test(v)
        );
    }

    @SuppressWarnings("unchecked")
    @Override
    public MapModelCriteriaBuilder<K, V, M> not(ModelCriteriaBuilder<M> builder) {
        MapModelCriteriaBuilder<K, V, M> b = builder.unwrap(MapModelCriteriaBuilder.class);
        if (b == null) {
            throw new ClassCastException("Incompatible class: " + builder.getClass());
        }
        Predicate<? super K> resIndexFilter = b.getKeyFilter() == ALWAYS_TRUE ? ALWAYS_TRUE : b.getKeyFilter().negate();
        Predicate<? super V> resEntityFilter = b.getEntityFilter() == ALWAYS_TRUE ? ALWAYS_TRUE : b.getEntityFilter().negate();

        return new MapModelCriteriaBuilder<>(
          fieldPredicates,
          v -> keyFilter.test(v) && resIndexFilter.test(v),
          v -> entityFilter.test(v) && resEntityFilter.test(v)
        );
    }

    public Predicate<? super K> getKeyFilter() {
        return keyFilter;
    }

    public Predicate<? super V> getEntityFilter() {
        return entityFilter;
    }

    protected MapModelCriteriaBuilder<K, V, M> idCompare(Operator op, Object[] values) {

        switch (op) {
            case LT:
            case LE:
            case GT:
            case GE:
            case EQ:
            case NE:
            case EXISTS:
            case NOT_EXISTS:
            case IN:
                return new MapModelCriteriaBuilder<>(fieldPredicates, this.keyFilter.and(CriteriaOperator.predicateFor(op, values)), this.entityFilter);
            default:
                throw new AssertionError("Invalid operator: " + op);
        }
    }

    protected MapModelCriteriaBuilder<K, V, M> fieldCompare(Operator op, Function<V, ?> getter, Object[] values) {
        Predicate<Object> valueComparator = CriteriaOperator.predicateFor(op, values);
        return fieldCompare(valueComparator, getter);
    }

    protected MapModelCriteriaBuilder<K, V, M> fieldCompare(Predicate<Object> valueComparator, Function<V, ?> getter) {
        final Predicate<? super V> resEntityFilter;
        if (entityFilter == ALWAYS_FALSE) {
            resEntityFilter = ALWAYS_FALSE;
        } else {
            final Predicate<V> p = v -> valueComparator.test(getter.apply(v));
            resEntityFilter = p.and(entityFilter);
        }
        return new MapModelCriteriaBuilder<>(fieldPredicates, this.keyFilter, resEntityFilter);
    }
}

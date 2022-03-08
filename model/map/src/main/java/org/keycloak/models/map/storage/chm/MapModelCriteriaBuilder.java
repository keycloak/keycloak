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

import org.keycloak.models.map.common.StringKeyConverter;
import org.keycloak.models.map.common.AbstractEntity;
import org.keycloak.storage.SearchableModelField;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;
import org.keycloak.models.map.storage.ModelCriteriaBuilder;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 *
 * @author hmlnarik
 */
public class MapModelCriteriaBuilder<K, V extends AbstractEntity, M> implements ModelCriteriaBuilder<M, MapModelCriteriaBuilder<K, V, M>> {

    @FunctionalInterface
    public interface UpdatePredicatesFunc<K, V extends AbstractEntity, M> {
        MapModelCriteriaBuilder<K, V, M> apply(MapModelCriteriaBuilder<K, V, M> builder, Operator op, Object[] params);
    }

    protected static final Predicate<Object> ALWAYS_TRUE = (e) -> true;
    protected static final Predicate<Object> ALWAYS_FALSE = (e) -> false;
    private final Predicate<? super K> keyFilter;
    private final Predicate<? super V> entityFilter;
    private final Map<SearchableModelField<? super M>, UpdatePredicatesFunc<K, V, M>> fieldPredicates;
    private final StringKeyConverter<K> keyConverter;

    public MapModelCriteriaBuilder(StringKeyConverter<K> keyConverter, Map<SearchableModelField<? super M>, UpdatePredicatesFunc<K, V, M>> fieldPredicates) {
        this(keyConverter, fieldPredicates, ALWAYS_TRUE, ALWAYS_TRUE);
    }

    protected MapModelCriteriaBuilder(StringKeyConverter<K> keyConverter, Map<SearchableModelField<? super M>, UpdatePredicatesFunc<K, V, M>> fieldPredicates, Predicate<? super K> indexReadFilter, Predicate<? super V> sequentialReadFilter) {
        this.keyConverter = keyConverter;
        this.fieldPredicates = fieldPredicates;
        this.keyFilter = indexReadFilter;
        this.entityFilter = sequentialReadFilter;
    }

    @Override
    public MapModelCriteriaBuilder<K, V, M> compare(SearchableModelField<? super M> modelField, Operator op, Object... values) {
        UpdatePredicatesFunc<K, V, M> method = fieldPredicates.get(modelField);
        if (method == null) {
            throw new IllegalArgumentException("Filter not implemented for field " + modelField);
        }

        return method.apply(this, op, values);
    }

    @SafeVarargs
    @SuppressWarnings("unchecked")
    @Override
    public final MapModelCriteriaBuilder<K, V, M> and(MapModelCriteriaBuilder<K, V, M>... builders) {
        Predicate<? super K> resIndexFilter = Stream.of(builders).map(MapModelCriteriaBuilder.class::cast).map(MapModelCriteriaBuilder::getKeyFilter).reduce(keyFilter, Predicate::and);
        Predicate<V> resEntityFilter = Stream.of(builders).map(MapModelCriteriaBuilder.class::cast).map(MapModelCriteriaBuilder::getEntityFilter).reduce(entityFilter, Predicate::and);
        return instantiateNewInstance(keyConverter, fieldPredicates, resIndexFilter, resEntityFilter);
    }

    @SafeVarargs
    @SuppressWarnings("unchecked")
    @Override
    public final MapModelCriteriaBuilder<K, V, M> or(MapModelCriteriaBuilder<K, V, M>... builders) {
        Predicate<? super K> resIndexFilter = Stream.of(builders).map(MapModelCriteriaBuilder.class::cast).map(MapModelCriteriaBuilder::getKeyFilter).reduce(ALWAYS_FALSE, Predicate::or);
        Predicate<V> resEntityFilter = Stream.of(builders).map(MapModelCriteriaBuilder.class::cast).map(MapModelCriteriaBuilder::getEntityFilter).reduce(ALWAYS_FALSE, Predicate::or);
        return instantiateNewInstance(
                keyConverter,
          fieldPredicates,
          v -> keyFilter.test(v) && resIndexFilter.test(v),
          v -> entityFilter.test(v) && resEntityFilter.test(v)
        );
    }

    @Override
    public MapModelCriteriaBuilder<K, V, M> not(MapModelCriteriaBuilder<K, V, M> builder) {
        Predicate<? super K> resIndexFilter = builder.getKeyFilter() == ALWAYS_TRUE ? ALWAYS_TRUE : builder.getKeyFilter().negate();
        Predicate<? super V> resEntityFilter = builder.getEntityFilter() == ALWAYS_TRUE ? ALWAYS_TRUE : builder.getEntityFilter().negate();

        return instantiateNewInstance(
                keyConverter,
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
        Object[] convertedValues = convertValuesToKeyType(values);
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
                return instantiateNewInstance(keyConverter, fieldPredicates, this.keyFilter.and(CriteriaOperator.predicateFor(op, convertedValues)), this.entityFilter);
            default:
                throw new AssertionError("Invalid operator: " + op);
        }
    }

    protected Object[] convertValuesToKeyType(Object[] values) {
        if (values == null) {
            return null;
        }
        Object[] res = new Object[values.length];
        for (int i = 0; i < values.length; i ++) {
            Object v = values[i];
            if (v instanceof String) {
                res[i] = keyConverter.fromStringSafe((String) v);
            } else if (v instanceof Stream) {
                res[i] = ((Stream<?>) v).map(o -> (o instanceof String) ? keyConverter.fromStringSafe((String) o) : o);
            } else if (v instanceof Collection) {
                res[i] = ((List<?>) v).stream().map(o -> (o instanceof String) ? keyConverter.fromStringSafe((String) o) : o).collect(Collectors.toList());
            } else if (v == null) {
                res[i] = null;
            } else {
                throw new IllegalArgumentException("Unknown type: " + v);
            }
        }
        return res;
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
        return instantiateNewInstance(keyConverter, fieldPredicates, this.keyFilter, resEntityFilter);
    }

    /**
     * Return a new instance for nodes in this criteria tree.
     *
     * Subclasses can override this method to instantiate a new instance of their subclass. This allows this class to
     * be extendable.
     */
    protected MapModelCriteriaBuilder<K, V, M> instantiateNewInstance(StringKeyConverter<K> keyConverter, Map<SearchableModelField<? super M>, UpdatePredicatesFunc<K, V, M>> fieldPredicates, Predicate<? super K> indexReadFilter, Predicate<? super V> sequentialReadFilter) {
        return new MapModelCriteriaBuilder<>(keyConverter, fieldPredicates, indexReadFilter, sequentialReadFilter);
    }
}

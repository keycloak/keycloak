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
package org.keycloak.models.map.common.delegate;

import org.keycloak.models.map.common.AbstractEntity;
import org.keycloak.models.map.common.EntityField;
import org.keycloak.models.map.common.UpdatableEntity;
import org.keycloak.models.map.storage.tree.TreeStorageNodeInstance;
import org.keycloak.models.map.storage.tree.TreeStorageNodePrescription.FieldContainedStatus;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 *
 * @author hmlnarik
 */
public class PerFieldDelegateProvider<V extends AbstractEntity> implements EntityFieldDelegate<V> {

    private final TreeStorageNodeInstance<V> node;

    private final V entity;

    private final LazilyInitialized<V> lowerEntity;

    public PerFieldDelegateProvider(TreeStorageNodeInstance<V> node, V entity, Supplier<V> fallbackProvider) {
        this.node = node;
        this.entity = entity;
        this.lowerEntity = new LazilyInitialized<>(fallbackProvider);
    }

    public PerFieldDelegateProvider(TreeStorageNodeInstance<V>.WithEntity nodeWithEntity, Supplier<V> fallbackProvider) {
        this(nodeWithEntity.getNode(), nodeWithEntity.getEntity(), fallbackProvider);
    }

    private V getEntityFromDescendantNode() {
        final V res = lowerEntity.get();
        Objects.requireNonNull(res, () -> "Descendant entity not found for node " + node);
        return res;
    }

    @Override
    public <K, EF extends Enum<? extends EntityField<V>> & EntityField<V>> Object mapRemove(EF field, K key) {
        Objects.requireNonNull(key, "Key must not be null");
        boolean needsSetEntity = false;
        boolean needsSetLowerEntity = false;

        switch (node.isCacheFor(field, key)) {
            case FULLY:
                needsSetEntity = true;
                break;

            case NOT_CONTAINED:
                needsSetLowerEntity = true;
                break;
        }

        switch (node.isPrimarySourceFor(field, key)) {
            case FULLY:
                needsSetEntity = true;
                needsSetLowerEntity = false;
                break;

            case NOT_CONTAINED:
                needsSetLowerEntity = true;
                break;
        }

        Object res = null;
        if (needsSetEntity) {
            res = field.mapRemove(entity, key);
        }
        if (needsSetLowerEntity) {
            res = field.mapRemove(getEntityFromDescendantNode(), key);
        }

        return res;
    }

    @Override
    public <K, T, EF extends Enum<? extends EntityField<V>> & EntityField<V>> void mapPut(EF field, K key, T value) {
        Objects.requireNonNull(key, "Key must not be null");
        boolean needsSetEntity = false;
        boolean needsSetLowerEntity = false;

        switch (node.isCacheFor(field, key)) {
            case FULLY:
                needsSetEntity = true;
                break;

            case NOT_CONTAINED:
                needsSetLowerEntity = true;
                break;
        }

        switch (node.isPrimarySourceFor(field, key)) {
            case FULLY:
                needsSetEntity = true;
                needsSetLowerEntity = false;
                break;

            case NOT_CONTAINED:
                needsSetLowerEntity = true;
                break;
        }

        if (needsSetEntity) {
            field.mapPut(entity, key, value);
        }
        if (needsSetLowerEntity) {
            field.mapPut(getEntityFromDescendantNode(), key, value);
        }
    }

    @Override
    public <K, EF extends Enum<? extends EntityField<V>> & EntityField<V>> Object mapGet(EF field, K key) {
        Objects.requireNonNull(key, "Key must not be null");
        switch (node.isCacheFor(field, key).max(() -> node.isPrimarySourceFor(field, key))) {
            case FULLY:
                return field.mapGet(entity, key);
            case NOT_CONTAINED:
                return field.mapGet(getEntityFromDescendantNode(), key);
        }
        throw new IllegalStateException("Field is not determined: " + field);
    }

    @Override
    public <T, EF extends Enum<? extends EntityField<V>> & EntityField<V>> Object collectionRemove(EF field, T value) {
        boolean needsSetEntity = false;
        boolean needsSetLowerEntity = false;

        switch (node.isCacheFor(field, value)) {
            case FULLY:
                needsSetEntity = true;
                break;

            case NOT_CONTAINED:
                needsSetLowerEntity = true;
                break;
        }

        switch (node.isPrimarySourceFor(field, value)) {
            case FULLY:
                needsSetEntity = true;
                needsSetLowerEntity = false;
                break;

            case NOT_CONTAINED:
                needsSetLowerEntity = true;
                break;
        }

        Object res = null;
        if (needsSetEntity) {
            res = field.collectionRemove(entity, value);
        }
        if (needsSetLowerEntity) {
            res = field.collectionRemove(getEntityFromDescendantNode(), value);
        }

        return res;
    }

    @Override
    public <T, EF extends Enum<? extends EntityField<V>> & EntityField<V>> void collectionAdd(EF field, T value) {
        boolean needsSetEntity = false;
        boolean needsSetLowerEntity = false;

        switch (node.isCacheFor(field, null)) {
            case FULLY:
                needsSetEntity = true;
                break;

            case NOT_CONTAINED:
                needsSetLowerEntity = true;
                break;
        }

        switch (node.isPrimarySourceFor(field, null)) {
            case FULLY:
                needsSetEntity = true;
                needsSetLowerEntity = false;
                break;

            case NOT_CONTAINED:
                needsSetLowerEntity = true;
                break;
        }

        if (needsSetEntity) {
            field.collectionAdd(entity, value);
        }
        if (needsSetLowerEntity) {
            field.collectionAdd(getEntityFromDescendantNode(), value);
        }
    }

    private final Collector<Map.Entry, ?, Map<Object, Object>> ENTRY_TO_HASH_MAP_OVERRIDING_KEYS_COLLECTOR = Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (Object a, Object b) -> b, HashMap::new);

    @Override
    @SuppressWarnings("unchecked")
    public <EF extends Enum<? extends EntityField<V>> & EntityField<V>> Object get(EF field) {
        switch (node.isCacheFor(field, null).max(() -> node.isPrimarySourceFor(field, null))) {
            case FULLY:
                return field.get(entity);
            case NOT_CONTAINED:
                return field.get(getEntityFromDescendantNode());
        }

        // It has to be partial field. The only supported partial field is a Map
        if (field.getMapKeyClass() == Void.class) {
            throw new IllegalStateException("Field " + field + " expected to be a map but is " + field.getFieldClass());
        }

        Map<Object, Object> m1 = (Map<Object, Object>) field.get(entity);
        Map m2 = (Map) field.get(getEntityFromDescendantNode());
        if (m1 == null) {
            return m2 == null ? null : new HashMap<>(m2);
        }
        Predicate<Map.Entry<Object, Object>> isInNode = me -> node.isCacheFor(field, me.getKey())
                          .max(() -> node.isPrimarySourceFor(field, me.getKey())) == FieldContainedStatus.FULLY;
        Stream<Map.Entry<Object, Object>> s = m1.entrySet().stream()
          .filter(isInNode);
        if (m2 == null) {
            return s.collect(ENTRY_TO_HASH_MAP_OVERRIDING_KEYS_COLLECTOR);
        }

        return Stream.concat(s, m2.entrySet().stream().filter(isInNode.negate()))
          .collect(ENTRY_TO_HASH_MAP_OVERRIDING_KEYS_COLLECTOR);
    }

    @Override
    public <T, EF extends Enum<? extends EntityField<V>> & EntityField<V>> void set(EF field, T value) {
        boolean needsSetEntity = false;
        boolean needsSetLowerEntity = false;

        switch (node.isCacheFor(field, null)) {
            case FULLY:
                needsSetEntity = true;
                break;

            case PARTIALLY:
                needsSetEntity = true;
                needsSetLowerEntity = true;
                break;
        }

        switch (node.isPrimarySourceFor(field, null)) {
            case FULLY:
                needsSetEntity = true;
                needsSetLowerEntity = false;
                break;

            case PARTIALLY:
                needsSetEntity = true;
                needsSetLowerEntity = true;
                break;

            case NOT_CONTAINED:
                needsSetLowerEntity = true;
                break;
        }

        if (needsSetEntity) {
            field.set(entity, value);
        }
        if (needsSetLowerEntity) {
            field.set(getEntityFromDescendantNode(), value);
        }
    }

    @Override
    public boolean isUpdated() {
        return entity instanceof UpdatableEntity ? ((UpdatableEntity) entity).isUpdated() : false;
    }

}

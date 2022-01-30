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

package org.keycloak.models.map.storage.hotRod.common;

import org.keycloak.models.map.common.AbstractEntity;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

public class HotRodTypesUtils {

    public static <MapKey, MapValue, SetValue> Set<SetValue> migrateMapToSet(Map<MapKey, MapValue> map, Function<Map.Entry<MapKey, MapValue>, SetValue> creator) {
        return map == null ? null : map.entrySet()
                .stream()
                .map(creator)
                .collect(Collectors.toSet());
    }

    public static <MapKey, MapValue, SetValue> Map<MapKey, MapValue> migrateSetToMap(Set<SetValue> set, Function<SetValue, MapKey> keyProducer, Function<SetValue, MapValue> valueProducer) {
        return set == null ? null : set.stream().collect(Collectors.toMap(keyProducer, valueProducer));
    }

    public static <T, V> HotRodPair<T, V> createHotRodPairFromMapEntry(Map.Entry<T, V> entry) {
        return new HotRodPair<>(entry.getKey(), entry.getValue());
    }

    public static HotRodAttributeEntity createHotRodAttributeEntityFromMapEntry(Map.Entry<String, List<String>> entry) {
        return new HotRodAttributeEntity(entry.getKey(), entry.getValue());
    }

    public static HotRodAttributeEntityNonIndexed createHotRodAttributeEntityNonIndexedFromMapEntry(Map.Entry<String, List<String>> entry) {
        return new HotRodAttributeEntityNonIndexed(entry.getKey(), entry.getValue());
    }

    public static <SetType, KeyType> boolean removeFromSetByMapKey(Set<SetType> set, KeyType key, Function<SetType, KeyType> keyGetter) {
        if (set == null || set.isEmpty()) { return false; }
        return set.stream()
                .filter(entry -> Objects.equals(keyGetter.apply(entry), key))
                .findFirst()
                .map(set::remove)
                .orElse(false);
    }

    public static <SetType, MapKey, MapValue> MapValue getMapValueFromSet(Set<SetType> set, MapKey key, Function<SetType, MapKey> keyGetter, Function<SetType, MapValue> valueGetter) {
        return set == null ? null : set.stream().filter(entry -> Objects.equals(keyGetter.apply(entry), key)).findFirst().map(valueGetter).orElse(null);
    }

    public static <K, V> K getKey(HotRodPair<K, V> hotRodPair) {
        return hotRodPair.getKey();
    }

    public static <K, V> V getValue(HotRodPair<K, V> hotRodPair) {
        return hotRodPair.getValue();
    }

    public static String getKey(HotRodAttributeEntity attributeEntity) {
        return attributeEntity.name;
    }

    public static String getKey(HotRodAttributeEntityNonIndexed attributeEntity) {
        return attributeEntity.name;
    }

    public static List<String> getValue(HotRodAttributeEntity attributeEntity) {
        return attributeEntity.values;
    }

    public static List<String> getValue(HotRodAttributeEntityNonIndexed attributeEntity) {
        return attributeEntity.values;
    }

    public static String getKey(AbstractEntity entity) {
        return entity.getId();
    }
}

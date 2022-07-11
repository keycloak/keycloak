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
import org.keycloak.models.map.common.DeepCloner;
import org.keycloak.models.map.storage.hotRod.authSession.HotRodAuthenticationSessionEntity;
import org.keycloak.models.map.storage.hotRod.realm.entity.HotRodLocalizationTexts;
import org.keycloak.models.map.storage.hotRod.user.HotRodUserConsentEntity;
import org.keycloak.models.map.storage.hotRod.user.HotRodUserFederatedIdentityEntity;
import org.keycloak.models.map.storage.hotRod.userSession.AuthenticatedClientSessionReferenceOnlyFieldDelegate;
import org.keycloak.models.map.storage.hotRod.userSession.HotRodAuthenticatedClientSessionEntityReference;
import org.keycloak.models.map.userSession.MapAuthenticatedClientSessionEntity;

import java.util.HashMap;
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
        return set == null ? null : set.stream().collect(HashMap::new, (m, v) -> m.put(keyProducer.apply(v), valueProducer.apply(v)), HashMap::putAll);
    }

    public static <T, V> HotRodPair<T, V> createHotRodPairFromMapEntry(Map.Entry<T, V> entry) {
        return new HotRodPair<>(entry.getKey(), entry.getValue());
    }

    public static HotRodStringPair createHotRodStringPairFromMapEntry(Map.Entry<String, String> entry) {
        return new HotRodStringPair(entry.getKey(), entry.getValue());
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

    public static String getKey(HotRodStringPair hotRodPair) {
        return hotRodPair.getKey();
    }

    public static String getValue(HotRodStringPair hotRodPair) {
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

    public static String getKey(HotRodUserFederatedIdentityEntity hotRodUserFederatedIdentityEntity) {
        return hotRodUserFederatedIdentityEntity.identityProvider;
    }

    public static String getKey(HotRodUserConsentEntity hotRodUserConsentEntity) {
        return hotRodUserConsentEntity.clientId;
    }

    public static <T, V> List<V> migrateList(List<T> p0, Function<T, V> migrator) {
        return p0 == null ? null : p0.stream().map(migrator).collect(Collectors.toList());
    }

    public static <T, V> Set<V> migrateSet(Set<T> p0, Function<T, V> migrator) {
        return p0 == null ? null : p0.stream().map(migrator).collect(Collectors.toSet());
    }

    public static String getKey(HotRodAuthenticationSessionEntity hotRodAuthenticationSessionEntity) {
        return hotRodAuthenticationSessionEntity.tabId;
    }

    public static String getKey(HotRodLocalizationTexts hotRodLocalizationTexts) {
        return hotRodLocalizationTexts.getLocale();
    }

    public static Map<String, String> getValue(HotRodLocalizationTexts hotRodLocalizationTexts) {
        Set<HotRodPair<String, String>> values = hotRodLocalizationTexts.getValues();
        return values == null ? null : values.stream().collect(Collectors.toMap(HotRodPair::getKey, HotRodPair::getValue));
    }

    public static HotRodLocalizationTexts migrateStringMapToHotRodLocalizationTexts(String p0, Map<String, String> p1) {
        HotRodLocalizationTexts hotRodLocalizationTexts = new HotRodLocalizationTexts();
        hotRodLocalizationTexts.setLocale(p0);
        hotRodLocalizationTexts.setValues(migrateMapToSet(p1, HotRodTypesUtils::createHotRodPairFromMapEntry));

        return hotRodLocalizationTexts;
    }

    public static HotRodAuthenticatedClientSessionEntityReference migrateMapAuthenticatedClientSessionEntityToHotRodAuthenticatedClientSessionEntityReference(MapAuthenticatedClientSessionEntity p0) {
        return new HotRodAuthenticatedClientSessionEntityReference(p0.getClientId(), p0.getId());
    }

    public static MapAuthenticatedClientSessionEntity migrateHotRodAuthenticatedClientSessionEntityReferenceToMapAuthenticatedClientSessionEntity(HotRodAuthenticatedClientSessionEntityReference collectionItem) {
        return DeepCloner.DUMB_CLONER.entityFieldDelegate(MapAuthenticatedClientSessionEntity.class, new AuthenticatedClientSessionReferenceOnlyFieldDelegate(collectionItem));
    }
}
